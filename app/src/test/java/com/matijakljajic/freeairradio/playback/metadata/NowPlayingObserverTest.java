package com.matijakljajic.freeairradio.playback.metadata;

import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class NowPlayingObserverTest {

    @Test
    public void parseTrackTitleSplitsSimpleDashTitle() {
        NowPlaying.ParsedTrack parsedTrack = NowPlaying.parseTrackTitle("Daft Punk - Digital Love");

        Assert.assertNotNull(parsedTrack);
        assertEquals("Daft Punk", parsedTrack.artist);
        assertEquals("Digital Love", parsedTrack.title);
    }

    @Test
    public void parseTrackTitleSplitsEnDashTitle() {
        NowPlaying.ParsedTrack parsedTrack = NowPlaying.parseTrackTitle("Daft Punk – Digital Love");

        Assert.assertNotNull(parsedTrack);
        assertEquals("Daft Punk", parsedTrack.artist);
        assertEquals("Digital Love", parsedTrack.title);
    }

    @Test
    public void parseTrackTitlePreservesAmbiguousMultiDashTitle() {
        NowPlaying.ParsedTrack parsedTrack = NowPlaying.parseTrackTitle("BBC Radio 4 - The World at One - 13:00");

        Assert.assertNotNull(parsedTrack);
        assertNull(parsedTrack.artist);
        assertEquals("BBC Radio 4 - The World at One - 13:00", parsedTrack.title);
    }

    @Test
    public void fromMetadataSplitsCombinedTrackTitle() {
        NowPlaying nowPlaying = NowPlaying.fromMetadata("Radio Caroline", null, "Daft Punk - Digital Love");

        Assert.assertNotNull(nowPlaying);
        assertEquals("Daft Punk", nowPlaying.getArtist());
        assertEquals("Digital Love", nowPlaying.getTitle());
    }

    @Test
    public void fromMetadataIgnoresStationNameOnly() {
        NowPlaying nowPlaying = NowPlaying.fromMetadata("Radio Caroline", null, "Radio Caroline");

        assertNull(nowPlaying);
    }

    @Test
    public void nowPlayingBuildDisplayTextPrefersArtistAndTitle() {
        NowPlaying nowPlaying = new NowPlaying("Artist", "Track");

        assertEquals("Artist - Track", nowPlaying.buildDisplayText());
    }

    @Test
    public void nowPlayingBuildDisplayTextReturnsNullWhenNoMetadataExists() {
        NowPlaying nowPlaying = new NowPlaying(null, null);

        assertNull(nowPlaying.buildDisplayText());
    }

    @Test
    public void nowPlayingDecodesNumericCharacterReferences() {
        NowPlaying nowPlaying = new NowPlaying("Bajaga", "Moji drugovi &#263;e misliti da je gotovo");

        assertEquals("Bajaga - Moji drugovi će misliti da je gotovo", nowPlaying.buildDisplayText());
    }
}
