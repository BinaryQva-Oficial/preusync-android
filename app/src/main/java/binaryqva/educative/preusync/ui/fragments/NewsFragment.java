/**
 * ============================================================================
 * Proyecto: PreuSync
 * Clase: NewsFragment.java
 * Versión: v10.0.0
 * Descripción: Fragmento de noticias institucionales y externas con soporte 
 *              de paginación y caché.
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
import binaryqva.educative.preusync.network.models.News;
import binaryqva.educative.preusync.ui.adapters.NewsAdapter;
import binaryqva.educative.preusync.ui.viewmodels.NewsViewModel;

public class NewsFragment extends PaginatedFeedFragment<News> {

    private static final String CACHE_KEY = "news_feed";

    private NewsViewModel newsViewModel;
    private NewsAdapter newsAdapter;

    @Override protected int getLayoutResId() { return R.layout.fragment_news; }
    @Override protected String getCacheKey() { return CACHE_KEY; }

    @Override
    protected RecyclerView.Adapter createAdapter(List<News> data) {
        if (newsAdapter == null) {
            newsAdapter = new NewsAdapter(getContext(), swipeRefreshLayout);
            newsAdapter.setOnRetryLoadMore(this::loadMoreItems);
        }
        newsAdapter.setData(data);
        return newsAdapter;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        newsViewModel = new ViewModelProvider(requireActivity()).get(NewsViewModel.class);
        
        if (recyclerView != null) {
            int paddingBottom = (int) (80 * getResources().getDisplayMetrics().density);
            recyclerView.setPadding(recyclerView.getPaddingLeft(), recyclerView.getPaddingTop(),
                    recyclerView.getPaddingRight(), paddingBottom);
            recyclerView.setClipToPadding(false);
        }

        observeViewModel();
        newsViewModel.refresh();
    }

    private void observeViewModel() {
        newsViewModel.getPaginationState().observe(getViewLifecycleOwner(), this::handlePaginationState);
    }

    @Override protected void loadMoreItems() { if (newsViewModel != null) newsViewModel.loadMore(); }

    @Override
    public void resetAndReload() {
        if (newsViewModel != null) newsViewModel.refresh();
        else super.resetAndReload();
    }

    public News getNewsById(String id) {
        for (News item : itemsList) { if (item.getId().equals(id)) return item; }
        return null;
    }
}
