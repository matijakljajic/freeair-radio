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

import com.matijakljajic.freeairradio.ui.history.RecentlyListenedFragment;
import com.matijakljajic.freeairradio.R;
import com.matijakljajic.freeairradio.ui.util.UiDimensions;

public final class PlayerOverlayController {

    private static final long OVERLAY_FADE_DURATION_MS = 220L;
    private static final long PLAYER_MOVE_DURATION_MS = 260L;

    @NonNull
    private final AppCompatActivity activity;
    @NonNull
    private final View rootView;
    @Nullable
    private final View overlayContainerView;
    @Nullable
    private final View overlayScrimView;
    @Nullable
    private final View collapsedPlayerContainerView;
    @Nullable
    private final View expandedPlayerContainerView;
    @Nullable
    private final View recentHistoryContainerView;
    @Nullable
    private final View recentHistoryBottomFadeView;
    @NonNull
    private final OnBackPressedCallback backPressedCallback;
    @NonNull
    private final View.OnLayoutChangeListener expandedPlayerLayoutChangeListener;
    @NonNull
    private final View.OnLayoutChangeListener collapsedPlayerLayoutChangeListener;
    @NonNull
    private final Rect recentHistoryClipBounds = new Rect();
    @Nullable
    private ValueAnimator playerRevealAnimator;
    private boolean overlayVisible;

    public PlayerOverlayController(@NonNull AppCompatActivity activity,
                                   @NonNull View rootView,
                                   @Nullable View overlayContainerView,
                                   @Nullable View overlayScrimView,
                                   @Nullable View collapsedPlayerContainerView,
                                   @Nullable View expandedPlayerContainerView,
                                   @Nullable View recentHistoryContainerView,
                                   @Nullable View recentHistoryBottomFadeView) {
        this.activity = activity;
        this.rootView = rootView;
        this.overlayContainerView = overlayContainerView;
        this.overlayScrimView = overlayScrimView;
        this.collapsedPlayerContainerView = collapsedPlayerContainerView;
        this.expandedPlayerContainerView = expandedPlayerContainerView;
        this.recentHistoryContainerView = recentHistoryContainerView;
        this.recentHistoryBottomFadeView = recentHistoryBottomFadeView;
        this.backPressedCallback = new OnBackPressedCallback(false) {
            @Override
            public void handleOnBackPressed() {
                close();
            }
        };
        this.expandedPlayerLayoutChangeListener =
                (view, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) ->
                        updateRecentHistoryBounds();
        this.collapsedPlayerLayoutChangeListener =
                (view, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) ->
                        updateRecentHistoryBounds();
    }

    public void attach() {
        activity.getOnBackPressedDispatcher().addCallback(activity, backPressedCallback);
        if (expandedPlayerContainerView != null) {
            expandedPlayerContainerView.addOnLayoutChangeListener(expandedPlayerLayoutChangeListener);
        }
        if (collapsedPlayerContainerView != null) {
            collapsedPlayerContainerView.addOnLayoutChangeListener(collapsedPlayerLayoutChangeListener);
        }
        if (overlayScrimView != null) {
            overlayScrimView.setOnClickListener(v -> close());
        }
        if (overlayContainerView != null) {
            overlayContainerView.setVisibility(View.INVISIBLE);
        }
        scheduleRecentHistoryBoundsUpdate();
    }

    public void detach() {
        backPressedCallback.remove();
        if (expandedPlayerContainerView != null) {
            expandedPlayerContainerView.removeOnLayoutChangeListener(expandedPlayerLayoutChangeListener);
        }
        if (collapsedPlayerContainerView != null) {
            collapsedPlayerContainerView.removeOnLayoutChangeListener(collapsedPlayerLayoutChangeListener);
        }
        if (overlayScrimView != null) {
            overlayScrimView.setOnClickListener(null);
        }
        restoreCollapsedPlayer();
        hideOverlayImmediately();
    }

    public boolean isOverlayVisible() {
        return overlayVisible;
    }

    public void open() {
        if (overlayVisible || !hasRequiredViews()) {
            return;
        }

        resetRecentHistoryState();
        configureExpandedPlayerPosition();
        updateRecentHistoryBounds();
        scheduleRecentHistoryBoundsUpdate();
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
        if (!overlayVisible || !hasRequiredViews()) {
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

    private boolean hasRequiredViews() {
        return overlayContainerView != null
                && overlayScrimView != null
                && collapsedPlayerContainerView != null
                && expandedPlayerContainerView != null
                && recentHistoryContainerView != null
                && recentHistoryBottomFadeView != null;
    }

    private void configureExpandedPlayerPosition() {
        if (expandedPlayerContainerView == null) {
            return;
        }

        ViewGroup.LayoutParams layoutParams = expandedPlayerContainerView.getLayoutParams();
        if (!(layoutParams instanceof ViewGroup.MarginLayoutParams)) {
            return;
        }

        ViewGroup.MarginLayoutParams marginLayoutParams = (ViewGroup.MarginLayoutParams) layoutParams;
        int desiredTopMargin = getStatusBarInsetTop()
                + UiDimensions.px(rootView.getContext(), R.dimen.top_content_gap);
        if (marginLayoutParams.topMargin == desiredTopMargin) {
            return;
        }

        marginLayoutParams.topMargin = desiredTopMargin;
        expandedPlayerContainerView.setLayoutParams(marginLayoutParams);
    }

    private void updateRecentHistoryBounds() {
        if (expandedPlayerContainerView == null
                || collapsedPlayerContainerView == null
                || recentHistoryContainerView == null) {
            return;
        }

        ViewGroup.LayoutParams layoutParams = recentHistoryContainerView.getLayoutParams();
        if (!(layoutParams instanceof ViewGroup.MarginLayoutParams)) {
            return;
        }

        ViewGroup.MarginLayoutParams marginLayoutParams = (ViewGroup.MarginLayoutParams) layoutParams;
        int desiredTopInset = getRecentHistoryTopInset();
        int desiredBottomMargin = Math.max(0, rootView.getHeight() - getCollapsedPlayerBottomInRoot());
        if (marginLayoutParams.topMargin == 0
                && marginLayoutParams.bottomMargin == desiredBottomMargin) {
            applyRecentHistoryBottomFadeHeight(desiredBottomMargin);
            applyRecentHistoryContentInsets(desiredTopInset);
            return;
        }

        marginLayoutParams.topMargin = 0;
        marginLayoutParams.bottomMargin = desiredBottomMargin;
        recentHistoryContainerView.setLayoutParams(marginLayoutParams);
        applyRecentHistoryBottomFadeHeight(desiredBottomMargin);
        applyRecentHistoryContentInsets(desiredTopInset);
    }

    private int calculateStartTranslationY() {
        if (collapsedPlayerContainerView == null || expandedPlayerContainerView == null) {
            return 0;
        }

        int[] collapsedLocation = new int[2];
        int[] expandedLocation = new int[2];
        collapsedPlayerContainerView.getLocationOnScreen(collapsedLocation);
        expandedPlayerContainerView.getLocationOnScreen(expandedLocation);
        return collapsedLocation[1] - expandedLocation[1];
    }

    private int getExpandedPlayerTop() {
        if (expandedPlayerContainerView == null) {
            return 0;
        }

        ViewGroup.LayoutParams layoutParams = expandedPlayerContainerView.getLayoutParams();
        if (!(layoutParams instanceof ViewGroup.MarginLayoutParams)) {
            return 0;
        }
        return ((ViewGroup.MarginLayoutParams) layoutParams).topMargin;
    }

    private int getCollapsedPlayerBottomInRoot() {
        if (collapsedPlayerContainerView == null) {
            return rootView.getHeight();
        }

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
        if (collapsedPlayerContainerView == null) {
            return;
        }
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
                resetRecentHistoryState();
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
        if (expandedPlayerContainerView == null || recentHistoryContainerView == null) {
            return;
        }

        expandedPlayerContainerView.setTranslationY(translationY);
        applyRecentHistoryClip(translationY);
    }

    private void applyRecentHistoryClip(float translationY) {
        if (recentHistoryContainerView == null || expandedPlayerContainerView == null) {
            return;
        }

        int width = recentHistoryContainerView.getWidth();
        int height = recentHistoryContainerView.getHeight();
        if (width <= 0 || height <= 0) {
            return;
        }

        int clipTop = Math.min(
                height,
                Math.max(
                        0,
                        Math.round(getRecentHistoryClipTop() + translationY)
                )
        );
        if (clipTop == 0) {
            recentHistoryContainerView.setClipBounds(null);
            return;
        }

        recentHistoryClipBounds.set(0, clipTop, width, height);
        recentHistoryContainerView.setClipBounds(recentHistoryClipBounds);
    }

    private void hideOverlayImmediately() {
        if (overlayContainerView == null
                || overlayScrimView == null
                || recentHistoryContainerView == null
                || expandedPlayerContainerView == null
                || recentHistoryBottomFadeView == null) {
            return;
        }

        overlayContainerView.setVisibility(View.INVISIBLE);
        overlayScrimView.setAlpha(1f);
        recentHistoryContainerView.setClipBounds(null);
        expandedPlayerContainerView.setTranslationY(0f);
        applyRecentHistoryBottomFadeHeight(0);
    }

    private int getRecentHistoryTopInset() {
        if (expandedPlayerContainerView == null) {
            return 0;
        }

        return getExpandedPlayerTop()
                + expandedPlayerContainerView.getHeight()
                + UiDimensions.px(rootView.getContext(), R.dimen.player_overlay_history_gap);
    }

    private int getRecentHistoryClipTop() {
        if (expandedPlayerContainerView == null) {
            return 0;
        }

        return getExpandedPlayerTop() + (expandedPlayerContainerView.getHeight() / 2);
    }

    private void applyRecentHistoryContentInsets(int topInset) {
        if (recentHistoryContainerView == null) {
            return;
        }

        RecentlyListenedFragment fragment = findRecentlyListenedFragment();
        if (fragment != null) {
            fragment.setContentTopInsetPx(topInset);
        }

        View emptyView = recentHistoryContainerView.findViewById(R.id.recently_listened_empty_view);
        if (emptyView != null && emptyView.getPaddingTop() != topInset) {
            emptyView.setPadding(
                    emptyView.getPaddingLeft(),
                    topInset,
                    emptyView.getPaddingRight(),
                    emptyView.getPaddingBottom()
            );
        }
    }

    private void scheduleRecentHistoryBoundsUpdate() {
        if (recentHistoryContainerView == null) {
            return;
        }
        recentHistoryContainerView.post(this::updateRecentHistoryBounds);
    }

    private void applyRecentHistoryBottomFadeHeight(int bottomFadeHeightPx) {
        if (recentHistoryBottomFadeView == null) {
            return;
        }

        ViewGroup.LayoutParams layoutParams = recentHistoryBottomFadeView.getLayoutParams();
        if (layoutParams.height == bottomFadeHeightPx) {
            return;
        }

        layoutParams.height = bottomFadeHeightPx;
        recentHistoryBottomFadeView.setLayoutParams(layoutParams);
    }

    @Nullable
    private RecentlyListenedFragment findRecentlyListenedFragment() {
        if (activity.getSupportFragmentManager().findFragmentById(R.id.recently_listened_fragment_container)
                instanceof RecentlyListenedFragment) {
            return (RecentlyListenedFragment) activity.getSupportFragmentManager()
                    .findFragmentById(R.id.recently_listened_fragment_container);
        }
        return null;
    }

    private void resetRecentHistoryState() {
        RecentlyListenedFragment fragment = findRecentlyListenedFragment();
        if (fragment != null) {
            fragment.resetToInitialState();
        }
    }
}
