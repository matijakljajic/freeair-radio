package com.matijakljajic.freeairradio.sensor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ShakeDetectorTest {

    @Test
    public void handleAcceleration_doesNotTriggerBelowThreshold() {
        CountingListener listener = new CountingListener();
        ShakeDetector detector = new ShakeDetector(listener);

        boolean triggered = detector.handleAcceleration(5f, 5f, 5f, 1_000L);

        assertFalse(triggered);
        assertEquals(0, listener.count);
    }

    @Test
    public void handleAcceleration_requiresThreeShakesToTrigger() {
        CountingListener listener = new CountingListener();
        ShakeDetector detector = new ShakeDetector(listener);

        assertFalse(detector.handleAcceleration(24f, 0f, 0f, 1_000L));
        assertFalse(detector.handleAcceleration(24f, 0f, 0f, 1_600L));
        assertTrue(detector.handleAcceleration(24f, 0f, 0f, 2_200L));
        assertEquals(0, listener.count);
    }

    @Test
    public void handleAcceleration_debouncesDuplicateMotion() {
        CountingListener listener = new CountingListener();
        ShakeDetector detector = new ShakeDetector(listener);

        assertFalse(detector.handleAcceleration(24f, 0f, 0f, 1_000L));
        assertFalse(detector.handleAcceleration(24f, 0f, 0f, 1_200L));
        assertFalse(detector.handleAcceleration(24f, 0f, 0f, 1_700L));
        assertTrue(detector.handleAcceleration(24f, 0f, 0f, 2_300L));
    }

    @Test
    public void handleAcceleration_resetsSequenceAfterTimeout() {
        CountingListener listener = new CountingListener();
        ShakeDetector detector = new ShakeDetector(listener);

        assertFalse(detector.handleAcceleration(24f, 0f, 0f, 1_000L));
        assertFalse(detector.handleAcceleration(24f, 0f, 0f, 1_600L));
        assertFalse(detector.handleAcceleration(24f, 0f, 0f, 5_500L));
        assertFalse(detector.handleAcceleration(24f, 0f, 0f, 6_100L));
        assertTrue(detector.handleAcceleration(24f, 0f, 0f, 6_700L));
    }

    private static final class CountingListener implements ShakeDetector.Listener {
        private int count;

        @Override
        public void onShake() {
            count++;
        }
    }
}
