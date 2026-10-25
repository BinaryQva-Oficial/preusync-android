/**
 * ============================================================================
 * Proyecto: PreuSync
 * Clase: HomeFeedAdapter.java
 * Versión: v3.0.0
 * Descripción: Adaptador maestro para el inicio que organiza carruseles 
 *              y secciones informativas dinámicas. Refactorizado para HomeViewModel.
 * Autor: JiroxDEV
 * Licensed under the GNU Affero General Public License v3
 * ============================================================================
 */

package binaryqva.educative.preusync.ui.adapters.home;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import binaryqva.educative.preusync.R;
import binaryqva.educative.preusync.network.models.Ephemeris;
import binaryqva.educative.preusync.network.models.News;
import binaryqva.educative.preusync.network.models.Post;
import binaryqva.educative.preusync.ui.activities.HomeActivity;
import binaryqva.educative.preusync.ui.viewmodels.HomeViewModel;

public class HomeFeedAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public static final int TYPE_TITLE_NEWS = 0;
    public static final int TYPE_VIEWPAGER_NEWS = 1;
    public static final int TYPE_TITLE_POSTS = 2;
    public static final int TYPE_VIEWPAGER_POSTS = 3;
    public static final int TYPE_EXTRA_HOME = 4;

    private final Context context;
    private final HomeViewModel viewModel;

    private List<News> newsData = new ArrayList<>();
    private List<Post> postsData = new ArrayList<>();
    private Ephemeris ephemerisData = null;
    private String shiftNumber = "", shiftTimeRange = "", shiftSubject = "";

    private boolean showTitleNews = true, showViewPagerNews = true;
    private boolean showTitlePosts = true, showViewPagerPosts = true;
    private boolean showEphemeris = true, showShift = true;

    private int newsState = HomeViewModel.STATE_LOADING;
    private int postsState = HomeViewModel.STATE_LOADING;
    private int ephemerisState = HomeViewModel.STATE_LOADING;
    private int shiftState = HomeViewModel.STATE_LOADING;

    private RecyclerView newsRecyclerViewRef, postsRecyclerViewRef;

    public HomeFeedAdapter(Context context, HomeViewModel viewModel) {
        this.context = context;
        this.viewModel = viewModel;
    }

    public void updateNews(List<News> data) { this.newsData = data; notifySectionChanged(TYPE_VIEWPAGER_NEWS); }
    public void updatePosts(List<Post> data) { this.postsData = data; notifySectionChanged(TYPE_VIEWPAGER_POSTS); }
    public void updateEphemeris(Ephemeris data) { this.ephemerisData = data; notifySectionChanged(TYPE_EXTRA_HOME); }
    public void updateShift(String number, String time, String subject) {
        this.shiftNumber = number; this.shiftTimeRange = time; this.shiftSubject = subject;
        notifySectionChanged(TYPE_EXTRA_HOME);
    }

    private void notifySectionChanged(int type) {
        int pos = getPosition(type);
        if (pos != -1) notifyItemChanged(pos);
    }

    public void setVisibility(boolean news, boolean posts, boolean ephemeris, boolean shift) {
        this.showTitleNews = news; this.showViewPagerNews = news;
        this.showTitlePosts = posts; this.showViewPagerPosts = posts;
        this.showEphemeris = ephemeris; this.showShift = shift;
        notifyDataSetChanged();
    }

    public void setNewsState(int s) { this.newsState = s; notifySectionChanged(TYPE_VIEWPAGER_NEWS); }
    public void setPostsState(int s) { this.postsState = s; notifySectionChanged(TYPE_VIEWPAGER_POSTS); }
    public void setEphemerisState(int s) { this.ephemerisState = s; notifySectionChanged(TYPE_EXTRA_HOME); }
    public void setShiftState(int s) { this.shiftState = s; notifySectionChanged(TYPE_EXTRA_HOME); }

    public void updatePostVote(String id, int state) {
        if (postsRecyclerViewRef != null && postsRecyclerViewRef.getAdapter() != null) {
            postsRecyclerViewRef.getAdapter().notifyDataSetChanged();
        }
    }

    public void updatePostCounts(String id, long l, long d) {
        if (postsRecyclerViewRef != null && postsRecyclerViewRef.getAdapter() != null) {
            postsRecyclerViewRef.getAdapter().notifyDataSetChanged();
        }
    }

    public RecyclerView getNewsRecyclerView() { return newsRecyclerViewRef; }
    public RecyclerView getPostsRecyclerView() { return postsRecyclerViewRef; }

    private int getPosition(int type) {
        int pos = 0;
        if (showTitleNews) { if (type == TYPE_TITLE_NEWS) return 0; pos++; }
        if (showViewPagerNews) { if (type == TYPE_VIEWPAGER_NEWS) return pos; pos++; }
        if (showTitlePosts) { if (type == TYPE_TITLE_POSTS) return pos; pos++; }
        if (showViewPagerPosts) { if (type == TYPE_VIEWPAGER_POSTS) return pos; pos++; }
        if (showEphemeris || showShift) { if (type == TYPE_EXTRA_HOME) return pos; }
        return -1;
    }

    @Override
    public int getItemViewType(int position) {
        int pos = 0;
        if (showTitleNews) { if (position == pos) return TYPE_TITLE_NEWS; pos++; }
        if (showViewPagerNews) { if (position == pos) return TYPE_VIEWPAGER_NEWS; pos++; }
        if (showTitlePosts) { if (position == pos) return TYPE_TITLE_POSTS; pos++; }
        if (showViewPagerPosts) { if (position == pos) return TYPE_VIEWPAGER_POSTS; pos++; }
        return TYPE_EXTRA_HOME;
    }

    @Override
    public int getItemCount() {
        int c = 0;
        if (showTitleNews) c++; if (showViewPagerNews) c++;
        if (showTitlePosts) c++; if (showViewPagerPosts) c++;
        if (showEphemeris || showShift) c++;
        return c;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inf = LayoutInflater.from(context);
        switch (viewType) {
            case TYPE_TITLE_NEWS: return new TitleNewsViewHolder(inf.inflate(R.layout.item_title_news, parent, false));
            case TYPE_VIEWPAGER_NEWS: return new NewsRecyclerViewHolder(inf.inflate(R.layout.item_viewpager_news, parent, false));
            case TYPE_TITLE_POSTS: return new TitlePostsViewHolder(inf.inflate(R.layout.item_title_posts, parent, false));
            case TYPE_VIEWPAGER_POSTS: return new PostsRecyclerViewHolder(inf.inflate(R.layout.item_viewpager_posts, parent, false));
            default: return new ExtraHomeViewHolder(inf.inflate(R.layout.item_extra_home, parent, false));
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof NewsRecyclerViewHolder) {
            newsRecyclerViewRef = ((NewsRecyclerViewHolder) holder).recyclerView;
            newsRecyclerViewRef.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false));
            newsRecyclerViewRef.setAdapter(new NewsCarouselAdapter(context, newsData));
            ((NewsRecyclerViewHolder) holder).bindState(newsState);
        } else if (holder instanceof PostsRecyclerViewHolder) {
            postsRecyclerViewRef = ((PostsRecyclerViewHolder) holder).recyclerView;
            postsRecyclerViewRef.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false));
            postsRecyclerViewRef.setAdapter(new PostsCarouselAdapter(context, postsData, viewModel));
            ((PostsRecyclerViewHolder) holder).bindState(postsState);
        } else if (holder instanceof ExtraHomeViewHolder) {
            ((ExtraHomeViewHolder) holder).bind(ephemerisData, shiftNumber, shiftTimeRange, shiftSubject, showEphemeris, showShift, ephemerisState, shiftState);
        }
    }

    static class TitleNewsViewHolder extends RecyclerView.ViewHolder { TitleNewsViewHolder(View v) { super(v); } }
    static class TitlePostsViewHolder extends RecyclerView.ViewHolder { TitlePostsViewHolder(View v) { super(v); } }

    class NewsRecyclerViewHolder extends RecyclerView.ViewHolder {
        RecyclerView recyclerView; View loader, empty, error;
        NewsRecyclerViewHolder(View v) {
            super(v);
            recyclerView = v.findViewById(R.id.newsRecyclerView);
            loader = v.findViewById(R.id.newsLoader);
            empty = v.findViewById(R.id.newsEmptyLayout);
            error = v.findViewById(R.id.newsErrorLayout);
            View.OnClickListener r = view -> { if (viewModel != null) viewModel.loadHomeData(); };
            if (error != null) error.setOnClickListener(r);
            View retryBtn = v.findViewById(R.id.newsRetryButton);
            if (retryBtn != null) retryBtn.setOnClickListener(r);
        }
        void bindState(int s) {
            loader.setVisibility(s == HomeViewModel.STATE_LOADING ? View.VISIBLE : View.GONE);
            recyclerView.setVisibility(s == HomeViewModel.STATE_SUCCESS ? View.VISIBLE : View.GONE);
            empty.setVisibility(s == HomeViewModel.STATE_EMPTY ? View.VISIBLE : View.GONE);
            error.setVisibility(s == HomeViewModel.STATE_ERROR ? View.VISIBLE : View.GONE);
        }
    }

    class PostsRecyclerViewHolder extends RecyclerView.ViewHolder {
        RecyclerView recyclerView; View loader, empty, error;
        PostsRecyclerViewHolder(View v) {
            super(v);
            recyclerView = v.findViewById(R.id.postsRecyclerView);
            loader = v.findViewById(R.id.postsLoader);
            empty = v.findViewById(R.id.postsEmptyLayout);
            error = v.findViewById(R.id.postsErrorLayout);
            View.OnClickListener r = view -> { if (viewModel != null) viewModel.loadHomeData(); };
            if (error != null) error.setOnClickListener(r);
            View retryBtn = v.findViewById(R.id.postsRetryButton);
            if (retryBtn != null) retryBtn.setOnClickListener(r);
        }
        void bindState(int s) {
            loader.setVisibility(s == HomeViewModel.STATE_LOADING ? View.VISIBLE : View.GONE);
            recyclerView.setVisibility(s == HomeViewModel.STATE_SUCCESS ? View.VISIBLE : View.GONE);
            empty.setVisibility(s == HomeViewModel.STATE_EMPTY ? View.VISIBLE : View.GONE);
            error.setVisibility(s == HomeViewModel.STATE_ERROR ? View.VISIBLE : View.GONE);
        }
    }

    static class ExtraHomeViewHolder extends RecyclerView.ViewHolder {
        View ephContainer, shContainer, ephCard, shCard;
        TextView ephTit, shNum, shTime, shSubj;
        ExtraHomeViewHolder(View v) {
            super(v);
            ephContainer = v.findViewById(R.id.dayEphemeryContainer); shContainer = v.findViewById(R.id.liveMiniScheduleContainer);
            ephTit = v.findViewById(R.id.ephemeryTitle); shNum = v.findViewById(R.id.shiftNumber);
            shTime = v.findViewById(R.id.shiftTimeRange); shSubj = v.findViewById(R.id.currentSubject);
            ephCard = v.findViewById(R.id.dayEphemery); shCard = v.findViewById(R.id.liveMiniSchedule);
        }
        void bind(Ephemeris eph, String num, String time, String subj, boolean sEph, boolean sSh, int eSt, int sSt) {
            final HomeActivity act = (HomeActivity) itemView.getContext();
            ephTit.setText(eSt == HomeViewModel.STATE_LOADING ? "Cargando..." : (eph == null ? "No hay efeméride hoy" : eph.getTitle()));
            ephCard.setOnClickListener(v -> { if (eph != null) act.showEphemerisDetailsBottomSheet(eph); });
            shSubj.setText(sSt == HomeViewModel.STATE_LOADING ? "Cargando..." : (num.isEmpty() ? "Sin turno asignado" : subj));
            shNum.setText(num); shTime.setText(time);
            shCard.setOnClickListener(v -> act.navigateToEventSubTab(3));
            ephContainer.setVisibility(sEph ? View.VISIBLE : View.GONE);
            shContainer.setVisibility(sSh ? View.VISIBLE : View.GONE);
        }
    }
}


