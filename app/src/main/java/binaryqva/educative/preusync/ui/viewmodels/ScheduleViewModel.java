/**
 * ============================================================================
 * Proyecto: PreuSync
 * Clase: ScheduleViewModel.java
 * Versión: v9.0.0
 * Descripción: ViewModel para la gestión del horario escolar. Soporta filtrado
 *              por escuela activa y procesamiento de matriz de 16 turnos.
 * Autor: JiroxDEV
 * Licensed under the GNU Affero General Public License v3
 * ============================================================================
 */

package binaryqva.educative.preusync.ui.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import binaryqva.educative.preusync.data.repositories.ScheduleRepository;
import binaryqva.educative.preusync.debug.AppLogger;
import binaryqva.educative.preusync.network.models.ApiResponse;
import binaryqva.educative.preusync.network.models.Schedule;
import binaryqva.educative.preusync.network.models.SchoolGroup;
import binaryqva.educative.preusync.utils.common.AppUtils;
import binaryqva.educative.preusync.utils.common.PreferenceManager;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ScheduleViewModel extends AndroidViewModel {

    private static final String TAG = "PreuSync_ScheduleVM";

    public static final int STATE_LOADING = 0;
    public static final int STATE_SUCCESS = 1;
    public static final int STATE_EMPTY = 2;
    public static final int STATE_ERROR = 3;

    private final MutableLiveData<List<SchoolGroup>> groupsList = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Integer> groupsState = new MutableLiveData<>(STATE_LOADING);
    
    private final MutableLiveData<Map<String, String[][]>> normalScheduleMap = new MutableLiveData<>(new HashMap<>());
    private final MutableLiveData<Map<String, String[][]>> continuousScheduleMap = new MutableLiveData<>(new HashMap<>());
    private final MutableLiveData<Map<String, Boolean>> scheduleTypeByGroup = new MutableLiveData<>(new HashMap<>());
    private final MutableLiveData<String> currentGroup = new MutableLiveData<>(null);
    private final MutableLiveData<Integer> scheduleState = new MutableLiveData<>(STATE_LOADING);
    private final MutableLiveData<Integer> combinedState = new MutableLiveData<>(STATE_LOADING);

    private final ScheduleRepository repository;
    private final PreferenceManager preferenceManager;

    public ScheduleViewModel(@NonNull Application application) {
        super(application);
        this.repository = new ScheduleRepository(application);
        this.preferenceManager = PreferenceManager.getInstance(application);
    }

    public LiveData<List<SchoolGroup>> getGroupsList() { return groupsList; }
    public LiveData<Map<String, String[][]>> getNormalScheduleMap() { return normalScheduleMap; }
    public LiveData<Map<String, String[][]>> getContinuousScheduleMap() { return continuousScheduleMap; }
    public LiveData<Map<String, Boolean>> getScheduleTypeByGroup() { return scheduleTypeByGroup; }
    public LiveData<String> getCurrentGroup() { return currentGroup; }
    public LiveData<Integer> getCombinedState() { return combinedState; }

    public void loadGroups() {
        groupsState.setValue(STATE_LOADING);
        String schoolId = preferenceManager.getSchoolId();
        AppLogger.d(TAG, "loadGroups para schoolId: " + schoolId);

        if (schoolId == null || schoolId.isEmpty()) {
            AppLogger.w(TAG, "loadGroups: No hay schoolId configurado en preferencias");
            groupsState.setValue(STATE_EMPTY);
            updateCombinedState();
            return;
        }

        if (!AppUtils.isConnected(getApplication())) {
            AppLogger.w(TAG, "loadGroups: Sin conexión a internet");
            updateCombinedState();
            return;
        }

        repository.getGroups(schoolId, new Callback<ApiResponse<List<SchoolGroup>>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<List<SchoolGroup>>> call, @NonNull Response<ApiResponse<List<SchoolGroup>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<SchoolGroup> groups = response.body().getData();
                    List<SchoolGroup> items = groups != null ? groups : new ArrayList<>();
                    groupsList.setValue(items);
                    groupsState.setValue(items.isEmpty() ? STATE_EMPTY : STATE_SUCCESS);
                    AppLogger.d(TAG, "loadGroups onResponse: " + items.size() + " grupos recibidos");
                    if (!items.isEmpty()) {
                        String userGrp = preferenceManager.getUserGroup();
                        String last = preferenceManager.getLastScheduleGroup();
                        String targetGroup = userGrp != null && !userGrp.isEmpty() ? userGrp : last;
                        
                        boolean found = false;
                        if (targetGroup != null) {
                            for (SchoolGroup sg : items) if (targetGroup.equals(sg.getName())) { found = true; break; }
                        }
                        String selected = found ? targetGroup : items.get(0).getName();
                        selectGroup(selected);
                    }
                } else {
                    AppLogger.w(TAG, "loadGroups error: HTTP " + response.code());
                    groupsState.setValue(STATE_EMPTY);
                }
                updateCombinedState();
            }

            @Override public void onFailure(@NonNull Call<ApiResponse<List<SchoolGroup>>> call, @NonNull Throwable t) {
                AppLogger.e(TAG, "loadGroups onFailure: " + t.getMessage(), t);
                groupsState.setValue(STATE_EMPTY);
                updateCombinedState();
            }
        });
    }

    public void loadScheduleForGroup(String group) {
        if (group == null || group.isEmpty()) return;
        scheduleState.setValue(STATE_LOADING);
        currentGroup.setValue(group);
        updateCombinedState();

        String schoolId = preferenceManager.getSchoolId();
        AppLogger.d(TAG, "loadScheduleForGroup: group=" + group + ", schoolId=" + schoolId);

        if (schoolId == null || schoolId.isEmpty()) {
            loadFromRoom(group);
            return;
        }

        if (!AppUtils.isConnected(getApplication())) {
            loadFromRoom(group);
            return;
        }

        repository.getSchedule(group, schoolId, new Callback<ApiResponse<List<Schedule>>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<List<Schedule>>> call, @NonNull Response<ApiResponse<List<Schedule>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<Schedule> data = response.body().getData();
                    if (data == null || data.isEmpty()) {
                        AppLogger.d(TAG, "getSchedule: Sin horario para grupo " + group);
                        scheduleState.setValue(STATE_EMPTY);
                    } else {
                        AppLogger.d(TAG, "getSchedule: Obtenidas " + data.size() + " entradas para grupo " + group);
                        processSchedule(group, data);
                        scheduleState.setValue(STATE_SUCCESS);
                        preferenceManager.setLastScheduleGroup(group);
                    }
                } else {
                    AppLogger.w(TAG, "getSchedule error: HTTP " + response.code() + ", consultando Room local...");
                    loadFromRoom(group);
                }
                updateCombinedState();
            }

            @Override public void onFailure(@NonNull Call<ApiResponse<List<Schedule>>> call, @NonNull Throwable t) {
                AppLogger.e(TAG, "getSchedule onFailure: " + t.getMessage(), t);
                loadFromRoom(group);
                updateCombinedState();
            }
        });
    }

    private void loadFromRoom(String group) {
        repository.getLocalSchedule(group, data -> {
            if (data != null && !data.isEmpty()) {
                AppLogger.d(TAG, "loadFromRoom: Recuperado horario de Room para " + group);
                processSchedule(group, data);
                scheduleState.setValue(STATE_SUCCESS);
            } else {
                AppLogger.w(TAG, "loadFromRoom: Sin datos en Room para " + group);
                scheduleState.setValue(STATE_EMPTY);
            }
            updateCombinedState();
        });
    }

    private void processSchedule(String group, List<Schedule> rows) {
        String[][] matrix = new String[16][5];
        for(int i=0; i<16; i++) for(int j=0; j<5; j++) { matrix[i][j]=""; }

        if (rows != null) {
            for (Schedule s : rows) {
                int d = getDayIndex(s.getDay());
                int r = getRowIndexForShift(s.getShift());
                if (d >= 0 && r >= 0 && r < 16) {
                    matrix[r][d] = s.getSubject() != null ? s.getSubject() : "";
                }
            }
        }
        
        Map<String, String[][]> nMap = new HashMap<>(normalScheduleMap.getValue() != null ? normalScheduleMap.getValue() : new HashMap<>());
        nMap.put(group, matrix);
        normalScheduleMap.setValue(nMap);
    }

    private int getRowIndexForShift(int shift) {
        if (shift >= 0 && shift < 16) {
            return shift;
        }
        return -1;
    }

    private int getDayIndex(String day) {
        if (day == null) return -1;
        String d = day.trim().toUpperCase();
        if (d.startsWith("L")) return 0;
        if (d.startsWith("MAR") || d.equals("M")) return 1;
        if (d.startsWith("MIÉR") || d.startsWith("MIER") || d.equals("X")) return 2;
        if (d.startsWith("J")) return 3;
        if (d.startsWith("V")) return 4;
        return -1;
    }

    public void selectGroup(String group) {
        if (group != null && !group.equals(currentGroup.getValue())) {
            currentGroup.setValue(group);
            loadScheduleForGroup(group);
        }
    }

    public void refreshAll() { loadGroups(); }

    private void updateCombinedState() {
        Integer gS = groupsState.getValue(), sS = scheduleState.getValue();
        if (gS == null || sS == null) { combinedState.setValue(STATE_LOADING); return; }
        if (gS == STATE_ERROR || sS == STATE_ERROR) combinedState.setValue(STATE_ERROR);
        else if (gS == STATE_LOADING || sS == STATE_LOADING) combinedState.setValue(STATE_LOADING);
        else if (gS == STATE_EMPTY || sS == STATE_EMPTY) combinedState.setValue(STATE_EMPTY);
        else combinedState.setValue(STATE_SUCCESS);
    }
}
