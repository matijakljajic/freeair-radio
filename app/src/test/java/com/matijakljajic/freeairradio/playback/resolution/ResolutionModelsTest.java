package com.matijakljajic.freeairradio.playback.resolution;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import org.junit.Test;

import java.util.List;

public class ResolutionModelsTest {

    @Test
    public void resolvedStreamCandidateExposesConstructedValues() {
        ResolvedStreamCandidate candidate = new ResolvedStreamCandidate(
                "https://example.com/live",
                "audio/mpeg",
                ResolvedStreamCandidate.StreamProtocol.CONTINUOUS_HTTP,
                true,
                false,
                ResolvedStreamCandidate.MetadataCapability.CONFIRMED_ICY,
                16000,
                "Station Name",
                "Station Description",
                "jazz",
                128,
                42,
                "Direct audio stream"
        );

        assertEquals("https://example.com/live", candidate.getUrl());
        assertEquals("audio/mpeg", candidate.getContentType());
        assertSame(ResolvedStreamCandidate.StreamProtocol.CONTINUOUS_HTTP, candidate.getProtocol());
        assertEquals(true, candidate.isPlayable());
        assertEquals(false, candidate.isHls());
        assertSame(ResolvedStreamCandidate.MetadataCapability.CONFIRMED_ICY, candidate.getMetadataCapability());
        assertEquals(Integer.valueOf(16000), candidate.getIcyMetaInterval());
        assertEquals("Station Name", candidate.getIcyName());
        assertEquals("Station Description", candidate.getIcyDescription());
        assertEquals("jazz", candidate.getIcyGenre());
        assertEquals(Integer.valueOf(128), candidate.getBitrateKbps());
        assertEquals(42, candidate.getPreferenceScore());
        assertEquals("Direct audio stream", candidate.getSelectionReason());
    }

    @Test
    public void resolutionResultExposesConstructedValues() {
        ResolvedStreamCandidate candidate = new ResolvedStreamCandidate(
                "https://example.com/live",
                null,
                ResolvedStreamCandidate.StreamProtocol.HLS,
                true,
                true,
                ResolvedStreamCandidate.MetadataCapability.HLS_TIMED_METADATA_POSSIBLE,
                null,
                null,
                null,
                null,
                null,
                12,
                "HLS manifest"
        );
        ResolutionResult result = new ResolutionResult(
                "https://example.com/original",
                candidate,
                List.of(candidate),
                List.of("https://example.com/original", "https://example.com/live"),
                ResolutionResult.ResolutionStatus.SUCCESS,
                null
        );

        assertEquals("https://example.com/original", result.getOriginalUrl());
        assertSame(candidate, result.getSelectedCandidate());
        assertEquals(List.of(candidate), result.getCandidates());
        assertEquals(List.of("https://example.com/original", "https://example.com/live"), result.getResolutionChain());
        assertSame(ResolutionResult.ResolutionStatus.SUCCESS, result.getStatus());
        assertNull(result.getFailureReason());
    }
}
