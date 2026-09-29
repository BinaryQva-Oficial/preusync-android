/**
 * ============================================================================
 * Proyecto: PreuSync
 * Clase: PostsFragment.java
 * Versión: v14.2.0
 * Descripción: Fragmento de la comunidad para visualizar e interactuar con 
 *              publicaciones (posts).
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
import binaryqva.educative.preusync.network.models.Post;
import binaryqva.educative.preusync.ui.activities.HomeActivity;
import binaryqva.educative.preusync.ui.adapters.PostsAdapter;
import binaryqva.educative.preusync.ui.viewmodels.PostsViewModel;
import binaryqva.educative.preusync.ui.viewmodels.HomeViewModel;

public class PostsFragment extends PaginatedFeedFragment<Post> {

    private static final String CACHE_KEY = "posts_feed";

    private PostsViewModel postsViewModel;
    private HomeViewModel homeViewModel;
    private PostsAdapter postsAdapter;

    @Override protected int getLayoutResId() { return R.layout.fragment_posts; }
    @Override protected String getCacheKey() { return CACHE_KEY; }

    @Override
    protected RecyclerView.Adapter createAdapter(List<Post> data) {
        if (postsAdapter == null) {
            HomeActivity activity = (HomeActivity) getActivity();
            postsAdapter = new PostsAdapter(getContext(), data, activity, homeViewModel, swipeRefreshLayout);
            postsAdapter.setOnRetryLoadMore(this::loadMoreItems);
        }
        postsAdapter.setData(data);
        return postsAdapter;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        homeViewModel = new ViewModelProvider(requireActivity()).get(HomeViewModel.class);
        super.onViewCreated(view, savedInstanceState);
        postsViewModel = new ViewModelProvider(requireActivity()).get(PostsViewModel.class);
        
        observeViewModel();
        postsViewModel.refresh();
    }

    private void observeViewModel() {
        postsViewModel.getPaginationState().observe(getViewLifecycleOwner(), this::handlePaginationState);
    }

    @Override protected void loadMoreItems() { if (postsViewModel != null) postsViewModel.loadMore(); }

    @Override
    public void resetAndReload() {
        if (postsViewModel != null) postsViewModel.refresh();
        else super.resetAndReload();
    }

    public Post getPostById(String id) {
        for (Post item : itemsList) { if (item.getId().equals(id)) return item; }
        return null;
    }

    public void createPost(String t, String c, String p) {
        postsViewModel.createPost(t, c, p);
    }

    public void deletePost(String id) {
        postsViewModel.deletePost(id);
    }

    public void reportPost(String id) {
        postsViewModel.reportPost(id);
    }
}
