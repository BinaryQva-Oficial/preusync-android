/**
 * ============================================================================
 * Proyecto: PreuSync
 * Clase: EventsAdapter.java
 * Versión: v1.0.0
 * Descripción: Adaptador unificado para el listado de eventos (Escolares y 
 *              Externos). Soporta estados de carga, error y pie de página.
 * Autor: JiroxDEV
 * Licensed under the GNU Affero General Public License v3
 * ============================================================================
 */

package binaryqva.educative.preusync.ui.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import binaryqva.educative.preusync.R;
import binaryqva.educative.preusync.network.models.Event;
import binaryqva.educative.preusync.ui.adapters.common.FooterableAdapter;

/**
 * Gestiona la visualización de una lista de eventos.
 */
public class EventsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements FooterableAdapter {

    private static final int TYPE_LOADING = 0;
    private static final int TYPE_EMPTY = 1;
    private static final int TYPE_ERROR = 2;
    private static final int TYPE_ITEM = 3;
    private static final int TYPE_FOOTER_LOADING = 4;
    private static final int TYPE_FOOTER_ERROR = 5;

    public static final int STATE_LOADING = 0;
    public static final int STATE_EMPTY = 1;
    public static final int STATE_ERROR = 2;
    public static final int STATE_CONTENT = 3;

    private final Context context;
    private List<Event> data;
    private int currentState = STATE_LOADING;
    private int footerState = FooterableAdapter.FOOTER_NONE;

    private OnEventClickListener itemClickListener;
    private Runnable onRetryListener;
    private Runnable onRetryLoadMoreListener;

    public interface OnEventClickListener {
        void onEventClick(Event event);
    }

    public EventsAdapter(Context context) {
        this.context = context;
        this.data = new ArrayList<>();
        setHasStableIds(true);
    }

    public void setData(List<Event> newData) {
        this.data = newData != null ? newData : new ArrayList<>();
        this.currentState = this.data.isEmpty() ? STATE_EMPTY : STATE_CONTENT;
        notifyDataSetChanged();
    }

    public void setState(int state) {
        if (this.currentState != state) {
            this.currentState = state;
            notifyDataSetChanged();
        }
    }

    public void setOnEventClickListener(OnEventClickListener listener) {
        this.itemClickListener = listener;
    }

    public void setOnRetryListener(Runnable listener) {
        this.onRetryListener = listener;
    }

    public void setOnRetryLoadMoreListener(Runnable listener) {
        this.onRetryLoadMoreListener = listener;
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
    public long getItemId(int position) {
        if (getItemViewType(position) == TYPE_ITEM && position < data.size()) {
            String id = data.get(position).getId();
            return id != null ? id.hashCode() : position;
        }
        return 1000000L + getItemViewType(position) * 1000 + position;
    }

    @Override
    public int getItemViewType(int position) {
        if (data.isEmpty()) {
            switch (currentState) {
                case STATE_EMPTY: return TYPE_EMPTY;
                case STATE_ERROR: return TYPE_ERROR;
                default: return TYPE_LOADING;
            }
        }
        if (footerState != FooterableAdapter.FOOTER_NONE && position == getItemCount() - 1) {
            return footerState == FooterableAdapter.FOOTER_LOADING ? TYPE_FOOTER_LOADING : TYPE_FOOTER_ERROR;
        }
        return TYPE_ITEM;
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
            case TYPE_LOADING: return new LoadingViewHolder(inflater.inflate(R.layout.item_loading, parent, false));
            case TYPE_EMPTY: return new EmptyViewHolder(inflater.inflate(R.layout.item_empty_events, parent, false));
            case TYPE_ERROR: return new ErrorViewHolder(inflater.inflate(R.layout.item_error_events, parent, false));
            case TYPE_FOOTER_LOADING: return new FooterViewHolder(inflater.inflate(R.layout.item_loading_footer, parent, false));
            case TYPE_FOOTER_ERROR: return new ErrorFooterViewHolder(inflater.inflate(R.layout.item_error_footer, parent, false));
            default: return new ItemViewHolder(inflater.inflate(R.layout.item_event_school, parent, false));
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof ItemViewHolder) {
            ((ItemViewHolder) holder).bind(data.get(position));
        } else if (holder instanceof ErrorViewHolder) {
            ((ErrorViewHolder) holder).bind(onRetryListener);
        } else if (holder instanceof ErrorFooterViewHolder) {
            ((ErrorFooterViewHolder) holder).bind(onRetryLoadMoreListener);
        }
    }

    static class LoadingViewHolder extends RecyclerView.ViewHolder { LoadingViewHolder(@NonNull View v) { super(v); } }
    static class EmptyViewHolder extends RecyclerView.ViewHolder { EmptyViewHolder(@NonNull View v) { super(v); } }
    static class FooterViewHolder extends RecyclerView.ViewHolder { FooterViewHolder(@NonNull View v) { super(v); } }

    static class ErrorFooterViewHolder extends RecyclerView.ViewHolder {
        ErrorFooterViewHolder(@NonNull View v) { super(v); }
        void bind(Runnable r) { itemView.setOnClickListener(v -> { if (r != null) r.run(); }); }
    }

    static class ErrorViewHolder extends RecyclerView.ViewHolder {
        private final View btn;
        ErrorViewHolder(@NonNull View v) { super(v); btn = v.findViewById(R.id.retryButton); }
        void bind(Runnable r) { if (btn != null) btn.setOnClickListener(v -> { if (r != null) r.run(); }); }
    }

    class ItemViewHolder extends RecyclerView.ViewHolder {
        private final TextView title;
        ItemViewHolder(@NonNull View v) { super(v); title = v.findViewById(R.id.heading); }
        void bind(Event item) {
            title.setText(item.getTitle());
            itemView.setOnClickListener(v -> {
                if (itemClickListener != null) itemClickListener.onEventClick(item);
            });
        }
    }
}


