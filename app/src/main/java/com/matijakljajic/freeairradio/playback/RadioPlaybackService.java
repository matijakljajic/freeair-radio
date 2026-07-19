package com.matijakljajic.freeairradio.playback;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.ServiceCompat;
import androidx.core.os.BundleCompat;
import androidx.media3.common.AudioAttributes;
import androidx.media3.common.C;
import androidx.media3.common.ForwardingPlayer;
import androidx.media3.common.HeartRating;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.Player;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Rating;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.datasource.DefaultHttpDataSource;
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
import com.matijakljajic.freeairradio.artwork.StationArtworkBitmapLoader;
import com.matijakljajic.freeairradio.artwork.StationArtworkResolver;
import com.matijakljajic.freeairradio.R;
import com.matijakljajic.freeairradio.data.model.RecentlyListenedSong;
import com.matijakljajic.freeairradio.data.model.Station;
import com.matijakljajic.freeairradio.data.model.StationOrigin;
import com.matijakljajic.freeairradio.data.remote.radiobrowser.RadioBrowserRepository;
import com.matijakljajic.freeairradio.data.repository.LibraryRepository;
import com.matijakljajic.freeairradio.data.repository.StationRepository;
import com.matijakljajic.freeairradio.playback.metadata.CurrentPlaybackState;
import com.matijakljajic.freeairradio.playback.metadata.NowPlaying;
import com.matijakljajic.freeairradio.playback.metadata.NowPlayingObserver;
import com.matijakljajic.freeairradio.playback.metadata.PlaybackMetadataMapper;
import com.matijakljajic.freeairradio.playback.resolution.ResolutionResult;
import com.matijakljajic.freeairradio.playback.resolution.ResolvedStreamCandidate;
import com.matijakljajic.freeairradio.playback.resolution.StreamResolutionEngine;
import com.matijakljajic.freeairradio.ui.MainActivity;
import com.matijakljajic.freeairradio.util.AppLog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

@SuppressWarnings("unused")
@UnstableApi
public class RadioPlaybackService extends MediaSessionService {

    private static final String TAG = "RadioPlaybackService";
    private static final String PLAYBACK_NOTIFICATION_CHANNEL_ID = "radio_playback";
    private static final int PLAYBACK_NOTIFICATION_ID = 1001;
    private static final long MIN_RECENTLY_LISTENED_TRACK_DURATION_MS = 20_000L;
    @NonNull
    private static final SessionCommand TOGGLE_FAVORITE_COMMAND =
            new SessionCommand(PlaybackSessionContract.COMMAND_TOGGLE_FAVORITE, Bundle.EMPTY);
    private static final int[] FAVORITE_BUTTON_SLOTS = {
            CommandButton.SLOT_OVERFLOW
    };
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
    private SessionPlayer sessionPlayer;
    @Nullable
    private Station currentStation;
    @Nullable
    private ResolutionResult currentResolutionResult;
    private int currentCandidateIndex;
    private long currentPlaybackGeneration;
    private long reportedUsageGeneration;
    private long currentNowPlayingStartedAtElapsedMs;
    @NonNull
    private final ExecutorService playbackResolverExecutor = Executors.newSingleThreadExecutor();
    @NonNull
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    @NonNull
    private final StreamResolutionEngine streamResolver = new StreamResolutionEngine();
    @NonNull
    private final FavoriteStationNavigator favoriteStationNavigator = new FavoriteStationNavigator();
    @NonNull
    private final CurrentPlaybackState currentPlaybackState = CurrentPlaybackState.getInstance();
    @Nullable
    private StationRepository stationRepository;
    @Nullable
    private LibraryRepository libraryRepository;
    @Nullable
    private AudioInterruptionSettings audioInterruptionSettings;
    @NonNull
    private final NowPlayingObserver nowPlayingObserver = new NowPlayingObserver(this::handleObservedNowPlayingChange);
    @NonNull
    private final LibraryRepository.FavoritesListener favoritesListener = this::refreshFavoritePresentation;
    @NonNull
    private final MediaSession.Callback mediaSessionCallback = new MediaSession.Callback() {
        @NonNull
        @Override
        public MediaSession.ConnectionResult onConnect(@NonNull MediaSession session,
                                                       @NonNull MediaSession.ControllerInfo controller) {
            return new MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                    .setAvailableSessionCommands(buildControllerSessionCommands(currentPlaybackState.getCurrentStation()))
                    .setAvailablePlayerCommands(buildControllerPlayerCommands(session))
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
    private final CurrentPlaybackState.Listener servicePlaybackStateListener =
            (station, nowPlaying, playbackStatus) -> {
                syncControllerTransportControls(station, playbackStatus);
                requestNotificationRefresh();
            };
    @NonNull
    private final AtomicInteger playSequence = new AtomicInteger();
    @Nullable
    private AudioFocusHandler audioFocusHandler;
    @NonNull
    private final HeadphoneUnplugReceiver headphoneUnplugReceiver =
            new HeadphoneUnplugReceiver(this::handleAudioOutputBecameNoisy);
    private boolean resumeAfterTemporaryFocusLoss;
    @NonNull
    private final Player.Listener playerListener = new Player.Listener() {
        @Override
        public void onPlayerError(@NonNull PlaybackException error) {
            Log.w(TAG, "Playback error for "
                    + getCurrentStationLogName()
                    + " candidate="
                    + getCurrentCandidateLogUrl()
                    + " code="
                    + error.errorCode,
                    error);
            if (!attemptNextCandidate()) {
                markCurrentStationStatus(CurrentPlaybackState.PlaybackStatus.ERROR);
            }
        }

        @Override
        public void onPlaybackStateChanged(int playbackState) {
            if (currentStation == null) {
                return;
            }

            AppLog.d(TAG, "Player state changed for "
                    + getCurrentStationLogName()
                    + " -> "
                    + playbackStateToString(playbackState)
                    + " candidate="
                    + getCurrentCandidateLogUrl());

            if (playbackState == Player.STATE_BUFFERING) {
                markCurrentStationStatus(CurrentPlaybackState.PlaybackStatus.CONNECTING);
            } else if (playbackState == Player.STATE_ENDED) {
                Log.w(TAG, "Playback ended for "
                        + getCurrentStationLogName()
                        + " candidate="
                        + getCurrentCandidateLogUrl());
                if (!attemptNextCandidate()) {
                    markCurrentStationStatus(CurrentPlaybackState.PlaybackStatus.ERROR);
                }
            }
        }

        @Override
        public void onIsPlayingChanged(boolean isPlaying) {
            if (currentStation == null) {
                return;
            }

            AppLog.d(TAG, "isPlaying changed for "
                    + getCurrentStationLogName()
                    + " -> "
                    + isPlaying
                    + " playerState="
                    + getPlayerStateLogValue()
                    + " appStatus="
                    + currentPlaybackState.getPlaybackStatus());

            if (isPlaying) {
                markCurrentStationStatus(CurrentPlaybackState.PlaybackStatus.PLAYING);
                reportCurrentStationUsageIfNeeded();
            } else if (player != null && player.getPlaybackState() == Player.STATE_BUFFERING) {
                markCurrentStationStatus(CurrentPlaybackState.PlaybackStatus.CONNECTING);
            } else if (player != null && player.getPlaybackState() == Player.STATE_READY) {
                markCurrentStationStatus(CurrentPlaybackState.PlaybackStatus.PAUSED);
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        audioFocusHandler = new AudioFocusHandler(this, new AudioFocusHandler.Listener() {
            @Override
            public void onAudioFocusGained() {
                handleAudioFocusGained();
            }

            @Override
            public void onAudioFocusLostTemporarily() {
                handleTemporaryAudioFocusLoss();
            }

            @Override
            public void onAudioFocusLostPermanently() {
                handlePermanentAudioFocusLoss();
            }
        });
        createPlayerAndSession();
        configureMediaNotification();
        bindRepositories();
        createPlaybackNotificationChannel();
        headphoneUnplugReceiver.register(this);
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
            handlePlaybackIntent(intent);
        }
        return START_STICKY;
    }

    public void play(@NonNull Station station) {
        if (player == null) {
            return;
        }
        AppLog.d(TAG, "Play requested for " + station.getName());
        if (audioFocusHandler == null || !audioFocusHandler.requestFocus()) {
            Log.w(TAG, "Audio focus denied for " + station.getName());
            handleDeniedPlaybackStart();
            return;
        }

        clearTemporaryFocusLossState();
        int sequence = beginPlaybackSequence();
        prepareStationPlayback(station);
        resolvePlaybackAsync(station, sequence);
    }

    public void stop() {
        if (player == null) {
            return;
        }

        AppLog.d(TAG, "Stopping playback for "
                + getCurrentStationLogName()
                + " playerState="
                + getPlayerStateLogValue()
                + " appStatus="
                + currentPlaybackState.getPlaybackStatus());

        clearTemporaryFocusLossState();
        cancelCurrentPlayback();
        currentStation = null;
        if (audioFocusHandler != null) {
            audioFocusHandler.abandonFocus();
        }
        stopForeground(STOP_FOREGROUND_REMOVE);
        currentPlaybackState.clear();
        stopSelf();
    }

    public void resume() {
        if (player == null || currentStation == null) {
            return;
        }
        AppLog.d(TAG, "Resume requested for " + currentStation.getName());
        if (audioFocusHandler == null || !audioFocusHandler.requestFocus()) {
            Log.w(TAG, "Audio focus denied while resuming " + currentStation.getName());
            return;
        }

        clearTemporaryFocusLossState();
        player.play();
    }

    @Override
    public void onDestroy() {
        currentPlaybackState.removeListener(servicePlaybackStateListener);
        headphoneUnplugReceiver.unregister(this);
        if (audioFocusHandler != null) {
            audioFocusHandler.abandonFocus();
            audioFocusHandler = null;
        }
        if (player != null) {
            player.removeListener(playerListener);
            clearNowPlayingObservation(currentPlaybackGeneration);
        }
        currentPlaybackState.clear();
        stationRepository = null;
        audioInterruptionSettings = null;
        if (libraryRepository != null) {
            libraryRepository.removeFavoritesListener(favoritesListener);
            libraryRepository = null;
        }
        releaseMediaSession();
        releasePlayer();
        playbackResolverExecutor.shutdownNow();
        super.onDestroy();
    }

    private void createPlayerAndSession() {
        player = new ExoPlayer.Builder(this)
                .setMediaSourceFactory(new DefaultMediaSourceFactory(createDataSourceFactory()))
                .build();
        player.setAudioAttributes(buildPlayerAudioAttributes(), false);
        player.addListener(playerListener);
        sessionPlayer = new SessionPlayer(player);
        mediaSession = new MediaSession.Builder(this, sessionPlayer)
                .setCallback(mediaSessionCallback)
                .setSessionActivity(buildContentIntent())
                .build();
        addSession(mediaSession);
    }

    @NonNull
    private AudioAttributes buildPlayerAudioAttributes() {
        return new AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .build();
    }

    @NonNull
    private DefaultHttpDataSource.Factory createDataSourceFactory() {
        return new DefaultHttpDataSource.Factory()
                .setUserAgent("FreeAirRadio/" + com.matijakljajic.freeairradio.BuildConfig.VERSION_NAME)
                .setDefaultRequestProperties(Collections.singletonMap("Icy-MetaData", "1"))
                .setAllowCrossProtocolRedirects(true);
    }

    private void configureMediaNotification() {
        setShowNotificationForIdlePlayer(SHOW_NOTIFICATION_FOR_IDLE_PLAYER_ALWAYS);
        setMediaNotificationProvider(new RadioPlaybackNotificationProvider(
                this,
                PLAYBACK_NOTIFICATION_ID,
                PLAYBACK_NOTIFICATION_CHANNEL_ID,
                R.string.playback_notification_channel_name
        ));
    }

    private void bindRepositories() {
        stationRepository = new RadioBrowserRepository(getApplicationContext());
        libraryRepository = LibraryRepository.getInstance(getApplicationContext());
        audioInterruptionSettings = new AudioInterruptionSettings(getApplicationContext());
        libraryRepository.addFavoritesListener(favoritesListener);
    }

    private void releaseMediaSession() {
        if (mediaSession != null) {
            mediaSession.release();
            mediaSession = null;
        }
        sessionPlayer = null;
    }

    private void releasePlayer() {
        if (player != null) {
            player.release();
            player = null;
        }
    }

    @Nullable
    private Station readStation(@NonNull Intent intent) {
        Bundle extras = intent.getExtras();
        if (extras == null) {
            return null;
        }
        return BundleCompat.getSerializable(extras, PlaybackSessionContract.EXTRA_STATION, Station.class);
    }

    private void handlePlaybackIntent(@NonNull Intent intent) {
        String action = intent.getAction();
        if (PlaybackSessionContract.ACTION_PLAY_STATION.equals(action)) {
            Station station = readStation(intent);
            if (station != null) {
                play(station);
            }
            return;
        }

        if (PlaybackSessionContract.ACTION_RESUME_PLAYBACK.equals(action)) {
            resume();
        } else if (PlaybackSessionContract.ACTION_STOP_PLAYBACK.equals(action)) {
            stop();
        }
    }

    private int beginPlaybackSequence() {
        int sequence = playSequence.incrementAndGet();
        resetActivePlayback();
        return sequence;
    }

    private void cancelCurrentPlayback() {
        playSequence.incrementAndGet();
        resetActivePlayback();
    }

    private void prepareStationPlayback(@NonNull Station station) {
        currentStation = station;
        setConnectingMediaPlaceholder(station);
        currentPlaybackState.setCurrentStation(station);
        resolveArtworkForStation(station);
        promoteToForeground(buildConnectingNotification(station));
    }

    private void resolvePlaybackAsync(@NonNull Station station, int sequence) {
        playbackResolverExecutor.execute(() -> resolveAndPlayStation(station, sequence));
    }

    private void resetActivePlayback() {
        clearNowPlayingObservation(currentPlaybackGeneration);
        currentPlaybackGeneration = 0L;
        clearResolvedPlaybackState();
        reportedUsageGeneration = 0L;
        clearCurrentNowPlayingTiming();
        clearTemporaryFocusLossState();
        stopAndClearPlayer();
    }

    private void clearNowPlayingObservation(long generation) {
        nowPlayingObserver.clearNowPlaying(generation);
        nowPlayingObserver.stopObserving();
    }

    private void clearResolvedPlaybackState() {
        currentResolutionResult = null;
        currentCandidateIndex = 0;
    }

    private void stopAndClearPlayer() {
        if (player == null) {
            return;
        }
        player.stop();
        player.clearMediaItems();
    }

    private void handleDeniedPlaybackStart() {
        if (currentStation == null
                && currentPlaybackState.getPlaybackStatus() == CurrentPlaybackState.PlaybackStatus.IDLE) {
            stopSelf();
        }
    }

    private void handleAudioFocusGained() {
        if (!resumeAfterTemporaryFocusLoss || player == null || currentStation == null) {
            AppLog.d(TAG, "Audio focus gained without pending auto-resume");
            resumeAfterTemporaryFocusLoss = false;
            return;
        }

        AppLog.d(TAG, "Resuming playback after temporary audio focus loss for "
                + currentStation.getName());
        resumeAfterTemporaryFocusLoss = false;
        player.play();
    }

    private void handleTemporaryAudioFocusLoss() {
        if (!shouldRespectAudioInterruptions()) {
            AppLog.d(TAG, "Ignoring temporary audio focus loss because interruptions are disabled");
            return;
        }
        if (player == null || currentStation == null) {
            AppLog.d(TAG, "Ignoring temporary audio focus loss because there is no active station");
            resumeAfterTemporaryFocusLoss = false;
            return;
        }

        resumeAfterTemporaryFocusLoss = shouldResumeAfterTemporaryFocusLoss();
        if (!resumeAfterTemporaryFocusLoss) {
            AppLog.d(TAG, "Ignoring temporary audio focus loss because playback is not active"
                    + " playerState="
                    + getPlayerStateLogValue()
                    + " appStatus="
                    + currentPlaybackState.getPlaybackStatus());
            return;
        }

        AppLog.d(TAG, "Pausing playback after temporary audio focus loss for "
                + currentStation.getName()
                + " playerState="
                + getPlayerStateLogValue()
                + " appStatus="
                + currentPlaybackState.getPlaybackStatus());
        player.pause();
        currentPlaybackState.setPlaybackStatus(
                currentStation,
                CurrentPlaybackState.PlaybackStatus.PAUSED
        );
    }

    private void handlePermanentAudioFocusLoss() {
        if (!shouldRespectAudioInterruptions()) {
            AppLog.d(TAG, "Ignoring permanent audio focus loss because interruptions are disabled");
            return;
        }
        if (!hasInterruptiblePlayback()) {
            AppLog.d(TAG, "Ignoring permanent audio focus loss because playback is not active");
            return;
        }

        AppLog.d(TAG, "Stopping playback after permanent audio focus loss");
        stop();
    }

    private void handleAudioOutputBecameNoisy() {
        if (!hasInterruptiblePlayback()) {
            AppLog.d(TAG, "Ignoring noisy-audio event because playback is not active");
            return;
        }

        AppLog.d(TAG, "Stopping playback because audio output became noisy");
        stop();
    }

    private boolean shouldResumeAfterTemporaryFocusLoss() {
        return currentPlaybackState.getPlaybackStatus() == CurrentPlaybackState.PlaybackStatus.CONNECTING
                || currentPlaybackState.getPlaybackStatus() == CurrentPlaybackState.PlaybackStatus.PLAYING;
    }

    private boolean hasInterruptiblePlayback() {
        if (currentStation == null) {
            return false;
        }

        CurrentPlaybackState.PlaybackStatus playbackStatus = currentPlaybackState.getPlaybackStatus();
        return playbackStatus == CurrentPlaybackState.PlaybackStatus.CONNECTING
                || playbackStatus == CurrentPlaybackState.PlaybackStatus.PLAYING
                || resumeAfterTemporaryFocusLoss;
    }

    private void clearTemporaryFocusLossState() {
        resumeAfterTemporaryFocusLoss = false;
    }

    private boolean shouldRespectAudioInterruptions() {
        return audioInterruptionSettings == null
                || audioInterruptionSettings.shouldRespectAudioInterruptions();
    }

    private void setConnectingMediaPlaceholder(@NonNull Station station) {
        if (player == null) {
            return;
        }

        player.setMediaItem(buildStationMediaItem(station, station.getPlayableStreamUrl()));
    }

    private void resolveArtworkForStation(@NonNull Station station) {
        StationArtworkResolver.resolve(station, resolvedUrls -> {
            preloadArtworkIfNeeded(station, resolvedUrls);
            refreshArtworkPresentation(station);
        });
    }

    private void resolveAndPlayStation(@NonNull Station station, int sequence) {
        ResolutionResult resolutionResult = streamResolver.resolveUrls(station);
        mainHandler.post(() -> {
            if (!isCurrentPlaySequence(sequence)) {
                return;
            }
            applyResolvedPlayback(station, resolutionResult, sequence);
        });
    }

    private boolean isCurrentPlaySequence(long sequence) {
        return player != null && sequence == playSequence.get();
    }

    private void applyResolvedPlayback(@NonNull Station station,
                                       @NonNull ResolutionResult resolutionResult,
                                       int sequence) {
        currentPlaybackGeneration = sequence;
        currentResolutionResult = resolutionResult;
        currentCandidateIndex = 0;
        nowPlayingObserver.startObserving(player, station, sequence);
        playCandidateAtIndex(station, resolutionResult, 0, sequence);
    }

    private void playCandidateAtIndex(@NonNull Station station,
                                      @NonNull ResolutionResult resolutionResult,
                                      int candidateIndex,
                                      long sequence) {
        if (!isCurrentPlaySequence(sequence)) {
            return;
        }

        String playableStreamUrl = resolvePlayableStreamUrl(station, resolutionResult, candidateIndex);
        currentCandidateIndex = candidateIndex;
        AppLog.d(TAG, "Starting playback for "
                + station.getName()
                + " candidateIndex="
                + candidateIndex
                + " url="
                + AppLog.redactUrl(playableStreamUrl));
        stopAndClearPlayer();
        player.setMediaItem(buildStationMediaItem(station, playableStreamUrl));
        currentPlaybackState.setPlaybackStatus(station, CurrentPlaybackState.PlaybackStatus.CONNECTING);
        player.prepare();
        player.play();
    }

    @NonNull
    private String resolvePlayableStreamUrl(@NonNull Station station,
                                            @NonNull ResolutionResult resolutionResult,
                                            int candidateIndex) {
        ResolvedStreamCandidate candidate = getPlaybackCandidate(resolutionResult, candidateIndex);
        return candidate != null ? candidate.getUrl() : station.getPlayableStreamUrl();
    }

    @Nullable
    private ResolvedStreamCandidate getPlaybackCandidate(@NonNull ResolutionResult resolutionResult,
                                                         int candidateIndex) {
        if (resolutionResult.getCandidates().isEmpty()) {
            return null;
        }
        int safeIndex = Math.min(candidateIndex, resolutionResult.getCandidates().size() - 1);
        return resolutionResult.getCandidates().get(safeIndex);
    }

    @NonNull
    private MediaItem buildStationMediaItem(@NonNull Station station, @NonNull String streamUrl) {
        return new MediaItem.Builder()
                .setUri(Uri.parse(streamUrl))
                .setMediaMetadata(buildBaseMediaMetadata(station))
                .build();
    }

    private boolean attemptNextCandidate() {
        if (player == null || currentStation == null || currentResolutionResult == null) {
            return false;
        }

        int nextIndex = currentCandidateIndex + 1;
        if (nextIndex >= currentResolutionResult.getCandidates().size()) {
            Log.w(TAG, "No more playback candidates for " + currentStation.getName());
            return false;
        }

        Log.w(TAG, "Trying next playback candidate for "
                + currentStation.getName()
                + " -> index "
                + nextIndex);
        nowPlayingObserver.startObserving(player, currentStation, currentPlaybackGeneration);
        playCandidateAtIndex(currentStation, currentResolutionResult, nextIndex, currentPlaybackGeneration);
        return true;
    }

    @NonNull
    private String getCurrentStationLogName() {
        return currentStation != null ? currentStation.getName() : "<none>";
    }

    @NonNull
    private String getCurrentCandidateLogUrl() {
        if (currentResolutionResult == null
                || currentCandidateIndex < 0
                || currentCandidateIndex >= currentResolutionResult.getCandidates().size()) {
            return "<none>";
        }
        return AppLog.redactUrl(currentResolutionResult.getCandidates().get(currentCandidateIndex).getUrl());
    }

    @NonNull
    private String getPlayerStateLogValue() {
        return player != null
                ? playbackStateToString(player.getPlaybackState())
                : "<no-player>";
    }

    @NonNull
    private static String playbackStateToString(int playbackState) {
        switch (playbackState) {
            case Player.STATE_IDLE:
                return "IDLE";
            case Player.STATE_BUFFERING:
                return "BUFFERING";
            case Player.STATE_READY:
                return "READY";
            case Player.STATE_ENDED:
                return "ENDED";
            default:
                return "UNKNOWN(" + playbackState + ")";
        }
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
        String connectingText = getString(R.string.playback_notification_connecting_to, station.getName());
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, PLAYBACK_NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_media_play)
                .setContentTitle(connectingText)
                .setContentIntent(buildContentIntent())
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setSilent(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
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

    private void syncControllerTransportControls(@Nullable Station station,
                                                 @NonNull CurrentPlaybackState.PlaybackStatus playbackStatus) {
        if (mediaSession == null) {
            return;
        }

        SessionCommands sessionCommands = buildControllerSessionCommands(station);
        Player.Commands playerCommands = buildControllerPlayerCommands(mediaSession);
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

    @NonNull
    private MediaMetadata buildBaseMediaMetadata(@NonNull Station station) {
        MediaMetadata.Builder builder = new MediaMetadata.Builder()
                .setStation(station.getName())
                .setUserRating(new HeartRating(isFavorite(station)));
        Uri artworkUri = PlaybackMetadataMapper.resolveStationArtworkUri(station);
        if (artworkUri != null) {
            builder.setArtworkUri(artworkUri);
        }
        return builder.build();
    }

    @NonNull
    private SessionCommands buildControllerSessionCommands(@Nullable Station station) {
        return station == null ? SessionCommands.EMPTY : ACTIVE_SESSION_COMMANDS;
    }

    @NonNull
    private Player.Commands buildControllerPlayerCommands(@NonNull MediaSession session) {
        return session.getPlayer().getAvailableCommands();
    }

    @NonNull
    private List<CommandButton> buildControllerMediaButtons(@Nullable Station station,
                                                            @NonNull CurrentPlaybackState.PlaybackStatus playbackStatus) {
        if (station == null || playbackStatus == CurrentPlaybackState.PlaybackStatus.IDLE) {
            return Collections.emptyList();
        }

        return Collections.singletonList(buildFavoriteMediaButton(station));
    }

    @NonNull
    private CommandButton buildFavoriteMediaButton(@NonNull Station station) {
        boolean favorite = isFavorite(station);
        return new CommandButton.Builder(favorite
                ? CommandButton.ICON_HEART_FILLED
                : CommandButton.ICON_HEART_UNFILLED)
                .setSessionCommand(TOGGLE_FAVORITE_COMMAND)
                .setDisplayName(getString(favorite
                        ? R.string.player_unfavorite_button
                        : R.string.player_favorite_button))
                // Keep transport slots free for previous/play-next; rating metadata carries the
                // favorite semantic for controller surfaces such as Android Auto.
                .setSlots(FAVORITE_BUTTON_SLOTS)
                .build();
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

    private void markCurrentStationStatus(@NonNull CurrentPlaybackState.PlaybackStatus playbackStatus) {
        if (currentStation == null) {
            return;
        }
        currentPlaybackState.setPlaybackStatus(currentStation, playbackStatus);
    }

    private void reportCurrentStationUsageIfNeeded() {
        if (currentStation == null
                || stationRepository == null
                || currentPlaybackGeneration == 0L
                || reportedUsageGeneration == currentPlaybackGeneration) {
            return;
        }

        reportedUsageGeneration = currentPlaybackGeneration;
        if (currentStation.getOrigin() == StationOrigin.RADIO_BROWSER) {
            stationRepository.reportStationUsage(currentStation);
        }
        if (libraryRepository != null) {
            libraryRepository.recordRecentlyPlayed(currentStation);
        }
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

        syncControllerTransportControls(currentStation, currentPlaybackState.getPlaybackStatus());
        refreshPresentedMetadata();
    }

    private void handleObservedNowPlayingChange(@NonNull Station station,
                                                @Nullable NowPlaying previousNowPlaying,
                                                @Nullable NowPlaying currentNowPlaying) {
        long changedAtElapsedMs = SystemClock.elapsedRealtime();
        currentPlaybackState.setCurrentNowPlaying(station, currentNowPlaying);
        dispatchPresentedMetadataChanged();
        onNowPlayingChanged(station, previousNowPlaying, currentNowPlaying, changedAtElapsedMs);
        updateCurrentNowPlayingTiming(currentNowPlaying, changedAtElapsedMs);
    }

    private void onNowPlayingChanged(@NonNull Station station,
                                     @Nullable NowPlaying previousNowPlaying,
                                     @Nullable NowPlaying currentNowPlaying,
                                     long changedAtElapsedMs) {
        if (libraryRepository == null
                || !isCurrentStation(station)
                || previousNowPlaying == null
                || currentNowPlaying == null
                || previousNowPlaying.equals(currentNowPlaying)) {
            return;
        }
        if (!wasCurrentTrackHeardLongEnough(changedAtElapsedMs)) {
            return;
        }

        libraryRepository.recordRecentlyListenedSong(
                station,
                new RecentlyListenedSong(
                        previousNowPlaying.getArtist(),
                        previousNowPlaying.getTitle(),
                        System.currentTimeMillis()
                )
        );
    }

    private boolean wasCurrentTrackHeardLongEnough(long changedAtElapsedMs) {
        if (currentNowPlayingStartedAtElapsedMs <= 0L) {
            return false;
        }

        return changedAtElapsedMs - currentNowPlayingStartedAtElapsedMs
                >= MIN_RECENTLY_LISTENED_TRACK_DURATION_MS;
    }

    private void updateCurrentNowPlayingTiming(@Nullable NowPlaying currentNowPlaying,
                                               long changedAtElapsedMs) {
        currentNowPlayingStartedAtElapsedMs = currentNowPlaying == null
                ? 0L
                : changedAtElapsedMs;
    }

    private void clearCurrentNowPlayingTiming() {
        currentNowPlayingStartedAtElapsedMs = 0L;
    }

    private void requestNotificationRefresh() {
        if (mediaSession == null || player == null || player.getMediaItemCount() == 0) {
            return;
        }
        if (shouldKeepConnectingNotification()) {
            return;
        }
        mainHandler.post(this::triggerNotificationUpdate);
    }

    private boolean shouldKeepConnectingNotification() {
        if (player == null) {
            return false;
        }

        return currentPlaybackState.getPlaybackStatus() == CurrentPlaybackState.PlaybackStatus.CONNECTING
                && !player.isPlaying();
    }

    private void refreshArtworkPresentation(@NonNull Station station) {
        if (!isCurrentStation(station)) {
            return;
        }
        refreshPresentedMetadata();
    }

    private void preloadArtworkIfNeeded(@NonNull Station station, @NonNull List<String> artworkUrls) {
        if (artworkUrls.isEmpty()) {
            return;
        }

        String bestArtworkUrl = artworkUrls.get(0);
        if (!StationArtworkResolver.isSvgUrl(bestArtworkUrl)
                || StationArtworkBitmapLoader.getCachedBitmap(bestArtworkUrl) != null) {
            return;
        }

        StationArtworkBitmapLoader.loadSvgBitmap(bestArtworkUrl, bitmap -> {
            if (bitmap == null) {
                return;
            }
            refreshArtworkPresentation(station);
        });
    }

    private void dispatchPresentedMetadataChanged() {
        if (sessionPlayer == null) {
            return;
        }

        mainHandler.post(sessionPlayer::dispatchPresentedMetadataChanged);
    }

    private void refreshPresentedMetadata() {
        dispatchPresentedMetadataChanged();
        requestNotificationRefresh();
    }

    private boolean isCurrentStation(@NonNull Station station) {
        return currentStation != null && currentStation.getId().equals(station.getId());
    }

    private void setFavorite(@NonNull Station station, boolean favorite) {
        if (libraryRepository != null) {
            libraryRepository.setFavorite(station, favorite);
        }
    }

    private boolean isFavorite(@NonNull Station station) {
        return libraryRepository != null && libraryRepository.isFavorite(station);
    }

    private boolean canNavigateFavoriteStations() {
        return favoriteStationNavigator.canNavigate(currentStation, getFavoriteStationsSnapshot());
    }

    @NonNull
    private List<Station> getFavoriteStationsSnapshot() {
        return libraryRepository != null
                ? libraryRepository.getFavoriteStationsSnapshot()
                : Collections.emptyList();
    }

    private void navigateFavoriteStation(boolean moveForward) {
        Station targetStation = getFavoriteNavigationTarget(moveForward);
        if (targetStation != null) {
            play(targetStation);
        }
    }

    @Nullable
    private Station getFavoriteNavigationTarget(boolean moveForward) {
        List<Station> favoriteStations = getFavoriteStationsSnapshot();
        return moveForward
                ? favoriteStationNavigator.getNext(currentStation, favoriteStations)
                : favoriteStationNavigator.getPrevious(currentStation, favoriteStations);
    }

    private void applyFavoriteNavigationCommands(@NonNull Player.Commands.Builder commandsBuilder) {
        if (canNavigateFavoriteStations()) {
            commandsBuilder
                    .add(Player.COMMAND_SEEK_TO_PREVIOUS)
                    .add(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
                    .add(Player.COMMAND_SEEK_TO_NEXT)
                    .add(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM);
            return;
        }

        commandsBuilder.removeAll(
                Player.COMMAND_SEEK_TO_PREVIOUS,
                Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM,
                Player.COMMAND_SEEK_TO_NEXT,
                Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM
        );
    }

    private void applyStopCommand(@NonNull Player.Commands.Builder commandsBuilder) {
        if (currentStation != null
                && currentPlaybackState.getPlaybackStatus() != CurrentPlaybackState.PlaybackStatus.IDLE) {
            commandsBuilder.add(Player.COMMAND_STOP);
            return;
        }

        commandsBuilder.remove(Player.COMMAND_STOP);
    }

    private final class SessionPlayer extends ForwardingPlayer {

        @NonNull
        private final List<Player.Listener> listeners = new ArrayList<>();
        @Nullable
        private MediaMetadata lastDispatchedMetadata;

        private SessionPlayer(@NonNull Player player) {
            super(player);
        }

        @Override
        public void addListener(@NonNull Player.Listener listener) {
            super.addListener(listener);
            if (!listeners.contains(listener)) {
                listeners.add(listener);
            }
        }

        @Override
        public void removeListener(@NonNull Player.Listener listener) {
            super.removeListener(listener);
            listeners.remove(listener);
        }

        @NonNull
        @Override
        public Commands getAvailableCommands() {
            Commands.Builder commandsBuilder = super.getAvailableCommands().buildUpon();
            applyFavoriteNavigationCommands(commandsBuilder);
            applyStopCommand(commandsBuilder);
            return commandsBuilder.build();
        }

        @NonNull
        @Override
        public MediaMetadata getMediaMetadata() {
            return PlaybackMetadataMapper.buildPresentedMediaMetadata(
                    super.getMediaMetadata(),
                    currentStation,
                    currentPlaybackState.getCurrentNowPlaying(),
                    currentStation != null && isFavorite(currentStation)
            );
        }

        @Override
        public boolean hasPreviousMediaItem() {
            return canNavigateFavoriteStations();
        }

        @Override
        public boolean hasNextMediaItem() {
            return canNavigateFavoriteStations();
        }

        @Override
        public void seekToPrevious() {
            navigateFavoriteStation(false);
        }

        @Override
        public void seekToPreviousMediaItem() {
            navigateFavoriteStation(false);
        }

        @Override
        public void seekToNext() {
            navigateFavoriteStation(true);
        }

        @Override
        public void seekToNextMediaItem() {
            navigateFavoriteStation(true);
        }

        private void dispatchPresentedMetadataChanged() {
            MediaMetadata presentedMetadata = getMediaMetadata();
            if (Objects.equals(lastDispatchedMetadata, presentedMetadata)) {
                return;
            }

            lastDispatchedMetadata = presentedMetadata;
            for (Player.Listener listener : new ArrayList<>(listeners)) {
                listener.onMediaMetadataChanged(presentedMetadata);
            }
        }
    }
}
