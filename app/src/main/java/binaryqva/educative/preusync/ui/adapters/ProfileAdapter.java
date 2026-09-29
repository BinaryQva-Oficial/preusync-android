/**
 * ============================================================================
 * Proyecto: PreuSync
 * Clase: ProfileAdapter.java
 * Versión: v3.6.0
 * Descripción: Adaptador para la pantalla de perfil que organiza la información 
 *              del usuario y sus publicaciones personales.
 * Autor: JiroxDEV
 * Licensed under the GNU Affero General Public License v3
 * ============================================================================
 */

package binaryqva.educative.preusync.ui.adapters;

import android.content.Context;
import android.net.Uri;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import binaryqva.educative.preusync.R;
import binaryqva.educative.preusync.debug.AppLogger;
import binaryqva.educative.preusync.network.models.Post;
import binaryqva.educative.preusync.network.models.Profile;
import binaryqva.educative.preusync.utils.common.NumberFormatUtil;
import binaryqva.educative.preusync.utils.common.RoleHelper;
import binaryqva.educative.preusync.utils.theme.ThemeManager;
import binaryqva.educative.preusync.utils.ui.CustomCircularProgressDrawable;

public class ProfileAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public static final int TYPE_PROFILE_HEADER = 0;
    public static final int TYPE_PROFILE_ACTIONS = 1;
    public static final int TYPE_PROFILE_STATS = 2;
    public static final int TYPE_SECTION_TITLE = 3;
    public static final int TYPE_POST_ITEM = 4;
    public static final int TYPE_LOAD_MORE = 5;
    public static final int TYPE_LOADING = 6;
    public static final int TYPE_EMPTY = 7;
    public static final int TYPE_ERROR = 8;

    public interface OnPostClickListener { void onPostClick(Post post); }
    public interface OnLoadMoreListener { void onLoadMore(); }
    public interface OnRetryListener { void onRetry(); }
    public interface OnEditProfileListener { void onEditProfile(); }
    public interface OnSettingsListener { void onSettings(); }
    public interface OnLogoutListener { void onLogout(); }

    private final Context context;
    private Profile profileData;
    private List<Post> postsData = new ArrayList<>();
    
    private boolean isLoading = false;
    private boolean hasMore = true;
    private boolean isError = false;
    private boolean isProfileLoaded = false;

    private OnPostClickListener postClickListener;
    private OnLoadMoreListener loadMoreListener;
    private OnRetryListener retryListener;
    private OnEditProfileListener editProfileListener;
    private OnSettingsListener settingsListener;
    private OnLogoutListener logoutListener;

    public ProfileAdapter(Context context) {
        this.context = context;
        setHasStableIds(true);
    }

    public void setProfileData(Profile data) {
        boolean wasLoaded = this.isProfileLoaded;
        this.profileData = data;
        this.isProfileLoaded = true;
        if (!wasLoaded) {
            notifyDataSetChanged();
        } else {
            notifyItemChanged(0);
            notifyItemChanged(2);
        }
    }

    public void setPostsData(List<Post> newData, boolean hasMore) {
        int oldSize = this.postsData.size();
        this.postsData = newData != null ? newData : new ArrayList<>();
        this.hasMore = hasMore;
        int newSize = this.postsData.size();

        if (oldSize == 0 && newSize > 0) {
            notifyItemRangeInserted(4, newSize);
        } else if (newSize > oldSize) {
            notifyItemRangeInserted(4 + oldSize, newSize - oldSize);
        } else {
            notifyDataSetChanged();
        }
    }

    public void setLoadingState(boolean loading) { this.isLoading = loading; notifyDataSetChanged(); }
    public void setErrorState(boolean error) { this.isError = error; notifyDataSetChanged(); }
    public int getPostsCount() { return postsData.size(); }

    public void setOnPostClickListener(OnPostClickListener l) { this.postClickListener = l; }
    public void setOnLoadMoreListener(OnLoadMoreListener l) { this.loadMoreListener = l; }
    public void setOnRetryListener(OnRetryListener l) { this.retryListener = l; }
    public void setOnEditProfileListener(OnEditProfileListener l) { this.editProfileListener = l; }
    public void setOnSettingsListener(OnSettingsListener l) { this.settingsListener = l; }
    public void setOnLogoutListener(OnLogoutListener l) { this.logoutListener = l; }

    private int getHeaderCount() { return isProfileLoaded ? 4 : 0; }
    private int getFooterCount() {
        if (!isProfileLoaded) return 0;
        if (postsData.isEmpty()) return 1;
        return hasMore ? 1 : 0;
    }

    @Override
    public int getItemViewType(int position) {
        if (!isProfileLoaded) return TYPE_LOADING;
        if (position == 0) return TYPE_PROFILE_HEADER;
        if (position == 1) return TYPE_PROFILE_ACTIONS;
        if (position == 2) return TYPE_PROFILE_STATS;
        if (position == 3) return TYPE_SECTION_TITLE;
        int postsEnd = 4 + postsData.size();
        if (position >= 4 && position < postsEnd) return TYPE_POST_ITEM;
        if (isError && postsData.isEmpty()) return TYPE_ERROR;
        if (postsData.isEmpty()) return TYPE_EMPTY;
        return TYPE_LOAD_MORE;
    }

    @Override
    public long getItemId(int position) {
        int type = getItemViewType(position);
        if (type == TYPE_POST_ITEM) {
            String id = postsData.get(position - getHeaderCount()).getId();
            return id != null ? id.hashCode() : position;
        }
        return 1000000L + type * 1000 + position;
    }

    @Override
    public int getItemCount() { return getHeaderCount() + postsData.size() + getFooterCount(); }

    @NonNull @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inf = LayoutInflater.from(context);
        switch (viewType) {
            case TYPE_PROFILE_HEADER: return new ProfileHeaderViewHolder(inf.inflate(R.layout.item_profile_header, parent, false));
            case TYPE_PROFILE_ACTIONS: return new ProfileActionsViewHolder(inf.inflate(R.layout.item_profile_actions, parent, false));
            case TYPE_PROFILE_STATS: return new ProfileStatsViewHolder(inf.inflate(R.layout.item_profile_stats, parent, false));
            case TYPE_SECTION_TITLE: return new SectionTitleViewHolder(inf.inflate(R.layout.item_profile_section_title, parent, false));
            case TYPE_POST_ITEM: return new PostViewHolder(inf.inflate(R.layout.item_user_post, parent, false));
            case TYPE_LOAD_MORE: return new LoadMoreViewHolder(inf.inflate(R.layout.item_load_more, parent, false));
            case TYPE_LOADING: return new LoadingViewHolder(inf.inflate(R.layout.item_loading, parent, false));
            case TYPE_EMPTY: return new EmptyViewHolder(inf.inflate(R.layout.item_empty_posts, parent, false));
            case TYPE_ERROR: return new ErrorViewHolder(inf.inflate(R.layout.item_error_posts, parent, false));
            default: return new EmptyViewHolder(inf.inflate(R.layout.item_empty_posts, parent, false));
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        int type = getItemViewType(position);
        switch (type) {
            case TYPE_PROFILE_HEADER: ((ProfileHeaderViewHolder) holder).bind(profileData); break;
            case TYPE_PROFILE_ACTIONS: ((ProfileActionsViewHolder) holder).bind(editProfileListener, settingsListener, logoutListener); break;
            case TYPE_PROFILE_STATS: ((ProfileStatsViewHolder) holder).bind(profileData); break;
            case TYPE_POST_ITEM: ((PostViewHolder) holder).bind(postsData.get(position - getHeaderCount())); break;
            case TYPE_LOAD_MORE: ((LoadMoreViewHolder) holder).bind(isLoading); break;
        }
    }

    static class ProfileHeaderViewHolder extends RecyclerView.ViewHolder {
        private final TextView fName, lName, uName, rName, gText, rExtra, schText, munText, provText, iText;
        private final ImageView rAvatar, sImg;
        private final Context ctx;

        ProfileHeaderViewHolder(@NonNull View v) {
            super(v); ctx = v.getContext();
            fName = v.findViewById(R.id.firstNameText);
            lName = v.findViewById(R.id.lastNameText);
            uName = v.findViewById(R.id.usernameText);
            rName = v.findViewById(R.id.roleText);
            gText = v.findViewById(R.id.groupText);
            rExtra = v.findViewById(R.id.roleExtraInfoText);
            schText = v.findViewById(R.id.schoolText);
            munText = v.findViewById(R.id.municipalityText);
            provText = v.findViewById(R.id.provinceText);
            iText = v.findViewById(R.id.initialAvatarText);
            rAvatar = v.findViewById(R.id.roleAvatarImage);
            sImg = v.findViewById(R.id.statusImage);
        }

        void bind(Profile p) {
            if (p == null) return;
            
            String first = p.getFirstName();
            String last = p.getLastName();

            if ((first == null || first.trim().isEmpty()) && p.getFullName() != null && !p.getFullName().trim().isEmpty()) {
                String full = p.getFullName().trim();
                int space = full.indexOf(' ');
                if (space > 0) {
                    first = full.substring(0, space);
                    last = full.substring(space + 1);
                } else {
                    first = full;
                }
            }

            if (first == null || first.trim().isEmpty()) {
                first = p.getUsername();
            }

            if (fName != null) fName.setText(first);

            if (lName != null) {
                if (last != null && !last.trim().isEmpty()) {
                    lName.setText(last);
                    lName.setVisibility(View.VISIBLE);
                } else {
                    lName.setVisibility(View.GONE);
                }
            }

            if (uName != null) uName.setText("@" + p.getUsername());
            if (rName != null) rName.setText(RoleHelper.getLocalizedFromRoleValue(p.getRole(), ctx));

            if (gText != null) {
                if (p.getGroup() != null && !p.getGroup().trim().isEmpty()) {
                    gText.setText(ctx.getString(R.string.profile_group_label, p.getGroup()));
                    gText.setVisibility(View.VISIBLE);
                } else {
                    gText.setVisibility(View.GONE);
                }
            }

            if (rExtra != null) {
                StringBuilder extra = new StringBuilder();
                if (p.getTutee() != null && !p.getTutee().trim().isEmpty()) {
                    extra.append("Educando: @").append(p.getTutee());
                }
                if (p.getResponsibilities() != null && !p.getResponsibilities().trim().isEmpty()) {
                    if (extra.length() > 0) extra.append("\n");
                    extra.append("Resp: ").append(p.getResponsibilities());
                }
                if (extra.length() > 0) {
                    rExtra.setText(extra.toString());
                    rExtra.setVisibility(View.VISIBLE);
                } else {
                    rExtra.setVisibility(View.GONE);
                }
            }

            if (schText != null) {
                String school = p.getSchool();
                if (school != null && !school.trim().isEmpty()) {
                    schText.setText(school);
                } else {
                    schText.setText("Sin escuela");
                }
                schText.setVisibility(View.VISIBLE);
            }

            if (munText != null) {
                String mun = p.getMunicipality();
                if (mun != null && !mun.trim().isEmpty()) {
                    munText.setText(mun);
                } else {
                    munText.setText("Sin municipio");
                }
                munText.setVisibility(View.VISIBLE);
            }

            if (provText != null) {
                String prov = p.getProvince();
                if (prov != null && !prov.trim().isEmpty()) {
                    provText.setText(prov);
                } else {
                    provText.setText("Sin provincia");
                }
                provText.setVisibility(View.VISIBLE);
            }

            if (sImg != null) {
                String s = p.getStatus();
                sImg.setImageResource("verified".equals(s) ? R.drawable.ic_verified : R.drawable.ic_warning);
                sImg.setColorFilter(ThemeManager.getThemeColor(ctx, "verified".equals(s) ? R.attr.colorAccent : R.attr.colorWarning));
            }

            renderAvatar(p);
        }

        private void renderAvatar(Profile p) {
            if (p == null) return;
            String url = p.getAvatarUrl();

            // 1. Caso: Configuración en JSON (type: "initial" / "role" / "inicial" / "rol")
            if (url != null && url.startsWith("{")) {
                try {
                    JSONObject j = new JSONObject(url);
                    String type = j.optString("type", j.optString("tipo", ""));
                    
                    if ("initial".equalsIgnoreCase(type) || "inicial".equalsIgnoreCase(type)) {
                        String initial = j.optString("initial", j.optString("inicial", ""));
                        if (initial.isEmpty()) {
                            String name = p.getFirstName();
                            if (name == null || name.trim().isEmpty()) name = p.getUsername();
                            initial = (name != null && !name.trim().isEmpty()) ? name.substring(0, 1).toUpperCase() : "?";
                        }
                        iText.setText(initial);
                        iText.setVisibility(View.VISIBLE);
                        rAvatar.setVisibility(View.GONE);
                        return;
                    } else if ("role".equalsIgnoreCase(type) || "rol".equalsIgnoreCase(type)) {
                        String role = j.optString("role", j.optString("rol", p.getRole()));
                        int iconRes = RoleHelper.getRoleIconResId(role);
                        rAvatar.setImageResource(iconRes);
                        rAvatar.setVisibility(View.VISIBLE);
                        iText.setVisibility(View.GONE);
                        return;
                    }
                } catch (Exception e) {
                    AppLogger.w("ProfileAdapter", "Error al parsear JSON de avatar: " + e.getMessage());
                }
            }

            // 2. Caso: Imagen Base64 o URL remota
            if (url != null && !url.trim().isEmpty()) {
                if (url.startsWith("data:image") || url.length() > 200) {
                    try {
                        byte[] decodedString = Base64.decode(
                                url.contains(",") ? url.split(",")[1] : url,
                                Base64.DEFAULT
                        );
                        iText.setVisibility(View.GONE);
                        rAvatar.setVisibility(View.VISIBLE);
                        Glide.with(ctx)
                                .load(decodedString)
                                .circleCrop()
                                .placeholder(R.drawable.ic_nav_profile)
                                .into(rAvatar);
                        return;
                    } catch (Exception e) {
                        AppLogger.w("ProfileAdapter", "Error al decodificar Base64 de avatar");
                    }
                } else {
                    iText.setVisibility(View.GONE);
                    rAvatar.setVisibility(View.VISIBLE);
                    Glide.with(ctx)
                            .load(url)
                            .circleCrop()
                            .placeholder(R.drawable.ic_nav_profile)
                            .into(rAvatar);
                    return;
                }
            }

            // 3. Fallback por defecto: Generar avatar de inicial con el nombre o usuario
            String name = p.getFirstName();
            if (name == null || name.trim().isEmpty()) name = p.getUsername();
            String initial = (name != null && !name.trim().isEmpty()) ? name.substring(0, 1).toUpperCase() : "?";
            iText.setText(initial);
            iText.setVisibility(View.VISIBLE);
            rAvatar.setVisibility(View.GONE);
        }
    }

    static class ProfileActionsViewHolder extends RecyclerView.ViewHolder {
        private final View btnE, btnS, btnL;
        ProfileActionsViewHolder(@NonNull View v) {
            super(v); btnE = v.findViewById(R.id.editButton); btnS = v.findViewById(R.id.settingsButton); btnL = v.findViewById(R.id.logoutButton);
        }
        void bind(OnEditProfileListener e, OnSettingsListener s, OnLogoutListener l) {
            btnE.setOnClickListener(v -> { if (e != null) e.onEditProfile(); });
            btnS.setOnClickListener(v -> { if (s != null) s.onSettings(); });
            btnL.setOnClickListener(v -> { if (l != null) l.onLogout(); });
        }
    }

    static class ProfileStatsViewHolder extends RecyclerView.ViewHolder {
        private final TextView pC, lC, dC;
        ProfileStatsViewHolder(@NonNull View v) {
            super(v); pC = v.findViewById(R.id.statsPostsCount); lC = v.findViewById(R.id.statsLikesCount); dC = v.findViewById(R.id.statsDislikesCount);
        }
        void bind(Profile p) {
            // Stats logic here
        }
    }

    static class SectionTitleViewHolder extends RecyclerView.ViewHolder { SectionTitleViewHolder(@NonNull View v) { super(v); } }

    class PostViewHolder extends RecyclerView.ViewHolder {
        private final ImageView img;
        private final TextView tit, dat, lik, dis;
        PostViewHolder(@NonNull View v) {
            super(v);
            img = v.findViewById(R.id.userPostImage); tit = v.findViewById(R.id.userPostTitle);
            dat = v.findViewById(R.id.userPostDate); lik = v.findViewById(R.id.userPostLikes);
            dis = v.findViewById(R.id.userPostDislikes);
        }
        void bind(Post post) {
            tit.setText(post.getDetails());
            dat.setText(post.getCreatedAt());
            lik.setText(NumberFormatUtil.formatNumber(post.getScore()));
            String url = post.getImageUrl();
            if (url != null && !url.isEmpty()) {
                img.setVisibility(View.VISIBLE);
                Glide.with(context).load(url).centerCrop().into(img);
            } else img.setVisibility(View.GONE);
            itemView.setOnClickListener(v -> { if (postClickListener != null) postClickListener.onPostClick(post); });
        }
    }

    class LoadMoreViewHolder extends RecyclerView.ViewHolder {
        private final MaterialButton btn; private final View prg;
        LoadMoreViewHolder(@NonNull View v) { super(v); btn = v.findViewById(R.id.loadMoreButton); prg = v.findViewById(R.id.loadMoreProgress); }
        void bind(boolean loading) {
            btn.setVisibility(loading ? View.GONE : View.VISIBLE); prg.setVisibility(loading ? View.VISIBLE : View.GONE);
            btn.setOnClickListener(v -> { if (loadMoreListener != null) loadMoreListener.onLoadMore(); });
        }
    }

    static class LoadingViewHolder extends RecyclerView.ViewHolder { LoadingViewHolder(@NonNull View v) { super(v); } }
    static class EmptyViewHolder extends RecyclerView.ViewHolder { EmptyViewHolder(@NonNull View v) { super(v); } }
    class ErrorViewHolder extends RecyclerView.ViewHolder {
        ErrorViewHolder(@NonNull View v) { super(v); v.findViewById(R.id.retryButton).setOnClickListener(view -> { if (retryListener != null) retryListener.onRetry(); }); }
    }
}


