package com.matijakljajic.freeairradio.data.remote.radiobrowser.serverselection;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class RadioBrowserServerDirectoryTest {

    @Test
    public void parseBaseUrlsReadsArrayOfServerObjects() {
        List<String> baseUrls = RadioBrowserServerDirectory.parseBaseUrls("["
                + "{\"name\":\"de1.api.radio-browser.info\"},"
                + "{\"url\":\"https://nl1.api.radio-browser.info/\"},"
                + "{\"base_url\":\"fr1.api.radio-browser.info\"}"
                + "]");

        assertEquals(3, baseUrls.size());
        assertEquals("https://de1.api.radio-browser.info/", baseUrls.get(0));
        assertEquals("https://nl1.api.radio-browser.info/", baseUrls.get(1));
        assertEquals("https://fr1.api.radio-browser.info/", baseUrls.get(2));
    }
}
