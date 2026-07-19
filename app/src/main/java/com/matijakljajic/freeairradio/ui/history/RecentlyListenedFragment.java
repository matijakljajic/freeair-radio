package com.matijakljajic.freeairradio.ui.history;

import android.os.Bundle;
import android.graphics.Rect;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.matijakljajic.freeairradio.R;
import com.matijakljajic.freeairradio.data.model.RecentlyListenedStation;
import com.matijakljajic.freeairradio.data.repository.LibraryRepository;

import java.util.List;

@SuppressWarnings("unused")
public final class RecentlyListenedFragment extends Fragment {

    @Nullable
    private LibraryRepository libraryRepository;
    @Nullable
    private RecyclerView recyclerView;
    @Nullable
    private TextView emptyView;
    @Nullable
    private View bottomFadeView;
    @Nullable
    private RecentlyListenedAdapter adapter;
    @NonNull
    private final FirstItemTopSpacingDecoration topSpacingDecoration = new FirstItemTopSpacingDecoration();
    private int contentTopInsetPx;
    @NonNull
    private final LibraryRepository.RecentlyListenedListener listener = this::refreshFromRepository;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_recently_listened, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        libraryRepository = LibraryRepository.getInstance(requireContext());
        recyclerView = view.findViewById(R.id.recently_listened_recycler_view);
        emptyView = view.findViewById(R.id.recently_listened_empty_view);
        bottomFadeView = view.findViewById(R.id.recently_listened_bottom_fade);
        adapter = new RecentlyListenedAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.addItemDecoration(topSpacingDecoration);
        recyclerView.setAdapter(adapter);
        applyContentTopInset();
        libraryRepository.addRecentlyListenedListener(listener);
        refreshFromRepository();
        if (!libraryRepository.hasLoadedRecentlyListened()) {
            libraryRepository.loadRecentlyListenedStations(new LibraryRepository.RecentlyListenedCallback() {
                @Override
                public void onRecentlyListenedLoaded(@NonNull List<RecentlyListenedStation> stations) {
                    renderStations(stations);
                }

                @Override
                public void onError(@NonNull Throwable throwable) {
                    renderStations(libraryRepository.getRecentlyListenedSnapshot());
                }
            });
        }
    }

    @Override
    public void onDestroyView() {
        if (libraryRepository != null) {
            libraryRepository.removeRecentlyListenedListener(listener);
        }
        libraryRepository = null;
        recyclerView = null;
        emptyView = null;
        bottomFadeView = null;
        adapter = null;
        super.onDestroyView();
    }

    public void setContentTopInsetPx(int contentTopInsetPx) {
        if (this.contentTopInsetPx == contentTopInsetPx) {
            return;
        }
        this.contentTopInsetPx = contentTopInsetPx;
        applyContentTopInset();
    }

    public void resetToInitialState() {
        if (adapter != null) {
            adapter.resetExpandedState();
        }
        if (recyclerView != null) {
            recyclerView.stopScroll();
            RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();
            if (layoutManager instanceof LinearLayoutManager) {
                ((LinearLayoutManager) layoutManager).scrollToPositionWithOffset(0, 0);
            } else {
                recyclerView.scrollToPosition(0);
            }
        }
    }

    private void refreshFromRepository() {
        if (libraryRepository == null) {
            return;
        }
        renderStations(libraryRepository.getRecentlyListenedSnapshot());
    }

    private void renderStations(@NonNull List<RecentlyListenedStation> stations) {
        boolean hasStations = !stations.isEmpty();
        if (adapter != null) {
            adapter.submitList(stations);
        }
        if (emptyView != null) {
            emptyView.setVisibility(hasStations ? View.GONE : View.VISIBLE);
        }
        if (bottomFadeView != null) {
            bottomFadeView.setVisibility(hasStations ? View.VISIBLE : View.GONE);
        }
    }

    private void applyContentTopInset() {
        topSpacingDecoration.setTopSpacingPx(contentTopInsetPx);
        if (recyclerView != null) {
            recyclerView.invalidateItemDecorations();
        }
        if (emptyView != null && emptyView.getPaddingTop() != contentTopInsetPx) {
            emptyView.setPadding(
                    emptyView.getPaddingLeft(),
                    contentTopInsetPx,
                    emptyView.getPaddingRight(),
                    emptyView.getPaddingBottom()
            );
        }
    }

    private static final class FirstItemTopSpacingDecoration extends RecyclerView.ItemDecoration {

        private int topSpacingPx;

        void setTopSpacingPx(int topSpacingPx) {
            this.topSpacingPx = topSpacingPx;
        }

        @Override
        public void getItemOffsets(@NonNull Rect outRect,
                                   @NonNull View view,
                                   @NonNull RecyclerView parent,
                                   @NonNull RecyclerView.State state) {
            outRect.setEmpty();
            if (parent.getChildAdapterPosition(view) == 0) {
                outRect.top = topSpacingPx;
            }
        }
    }
}
