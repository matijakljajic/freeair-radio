package com.matijakljajic.freeairradio.playback;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import androidx.media3.common.MediaMetadata;

import org.junit.Test;

public class RadioPlaybackNotificationProviderTest {

    @Test
    public void buildNotificationTitleUsesTrackTitleWhenTrackIsComplete() {
        MediaMetadata metadata = new MediaMetadata.Builder()
                .setStation("Radio Caroline")
                .setArtist("Artist")
                .setTitle("Track Title")
                .build();

        CharSequence title = RadioPlaybackNotificationProvider.buildNotificationTitle(metadata);

        assertEquals("Track Title", title);
    }

    @Test
    public void buildNotificationTextUsesArtistWhenTrackIsComplete() {
        MediaMetadata metadata = new MediaMetadata.Builder()
                .setStation("Radio Caroline")
                .setArtist("Artist")
                .setTitle("Track Title")
                .build();

        CharSequence text = RadioPlaybackNotificationProvider.buildNotificationText(metadata);

        assertEquals("Artist", text);
    }

    @Test
    public void buildNotificationFallsBackToStationWhenTrackIsIncomplete() {
        MediaMetadata metadata = new MediaMetadata.Builder()
                .setStation("Radio Caroline")
                .setTitle("Track Title")
                .build();

        CharSequence title = RadioPlaybackNotificationProvider.buildNotificationTitle(metadata);
        CharSequence text = RadioPlaybackNotificationProvider.buildNotificationText(metadata);

        assertEquals("Radio Caroline", title);
        assertNull(text);
    }
}
