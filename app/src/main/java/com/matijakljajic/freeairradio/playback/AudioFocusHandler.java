package com.matijakljajic.freeairradio.playback;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.matijakljajic.freeairradio.util.AppLog;

import java.util.Objects;

@SuppressWarnings("deprecation")
public final class AudioFocusHandler {

    private static final String TAG = "AudioFocusHandler";

    public interface Listener {
        void onAudioFocusGained();

        void onAudioFocusLostTemporarily();

        void onAudioFocusShouldDuck();

        void onAudioFocusLostPermanently();
    }

    @NonNull
    private final AudioManager audioManager;
    @NonNull
    private final Listener listener;
    @NonNull
    private final AudioManager.OnAudioFocusChangeListener focusChangeListener =
            this::handleAudioFocusChange;
    @Nullable
    private final AudioFocusRequest audioFocusRequest;

    public AudioFocusHandler(@NonNull Context context, @NonNull Listener listener) {
        this.listener = listener;
        audioManager = Objects.requireNonNull(
                (AudioManager) context.getApplicationContext().getSystemService(Context.AUDIO_SERVICE)
        );
        audioFocusRequest = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setOnAudioFocusChangeListener(focusChangeListener)
                .setWillPauseWhenDucked(false)
                .setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build())
                .build()
                : null;
    }

    public boolean requestFocus() {
        int result = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? audioManager.requestAudioFocus(audioFocusRequest)
                : requestLegacyFocus();
        AppLog.d(TAG, "requestFocus -> " + requestResultToString(result));
        return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
    }

    public void abandonFocus() {
        AppLog.d(TAG, "abandonFocus");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioManager.abandonAudioFocusRequest(audioFocusRequest);
            return;
        }
        audioManager.abandonAudioFocus(focusChangeListener);
    }

    @SuppressWarnings("deprecation")
    private int requestLegacyFocus() {
        return audioManager.requestAudioFocus(
                focusChangeListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
        );
    }

    private void handleAudioFocusChange(int focusChange) {
        AppLog.d(TAG, "onAudioFocusChange -> " + focusChangeToString(focusChange));
        switch (focusChange) {
            case AudioManager.AUDIOFOCUS_GAIN:
            case AudioManager.AUDIOFOCUS_GAIN_TRANSIENT:
            case AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK:
                listener.onAudioFocusGained();
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                listener.onAudioFocusLostTemporarily();
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                listener.onAudioFocusShouldDuck();
                break;
            case AudioManager.AUDIOFOCUS_LOSS:
                listener.onAudioFocusLostPermanently();
                break;
            default:
                break;
        }
    }

    @NonNull
    private static String requestResultToString(int requestResult) {
        switch (requestResult) {
            case AudioManager.AUDIOFOCUS_REQUEST_GRANTED:
                return "GRANTED";
            case AudioManager.AUDIOFOCUS_REQUEST_FAILED:
                return "FAILED";
            case AudioManager.AUDIOFOCUS_REQUEST_DELAYED:
                return "DELAYED";
            default:
                return "UNKNOWN(" + requestResult + ")";
        }
    }

    @NonNull
    private static String focusChangeToString(int focusChange) {
        switch (focusChange) {
            case AudioManager.AUDIOFOCUS_GAIN:
                return "GAIN";
            case AudioManager.AUDIOFOCUS_GAIN_TRANSIENT:
                return "GAIN_TRANSIENT";
            case AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK:
                return "GAIN_TRANSIENT_MAY_DUCK";
            case AudioManager.AUDIOFOCUS_LOSS:
                return "LOSS";
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                return "LOSS_TRANSIENT";
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                return "LOSS_TRANSIENT_CAN_DUCK";
            default:
                return "UNKNOWN(" + focusChange + ")";
        }
    }
}
