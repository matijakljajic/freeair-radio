package com.matijakljajic.freeairradio.playback.resolution;

import org.junit.Test;

import com.matijakljajic.freeairradio.data.model.Station;
import com.matijakljajic.freeairradio.data.model.StationOrigin;

import static org.junit.Assert.assertEquals;

public class StreamResolutionEngineTest {

    @Test
    public void selectBestPlaylistTargetPrefersStreamLikeEntryOverIntroFile() {
        String body = "#EXTM3U\n#EXTINF:-1,Intro\nhttps://example.com/intro.mp3\n#EXTINF:-1,Live stream\nhttps://example.com/live";

        assertEquals("https://example.com/live", extractPlaylistTarget(body, "https://example.com/playlist.m3u"));
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
                new PlaylistParser().parse(body, baseUrl)
        );
    }
}
