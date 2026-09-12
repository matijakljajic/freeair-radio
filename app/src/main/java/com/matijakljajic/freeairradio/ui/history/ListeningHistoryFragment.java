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
import com.matijakljajic.freeairradio.data.model.ListeningHistoryEntry;
import com.matijakljajic.freeairradio.data.repository.LibraryRepository;

import java.util.List;

@SuppressWarnings("unused")
public final class ListeningHistoryFragment extends Fragment {

    @Nullable
    private LibraryRepository libraryRepository;
    @Nullable
    private RecyclerView recyclerView;
    @Nullable
    private TextView emptyView;
    @Nullable
    private View bottomFadeView;
    @Nullable
    private ListeningHistoryAdapter adapter;
    @NonNull
    private final FirstItemTopSpacingDecoration topSpacingDecoration = new FirstItemTopSpacingDecoration();
    private int contentTopInsetPx;
    @NonNull
    private final LibraryRepository.ListeningHistoryListener listener = this::refreshFromRepository;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_listening_history, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        libraryRepository = LibraryRepository.getInstance(requireContext());
        recyclerView = view.findViewById(R.id.listening_history_recycler_view);
        emptyView = view.findViewById(R.id.listening_history_empty_view);
        adapter = new ListeningHistoryAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.addItemDecoration(topSpacingDecoration);
        recyclerView.setAdapter(adapter);
        applyContentTopInset();
        libraryRepository.addListeningHistoryListener(listener);
        refreshFromRepository();
        if (!libraryRepository.hasLoadedListeningHistory()) {
            libraryRepository.loadListeningHistoryEntries(new LibraryRepository.ListeningHistoryCallback() {
                @Override
                public void onListeningHistoryLoaded(@NonNull List<ListeningHistoryEntry> entries) {
                    renderEntries(entries);
                }

                @Override
                public void onError(@NonNull Throwable throwable) {
                    renderEntries(libraryRepository.getListeningHistorySnapshot());
                }
            });
        }
    }

    @Override
    public void onDestroyView() {
        if (libraryRepository != null) {
            libraryRepository.removeListeningHistoryListener(listener);
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
        renderEntries(libraryRepository.getListeningHistorySnapshot());
    }

    private void renderEntries(@NonNull List<ListeningHistoryEntry> entries) {
        boolean hasEntries = !entries.isEmpty();
        if (adapter != null) {
            adapter.submitList(entries);
        }
        if (emptyView != null) {
            emptyView.setVisibility(hasEntries ? View.GONE : View.VISIBLE);
        }
        if (bottomFadeView != null) {
            bottomFadeView.setVisibility(hasEntries ? View.VISIBLE : View.GONE);
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
