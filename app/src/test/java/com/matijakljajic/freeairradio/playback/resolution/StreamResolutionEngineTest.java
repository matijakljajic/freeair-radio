package com.matijakljajic.freeairradio.playback.resolution;

import org.junit.Test;

import com.matijakljajic.freeairradio.data.model.Station;
import com.matijakljajic.freeairradio.data.model.StationOrigin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class StreamResolutionEngineTest {

    @Test
    public void isPlaylistResponseDetectsCommonPlaylistBodiesAndExtensions() {
        assertTrue(StreamResolutionEngine.isPlaylistResponse("https://example.com/stream.m3u", null, ""));
        assertTrue(StreamResolutionEngine.isPlaylistResponse("https://example.com/stream", null, "#EXTM3U\nhttps://example.com/live"));
    }

    @Test
    public void isHlsManifestDetectsHlsBodiesAndExtensions() {
        assertTrue(StreamResolutionEngine.isHlsManifest("https://example.com/stream.m3u8", null, ""));
        assertTrue(StreamResolutionEngine.isHlsManifest("https://example.com/stream", "application/vnd.apple.mpegurl", ""));
        assertTrue(StreamResolutionEngine.isHlsManifest("https://example.com/stream", null, "#EXTM3U\n#EXT-X-STREAM-INF"));
    }

    @Test
    public void isPlaylistResponseDoesNotTreatHlsAsGenericPlaylist() {
        assertFalse(StreamResolutionEngine.isPlaylistResponse("https://example.com/stream.m3u8", "application/vnd.apple.mpegurl", "#EXTM3U"));
    }

    @Test
    public void extractPlaylistTargetPrefersStreamLikeEntryOverIntroFile() {
        String body = "#EXTM3U\n#EXTINF:-1,Intro\nhttps://example.com/intro.mp3\n#EXTINF:-1,Live stream\nhttps://example.com/live";

        assertEquals("https://example.com/live", extractPlaylistTarget(body, "https://example.com/playlist.m3u"));
    }

    @Test
    public void extractPlaylistTargetResolvesRelativePlsEntryAgainstBaseUrl() {
        String body = "[playlist]\nFile1=/live\nFile2=/backup";

        assertEquals("https://example.com/live", extractPlaylistTarget(body, "https://example.com/playlist.pls"));
    }

    @Test
    public void extractPlaylistTargetReturnsDirectUrlBody() {
        assertEquals("https://example.com/live", extractPlaylistTarget("https://example.com/live", "https://example.com/stream"));
    }

    @Test
    public void extractPlaylistTargetReturnsNullForNonPlaylistText() {
        assertNull(extractPlaylistTarget("not a playlist", "https://example.com/stream"));
    }

    @Test
    public void stationGetPlayableStreamUrlUsesResolvedUrlDirectly() {
        Station station = Station.builder("RADIO_BROWSER:1", "Station", "https://example.com/stream", StationOrigin.RADIO_BROWSER)
                .setResolvedStreamUrl("https://example.com/final")
                .setCodec("MP3")
                .setHls(Boolean.FALSE)
                .build();

        assertEquals("https://example.com/final", station.getPlayableStreamUrl());
    }

    @Test
    public void buildCandidatesPrefersHttpsVersionOfHttpUrl() {
        assertEquals(java.util.List.of(
                "https://example.com/stream",
                "http://example.com/stream"
        ), StreamResolutionEngine.buildCandidates("http://example.com/stream"));
    }

    @Test
    public void buildCandidatesAddsHttpFallbackForHttpsUrl() {
        assertEquals(java.util.List.of(
                "https://example.com/stream",
                "http://example.com/stream"
        ), StreamResolutionEngine.buildCandidates("https://example.com/stream"));
    }

    private static String extractPlaylistTarget(String body, String baseUrl) {
        return StreamResolutionEngine.selectBestPlaylistTarget(
                StreamResolutionEngine.extractPlaylistTargets(body, baseUrl)
        );
    }
}
