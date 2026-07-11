package com.matijakljajic.freeairradio.playback;

import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class NowPlayingObserverTest {

    @Test
    public void parseTrackTitleSplitsSimpleDashTitle() {
        NowPlayingObserver.ParsedTrack parsedTrack = NowPlayingObserver.parseTrackTitle("Daft Punk - Digital Love");

        Assert.assertNotNull(parsedTrack);
        assertEquals("Daft Punk", parsedTrack.getArtist());
        assertEquals("Digital Love", parsedTrack.getTitle());
        assertEquals("Daft Punk - Digital Love", parsedTrack.getRawTitle());
    }

    @Test
    public void parseTrackTitleSplitsEnDashTitle() {
        NowPlayingObserver.ParsedTrack parsedTrack = NowPlayingObserver.parseTrackTitle("Daft Punk – Digital Love");

        Assert.assertNotNull(parsedTrack);
        assertEquals("Daft Punk", parsedTrack.getArtist());
        assertEquals("Digital Love", parsedTrack.getTitle());
    }

    @Test
    public void parseTrackTitlePreservesAmbiguousMultiDashTitle() {
        NowPlayingObserver.ParsedTrack parsedTrack = NowPlayingObserver.parseTrackTitle("BBC Radio 4 - The World at One - 13:00");

        Assert.assertNotNull(parsedTrack);
        assertNull(parsedTrack.getArtist());
        assertEquals("BBC Radio 4 - The World at One - 13:00", parsedTrack.getTitle());
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
}
