/**
 * ============================================================================
 * Proyecto: PreuSync
 * Clase: PostsAdapter.java
 * Versión: v4.1.0
 * Descripción: Adaptador para el listado de publicaciones con soporte para
 *              votos en tiempo real y pie de página de carga.
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
import java.util.Map;
import java.util.Set;

import binaryqva.educative.preusync.R;
import binaryqva.educative.preusync.data.repositories.VoteRepository;
import binaryqva.educative.preusync.network.models.Post;
import binaryqva.educative.preusync.ui.activities.HomeActivity;
import binaryqva.educative.preusync.ui.adapters.common.FooterableAdapter;
import binaryqva.educative.preusync.ui.viewmodels.HomeViewModel;
import binaryqva.educative.preusync.utils.common.AppUtils;
import binaryqva.educative.preusync.utils.common.AvatarHelper;
import binaryqva.educative.preusync.utils.common.NumberFormatUtil;
import binaryqva.educative.preusync.utils.common.PreferenceManager;
import binaryqva.educative.preusync.utils.ui.CustomCircularProgressDrawable;
import binaryqva.educative.preusync.utils.ui.ImageTextColorHelper;
import binaryqva.educative.preusync.utils.theme.ThemeManager;

public class PostsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements FooterableAdapter {

    private static final int VIEW_TYPE_ITEM = 0;
    private static final int VIEW_TYPE_LOADING_FOOTER = 1;
    private static final int VIEW_TYPE_ERROR_FOOTER = 2;

    private final Context context;
    private final List<Post> data;
    private final HomeActivity homeActivity;
    private final HomeViewModel viewModel;
    private final SwipeRefreshLayout swipeRefreshLayout;

    private int footerState = FooterableAdapter.FOOTER_NONE;
    private Runnable onRetryLoadMore;

    public interface OnPostInteractionListener {
        void onPostClick(Post post);
        void onVoteAction(String postId, String type);
    }

    private OnPostInteractionListener interactionListener;

    public PostsAdapter(Context context, List<Post> data, 
                        HomeActivity homeActivity, HomeViewModel viewModel,
                        SwipeRefreshLayout swipeRefreshLayout) {
        this.context = context;
        this.data = data != null ? data : new ArrayList<>();
        this.homeActivity = homeActivity;
        this.viewModel = viewModel;
        this.swipeRefreshLayout = swipeRefreshLayout;
        setHasStableIds(true);
    }

    public void setInteractionListener(OnPostInteractionListener listener) {
        this.interactionListener = listener;
    }

    public void setOnRetryLoadMore(Runnable listener) {
        this.onRetryLoadMore = listener;
    }

    public void setData(List<Post> newData) {
        this.data.clear();
        if (newData != null) this.data.addAll(newData);
        notifyDataSetChanged();
    }

    public void addItems(List<Post> moreData) {
        if (moreData == null || moreData.isEmpty()) return;
        int start = this.data.size();
        this.data.addAll(moreData);
        notifyItemRangeInserted(start, moreData.size());
    }

    public void updateUserVote(String postId, int state) {
        for (int i = 0; i < data.size(); i++) {
            if (data.get(i).getId().equals(postId)) {
                notifyItemChanged(i);
                return;
            }
        }
    }

    public void updatePostCounts(String postId, long likes, long dislikes) {
        for (int i = 0; i < data.size(); i++) {
            if (data.get(i).getId().equals(postId)) {
                notifyItemChanged(i);
                return;
            }
        }
    }

    @Override
    public int getFooterState() { return footerState; }

    @Override
    public void setFooterState(int state) {
        if (this.footerState == state) return;
        this.footerState = state;
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        if (footerState != FooterableAdapter.FOOTER_NONE && position == data.size()) {
            return footerState == FooterableAdapter.FOOTER_LOADING ? VIEW_TYPE_LOADING_FOOTER : VIEW_TYPE_ERROR_FOOTER;
        }
        return VIEW_TYPE_ITEM;
    }

    @Override
    public int getItemCount() {
        return data.size() + (footerState != FooterableAdapter.FOOTER_NONE ? 1 : 0);
    }

    @Override
    public long getItemId(int position) {
        if (position < data.size()) {
            String id = data.get(position).getId();
            return id != null ? id.hashCode() : position;
        }
        return 1000000L + position;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        switch (viewType) {
            case VIEW_TYPE_LOADING_FOOTER:
                return new LoadingFooterViewHolder(inflater.inflate(R.layout.item_loading_footer, parent, false));
            case VIEW_TYPE_ERROR_FOOTER:
                return new ErrorFooterViewHolder(inflater.inflate(R.layout.item_error_footer, parent, false));
            default:
                return new PostViewHolder(inflater.inflate(R.layout.item_post, parent, false));
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof PostViewHolder) {
            ((PostViewHolder) holder).bind(data.get(position));
        } else if (holder instanceof ErrorFooterViewHolder) {
            ((ErrorFooterViewHolder) holder).bind(onRetryLoadMore);
        }
    }

    class PostViewHolder extends RecyclerView.ViewHolder {
        TextView title, author, likes;
        ImageView featuredImage, authorAvatar, likeIcon, dislikeIcon;
        View likeContainer, dislikeContainer, body;
        MaterialCardView card;

        PostViewHolder(View v) {
            super(v);
            title = v.findViewById(R.id.heading); author = v.findViewById(R.id.autor);
            likes = v.findViewById(R.id.likeCount);
            featuredImage = v.findViewById(R.id.featuredImage); authorAvatar = v.findViewById(R.id.postAuthorAvatar);
            likeIcon = v.findViewById(R.id.likeIcon); dislikeIcon = v.findViewById(R.id.dislikeIcon);
            likeContainer = v.findViewById(R.id.likeContainer); dislikeContainer = v.findViewById(R.id.dislikeContainer);
            body = v.findViewById(R.id.body); card = v.findViewById(R.id.cardView);
        }

        void bind(Post p) {
            title.setText(p.getTitle());
            author.setText(p.getAuthor());
            
            String imageUrl = p.getImageUrl();
            boolean isDataSaver = PreferenceManager.getInstance(context).isDataSaverMode();
            if (imageUrl != null && !imageUrl.isEmpty() && !isDataSaver) {
                featuredImage.setVisibility(View.VISIBLE);
                Glide.with(context).load(Uri.parse(imageUrl)).placeholder(new CustomCircularProgressDrawable(context)).into(featuredImage);
            } else featuredImage.setVisibility(View.GONE);

            updateVotes(p.getId());

            body.setOnClickListener(v -> { if (interactionListener != null) interactionListener.onPostClick(p); });
            likeContainer.setOnClickListener(v -> { if (interactionListener != null) interactionListener.onVoteAction(p.getId(), "up"); });
            dislikeContainer.setOnClickListener(v -> { if (interactionListener != null) interactionListener.onVoteAction(p.getId(), "down"); });

            homeActivity.fetchAuthorData(p.getAuthor(), profile -> homeActivity.updateAuthorAvatar(authorAvatar, profile, p.getAuthor()));
        }

        private void updateVotes(String id) {
            Map<String, Integer> votes = viewModel.getVoteStates().getValue();
            Map<String, VoteRepository.PostCounts> counts = viewModel.getPostCounts().getValue();
            Set<String> processing = viewModel.getProcessingPosts().getValue();

            int state = (votes != null && votes.containsKey(id)) ? votes.get(id) : 0;
            VoteRepository.PostCounts c = (counts != null && counts.containsKey(id)) ? counts.get(id) : new VoteRepository.PostCounts(0, 0);
            boolean isProcessing = processing != null && processing.contains(id);

            likeIcon.setImageResource(state == 1 ? R.drawable.ic_like_filled : R.drawable.ic_like_outline);
            dislikeIcon.setImageResource(state == 2 ? R.drawable.ic_dislike_filled : R.drawable.ic_dislike_outline);
            likes.setText(NumberFormatUtil.formatNumber(c.likes));
            
            likeContainer.setEnabled(!isProcessing);
            dislikeContainer.setEnabled(!isProcessing);
            likeContainer.setAlpha(isProcessing ? 0.5f : 1.0f);
            dislikeContainer.setAlpha(isProcessing ? 0.5f : 1.0f);
        }
    }

    static class LoadingFooterViewHolder extends RecyclerView.ViewHolder { LoadingFooterViewHolder(View v) { super(v); } }
    static class ErrorFooterViewHolder extends RecyclerView.ViewHolder {
        ErrorFooterViewHolder(View v) { super(v); }
        void bind(Runnable r) { itemView.setOnClickListener(v -> { if (r != null) r.run(); }); }
    }
}
