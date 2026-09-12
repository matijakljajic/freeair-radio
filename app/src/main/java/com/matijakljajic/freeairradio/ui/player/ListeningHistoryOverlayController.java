package com.matijakljajic.freeairradio.ui.player;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.graphics.Rect;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.matijakljajic.freeairradio.ui.history.ListeningHistoryFragment;
import com.matijakljajic.freeairradio.R;
import com.matijakljajic.freeairradio.ui.util.UiDimensions;

public final class ListeningHistoryOverlayController {

    private static final long OVERLAY_FADE_DURATION_MS = 220L;
    private static final long PLAYER_MOVE_DURATION_MS = 260L;

    @NonNull
    private final AppCompatActivity activity;
    @NonNull
    private final View rootView;
    @NonNull
    private final View overlayContainerView;
    @NonNull
    private final View overlayScrimView;
    @NonNull
    private final View collapsedPlayerContainerView;
    @NonNull
    private final View expandedPlayerContainerView;
    @NonNull
    private final View listeningHistoryContainerView;
    private final int expandedPlayerBaseTopMarginPx;
    @NonNull
    private final OnBackPressedCallback backPressedCallback;
    @NonNull
    private final View.OnLayoutChangeListener expandedPlayerLayoutChangeListener;
    @NonNull
    private final View.OnLayoutChangeListener collapsedPlayerLayoutChangeListener;
    @NonNull
    private final View.OnLayoutChangeListener rootLayoutChangeListener;
    @NonNull
    private final Rect listeningHistoryClipBounds = new Rect();
    @Nullable
    private ValueAnimator playerRevealAnimator;
    private boolean overlayVisible;

    public ListeningHistoryOverlayController(@NonNull AppCompatActivity activity,
                                          @NonNull View rootView,
                                          @NonNull View overlayContainerView,
                                          @NonNull View overlayScrimView,
                                          @NonNull View collapsedPlayerContainerView,
                                          @NonNull View expandedPlayerContainerView,
                                          @NonNull View listeningHistoryContainerView) {
        this.activity = activity;
        this.rootView = rootView;
        this.overlayContainerView = overlayContainerView;
        this.overlayScrimView = overlayScrimView;
        this.collapsedPlayerContainerView = collapsedPlayerContainerView;
        this.expandedPlayerContainerView = expandedPlayerContainerView;
        this.listeningHistoryContainerView = listeningHistoryContainerView;
        this.expandedPlayerBaseTopMarginPx = getTopMargin(expandedPlayerContainerView);
        this.backPressedCallback = new OnBackPressedCallback(false) {
            @Override
            public void handleOnBackPressed() {
                close();
            }
        };
        this.expandedPlayerLayoutChangeListener =
                (view, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) ->
                        updateListeningHistoryBounds();
        this.collapsedPlayerLayoutChangeListener =
                (view, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) ->
                        updateListeningHistoryBounds();
        this.rootLayoutChangeListener =
                (view, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) ->
                        updateListeningHistoryBounds();
    }

    public void attach() {
        activity.getOnBackPressedDispatcher().addCallback(activity, backPressedCallback);
        rootView.addOnLayoutChangeListener(rootLayoutChangeListener);
        expandedPlayerContainerView.addOnLayoutChangeListener(expandedPlayerLayoutChangeListener);
        collapsedPlayerContainerView.addOnLayoutChangeListener(collapsedPlayerLayoutChangeListener);
        overlayScrimView.setOnClickListener(v -> close());
        overlayContainerView.setVisibility(View.INVISIBLE);
        scheduleListeningHistoryBoundsUpdate();
    }

    public void detach() {
        backPressedCallback.remove();
        rootView.removeOnLayoutChangeListener(rootLayoutChangeListener);
        expandedPlayerContainerView.removeOnLayoutChangeListener(expandedPlayerLayoutChangeListener);
        collapsedPlayerContainerView.removeOnLayoutChangeListener(collapsedPlayerLayoutChangeListener);
        overlayScrimView.setOnClickListener(null);
        restoreCollapsedPlayer();
        hideOverlayImmediately();
    }

    public boolean isOverlayVisible() {
        return overlayVisible;
    }

    public void open() {
        if (overlayVisible) {
            return;
        }

        resetListeningHistoryState();
        configureExpandedPlayerPosition();
        updateListeningHistoryBounds();
        scheduleListeningHistoryBoundsUpdate();
        int startTranslationY = calculateStartTranslationY();
        overlayVisible = true;
        backPressedCallback.setEnabled(true);

        overlayContainerView.setVisibility(View.VISIBLE);
        overlayScrimView.animate().cancel();
        cancelPlayerRevealAnimator();

        overlayScrimView.setAlpha(0f);
        applyPlayerReveal(startTranslationY);
        collapsedPlayerContainerView.setAlpha(0f);

        overlayScrimView.animate()
                .alpha(1f)
                .setDuration(OVERLAY_FADE_DURATION_MS)
                .start();
        animatePlayerReveal(startTranslationY, 0f, true);
    }

    public void close() {
        if (!overlayVisible) {
            return;
        }

        int endTranslationY = calculateStartTranslationY();
        overlayVisible = false;
        backPressedCallback.setEnabled(false);

        overlayScrimView.animate().cancel();
        cancelPlayerRevealAnimator();

        overlayScrimView.animate()
                .alpha(0f)
                .setDuration(OVERLAY_FADE_DURATION_MS)
                .start();
        animatePlayerReveal(0f, endTranslationY, false);
    }

    private void configureExpandedPlayerPosition() {
        ViewGroup.LayoutParams layoutParams = expandedPlayerContainerView.getLayoutParams();
        if (!(layoutParams instanceof ViewGroup.MarginLayoutParams)) {
            return;
        }

        ViewGroup.MarginLayoutParams marginLayoutParams = (ViewGroup.MarginLayoutParams) layoutParams;
        int desiredTopMargin = getStatusBarInsetTop() + expandedPlayerBaseTopMarginPx;
        if (marginLayoutParams.topMargin == desiredTopMargin) {
            return;
        }

        marginLayoutParams.topMargin = desiredTopMargin;
        expandedPlayerContainerView.setLayoutParams(marginLayoutParams);
    }

    private void updateListeningHistoryBounds() {
        ViewGroup.LayoutParams layoutParams = listeningHistoryContainerView.getLayoutParams();
        if (!(layoutParams instanceof ViewGroup.MarginLayoutParams)) {
            return;
        }

        ViewGroup.MarginLayoutParams marginLayoutParams = (ViewGroup.MarginLayoutParams) layoutParams;
        int desiredTopInset = getListeningHistoryTopInset();
        int desiredBottomMargin = Math.max(0, rootView.getHeight() - getCollapsedPlayerBottomInRoot());
        if (marginLayoutParams.topMargin == 0
                && marginLayoutParams.bottomMargin == desiredBottomMargin) {
            applyListeningHistoryContentInsets(desiredTopInset);
            return;
        }

        marginLayoutParams.topMargin = 0;
        marginLayoutParams.bottomMargin = desiredBottomMargin;
        listeningHistoryContainerView.setLayoutParams(marginLayoutParams);
        applyListeningHistoryContentInsets(desiredTopInset);
    }

    private int calculateStartTranslationY() {
        int[] collapsedLocation = new int[2];
        int[] expandedLocation = new int[2];
        collapsedPlayerContainerView.getLocationOnScreen(collapsedLocation);
        expandedPlayerContainerView.getLocationOnScreen(expandedLocation);
        return collapsedLocation[1] - expandedLocation[1];
    }

    private int getExpandedPlayerTop() {
        return getTopMargin(expandedPlayerContainerView);
    }

    private int getTopMargin(@NonNull View view) {
        ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
        return layoutParams instanceof ViewGroup.MarginLayoutParams
                ? ((ViewGroup.MarginLayoutParams) layoutParams).topMargin
                : 0;
    }

    private int getCollapsedPlayerBottomInRoot() {
        int[] rootLocation = new int[2];
        int[] collapsedLocation = new int[2];
        rootView.getLocationOnScreen(rootLocation);
        collapsedPlayerContainerView.getLocationOnScreen(collapsedLocation);
        return collapsedLocation[1] - rootLocation[1] + collapsedPlayerContainerView.getHeight();
    }

    private int getStatusBarInsetTop() {
        WindowInsetsCompat windowInsets = ViewCompat.getRootWindowInsets(rootView);
        if (windowInsets == null) {
            return 0;
        }
        Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
        return insets.top;
    }

    private void restoreCollapsedPlayer() {
        collapsedPlayerContainerView.animate().cancel();
        collapsedPlayerContainerView.setAlpha(1f);
    }

    private void animatePlayerReveal(float startTranslationY,
                                     float endTranslationY,
                                     boolean opening) {
        ValueAnimator animator = ValueAnimator.ofFloat(startTranslationY, endTranslationY);
        animator.setDuration(PLAYER_MOVE_DURATION_MS);
        animator.addUpdateListener(animation ->
                applyPlayerReveal((float) animation.getAnimatedValue()));
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(@NonNull Animator animation) {
                if (playerRevealAnimator != animation) {
                    return;
                }
                playerRevealAnimator = null;
                if (opening) {
                    applyPlayerReveal(0f);
                    return;
                }
                restoreCollapsedPlayer();
                hideOverlayImmediately();
                resetListeningHistoryState();
            }
        });
        playerRevealAnimator = animator;
        animator.start();
    }

    private void cancelPlayerRevealAnimator() {
        if (playerRevealAnimator != null) {
            playerRevealAnimator.cancel();
            playerRevealAnimator = null;
        }
    }

    private void applyPlayerReveal(float translationY) {
        expandedPlayerContainerView.setTranslationY(translationY);
        applyListeningHistoryClip(translationY);
    }

    private void applyListeningHistoryClip(float translationY) {
        int width = listeningHistoryContainerView.getWidth();
        int height = listeningHistoryContainerView.getHeight();
        if (width <= 0 || height <= 0) {
            return;
        }

        int clipTop = Math.min(
                height,
                Math.max(
                        0,
                        Math.round(getListeningHistoryClipTop() + translationY)
                )
        );
        if (clipTop == 0) {
            listeningHistoryContainerView.setClipBounds(null);
            return;
        }

        listeningHistoryClipBounds.set(0, clipTop, width, height);
        listeningHistoryContainerView.setClipBounds(listeningHistoryClipBounds);
    }

    private void hideOverlayImmediately() {
        overlayContainerView.setVisibility(View.INVISIBLE);
        overlayScrimView.setAlpha(1f);
        listeningHistoryContainerView.setClipBounds(null);
        expandedPlayerContainerView.setTranslationY(0f);
    }

    private int getListeningHistoryTopInset() {
        return getExpandedPlayerTop()
                + expandedPlayerContainerView.getHeight()
                + UiDimensions.px(rootView.getContext(), R.dimen.player_history_overlay_gap);
    }

    private int getListeningHistoryClipTop() {
        return getExpandedPlayerTop() + (expandedPlayerContainerView.getHeight() / 2);
    }

    private void applyListeningHistoryContentInsets(int topInset) {
        ListeningHistoryFragment fragment = findListeningHistoryFragment();
        if (fragment != null) {
            fragment.setContentTopInsetPx(topInset);
        }
    }

    private void scheduleListeningHistoryBoundsUpdate() {
        listeningHistoryContainerView.post(this::updateListeningHistoryBounds);
    }

    @Nullable
    private ListeningHistoryFragment findListeningHistoryFragment() {
        Fragment fragment = activity.getSupportFragmentManager()
                .findFragmentById(R.id.listening_history_fragment_container);
        return fragment instanceof ListeningHistoryFragment
                ? (ListeningHistoryFragment) fragment
                : null;
    }

    private void resetListeningHistoryState() {
        ListeningHistoryFragment fragment = findListeningHistoryFragment();
        if (fragment != null) {
            fragment.resetToInitialState();
        }
    }
}
