package com.matijakljajic.freeairradio.playback;

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
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.Player;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.common.MediaItem;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import androidx.media3.session.MediaSession;
import androidx.media3.session.MediaSessionService;

import com.matijakljajic.freeairradio.R;
import com.matijakljajic.freeairradio.data.model.Station;
import com.matijakljajic.freeairradio.playback.resolution.ResolutionResult;
import com.matijakljajic.freeairradio.playback.resolution.ResolvedStreamCandidate;
import com.matijakljajic.freeairradio.ui.MainActivity;

import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

@UnstableApi
public class RadioPlaybackService extends MediaSessionService {

    private static final String PLAYBACK_NOTIFICATION_CHANNEL_ID = "radio_playback";
    private static final int PLAYBACK_NOTIFICATION_ID = 1001;

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
    @NonNull
    private final CurrentPlaybackState.Listener foregroundPlaybackStateListener = (station, nowPlaying, playbackStatus) -> {
        if (station != null) {
            promoteToForeground(station, nowPlaying);
        }
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
        mediaSession = new MediaSession.Builder(this, player).build();
        createPlaybackNotificationChannel();
        currentPlaybackState.addListener(foregroundPlaybackStateListener);
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
        currentPlaybackState.removeListener(foregroundPlaybackStateListener);
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
        MediaMetadata mediaMetadata = new MediaMetadata.Builder()
                .setTitle(station.getName())
                .build();
        MediaItem mediaItem = new MediaItem.Builder()
                .setUri(Uri.parse(playableStreamUrl))
                .setMediaMetadata(mediaMetadata)
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

    private void promoteToForeground(@NonNull Station station, @Nullable NowPlaying nowPlaying) {
        int foregroundServiceType = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                ? ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                : 0;
        ServiceCompat.startForeground(
                this,
                PLAYBACK_NOTIFICATION_ID,
                buildPlaybackNotification(station, nowPlaying),
                foregroundServiceType
        );
    }

    @NonNull
    private android.app.Notification buildPlaybackNotification(@NonNull Station station,
                                                               @Nullable NowPlaying nowPlaying) {
        Intent activityIntent = new Intent(this, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(
                this,
                0,
                activityIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, PLAYBACK_NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_media_play)
                .setContentTitle(station.getName())
                .setContentIntent(contentIntent)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setSilent(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE);

        if (nowPlaying != null) {
            String nowPlayingText = nowPlaying.buildDisplayText();
            if (nowPlayingText != null) {
                builder.setContentText(nowPlayingText);
            }
        }

        return builder.build();
    }

    private void markCurrentStationStatus(@NonNull CurrentPlaybackState.PlaybackStatus playbackStatus) {
        if (currentStation == null) {
            return;
        }
        currentPlaybackState.setPlaybackStatus(currentStation, playbackStatus);
    }
}
