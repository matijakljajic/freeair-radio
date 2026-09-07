package com.matijakljajic.freeairradio.playback.resolution;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PlaylistParserTest {

    private final PlaylistParser parser = new PlaylistParser();

    @Test
    public void detectsPlaylistBodiesAndExtensions() {
        assertTrue(PlaylistParser.isPlaylistResponse("https://example.com/stream.m3u", null, ""));
        assertTrue(PlaylistParser.isPlaylistResponse(
                "https://example.com/stream",
                null,
                "#EXTM3U\nhttps://example.com/live"
        ));
    }

    @Test
    public void detectsHlsWithoutTreatingItAsARegularPlaylist() {
        assertTrue(PlaylistParser.isHlsManifest("https://example.com/stream.m3u8", null, ""));
        assertTrue(PlaylistParser.isHlsManifest(
                "https://example.com/stream",
                "application/vnd.apple.mpegurl",
                ""
        ));
        assertFalse(PlaylistParser.isPlaylistResponse(
                "https://example.com/stream.m3u8",
                "application/vnd.apple.mpegurl",
                "#EXTM3U"
        ));
    }

    @Test
    public void parsesPlsAndResolvesRelativeUrls() {
        assertEquals(List.of(
                "https://example.com/live",
                "https://example.com/backup"
        ), parser.parse("[playlist]\nFile1=/live\nFile2=/backup", "https://example.com/playlist.pls"));
    }

    @Test
    public void parsesXspfAndAsxUrls() {
        assertEquals(List.of("https://example.com/live"), parser.parse(
                "<playlist><trackList><track><location>/live</location></track></trackList></playlist>",
                "https://example.com/playlist.xspf"
        ));
        assertEquals(List.of("https://example.com/live"), parser.parse(
                "<asx><entry><ref href=\"/live\"/></entry></asx>",
                "https://example.com/playlist.asx"
        ));
    }

    @Test
    public void parsesPlainTextUrlsAndIgnoresInvalidEntries() {
        assertEquals(List.of("https://example.com/live"), parser.parse(
                "not a URL\nhttps://example.com/live\nftp://example.com/unsupported",
                "https://example.com/stream"
        ));
    }
}
