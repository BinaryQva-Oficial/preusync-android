/**
 * ============================================================================
 * Proyecto: PreuSync
 * Clase: EphemeridesViewModel.java
 * Versión: v7.0.0
 * Descripción: ViewModel para la gestión de efemérides. Implementa soporte 
 *              híbrido Room/API.
 * Autor: JiroxDEV
 * Licensed under the GNU Affero General Public License v3
 * ============================================================================
 */

package binaryqva.educative.preusync.ui.viewmodels;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import binaryqva.educative.preusync.data.repositories.EphemerisRepository;
import binaryqva.educative.preusync.network.models.ApiResponse;
import binaryqva.educative.preusync.network.models.Ephemeris;
import binaryqva.educative.preusync.utils.common.AppUtils;
import binaryqva.educative.preusync.utils.common.PreferenceManager;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EphemeridesViewModel extends AndroidViewModel {

    public static final int STATE_LOADING = 0;
    public static final int STATE_SUCCESS = 1;
    public static final int STATE_EMPTY = 2;
    public static final int STATE_ERROR = 3;

    private final MutableLiveData<List<Ephemeris>> ephemeridesList = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Integer> ephemeridesState = new MutableLiveData<>(STATE_LOADING);
    private final MutableLiveData<String> formattedDate = new MutableLiveData<>();

    private final EphemerisRepository repository;
    private final PreferenceManager preferenceManager;

    private final SimpleDateFormat uiDateFormat = new SimpleDateFormat("d 'de' MMMM", new Locale("es"));
    private final SimpleDateFormat dbDateFormat = new SimpleDateFormat("dd/MM", Locale.getDefault());

    private Calendar selectedDate = Calendar.getInstance();

    public EphemeridesViewModel(@NonNull Application application) {
        super(application);
        this.repository = new EphemerisRepository(application);
        this.preferenceManager = PreferenceManager.getInstance(application);
        updateDateUI();
        loadEphemerides();
    }

    public LiveData<List<Ephemeris>> getEphemerisList() { return ephemeridesList; }
    public LiveData<Integer> getEphemerisState() { return ephemeridesState; }
    public LiveData<String> getFormattedDate() { return formattedDate; }

    public void setSelectedDate(int day, int month) {
        selectedDate.set(Calendar.DAY_OF_MONTH, day);
        selectedDate.set(Calendar.MONTH, month);
        updateDateUI();
        loadEphemerides();
    }

    public Calendar getSelectedDate() { return (Calendar) selectedDate.clone(); }

    private void updateDateUI() { formattedDate.setValue(uiDateFormat.format(selectedDate.getTime())); }

    public void loadEphemerides() {
        final String dateStr = dbDateFormat.format(selectedDate.getTime());
        ephemeridesState.setValue(STATE_LOADING);

        if (!AppUtils.isConnected(getApplication())) {
            loadFromRoom(dateStr);
            return;
        }

        repository.getEphemeris(dateStr, new Callback<ApiResponse<List<Ephemeris>>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<List<Ephemeris>>> call, @NonNull Response<ApiResponse<List<Ephemeris>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<Ephemeris> data = response.body().getData();
                    ephemeridesList.setValue(data);
                    ephemeridesState.setValue(data.isEmpty() ? STATE_EMPTY : STATE_SUCCESS);
                } else loadFromRoom(dateStr);
            }

            @Override public void onFailure(@NonNull Call<ApiResponse<List<Ephemeris>>> call, @NonNull Throwable t) {
                loadFromRoom(dateStr);
            }
        });
    }

    private void loadFromRoom(String date) {
        repository.getLocalEphemeris(date, data -> {
            if (data != null && !data.isEmpty()) {
                ephemeridesList.setValue(data);
                ephemeridesState.setValue(STATE_SUCCESS);
            } else {
                ephemeridesState.setValue(STATE_ERROR);
            }
        });
    }

    public void refresh() { loadEphemerides(); }
}


