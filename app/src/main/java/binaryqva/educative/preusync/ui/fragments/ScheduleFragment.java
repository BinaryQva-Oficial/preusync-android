/**
 * ============================================================================
 * Proyecto: PreuSync
 * Clase: ScheduleFragment.java
 * Versión: v13.0.0
 * Descripción: Fragmento de horario escolar que muestra la tabla oficial
 *              de 16 turnos (5 mañana, 3 tarde, recesos, almuerzo) según la ODS.
 * Autor: JiroxDEV
 * Licensed under the GNU Affero General Public License v3
 * ============================================================================
 */

package binaryqva.educative.preusync.ui.fragments;

import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.ViewFlipper;

import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import binaryqva.educative.preusync.R;
import binaryqva.educative.preusync.debug.AppLogger;
import binaryqva.educative.preusync.network.models.SchoolGroup;
import binaryqva.educative.preusync.ui.viewmodels.ScheduleViewModel;
import binaryqva.educative.preusync.utils.ui.SmartSwipeRefreshLayout;
import binaryqva.educative.preusync.utils.ui.SwipeRefreshHelper;
import binaryqva.educative.preusync.utils.theme.ThemeManager;

public class ScheduleFragment extends BaseFragment {

    private static final String TAG = "PreuSync_ScheduleFragment";
    private static final int DAYS = 5;
    private static final int ROWS = 16;

    private static final String[] TURN_LABELS = {
        "CI", "Turno 1", "Descanso", "Turno 2", "Descanso", "Turno 3", "Merienda",
        "Turno 4", "Descanso", "Turno 5", "Almuerzo", "Turno 6", "Descanso",
        "Turno 7", "Descanso", "Turno 8"
    };

    private static final String[] OFFICIAL_TIME_RANGES = {
        "07:50 - 08:00", "08:00 - 08:45", "08:45 - 08:50", "08:50 - 09:35",
        "09:35 - 09:40", "09:40 - 10:25", "10:25 - 10:50", "10:50 - 11:35",
        "11:35 - 11:40", "11:40 - 12:25", "12:25 - 13:25", "13:25 - 14:10",
        "14:10 - 14:15", "14:15 - 15:00", "15:00 - 15:05", "15:05 - 15:50"
    };

    private ViewFlipper viewFlipper;
    private TableLayout scheduleTable;
    private TextInputLayout groupTextInputLayout;
    private AutoCompleteTextView groupAutoComplete;
    private SwipeRefreshLayout swipeRefreshLayout;
    private MaterialButton retryButton;

    private SwipeRefreshHelper swipeRefreshHelper;
    private ScheduleViewModel viewModel;
    private TextView[][] cells = new TextView[ROWS][DAYS + 2];
    private Typeface monoTypeface;

    private final Handler autoUpdateHandler = new Handler(Looper.getMainLooper());
    private Runnable updateHighlightRunnable;

    @Override protected int getLayoutResId() { return R.layout.fragment_schedule; }

    @Override
    protected void setupViews(View view) {
        viewFlipper = view.findViewById(R.id.viewFlipper);
        scheduleTable = view.findViewById(R.id.scheduleTable);
        groupTextInputLayout = view.findViewById(R.id.groupTextInputLayout);
        groupAutoComplete = view.findViewById(R.id.groupAutoComplete);
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        retryButton = view.findViewById(R.id.retryButton);

        if (swipeRefreshLayout instanceof SmartSwipeRefreshLayout) {
            ((SmartSwipeRefreshLayout) swipeRefreshLayout).setTargetScrollableView(view.findViewById(R.id.scrollView));
        }

        int colorAccent = ThemeManager.getThemeColor(requireContext(), R.attr.colorAccent);
        swipeRefreshHelper = new SwipeRefreshHelper(swipeRefreshLayout, colorAccent);
        swipeRefreshHelper.setOnRefreshListener(() -> viewModel.refreshAll());

        try {
            monoTypeface = ResourcesCompat.getFont(requireContext(), R.font.roboto_mono);
        } catch (Exception e) {
            AppLogger.w(TAG, "No se pudo cargar la fuente roboto_mono; usando Typeface.MONOSPACE de respaldo");
            monoTypeface = Typeface.MONOSPACE;
        }
        if (monoTypeface == null) monoTypeface = Typeface.MONOSPACE;

        View.OnClickListener retryListener = v -> viewModel.refreshAll();
        if (retryButton != null) retryButton.setOnClickListener(retryListener);
        if (view.findViewById(R.id.errorLayout) != null) view.findViewById(R.id.errorLayout).setOnClickListener(retryListener);
        if (view.findViewById(R.id.emptyLayout) != null) view.findViewById(R.id.emptyLayout).setOnClickListener(retryListener);

        viewModel = new ViewModelProvider(requireActivity()).get(ScheduleViewModel.class);
        viewModel.loadGroups();
    }

    @Override
    protected void setupObservers() {
        viewModel.getCombinedState().observe(getViewLifecycleOwner(), this::updateUiState);

        viewModel.getGroupsList().observe(getViewLifecycleOwner(), groups -> {
            if (groups != null && !groups.isEmpty()) {
                List<String> names = new ArrayList<>();
                for (SchoolGroup sg : groups) names.add(sg.getName());
                
                ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, names);
                groupAutoComplete.setAdapter(adapter);
                groupAutoComplete.setOnItemClickListener((p, v, pos, id) -> viewModel.selectGroup(names.get(pos)));
                String current = viewModel.getCurrentGroup().getValue();
                groupAutoComplete.setText(current != null && names.contains(current) ? current : names.get(0), false);
            }
        });

        viewModel.getNormalScheduleMap().observe(getViewLifecycleOwner(), map -> {
            if (viewModel.getCombinedState().getValue() != null && viewModel.getCombinedState().getValue() == ScheduleViewModel.STATE_SUCCESS) {
                displaySchedule();
            }
        });
    }

    private void updateUiState(int state) {
        if (viewFlipper != null) {
            int childIndex = 0; // Loading
            if (state == ScheduleViewModel.STATE_SUCCESS) childIndex = 1; // Content (scrollView)
            else if (state == ScheduleViewModel.STATE_EMPTY) childIndex = 2; // Empty
            else if (state == ScheduleViewModel.STATE_ERROR) childIndex = 3; // Error
            viewFlipper.setDisplayedChild(childIndex);
        }
        if (swipeRefreshHelper != null && state != ScheduleViewModel.STATE_LOADING) {
            swipeRefreshHelper.finishRefresh();
        }
        if (groupTextInputLayout != null) {
            groupTextInputLayout.setVisibility(state == ScheduleViewModel.STATE_SUCCESS || state == ScheduleViewModel.STATE_EMPTY ? View.VISIBLE : View.GONE);
        }
    }

    private void displaySchedule() {
        String group = viewModel.getCurrentGroup().getValue();
        if (group == null) return;

        Map<String, String[][]> map = viewModel.getNormalScheduleMap().getValue();
        String[][] matrix = map != null ? map.get(group) : null;

        if (matrix == null) return;
        renderTable(matrix);
        applyHighlight();
        startPeriodicUpdate();
    }

    private void renderTable(String[][] matrix) {
        int sCol = ThemeManager.getThemeColor(requireContext(), R.attr.colorScheduleSubject);
        int fCol = ThemeManager.getThemeColor(requireContext(), R.attr.colorScheduleBreak);
        int hCol = ThemeManager.getThemeColor(requireContext(), R.attr.colorScheduleHeader);
        int tCol = ThemeManager.getThemeColor(requireContext(), R.attr.colorText);

        scheduleTable.removeAllViews();
        addHeaderRow(hCol, tCol);

        for (int r = 0; r < ROWS; r++) {
            TableRow row = new TableRow(requireContext());
            boolean isBreak = (r == 0 || r == 2 || r == 4 || r == 6 || r == 8 || r == 10 || r == 12 || r == 14);
            
            cells[r][0] = createCell(TURN_LABELS[r], hCol, tCol, true, false);
            cells[r][1] = createCell(OFFICIAL_TIME_RANGES[r], hCol, tCol, true, false);
            row.addView(cells[r][0]);
            row.addView(cells[r][1]);

            for (int d = 0; d < DAYS; d++) {
                String sub = (matrix != null && r < matrix.length && d < matrix[r].length) ? matrix[r][d] : "";
                if (sub == null) sub = "";
                
                int bg = sCol;
                if (sub.equals("-") || sub.equals("—") || sub.equals("///") || isBreak) {
                    bg = fCol;
                } else if (sub.equals("?") || sub.isEmpty()) {
                    bg = fCol;
                }

                cells[r][d + 2] = createCell(sub.isEmpty() ? "?" : sub, bg, tCol, false, true);
                row.addView(cells[r][d + 2]);
            }
            scheduleTable.addView(row);
        }
    }

    private void addHeaderRow(int bg, int text) {
        TableRow hRow = new TableRow(requireContext());
        hRow.addView(createCell(getString(R.string.label_shift), bg, text, true, false));
        hRow.addView(createCell(getString(R.string.label_time), bg, text, true, false));
        String[] days = {"Lunes", "Martes", "Miércoles", "Jueves", "Viernes"};
        for (String d : days) hRow.addView(createCell(d, bg, text, true, false));
        scheduleTable.addView(hRow);
    }

    private TextView createCell(String text, int bg, int textColor, boolean bold, boolean center) {
        TextView tv = new TextView(requireContext());
        tv.setText(text);
        tv.setTextSize(12);
        tv.setPadding(16, 16, 16, 16);
        tv.setBackgroundColor(bg);
        tv.setTextColor(textColor);
        tv.setGravity(center ? Gravity.CENTER : Gravity.CENTER_VERTICAL);
        tv.setTypeface(monoTypeface, bold ? Typeface.BOLD : Typeface.NORMAL);
        return tv;
    }

    private void applyHighlight() {
        if (OFFICIAL_TIME_RANGES == null) return;
        Calendar now = Calendar.getInstance();
        int day = now.get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY;
        if (day < 0 || day >= DAYS) return;

        int nowMin = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE);
        int activeRow = -1;

        for (int r = 0; r < ROWS; r++) {
            String[] parts = OFFICIAL_TIME_RANGES[r].split("-");
            if (parts.length == 2) {
                int start = parseMin(parts[0].trim());
                int end = parseMin(parts[1].trim());
                if (nowMin >= start && nowMin < end) { activeRow = r; break; }
            }
        }

        if (activeRow != -1 && activeRow < ROWS) {
            int hPrim = ThemeManager.getThemeColor(requireContext(), R.attr.colorScheduleHighlightHeader);
            int hSec = ThemeManager.getThemeColor(requireContext(), R.attr.colorScheduleHighlightCell);
            if (cells[activeRow][0] != null) cells[activeRow][0].setBackgroundColor(hPrim);
            if (cells[activeRow][1] != null) cells[activeRow][1].setBackgroundColor(hPrim);
            if (cells[activeRow][day + 2] != null) cells[activeRow][day + 2].setBackgroundColor(hSec);
        }
    }

    private int parseMin(String time) {
        String[] p = time.split(":");
        return Integer.parseInt(p[0]) * 60 + Integer.parseInt(p[1]);
    }

    private void startPeriodicUpdate() {
        stopPeriodicUpdate();
        updateHighlightRunnable = new Runnable() {
            @Override
            public void run() {
                applyHighlight();
                autoUpdateHandler.postDelayed(this, 60000);
            }
        };
        autoUpdateHandler.postDelayed(updateHighlightRunnable, 60000);
    }

    private void stopPeriodicUpdate() {
        if (updateHighlightRunnable != null) autoUpdateHandler.removeCallbacks(updateHighlightRunnable);
    }

    @Override
    public void onPause() {
        super.onPause();
        stopPeriodicUpdate();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        stopPeriodicUpdate();
        if (swipeRefreshHelper != null) swipeRefreshHelper.cleanup();
    }
}
