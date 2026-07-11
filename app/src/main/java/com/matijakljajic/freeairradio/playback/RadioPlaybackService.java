package com.matijakljajic.freeairradio.playback;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.net.Uri;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.ServiceCompat;
import androidx.core.os.BundleCompat;
import androidx.media3.common.ForwardingPlayer;
import androidx.media3.common.HeartRating;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.Player;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Rating;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.common.MediaItem;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import androidx.media3.session.CommandButton;
import androidx.media3.session.MediaSession;
import androidx.media3.session.MediaStyleNotificationHelper;
import androidx.media3.session.SessionCommand;
import androidx.media3.session.SessionCommands;
import androidx.media3.session.MediaSessionService;
import androidx.media3.session.SessionResult;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.matijakljajic.freeairradio.R;
import com.matijakljajic.freeairradio.data.model.Station;
import com.matijakljajic.freeairradio.playback.resolution.ResolutionResult;
import com.matijakljajic.freeairradio.playback.resolution.ResolvedStreamCandidate;
import com.matijakljajic.freeairradio.ui.MainActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

@UnstableApi
public class RadioPlaybackService extends MediaSessionService {

    private static final String PLAYBACK_NOTIFICATION_CHANNEL_ID = "radio_playback";
    private static final int PLAYBACK_NOTIFICATION_ID = 1001;
    @NonNull
    private static final SessionCommand TOGGLE_FAVORITE_COMMAND =
            new SessionCommand(PlaybackSessionContract.COMMAND_TOGGLE_FAVORITE, Bundle.EMPTY);
    @NonNull
    private static final SessionCommands ACTIVE_SESSION_COMMANDS = new SessionCommands.Builder()
            .add(SessionCommand.COMMAND_CODE_SESSION_SET_RATING)
            .add(TOGGLE_FAVORITE_COMMAND)
            .build();

    @Nullable
    private ExoPlayer player;

    @Nullable
    private MediaSession mediaSession;
    @Nullable
    private Station currentStation;
    @Nullable
    private ResolutionResult currentResolutionResult;
    private int currentCandidateIndex;
    private long currentPlaybackGeneration;
    @NonNull
    private final ExecutorService playbackResolverExecutor = Executors.newSingleThreadExecutor();
    @NonNull
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    @NonNull
    private final StreamResolutionEngine streamResolver = new StreamResolutionEngine();
    @NonNull
    private final CurrentPlaybackState currentPlaybackState = CurrentPlaybackState.getInstance();
    @NonNull
    private final NowPlayingObserver nowPlayingObserver = new NowPlayingObserver(currentPlaybackState);
    // TODO: Replace this temporary in-memory favorite set with LibraryRepository once Room favorites exist.
    @NonNull
    private final Set<String> favoriteStationIds = new HashSet<>();
    @NonNull
    private final MediaSession.Callback mediaSessionCallback = new MediaSession.Callback() {
        @NonNull
        @Override
        public MediaSession.ConnectionResult onConnect(@NonNull MediaSession session,
                                                       @NonNull MediaSession.ControllerInfo controller) {
            return new MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                    .setAvailableSessionCommands(buildControllerSessionCommands(currentPlaybackState.getCurrentStation()))
                    .setAvailablePlayerCommands(buildControllerPlayerCommands(session, currentPlaybackState.getCurrentStation()))
                    .setMediaButtonPreferences(buildControllerMediaButtons(currentPlaybackState.getCurrentStation(), currentPlaybackState.getPlaybackStatus()))
                    .build();
        }

        @SuppressWarnings("deprecation")
        @Override
        public int onPlayerCommandRequest(@NonNull MediaSession session,
                                          @NonNull MediaSession.ControllerInfo controller,
                                          int playerCommand) {
            if (playerCommand == Player.COMMAND_STOP) {
                stop();
                return SessionResult.RESULT_INFO_SKIPPED;
            }

            if (playerCommand == Player.COMMAND_PLAY_PAUSE && shouldTreatPlayPauseAsStop()) {
                stop();
                return SessionResult.RESULT_INFO_SKIPPED;
            }

            return SessionResult.RESULT_SUCCESS;
        }

        @NonNull
        @Override
        public ListenableFuture<SessionResult> onCustomCommand(
                @NonNull MediaSession session,
                @NonNull MediaSession.ControllerInfo controller,
                @NonNull SessionCommand customCommand,
                @NonNull Bundle args) {
            if (PlaybackSessionContract.COMMAND_TOGGLE_FAVORITE.equals(customCommand.customAction)) {
                return Futures.immediateFuture(handleFavoriteToggle());
            }
            return MediaSession.Callback.super.onCustomCommand(session, controller, customCommand, args);
        }

        @NonNull
        @Override
        public ListenableFuture<SessionResult> onSetRating(
                @NonNull MediaSession session,
                @NonNull MediaSession.ControllerInfo controller,
                @NonNull Rating rating) {
            if (rating instanceof HeartRating) {
                return Futures.immediateFuture(applyFavoriteRating((HeartRating) rating));
            }
            return MediaSession.Callback.super.onSetRating(session, controller, rating);
        }
    };
    @NonNull
    private final CurrentPlaybackState.Listener servicePlaybackStateListener = (station, nowPlaying, playbackStatus) -> {
        syncSessionMetadata(station, nowPlaying);
        syncControllerTransportControls(station, playbackStatus);
    };
    @NonNull
    private final AtomicInteger playSequence = new AtomicInteger();
    @NonNull
    private final Player.Listener playerListener = new Player.Listener() {
        @Override
        public void onPlayerError(@NonNull PlaybackException error) {
            if (!attemptNextCandidate()) {
                markCurrentStationStatus(CurrentPlaybackState.PlaybackStatus.ERROR);
            }
        }

        @Override
        public void onPlaybackStateChanged(int playbackState) {
            if (currentStation == null) {
                return;
            }

            if (playbackState == Player.STATE_BUFFERING) {
                markCurrentStationStatus(CurrentPlaybackState.PlaybackStatus.CONNECTING);
            } else if (playbackState == Player.STATE_ENDED) {
                markCurrentStationStatus(CurrentPlaybackState.PlaybackStatus.ERROR);
            }
        }

        @Override
        public void onIsPlayingChanged(boolean isPlaying) {
            if (currentStation == null) {
                return;
            }

            if (isPlaying) {
                markCurrentStationStatus(CurrentPlaybackState.PlaybackStatus.PLAYING);
            } else if (player != null && player.getPlaybackState() == Player.STATE_BUFFERING) {
                markCurrentStationStatus(CurrentPlaybackState.PlaybackStatus.CONNECTING);
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        DefaultHttpDataSource.Factory dataSourceFactory = new DefaultHttpDataSource.Factory()
                .setUserAgent("FreeAirRadio/" + com.matijakljajic.freeairradio.BuildConfig.VERSION_NAME)
                .setDefaultRequestProperties(Collections.singletonMap("Icy-MetaData", "1"))
                .setAllowCrossProtocolRedirects(true);
        player = new ExoPlayer.Builder(this)
                .setMediaSourceFactory(new DefaultMediaSourceFactory(dataSourceFactory))
                .build();
        player.addListener(playerListener);
        mediaSession = new MediaSession.Builder(this, new SessionPlayer(player))
                .setCallback(mediaSessionCallback)
                .setSessionActivity(buildContentIntent())
                .build();
        addSession(mediaSession);
        setMediaNotificationProvider(new RadioPlaybackNotificationProvider(
                this,
                PLAYBACK_NOTIFICATION_ID,
                PLAYBACK_NOTIFICATION_CHANNEL_ID,
                R.string.playback_notification_channel_name
        ));
        createPlaybackNotificationChannel();
        currentPlaybackState.addListener(servicePlaybackStateListener);
    }

    @Nullable
    @Override
    public MediaSession onGetSession(@NonNull MediaSession.ControllerInfo controllerInfo) {
        return mediaSession;
    }

    @Override
    public int onStartCommand(@Nullable Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        if (intent != null) {
            String action = intent.getAction();
            if (PlaybackSessionContract.ACTION_PLAY_STATION.equals(action)) {
                Station station = readStation(intent);
                if (station != null) {
                    play(station);
                }
            } else if (PlaybackSessionContract.ACTION_STOP_PLAYBACK.equals(action)) {
                stop();
            }
        }
        return START_STICKY;
    }

    public void play(@NonNull Station station) {
        if (player == null) {
            return;
        }

        int sequence = playSequence.incrementAndGet();
        long previousGeneration = currentPlaybackGeneration;
        currentPlaybackGeneration = 0L;
        nowPlayingObserver.clearNowPlaying(previousGeneration);
        nowPlayingObserver.stopObserving();
        player.stop();
        player.clearMediaItems();

        currentStation = station;
        currentResolutionResult = null;
        currentCandidateIndex = 0;
        currentPlaybackState.setCurrentStation(station);
        promoteToForeground(buildConnectingNotification(station));
        playbackResolverExecutor.execute(() -> resolveAndPlayStation(station, sequence));
    }

    public void stop() {
        if (player == null) {
            return;
        }

        long generation = currentPlaybackGeneration;
        playSequence.incrementAndGet();
        nowPlayingObserver.clearNowPlaying(generation);
        nowPlayingObserver.stopObserving();
        player.stop();
        player.clearMediaItems();
        currentStation = null;
        currentResolutionResult = null;
        currentCandidateIndex = 0;
        currentPlaybackGeneration = 0L;
        stopForeground(STOP_FOREGROUND_REMOVE);
        currentPlaybackState.clear();
        stopSelf();
    }

    @Override
    public void onDestroy() {
        currentPlaybackState.removeListener(servicePlaybackStateListener);
        if (player != null) {
            player.removeListener(playerListener);
            if (currentPlaybackGeneration != 0L) {
                nowPlayingObserver.clearNowPlaying(currentPlaybackGeneration);
            }
            nowPlayingObserver.stopObserving();
        }
        currentPlaybackState.clear();
        if (mediaSession != null) {
            mediaSession.release();
            mediaSession = null;
        }
        if (player != null) {
            player.release();
            player = null;
        }
        playbackResolverExecutor.shutdownNow();
        super.onDestroy();
    }

    @Nullable
    private Station readStation(@NonNull Intent intent) {
        Bundle extras = intent.getExtras();
        if (extras == null) {
            return null;
        }
        return BundleCompat.getSerializable(extras, PlaybackSessionContract.EXTRA_STATION, Station.class);
    }

    private void resolveAndPlayStation(@NonNull Station station, int sequence) {
        ResolutionResult resolutionResult = streamResolver.resolveUrls(station);
        mainHandler.post(() -> {
            if (player == null || sequence != playSequence.get()) {
                return;
            }
            currentPlaybackGeneration = sequence;
            currentResolutionResult = resolutionResult;
            currentCandidateIndex = 0;
            nowPlayingObserver.startObserving(player, station, sequence);
            playCandidateAtIndex(station, resolutionResult, 0, sequence);
        });
    }

    private void playCandidateAtIndex(@NonNull Station station,
                                      @NonNull ResolutionResult resolutionResult,
                                      int candidateIndex,
                                      long sequence) {
        if (player == null || sequence != playSequence.get()) {
            return;
        }

        ResolvedStreamCandidate candidate = resolutionResult.getCandidates().isEmpty()
                ? null
                : resolutionResult.getCandidates().get(Math.min(candidateIndex, resolutionResult.getCandidates().size() - 1));
        String playableStreamUrl = candidate != null ? candidate.getUrl() : station.getPlayableStreamUrl();
        MediaItem mediaItem = new MediaItem.Builder()
                .setUri(Uri.parse(playableStreamUrl))
                .setMediaMetadata(buildSessionMediaMetadata(station, null))
                .build();

        player.stop();
        player.clearMediaItems();
        player.setMediaItem(mediaItem);
        currentPlaybackState.setPlaybackStatus(station, CurrentPlaybackState.PlaybackStatus.CONNECTING);
        player.prepare();
        player.play();
    }

    private boolean attemptNextCandidate() {
        if (player == null || currentStation == null || currentResolutionResult == null) {
            return false;
        }

        int nextIndex = currentCandidateIndex + 1;
        if (nextIndex >= currentResolutionResult.getCandidates().size()) {
            return false;
        }

        currentCandidateIndex = nextIndex;
        nowPlayingObserver.startObserving(player, currentStation, currentPlaybackGeneration);
        playCandidateAtIndex(currentStation, currentResolutionResult, currentCandidateIndex, currentPlaybackGeneration);
        return true;
    }

    private void createPlaybackNotificationChannel() {
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        if (notificationManager == null) {
            return;
        }

        NotificationChannel channel = new NotificationChannel(
                PLAYBACK_NOTIFICATION_CHANNEL_ID,
                getString(R.string.playback_notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
        );
        notificationManager.createNotificationChannel(channel);
    }

    private void promoteToForeground(@NonNull Notification notification) {
        int foregroundServiceType = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                ? ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                : 0;
        ServiceCompat.startForeground(
                this,
                PLAYBACK_NOTIFICATION_ID,
                notification,
                foregroundServiceType
        );
    }

    @NonNull
    private Notification buildConnectingNotification(@NonNull Station station) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, PLAYBACK_NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_media_play)
                .setContentTitle(station.getName())
                .setContentText(getString(R.string.playback_notification_connecting))
                .setContentIntent(buildContentIntent())
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setSilent(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
                .addAction(buildStopAction())
                .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE);
        if (mediaSession != null) {
            builder.setStyle(new MediaStyleNotificationHelper.MediaStyle(mediaSession)
                    .setShowActionsInCompactView(0));
        }
        return builder.build();
    }

    @NonNull
    private PendingIntent buildContentIntent() {
        Intent activityIntent = new Intent(this, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return PendingIntent.getActivity(
                this,
                0,
                activityIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }

    @NonNull
    private NotificationCompat.Action buildStopAction() {
        PendingIntent stopIntent = PendingIntent.getService(
                this,
                1,
                PlaybackSessionContract.createStopIntent(this),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        return new NotificationCompat.Action(
                R.drawable.ic_stop,
                getString(R.string.player_stop_button),
                stopIntent
        );
    }

    private void syncControllerTransportControls(@Nullable Station station,
                                                 @NonNull CurrentPlaybackState.PlaybackStatus playbackStatus) {
        if (mediaSession == null) {
            return;
        }

        SessionCommands sessionCommands = buildControllerSessionCommands(station);
        Player.Commands playerCommands = buildControllerPlayerCommands(mediaSession, station);
        List<CommandButton> mediaButtons = buildControllerMediaButtons(station, playbackStatus);
        for (MediaSession.ControllerInfo controller : getSessionControllers()) {
            mediaSession.setAvailableCommands(
                    controller,
                    sessionCommands,
                    playerCommands
            );
            mediaSession.setMediaButtonPreferences(controller, mediaButtons);
        }
        mediaSession.setMediaButtonPreferences(mediaButtons);
    }

    private void syncSessionMetadata(@Nullable Station station, @Nullable NowPlaying nowPlaying) {
        if (player == null || station == null || player.getMediaItemCount() == 0) {
            return;
        }

        MediaItem currentMediaItem = player.getCurrentMediaItem();
        if (currentMediaItem == null) {
            return;
        }

        MediaMetadata desiredMetadata = buildSessionMediaMetadata(station, nowPlaying);
        if (Objects.equals(currentMediaItem.mediaMetadata, desiredMetadata)) {
            return;
        }

        int currentMediaItemIndex = player.getCurrentMediaItemIndex();
        if (currentMediaItemIndex < 0) {
            return;
        }

        player.replaceMediaItem(
                currentMediaItemIndex,
                currentMediaItem.buildUpon()
                        .setMediaMetadata(desiredMetadata)
                        .build()
        );
    }

    @NonNull
    private MediaMetadata buildSessionMediaMetadata(@NonNull Station station,
                                                    @Nullable NowPlaying nowPlaying) {
        MediaMetadata.Builder builder = new MediaMetadata.Builder()
                .setStation(station.getName())
                .setUserRating(new HeartRating(isFavorite(station)));

        if (nowPlaying != null) {
            if (nowPlaying.getTitle() != null) {
                builder.setTitle(nowPlaying.getTitle());
            }
            if (nowPlaying.getArtist() != null) {
                builder.setArtist(nowPlaying.getArtist());
            }
            if (nowPlaying.buildDisplayText() == null) {
                builder.setTitle(station.getName());
            }
        } else {
            builder.setTitle(station.getName());
        }
        return builder.build();
    }

    @NonNull
    private SessionCommands buildControllerSessionCommands(@Nullable Station station) {
        return station == null ? SessionCommands.EMPTY : ACTIVE_SESSION_COMMANDS;
    }

    @NonNull
    private Player.Commands buildControllerPlayerCommands(@NonNull MediaSession session,
                                                          @Nullable Station station) {
        Player.Commands.Builder commandsBuilder = session.getPlayer()
                .getAvailableCommands()
                .buildUpon()
                .removeAll(
                        Player.COMMAND_SEEK_TO_PREVIOUS,
                        Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM,
                        Player.COMMAND_SEEK_TO_NEXT,
                        Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM
                );
        if (station == null) {
            commandsBuilder.remove(Player.COMMAND_STOP);
        }
        return commandsBuilder.build();
    }

    @NonNull
    private List<CommandButton> buildControllerMediaButtons(@Nullable Station station,
                                                            @NonNull CurrentPlaybackState.PlaybackStatus playbackStatus) {
        if (station == null || playbackStatus == CurrentPlaybackState.PlaybackStatus.IDLE) {
            return Collections.emptyList();
        }

        boolean favorite = isFavorite(station);
        List<CommandButton> buttons = new ArrayList<>(2);
        buttons.add(
                new CommandButton.Builder(CommandButton.ICON_STOP)
                        .setPlayerCommand(Player.COMMAND_STOP)
                        .setDisplayName(getString(R.string.player_stop_button))
                        .setSlots(CommandButton.SLOT_CENTRAL)
                        .build()
        );
        buttons.add(
                new CommandButton.Builder(favorite
                        ? CommandButton.ICON_HEART_FILLED
                        : CommandButton.ICON_HEART_UNFILLED)
                        .setSessionCommand(TOGGLE_FAVORITE_COMMAND)
                        .setDisplayName(getString(favorite
                                ? R.string.player_unfavorite_button
                                : R.string.player_favorite_button))
                        .setSlots(CommandButton.SLOT_FORWARD)
                        .build()
        );
        return buttons;
    }

    @NonNull
    private List<MediaSession.ControllerInfo> getSessionControllers() {
        List<MediaSession.ControllerInfo> controllers = new ArrayList<>(mediaSession != null
                ? mediaSession.getConnectedControllers()
                : Collections.emptyList());
        if (mediaSession == null) {
            return controllers;
        }

        MediaSession.ControllerInfo notificationController = mediaSession.getMediaNotificationControllerInfo();
        if (notificationController != null && !controllers.contains(notificationController)) {
            controllers.add(notificationController);
        }
        return controllers;
    }

    private boolean shouldTreatPlayPauseAsStop() {
        CurrentPlaybackState.PlaybackStatus playbackStatus = currentPlaybackState.getPlaybackStatus();
        return currentStation != null
                && (playbackStatus == CurrentPlaybackState.PlaybackStatus.CONNECTING
                || playbackStatus == CurrentPlaybackState.PlaybackStatus.PLAYING);
    }

    private void markCurrentStationStatus(@NonNull CurrentPlaybackState.PlaybackStatus playbackStatus) {
        if (currentStation == null) {
            return;
        }
        currentPlaybackState.setPlaybackStatus(currentStation, playbackStatus);
    }

    @NonNull
    private SessionResult handleFavoriteToggle() {
        if (currentStation == null) {
            return new SessionResult(SessionResult.RESULT_SUCCESS);
        }

        return updateFavoriteState(!isFavorite(currentStation));
    }

    @NonNull
    private SessionResult applyFavoriteRating(@NonNull HeartRating heartRating) {
        if (currentStation == null) {
            return new SessionResult(SessionResult.RESULT_SUCCESS);
        }

        return updateFavoriteState(heartRating.isRated() && heartRating.isHeart());
    }

    @NonNull
    private SessionResult updateFavoriteState(boolean favorite) {
        if (currentStation == null) {
            return new SessionResult(SessionResult.RESULT_SUCCESS);
        }

        setFavorite(currentStation, favorite);
        refreshFavoritePresentation();
        return new SessionResult(SessionResult.RESULT_SUCCESS);
    }

    private void refreshFavoritePresentation() {
        if (currentStation == null) {
            return;
        }

        syncSessionMetadata(currentStation, currentPlaybackState.getCurrentNowPlaying());
        syncControllerTransportControls(currentStation, currentPlaybackState.getPlaybackStatus());
    }

    private void setFavorite(@NonNull Station station, boolean favorite) {
        //TODO: wire to the future Room or smth for the FavoriteStationList
        if (favorite) {
            favoriteStationIds.add(station.getId());
        } else {
            favoriteStationIds.remove(station.getId());
        }
    }

    private boolean isFavorite(@NonNull Station station) {
        return favoriteStationIds.contains(station.getId());
    }

    private final class SessionPlayer extends ForwardingPlayer {

        private SessionPlayer(@NonNull Player player) {
            super(player);
        }

        @NonNull
        @Override
        public Commands getAvailableCommands() {
            Commands.Builder commandsBuilder = super.getAvailableCommands()
                    .buildUpon()
                    .removeAll(
                            Player.COMMAND_SEEK_TO_PREVIOUS,
                            Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM,
                            Player.COMMAND_SEEK_TO_NEXT,
                            Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM
                    );
            if (currentStation != null
                    && currentPlaybackState.getPlaybackStatus() != CurrentPlaybackState.PlaybackStatus.IDLE) {
                commandsBuilder.remove(Player.COMMAND_PLAY_PAUSE);
                commandsBuilder.add(Player.COMMAND_STOP);
            } else {
                commandsBuilder.remove(Player.COMMAND_STOP);
            }
            return commandsBuilder.build();
        }
    }
}
