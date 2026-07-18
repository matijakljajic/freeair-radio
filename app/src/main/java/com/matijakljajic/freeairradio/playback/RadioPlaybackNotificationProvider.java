package com.matijakljajic.freeairradio.playback;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.session.DefaultMediaNotificationProvider;

import com.matijakljajic.freeairradio.R;
import com.matijakljajic.freeairradio.data.model.Station;
import com.matijakljajic.freeairradio.playback.metadata.CurrentPlaybackState;

@UnstableApi
public final class RadioPlaybackNotificationProvider extends DefaultMediaNotificationProvider {

    @NonNull
    private final Context appContext;
    @NonNull
    private final CurrentPlaybackState currentPlaybackState = CurrentPlaybackState.getInstance();

    public RadioPlaybackNotificationProvider(@NonNull Context context,
                                             int notificationId,
                                             @NonNull String channelId,
                                             int channelNameResId) {
        super(context, session -> notificationId, channelId, channelNameResId);
        appContext = context.getApplicationContext();
        setSmallIcon(R.drawable.ic_notification_small);
    }

    @Nullable
    @Override
    protected CharSequence getNotificationContentTitle(@NonNull MediaMetadata metadata) {
        CharSequence connectingTitle = buildConnectingTitle();
        if (connectingTitle != null) {
            return connectingTitle;
        }
        return buildNotificationTitle(metadata);
    }

    @Nullable
    @Override
    protected CharSequence getNotificationContentText(@NonNull MediaMetadata metadata) {
        if (isConnecting()) {
            return null;
        }
        return buildNotificationText(metadata);
    }

    @Nullable
    private CharSequence buildConnectingTitle() {
        if (!isConnecting()) {
            return null;
        }

        Station station = currentPlaybackState.getCurrentStation();
        if (station == null || !hasText(station.getName())) {
            return appContext.getString(R.string.playback_notification_connecting);
        }
        return appContext.getString(R.string.playback_notification_connecting_to, station.getName());
    }

    private boolean isConnecting() {
        return currentPlaybackState.getPlaybackStatus() == CurrentPlaybackState.PlaybackStatus.CONNECTING;
    }

    @Nullable
    static CharSequence buildNotificationTitle(@NonNull MediaMetadata metadata) {
        if (hasCompleteTrackInfo(metadata)
                && hasText(metadata.title)
                && !contentEquals(metadata.title, metadata.station)) {
            return metadata.title;
        }
        if (hasText(metadata.station)) {
            return metadata.station;
        }
        if (hasText(metadata.displayTitle)) {
            return metadata.displayTitle;
        }
        return metadata.artist;
    }

    @Nullable
    static CharSequence buildNotificationText(@NonNull MediaMetadata metadata) {
        if (hasCompleteTrackInfo(metadata)
                && hasText(metadata.artist)
                && !contentEquals(metadata.artist, metadata.title)) {
            return metadata.artist;
        }
        return null;
    }

    private static boolean hasCompleteTrackInfo(@NonNull MediaMetadata metadata) {
        return hasText(metadata.title) && hasText(metadata.artist);
    }

    private static boolean hasText(@Nullable CharSequence text) {
        return text != null && text.length() > 0;
    }

    private static boolean contentEquals(@Nullable CharSequence left, @Nullable CharSequence right) {
        if (left == right) {
            return true;
        }
        if (left == null || right == null) {
            return false;
        }
        return left.toString().contentEquals(right);
    }
}
