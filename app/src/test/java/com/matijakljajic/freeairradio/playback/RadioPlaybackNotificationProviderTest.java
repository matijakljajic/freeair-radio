package com.matijakljajic.freeairradio.playback;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import androidx.media3.common.MediaMetadata;

import org.junit.Test;

public class RadioPlaybackNotificationProviderTest {

    @Test
    public void buildNotificationTitlePrefersStationName() {
        MediaMetadata metadata = new MediaMetadata.Builder()
                .setStation("Radio Caroline")
                .setTitle("Track Title")
                .build();

        CharSequence title = RadioPlaybackNotificationProvider.buildNotificationTitle(metadata);

        assertEquals("Radio Caroline", title);
    }

    @Test
    public void buildNotificationTextUsesArtistAndTitle() {
        MediaMetadata metadata = new MediaMetadata.Builder()
                .setStation("Radio Caroline")
                .setArtist("Artist")
                .setTitle("Track Title")
                .build();

        CharSequence text = RadioPlaybackNotificationProvider.buildNotificationText(metadata);

        assertEquals("Artist - Track Title", text);
    }

    @Test
    public void buildNotificationTextAvoidsDuplicatingStationName() {
        MediaMetadata metadata = new MediaMetadata.Builder()
                .setStation("Radio Caroline")
                .setTitle("Radio Caroline")
                .build();

        CharSequence text = RadioPlaybackNotificationProvider.buildNotificationText(metadata);

        assertNull(text);
    }
}
