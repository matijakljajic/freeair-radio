package com.matijakljajic.freeairradio.sensor;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import androidx.annotation.NonNull;

public final class ShakeDetector implements SensorEventListener {

    private static final float SHAKE_THRESHOLD_G = 3f;
    private static final int REQUIRED_SHAKE_COUNT = 3;
    private static final long MIN_SHAKE_INTERVAL_MS = 450L;
    private static final long SHAKE_SEQUENCE_TIMEOUT_MS = 4_000L;

    public interface Listener {
        void onShake();
    }

    @NonNull
    private final Listener listener;
    private int shakeCount;
    private long firstShakeAtMs;
    private long lastShakeAtMs;

    public ShakeDetector(@NonNull Listener listener) {
        this.listener = listener;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() != Sensor.TYPE_ACCELEROMETER || event.values.length < 3) {
            return;
        }

        if (handleAcceleration(
                event.values[0],
                event.values[1],
                event.values[2],
                System.currentTimeMillis()
        )) {
            listener.onShake();
        }
    }

    boolean handleAcceleration(float x, float y, float z, long eventTimeMs) {
        float gX = x / SensorManager.GRAVITY_EARTH;
        float gY = y / SensorManager.GRAVITY_EARTH;
        float gZ = z / SensorManager.GRAVITY_EARTH;
        float gForce = (float) Math.sqrt(gX * gX + gY * gY + gZ * gZ);
        if (gForce < SHAKE_THRESHOLD_G) {
            return false;
        }

        if (lastShakeAtMs > 0L && eventTimeMs - lastShakeAtMs < MIN_SHAKE_INTERVAL_MS) {
            return false;
        }

        if (firstShakeAtMs <= 0L || eventTimeMs - firstShakeAtMs > SHAKE_SEQUENCE_TIMEOUT_MS) {
            firstShakeAtMs = eventTimeMs;
            lastShakeAtMs = eventTimeMs;
            shakeCount = 1;
            return false;
        }

        lastShakeAtMs = eventTimeMs;
        shakeCount++;
        if (shakeCount < REQUIRED_SHAKE_COUNT) {
            return false;
        }

        resetSequence();
        return true;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    private void resetSequence() {
        shakeCount = 0;
        firstShakeAtMs = 0L;
        lastShakeAtMs = 0L;
    }
}
