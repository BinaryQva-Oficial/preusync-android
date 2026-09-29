/**
 * ============================================================================
 * Proyecto: PreuSync
 * Clase: NewsAdapter.java
 * Versión: v4.0.6
 * Descripción: Adaptador para el listado de noticias institucionales y externas.
 * Autor: JiroxDEV
 * Licensed under the GNU Affero General Public License v3
 * ============================================================================
 */

package binaryqva.educative.preusync.ui.adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;

import binaryqva.educative.preusync.R;
import binaryqva.educative.preusync.network.models.News;
import binaryqva.educative.preusync.ui.activities.HomeActivity;
import binaryqva.educative.preusync.ui.adapters.common.FooterableAdapter;
import binaryqva.educative.preusync.utils.ui.CustomCircularProgressDrawable;
import binaryqva.educative.preusync.utils.ui.ImageTextColorHelper;

public class NewsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements FooterableAdapter {

    private static final int VIEW_TYPE_ITEM = 0;
    private static final int VIEW_TYPE_LOADING = 1;
    private static final int VIEW_TYPE_EMPTY = 2;
    private static final int VIEW_TYPE_ERROR = 3;
    private static final int VIEW_TYPE_LOADING_FOOTER = 4;
    private static final int VIEW_TYPE_ERROR_FOOTER = 5;

    private final Context context;
    private final SwipeRefreshLayout swipeRefreshLayout;
    private List<News> data = new ArrayList<>();
    
    private int state = STATE_LOADING;
    private int footerState = FooterableAdapter.FOOTER_NONE;

    public static final int STATE_LOADING = 0;
    public static final int STATE_EMPTY = 1;
    public static final int STATE_ERROR = 2;
    public static final int STATE_CONTENT = 3;

    private Runnable onRetryListener;
    private Runnable onRetryLoadMore;

    public NewsAdapter(Context context, SwipeRefreshLayout swipeRefreshLayout) {
        this.context = context;
        this.swipeRefreshLayout = swipeRefreshLayout;
        setHasStableIds(true);
    }

    public void setData(List<News> data) {
        this.data = data != null ? data : new ArrayList<>();
        setState(this.data.isEmpty() ? STATE_EMPTY : STATE_CONTENT);
        notifyDataSetChanged();
    }

    public void setState(int state) {
        if (this.state != state) {
            this.state = state;
            notifyDataSetChanged();
        }
    }

    public void setLoading() { setState(STATE_LOADING); }
    public void setEmpty() { setState(STATE_EMPTY); }
    public void setError() { setState(STATE_ERROR); }

    public void setOnRetryListener(Runnable listener) { this.onRetryListener = listener; }
    public void setOnRetryLoadMore(Runnable listener) { this.onRetryLoadMore = listener; }

    @Override public int getFooterState() { return footerState; }
    @Override public void setFooterState(int state) {
        if (this.footerState == state) return;
        int currentSize = data.size();
        int oldState = this.footerState;
        this.footerState = state;
        if (oldState == FooterableAdapter.FOOTER_NONE && state != FooterableAdapter.FOOTER_NONE) notifyItemInserted(currentSize);
        else if (oldState != FooterableAdapter.FOOTER_NONE && state == FooterableAdapter.FOOTER_NONE) notifyItemRemoved(currentSize);
        else notifyItemChanged(currentSize);
    }

    @Override
    public long getItemId(int position) {
        if (getItemViewType(position) == VIEW_TYPE_ITEM && position < data.size()) {
            String id = data.get(position).getId();
            return id != null ? id.hashCode() : super.getItemId(position);
        }
        return super.getItemId(position);
    }

    @Override
    public int getItemViewType(int position) {
        if (data.isEmpty()) {
            switch (state) {
                case STATE_EMPTY: return VIEW_TYPE_EMPTY;
                case STATE_ERROR: return VIEW_TYPE_ERROR;
                default: return VIEW_TYPE_LOADING;
            }
        }
        if (position < data.size()) return VIEW_TYPE_ITEM;
        return footerState == FooterableAdapter.FOOTER_LOADING ? VIEW_TYPE_LOADING_FOOTER : VIEW_TYPE_ERROR_FOOTER;
    }

    @Override
    public int getItemCount() {
        if (data.isEmpty()) return 0;
        return data.size() + (footerState != FooterableAdapter.FOOTER_NONE ? 1 : 0);
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        switch (viewType) {
            case VIEW_TYPE_LOADING: return new LoadingViewHolder(inflater.inflate(R.layout.item_loading, parent, false));
            case VIEW_TYPE_EMPTY: return new EmptyViewHolder(inflater.inflate(R.layout.item_empty_news, parent, false));
            case VIEW_TYPE_ERROR: return new ErrorViewHolder(inflater.inflate(R.layout.item_error_news, parent, false));
            case VIEW_TYPE_LOADING_FOOTER: return new FooterViewHolder(inflater.inflate(R.layout.item_loading_footer, parent, false));
            case VIEW_TYPE_ERROR_FOOTER: return new ErrorFooterViewHolder(inflater.inflate(R.layout.item_error_footer, parent, false));
            default: return new NewsViewHolder(inflater.inflate(R.layout.item_news, parent, false));
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof NewsViewHolder) ((NewsViewHolder) holder).bind(data.get(position));
        else if (holder instanceof ErrorViewHolder) ((ErrorViewHolder) holder).bind(onRetryListener);
        else if (holder instanceof ErrorFooterViewHolder) holder.itemView.setOnClickListener(v -> { if (onRetryLoadMore != null) onRetryLoadMore.run(); });
    }

    static class LoadingViewHolder extends RecyclerView.ViewHolder { LoadingViewHolder(@NonNull View v) { super(v); } }
    static class EmptyViewHolder extends RecyclerView.ViewHolder { EmptyViewHolder(@NonNull View v) { super(v); } }
    static class FooterViewHolder extends RecyclerView.ViewHolder { FooterViewHolder(@NonNull View v) { super(v); } }
    static class ErrorFooterViewHolder extends RecyclerView.ViewHolder { ErrorFooterViewHolder(@NonNull View v) { super(v); } }
    static class ErrorViewHolder extends RecyclerView.ViewHolder {
        private final View retryButton;
        ErrorViewHolder(@NonNull View v) { super(v); retryButton = v.findViewById(R.id.retryButton); }
        void bind(Runnable onRetry) { retryButton.setOnClickListener(v -> { if (onRetry != null) onRetry.run(); }); }
    }

    class NewsViewHolder extends RecyclerView.ViewHolder {
        private final MaterialCardView cardView;
        private final TextView title, sourceText;
        private final ImageView featuredImage;
        private final View scrim, detailsImageView;

        NewsViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView instanceof MaterialCardView ? (MaterialCardView) itemView : itemView.findViewById(R.id.cardView);
            title = itemView.findViewById(R.id.heading);
            sourceText = itemView.findViewById(R.id.sourceText);
            featuredImage = itemView.findViewById(R.id.featuredImage);
            scrim = itemView.findViewById(R.id.scrim);
            detailsImageView = itemView.findViewById(R.id.detailsImageView);

            itemView.setOnTouchListener((v, event) -> {
                if (event.getAction() == MotionEvent.ACTION_DOWN && swipeRefreshLayout != null) swipeRefreshLayout.setEnabled(false);
                else if ((event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) && swipeRefreshLayout != null) {
                    swipeRefreshLayout.postDelayed(() -> { if (swipeRefreshLayout != null) swipeRefreshLayout.setEnabled(true); }, 300);
                }
                return false;
            });
        }

        void bind(News news) {
            title.setText(news.getHeadline());
            sourceText.setText(news.getSource());

            String photoUrl = news.getImageUrl();
            if (photoUrl != null && !photoUrl.isEmpty() && !"null".equals(photoUrl)) {
                featuredImage.setVisibility(View.VISIBLE);
                if (scrim != null) scrim.setVisibility(View.VISIBLE);
                CustomCircularProgressDrawable progress = new CustomCircularProgressDrawable(context);
                progress.start();
                Glide.with(context).asBitmap().load(Uri.parse(photoUrl))
                        .placeholder(progress).error(R.drawable.ic_image_broken)
                        .listener(new RequestListener<Bitmap>() {
                            @Override public boolean onResourceReady(Bitmap resource, Object model, Target<Bitmap> target, DataSource ds, boolean isFirst) {
                                progress.stop();
                                featuredImage.post(() -> { ImageTextColorHelper.adjustTextColor(featuredImage, title); ImageTextColorHelper.adjustTextColor(featuredImage, sourceText); });
                                return false;
                            }
                            @Override public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Bitmap> target, boolean isFirst) {
                                progress.stop(); return false;
                            }
                        }).into(featuredImage);
            } else {
                featuredImage.setVisibility(View.GONE);
                if (scrim != null) scrim.setVisibility(View.GONE);
            }

            View.OnClickListener detailListener = v -> {
                if (context instanceof HomeActivity) ((HomeActivity) context).showNewsDetailsBottomSheet(news);
            };
            cardView.setOnClickListener(detailListener);
            if (detailsImageView != null) detailsImageView.setOnClickListener(detailListener);
        }
    }
}


