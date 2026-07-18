package com.matijakljajic.freeairradio.data.local;

import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class LocalStationIdFactoryTest {

    @Test
    public void create_returnsUniqueLocalIds() {
        String firstId = LocalStationIdFactory.create();
        String secondId = LocalStationIdFactory.create();

        assertTrue(firstId.startsWith("LOCAL:"));
        assertTrue(secondId.startsWith("LOCAL:"));
        assertNotEquals(firstId, secondId);
    }
}
