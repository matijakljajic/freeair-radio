package com.matijakljajic.freeairradio.playback.metadata;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import androidx.media3.common.HeartRating;
import androidx.media3.common.MediaMetadata;

import com.matijakljajic.freeairradio.data.model.Station;
import com.matijakljajic.freeairradio.data.model.StationOrigin;

import org.junit.Test;

public class PlaybackMetadataMapperTest {

    @Test
    public void buildPresentedMediaMetadataUsesStationAndNormalizedNowPlaying() {
        Station station = createStation("Radio Caroline");
        MediaMetadata rawMetadata = new MediaMetadata.Builder()
                .setStation("Wrong Station")
                .setArtist("Ignored Artist")
                .setTitle("Ignored Title")
                .build();
        NowPlaying nowPlaying = new NowPlaying("Bajaga", "Moji drugovi &#263;e misliti da je gotovo");

        MediaMetadata presentedMetadata = PlaybackMetadataMapper.buildPresentedMediaMetadata(
                rawMetadata,
                station,
                nowPlaying,
                true
        );

        assertEquals("Radio Caroline", presentedMetadata.station);
        assertEquals("Bajaga", presentedMetadata.artist);
        assertEquals("Moji drugovi će misliti da je gotovo", presentedMetadata.title);
        assertTrue(((HeartRating) presentedMetadata.userRating).isHeart());
    }

    @Test
    public void buildPresentedMediaMetadataFallsBackToRawTimedMetadataWhenNeeded() {
        Station station = createStation("Radio Caroline");
        MediaMetadata rawMetadata = new MediaMetadata.Builder()
                .setStation("Radio Caroline")
                .setArtist("Bajaga")
                .setTitle("Moji drugovi &#263;e misliti da je gotovo")
                .build();

        MediaMetadata presentedMetadata = PlaybackMetadataMapper.buildPresentedMediaMetadata(
                rawMetadata,
                station,
                null,
                false
        );

        assertEquals("Radio Caroline", presentedMetadata.station);
        assertEquals("Bajaga", presentedMetadata.artist);
        assertEquals("Moji drugovi će misliti da je gotovo", presentedMetadata.title);
        assertFalse(((HeartRating) presentedMetadata.userRating).isHeart());
    }

    @Test
    public void buildPresentedMediaMetadataSplitsCombinedRawTrackTitle() {
        Station station = createStation("Radio Caroline");
        MediaMetadata rawMetadata = new MediaMetadata.Builder()
                .setStation("Radio Caroline")
                .setTitle("Bajaga - Moji drugovi &#263;e misliti da je gotovo")
                .build();

        MediaMetadata presentedMetadata = PlaybackMetadataMapper.buildPresentedMediaMetadata(
                rawMetadata,
                station,
                null,
                false
        );

        assertEquals("Radio Caroline", presentedMetadata.station);
        assertEquals("Bajaga", presentedMetadata.artist);
        assertEquals("Moji drugovi će misliti da je gotovo", presentedMetadata.title);
    }

    @Test
    public void buildPresentedMediaMetadataDoesNotTreatStationNameAsTrack() {
        Station station = createStation("Radio Caroline");
        MediaMetadata rawMetadata = new MediaMetadata.Builder()
                .setStation("Radio Caroline")
                .setTitle("Radio Caroline")
                .build();

        MediaMetadata presentedMetadata = PlaybackMetadataMapper.buildPresentedMediaMetadata(
                rawMetadata,
                station,
                null,
                false
        );

        assertEquals("Radio Caroline", presentedMetadata.station);
        assertNull(presentedMetadata.artist);
        assertNull(presentedMetadata.title);
    }

    private static Station createStation(String name) {
        return Station.builder("RADIO_BROWSER:1", name, "https://example.com/stream", StationOrigin.RADIO_BROWSER)
                .build();
    }
}
