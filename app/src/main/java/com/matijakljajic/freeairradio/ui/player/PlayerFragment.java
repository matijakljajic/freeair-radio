package com.matijakljajic.freeairradio.ui.player;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.matijakljajic.freeairradio.R;
import com.matijakljajic.freeairradio.data.model.Station;
import com.matijakljajic.freeairradio.playback.RadioPlayer;
import com.matijakljajic.freeairradio.playback.metadata.CurrentPlaybackState;
import com.matijakljajic.freeairradio.playback.metadata.NowPlaying;
import com.matijakljajic.freeairradio.ui.util.MarqueeTextView;
import com.matijakljajic.freeairradio.ui.util.StationFaviconLoader;
import com.matijakljajic.freeairradio.ui.util.UiDimensions;

@SuppressWarnings("unused")
public class PlayerFragment extends Fragment {
    private static final long MARQUEE_START_DELAY_MS = 1000L;
    private static final long METADATA_ANIMATION_DURATION_MS = 180L;

    @Nullable
    private Station selectedStation;
    @Nullable
    private Station currentStation;
    @Nullable
    private String renderedStationTitle;
    @Nullable
    private String renderedNowPlayingText;
    private boolean nowPlayingVisible;
    private boolean pendingUserStopAnimation;
    @NonNull
    private CurrentPlaybackState.PlaybackStatus currentPlaybackStatus = CurrentPlaybackState.PlaybackStatus.IDLE;
    private float singleLineTitleOffsetPx;
    private int metadataShiftPx;
    private View playerView;
    private ImageView stationFaviconView;
    private MarqueeTextView stationNameText;
    private MarqueeTextView nowPlayingText;
    private MaterialButton playStopButton;
    private CircularProgressIndicator playStopLoadingIndicator;
    @Nullable
    private RadioPlayer radioPlayer;
    private final CurrentPlaybackState.Listener playbackStateListener = this::renderPlaybackState;
    private final Runnable activateMarqueeRunnable = this::activateMiniPlayerMarquee;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_player, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        playerView = view;
        stationFaviconView = view.findViewById(R.id.player_station_favicon);
        stationNameText = view.findViewById(R.id.player_station_name);
        nowPlayingText = view.findViewById(R.id.player_now_playing);
        playStopButton = view.findViewById(R.id.player_play_stop_button);
        playStopLoadingIndicator = view.findViewById(R.id.player_play_stop_loading_indicator);
        playStopButton.setOnClickListener(v -> onPlayStopClicked());
        radioPlayer = new RadioPlayer(requireContext());
        metadataShiftPx = UiDimensions.px(requireContext(), R.dimen.mini_player_metadata_shift);
        singleLineTitleOffsetPx = calculateSingleLineTitleOffset();
        resetMetadataLayout();
        CurrentPlaybackState.getInstance().addListener(playbackStateListener);
    }

    public void showStation(@Nullable Station station) {
        selectedStation = station;
        CurrentPlaybackState playbackState = CurrentPlaybackState.getInstance();
        renderPlaybackState(
                playbackState.getCurrentStation(),
                playbackState.getCurrentNowPlaying(),
                playbackState.getPlaybackStatus()
        );
    }

    private void renderPlaybackState(@Nullable Station playbackStation,
                                     @Nullable NowPlaying nowPlaying,
                                     @NonNull CurrentPlaybackState.PlaybackStatus playbackStatus) {
        currentStation = playbackStation;
        currentPlaybackStatus = playbackStatus;
        if (playbackStation != null) {
            selectedStation = playbackStation;
        }
        if (stationNameText == null || nowPlayingText == null || playStopButton == null) {
            return;
        }

        Station stationToShow = getDisplayedStation();
        boolean stationChanged = renderStation(stationToShow);
        boolean nowPlayingChanged = renderNowPlaying(stationToShow, nowPlaying);
        if (stationChanged || nowPlayingChanged) {
            scheduleMiniPlayerMarquee();
        }

        renderPlayStopButton(stationToShow);
    }

    private boolean renderStation(@Nullable Station station) {
        renderStationFavicon(station);
        if (stationNameText == null) {
            return false;
        }

        String desiredTitle = station == null
                ? getString(R.string.player_no_station_selected)
                : station.getName();
        if (TextUtils.equals(renderedStationTitle, desiredTitle)) {
            return false;
        }

        renderedStationTitle = desiredTitle;
        stationNameText.setText(desiredTitle);
        return true;
    }

    private void renderStationFavicon(@Nullable Station station) {
        if (stationFaviconView == null) {
            return;
        }
        if (station == null) {
            stationFaviconView.setVisibility(View.GONE);
            StationFaviconLoader.clear(stationFaviconView);
            return;
        }

        stationFaviconView.setVisibility(View.VISIBLE);
        StationFaviconLoader.loadInto(stationFaviconView, station);
    }

    @Override
    public void onDestroyView() {
        if (playerView != null) {
            playerView.removeCallbacks(activateMarqueeRunnable);
        }
        CurrentPlaybackState.getInstance().removeListener(playbackStateListener);
        selectedStation = null;
        currentStation = null;
        renderedStationTitle = null;
        renderedNowPlayingText = null;
        nowPlayingVisible = false;
        pendingUserStopAnimation = false;
        currentPlaybackStatus = CurrentPlaybackState.PlaybackStatus.IDLE;
        if (playStopButton != null) {
            playStopButton.setOnClickListener(null);
        }
        radioPlayer = null;
        playerView = null;
        stationFaviconView = null;
        stationNameText = null;
        nowPlayingText = null;
        playStopButton = null;
        playStopLoadingIndicator = null;
        super.onDestroyView();
    }

    private boolean renderNowPlaying(@Nullable Station stationToShow, @Nullable NowPlaying nowPlaying) {
        if (nowPlayingText == null) {
            return false;
        }

        String displayText = resolveNowPlayingText(stationToShow, nowPlaying);

        if (displayText == null) {
            if (!nowPlayingVisible && renderedNowPlayingText == null) {
                pendingUserStopAnimation = false;
                return false;
            }
            if (pendingUserStopAnimation && nowPlayingVisible) {
                pendingUserStopAnimation = false;
                animateMetadataOut();
            } else {
                pendingUserStopAnimation = false;
                renderedNowPlayingText = null;
                resetMetadataLayout();
            }
            return true;
        }

        boolean textChanged = !TextUtils.equals(renderedNowPlayingText, displayText);
        renderedNowPlayingText = displayText;

        if (textChanged) {
            nowPlayingText.setText(displayText);
        }

        if (nowPlayingVisible) {
            return textChanged;
        }

        animateMetadataIn();
        return true;
    }

    private void animateMetadataIn() {
        if (stationNameText == null || nowPlayingText == null) {
            return;
        }

        nowPlayingVisible = true;
        stationNameText.animate().cancel();
        nowPlayingText.animate().cancel();
        nowPlayingText.setVisibility(View.VISIBLE);
        nowPlayingText.setAlpha(0f);
        nowPlayingText.setTranslationY(-metadataShiftPx);
        stationNameText.setTranslationY(singleLineTitleOffsetPx);

        stationNameText.animate()
                .translationY(0f)
                .setDuration(METADATA_ANIMATION_DURATION_MS)
                .start();
        nowPlayingText.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(METADATA_ANIMATION_DURATION_MS)
                .start();
    }

    private void resetMetadataLayout() {
        if (stationNameText == null || nowPlayingText == null) {
            return;
        }

        stationNameText.animate().cancel();
        nowPlayingText.animate().cancel();
        stationNameText.setTranslationY(singleLineTitleOffsetPx);
        nowPlayingText.setAlpha(0f);
        nowPlayingText.setTranslationY(-metadataShiftPx / 2f);
        nowPlayingText.setText(null);
        nowPlayingText.setVisibility(View.INVISIBLE);
        nowPlayingVisible = false;
    }

    private void animateMetadataOut() {
        if (stationNameText == null || nowPlayingText == null) {
            return;
        }

        stationNameText.animate().cancel();
        nowPlayingText.animate().cancel();

        stationNameText.animate()
                .translationY(singleLineTitleOffsetPx)
                .setDuration(METADATA_ANIMATION_DURATION_MS)
                .start();
        nowPlayingText.animate()
                .alpha(0f)
                .translationY(-metadataShiftPx / 2f)
                .setDuration(METADATA_ANIMATION_DURATION_MS)
                .withEndAction(() -> {
                    if (nowPlayingText == null) {
                        return;
                    }
                    renderedNowPlayingText = null;
                    nowPlayingText.setText(null);
                    nowPlayingText.setVisibility(View.INVISIBLE);
                    nowPlayingVisible = false;
                })
                .start();
    }

    private void scheduleMiniPlayerMarquee() {
        if (playerView == null) {
            return;
        }

        playerView.removeCallbacks(activateMarqueeRunnable);
        stationNameText.setSelected(false);
        nowPlayingText.setSelected(false);
        playerView.postDelayed(activateMarqueeRunnable, MARQUEE_START_DELAY_MS);
    }

    private void activateMiniPlayerMarquee() {
        if (stationNameText == null || nowPlayingText == null || playerView == null) {
            return;
        }
        stationNameText.setSelected(true);
        nowPlayingText.setSelected(nowPlayingText.getVisibility() == View.VISIBLE);
    }

    private void renderPlayStopButton(@Nullable Station stationToShow) {
        if (playStopButton == null) {
            return;
        }

        boolean hasStation = stationToShow != null;
        boolean isLoading = isStopAction(stationToShow)
                && currentPlaybackStatus == CurrentPlaybackState.PlaybackStatus.CONNECTING;
        boolean canStop = isStopAction(stationToShow);

        playStopButton.setEnabled(hasStation);
        playStopButton.setIconResource(canStop ? R.drawable.ic_stop : R.drawable.ic_play);
        playStopButton.setContentDescription(getString(
                canStop ? R.string.player_stop_button : R.string.player_play_button
        ));
        if (playStopLoadingIndicator != null) {
            playStopLoadingIndicator.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        }
    }

    private void onPlayStopClicked() {
        Station stationToShow = getDisplayedStation();
        if (stationToShow == null || radioPlayer == null) {
            return;
        }

        if (isStopAction(stationToShow)) {
            pendingUserStopAnimation = true;
            radioPlayer.stop();
            return;
        }

        if (isCurrentStation(stationToShow)
                && currentPlaybackStatus == CurrentPlaybackState.PlaybackStatus.PAUSED) {
            pendingUserStopAnimation = false;
            radioPlayer.resume();
            return;
        }

        pendingUserStopAnimation = false;
        radioPlayer.play(stationToShow);
    }

    @Nullable
    private Station getDisplayedStation() {
        return selectedStation;
    }

    @Nullable
    private String resolveNowPlayingText(@Nullable Station stationToShow, @Nullable NowPlaying nowPlaying) {
        if (nowPlaying == null || !isCurrentStation(stationToShow)) {
            return null;
        }
        return nowPlaying.buildDisplayText();
    }

    private float calculateSingleLineTitleOffset() {
        if (nowPlayingText == null) {
            return 0f;
        }

        ViewGroup.LayoutParams layoutParams = nowPlayingText.getLayoutParams();
        int topMarginPx = 0;
        if (layoutParams instanceof ViewGroup.MarginLayoutParams) {
            topMarginPx = ((ViewGroup.MarginLayoutParams) layoutParams).topMargin;
        }
        return (nowPlayingText.getLineHeight() + topMarginPx) / 2f;
    }

    private boolean isCurrentStation(@Nullable Station station) {
        return station != null
                && currentStation != null
                && currentStation.getId().equals(station.getId());
    }

    private boolean isStopAction(@Nullable Station station) {
        if (!isCurrentStation(station)) {
            return false;
        }
        return currentPlaybackStatus == CurrentPlaybackState.PlaybackStatus.CONNECTING
                || currentPlaybackStatus == CurrentPlaybackState.PlaybackStatus.PLAYING;
    }
}
