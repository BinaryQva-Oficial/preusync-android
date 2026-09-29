/**
 * ============================================================================
 * Proyecto: PreuSync
 * Clase: ExternalEventsFragment.java
 * Versión: v8.1.0
 * Descripción: Fragmento para la visualización de eventos externos. 
 *              Utiliza el adaptador unificado EventsAdapter.
 * Autor: JiroxDEV
 * Licensed under the GNU Affero General Public License v3
 * ============================================================================
 */

package binaryqva.educative.preusync.ui.fragments;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import binaryqva.educative.preusync.R;
import binaryqva.educative.preusync.network.models.Event;
import binaryqva.educative.preusync.ui.activities.HomeActivity;
import binaryqva.educative.preusync.ui.adapters.EventsAdapter;
import binaryqva.educative.preusync.ui.viewmodels.ExternalEventsViewModel;

public class ExternalEventsFragment extends PaginatedFeedFragment<Event> {

    private static final String CACHE_KEY_EVENTS_EXTERNAL = "external_events_feed";

    private ExternalEventsViewModel eventsViewModel;
    private EventsAdapter eventsAdapter;

    @Override protected int getLayoutResId() { return R.layout.fragment_events_external; }
    @Override protected String getCacheKey() { return CACHE_KEY_EVENTS_EXTERNAL; }

    @Override
    protected RecyclerView.Adapter createAdapter(List<Event> data) {
        if (eventsAdapter == null) {
            eventsAdapter = new EventsAdapter(requireContext());
            eventsAdapter.setOnEventClickListener(event -> {
                if (getActivity() instanceof HomeActivity) {
                    ((HomeActivity) getActivity()).showEventDetailsBottomSheet(event);
                }
            });
            eventsAdapter.setOnRetryListener(() -> eventsViewModel.refresh());
            eventsAdapter.setOnRetryLoadMoreListener(() -> eventsViewModel.loadMore());
        }
        eventsAdapter.setData(data);
        return eventsAdapter;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        eventsViewModel = new ViewModelProvider(requireActivity()).get(ExternalEventsViewModel.class);
        
        if (recyclerView != null) {
            int paddingBottom = (int) (80 * getResources().getDisplayMetrics().density);
            recyclerView.setPadding(recyclerView.getPaddingLeft(), recyclerView.getPaddingTop(),
                    recyclerView.getPaddingRight(), paddingBottom);
            recyclerView.setClipToPadding(false);
        }

        observeViewModel();
        eventsViewModel.refresh();
    }

    private void observeViewModel() {
        eventsViewModel.getPaginationState().observe(getViewLifecycleOwner(), this::handlePaginationState);
    }

    @Override protected void loadMoreItems() { if (eventsViewModel != null) eventsViewModel.loadMore(); }

    @Override
    public void resetAndReload() {
        if (eventsViewModel != null) eventsViewModel.refresh();
        else super.resetAndReload();
    }
}
