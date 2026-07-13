package com.matijakljajic.freeairradio.data.remote.radiobrowser.serverselection;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class RadioBrowserServerSelectorTest {

    @Test
    public void configuredSelectorIsReadyImmediately() {
        RadioBrowserServerSelector selector = new RadioBrowserServerSelector(Arrays.asList(
                "https://mirror-one/"
        ));

        assertTrue(selector.awaitReady(10L));
        assertEquals("https://mirror-one/", selector.getSelectedBaseUrl());
    }

    @Test
    public void rotatesThroughConfiguredServers() {
        RadioBrowserServerSelector selector = new RadioBrowserServerSelector(Arrays.asList(
                "https://mirror-one/",
                "https://mirror-two/",
                "https://mirror-three/"
        ));

        assertEquals("https://mirror-one/", selector.getSelectedBaseUrl());
        assertTrue(selector.rotateToNextServer("https://mirror-one/"));
        assertEquals("https://mirror-two/", selector.getSelectedBaseUrl());

        selector.rememberSuccess("https://mirror-three/");
        assertEquals("https://mirror-three/", selector.getSelectedBaseUrl());
    }

    @Test
    public void startupBaseUrlsUseBootstrapWhenNoCacheOrPreferenceExists() {
        List<String> startupBaseUrls = RadioBrowserServerSelector.buildStartupBaseUrls(
                Collections.emptyList(),
                null
        );

        assertEquals(1, startupBaseUrls.size());
        assertEquals(
                RadioBrowserServerDirectory.getBootstrapBaseUrl(),
                startupBaseUrls.get(0)
        );
    }
}
