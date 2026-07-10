package com.matijakljajic.freeairradio.data.remote.radiobrowser.serverselection;

import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class RadioBrowserServerSelectorTest {

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
}
