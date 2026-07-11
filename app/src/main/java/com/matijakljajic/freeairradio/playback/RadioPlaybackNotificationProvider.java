package com.matijakljajic.freeairradio.playback;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.session.CommandButton;
import androidx.media3.session.DefaultMediaNotificationProvider;
import androidx.media3.session.MediaSession;

import com.google.common.collect.ImmutableList;

@UnstableApi
public final class RadioPlaybackNotificationProvider extends DefaultMediaNotificationProvider {

    public RadioPlaybackNotificationProvider(@NonNull Context context,
                                             int notificationId,
                                             @NonNull String channelId,
                                             int channelNameResId) {
        super(context, session -> notificationId, channelId, channelNameResId);
        setSmallIcon(android.R.drawable.ic_media_play);
    }

    @NonNull
    @Override
    protected ImmutableList<CommandButton> getMediaButtons(@NonNull MediaSession session,
                                                           @NonNull Player.Commands playerCommands,
                                                           @NonNull ImmutableList<CommandButton> mediaButtonPreferences,
                                                           boolean showPauseButton) {
        return playerCommands.contains(Player.COMMAND_STOP)
                ? ImmutableList.copyOf(mediaButtonPreferences)
                : ImmutableList.of();
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
        if (hasText(metadata.station)) {
            return metadata.station;
        }
        if (hasText(metadata.displayTitle)) {
            return metadata.displayTitle;
        }
        return metadata.title;
    }

    @Nullable
    static CharSequence buildNotificationText(@NonNull MediaMetadata metadata) {
        if (hasText(metadata.artist) && hasText(metadata.title)) {
            return metadata.artist + " - " + metadata.title;
        }
        if (hasText(metadata.title) && !contentEquals(metadata.title, metadata.station)) {
            return metadata.title;
        }
        if (hasText(metadata.artist)) {
            return metadata.artist;
        }
        return null;
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
