package com.matijakljajic.freeairradio.data.remote.radiobrowser.serverselection;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
public class RadioBrowserServerSelectorTest {

    @Test
    public void configuredSelectorExposesConfiguredServerImmediately() {
        RadioBrowserServerSelector selector = new RadioBrowserServerSelector(Arrays.asList(
                "https://mirror-one/"
        ));

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

    @Test
    public void refreshAndRotateSkipsFailedServerWhenDiscoveryFindsAlternatives() {
        RadioBrowserServerSelector selector = new RadioBrowserServerSelector(
                null,
                false,
                Collections.singletonList("https://mirror-one/"),
                () -> Arrays.asList(
                        "https://mirror-one/",
                        "https://mirror-two/"
                )
        );

        assertTrue(selector.refreshAndRotate("https://mirror-one/"));
        assertEquals("https://mirror-two/", selector.getSelectedBaseUrl());
    }

    @Test
    public void refreshAndRotateFailsWhenDiscoveryOnlyReturnsFailedServer() {
        RadioBrowserServerSelector selector = new RadioBrowserServerSelector(
                null,
                false,
                Collections.singletonList("https://mirror-one/"),
                () -> Collections.singletonList("https://mirror-one/")
        );

        assertEquals("https://mirror-one/", selector.getSelectedBaseUrl());
        org.junit.Assert.assertFalse(selector.refreshAndRotate("https://mirror-one/"));
        assertEquals("https://mirror-one/", selector.getSelectedBaseUrl());
    }
}
