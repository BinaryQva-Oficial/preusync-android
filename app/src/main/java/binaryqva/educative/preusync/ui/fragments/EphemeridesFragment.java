/**
 * ============================================================================
 * Proyecto: PreuSync
 * Clase: EphemeridesFragment.java
 * Versión: v8.0.0
 * Descripción: Fragmento de efemérides. Herencia de BaseFragment con
 *              gestión unificada de estados y soporte Room.
 * Autor: JiroxDEV
 * Licensed under the GNU Affero General Public License v3
 * ============================================================================
 */

package binaryqva.educative.preusync.ui.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import binaryqva.educative.preusync.R;
import binaryqva.educative.preusync.network.models.Ephemeris;
import binaryqva.educative.preusync.ui.viewmodels.EphemeridesViewModel;
import binaryqva.educative.preusync.utils.common.DialogHelper;
import binaryqva.educative.preusync.utils.common.OfflineSnackbarManager;
import binaryqva.educative.preusync.utils.ui.SmartSwipeRefreshLayout;
import binaryqva.educative.preusync.utils.ui.SwipeRefreshHelper;
import binaryqva.educative.preusync.utils.theme.ThemeManager;

public class EphemeridesFragment extends BaseFragment {

    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private TextView dateTextView;
    private ImageButton changeDateButton;

    private SwipeRefreshHelper swipeRefreshHelper;
    private OfflineSnackbarManager offlineSnackbarManager;

    private EphemeridesViewModel ephemeridesViewModel;
    private EphemeridesAdapter adapter;

    @Override protected int getLayoutResId() { return R.layout.fragment_events_ephemeris; }

    @Override
    protected void setupViews(View view) {
        int colorAccent = ThemeManager.getThemeColor(requireContext(), R.attr.colorAccent);

        recyclerView = view.findViewById(R.id.recyclerView);
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        dateTextView = view.findViewById(R.id.dateText);
        changeDateButton = view.findViewById(R.id.changeDateButton);

        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        if (swipeRefreshLayout instanceof SmartSwipeRefreshLayout) {
            ((SmartSwipeRefreshLayout) swipeRefreshLayout).setTargetScrollableView(recyclerView);
        }
        adapter = new EphemeridesAdapter();
        recyclerView.setAdapter(adapter);

        swipeRefreshHelper = new SwipeRefreshHelper(swipeRefreshLayout, colorAccent);
        swipeRefreshHelper.setOnRefreshListener(() -> ephemeridesViewModel.refresh());

        offlineSnackbarManager = new OfflineSnackbarManager(this, view, () -> ephemeridesViewModel.refresh());
        
        ephemeridesViewModel = new ViewModelProvider(requireActivity()).get(EphemeridesViewModel.class);
        changeDateButton.setOnClickListener(v -> showDayMonthPicker());
    }

    @Override
    protected void onRetry() {
        if (ephemeridesViewModel != null) ephemeridesViewModel.refresh();
    }

    @Override
    protected void setupObservers() {
        ephemeridesViewModel.getEphemerisList().observe(getViewLifecycleOwner(), list -> {
            boolean hasData = list != null && !list.isEmpty();
            adapter.setData(list != null ? list : new ArrayList<>());
            updateUiState(hasData ? STATE_CONTENT : STATE_EMPTY, hasData);
            if (swipeRefreshHelper != null) swipeRefreshHelper.finishRefresh();
            offlineSnackbarManager.checkAndUpdate();
        });

        ephemeridesViewModel.getEphemerisState().observe(getViewLifecycleOwner(), state -> {
            if (state == null) return;
            boolean hasData = adapter.getItemCount() > 0;
            int targetState = STATE_CONTENT;
            if (state == EphemeridesViewModel.STATE_LOADING) targetState = STATE_LOADING;
            else if (state == EphemeridesViewModel.STATE_EMPTY) targetState = STATE_EMPTY;
            else if (state == EphemeridesViewModel.STATE_ERROR) targetState = STATE_ERROR;
            updateUiState(targetState, hasData);
            if (swipeRefreshHelper != null) swipeRefreshHelper.finishRefresh();
        });

        ephemeridesViewModel.getFormattedDate().observe(getViewLifecycleOwner(), date -> {
            if (date != null) dateTextView.setText(date);
        });
    }

    @Override public void onResume() { super.onResume(); offlineSnackbarManager.checkAndUpdate(); }
    @Override public void onPause() { super.onPause(); offlineSnackbarManager.dismiss(); }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (swipeRefreshHelper != null) swipeRefreshHelper.cleanup();
        offlineSnackbarManager.onDestroy();
    }

    private void showDayMonthPicker() {
        Calendar c = ephemeridesViewModel.getSelectedDate();
        DialogHelper.showDatePickerDialog(getActivity(), getString(R.string.label_select_date),
                c.get(Calendar.DAY_OF_MONTH), c.get(Calendar.MONTH), (day, month) -> {
                    updateUiState(STATE_LOADING, false);
                    ephemeridesViewModel.setSelectedDate(day, month);
                });
    }

    private static class EphemeridesAdapter extends RecyclerView.Adapter<EphemeridesAdapter.ItemViewHolder> {
        private List<Ephemeris> data = new ArrayList<>();

        @Override public int getItemCount() { return data != null ? data.size() : 0; }

        @Override public void onBindViewHolder(@NonNull ItemViewHolder h, int p) {
            h.bind(data.get(p));
        }

        @NonNull @Override public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup pr, int vt) {
            LayoutInflater inf = LayoutInflater.from(pr.getContext());
            return new ItemViewHolder(inf.inflate(R.layout.item_ephemeris, pr, false));
        }

        void setData(List<Ephemeris> d) { this.data = d != null ? d : new ArrayList<>(); notifyDataSetChanged(); }

        static class ItemViewHolder extends RecyclerView.ViewHolder {
            TextView title, desc;
            ItemViewHolder(View v) { super(v); title = v.findViewById(R.id.heading); desc = v.findViewById(R.id.detailsText); }
            void bind(Ephemeris e) {
                if (title != null) title.setText(e.getTitle());
                if (desc != null) desc.setText(e.getDetails());
            }
        }
    }
}
