package com.matijakljajic.freeairradio.playback;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.session.DefaultMediaNotificationProvider;

import com.matijakljajic.freeairradio.R;

@UnstableApi
public final class RadioPlaybackNotificationProvider extends DefaultMediaNotificationProvider {

    public RadioPlaybackNotificationProvider(@NonNull Context context,
                                             int notificationId,
                                             @NonNull String channelId,
                                             int channelNameResId) {
        super(context, session -> notificationId, channelId, channelNameResId);
        setSmallIcon(R.drawable.ic_notification_small);
    }

    @Nullable
    @Override
    protected CharSequence getNotificationContentTitle(@NonNull MediaMetadata metadata) {
        return buildNotificationTitle(metadata);
    }

    @Nullable
    @Override
    protected CharSequence getNotificationContentText(@NonNull MediaMetadata metadata) {
        return buildNotificationText(metadata);
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
