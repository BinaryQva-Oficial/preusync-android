/**
 * ============================================================================
 * Proyecto: PreuSync
 * Clase: SchoolEventsViewModel.java
 * Versión: v8.0.0
 * Descripción: ViewModel encargado de gestionar los eventos escolares.
 *              Soporte de logs estratégicos y filtrado por schoolId.
 * Autor: JiroxDEV
 * Licensed under the GNU Affero General Public License v3
 * ============================================================================
 */

package binaryqva.educative.preusync.ui.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

import binaryqva.educative.preusync.data.repositories.EventsRepository;
import binaryqva.educative.preusync.debug.AppLogger;
import binaryqva.educative.preusync.network.models.ApiResponse;
import binaryqva.educative.preusync.network.models.Event;
import binaryqva.educative.preusync.utils.common.AppUtils;
import binaryqva.educative.preusync.utils.common.PaginationState;
import binaryqva.educative.preusync.utils.common.PreferenceManager;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SchoolEventsViewModel extends BaseViewModel<Event> {

    private static final String TAG = "PreuSync_SchoolEventsVM";
    private static final int PAGE_SIZE = 10;
    private final EventsRepository repository;
    private int currentPage = 0;
    private boolean isLoadingMore = false;

    public SchoolEventsViewModel(@NonNull Application application) {
        super(application);
        this.repository = new EventsRepository(application);
    }

    @Override
    public void refresh() {
        AppLogger.d(TAG, "refresh: Iniciando recarga de eventos escolares...");
        currentPage = 0;
        isLoadingMore = false;
        paginationState.setValue(PaginationState.loading());
        loadPage(0, true);
    }

    public void loadMore() {
        PaginationState<Event> current = paginationState.getValue();
        if (current == null || isLoadingMore || !current.hasMore || current.state == PaginationState.STATE_ERROR) return;
        AppLogger.d(TAG, "loadMore: Cargando página siguiente: " + currentPage);
        isLoadingMore = true;
        loadPage(currentPage, false);
    }

    private void loadPage(int page, boolean isRefresh) {
        String schoolId = PreferenceManager.getInstance(getApplication()).getSchoolId();
        AppLogger.d(TAG, "loadPage: page=" + page + ", isRefresh=" + isRefresh + ", schoolId=" + schoolId);

        if (!AppUtils.isConnected(getApplication())) {
            AppLogger.w(TAG, "loadPage: Sin conexión a internet, consultando Room local...");
            if (isRefresh) {
                repository.getLocalEvents("school", localData -> {
                    if (localData != null && !localData.isEmpty()) {
                        AppLogger.d(TAG, "loadPage: Obtenidos " + localData.size() + " eventos escolares de Room");
                        paginationState.setValue(PaginationState.content(localData, false));
                    } else {
                        AppLogger.w(TAG, "loadPage: Room local vacío para eventos escolares");
                        paginationState.setValue(PaginationState.empty());
                    }
                });
            }
            isLoadingMore = false;
            return;
        }

        int start = page * PAGE_SIZE;
        repository.getSchoolEvents(start, PAGE_SIZE, schoolId, new Callback<ApiResponse<List<Event>>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<List<Event>>> call, @NonNull Response<ApiResponse<List<Event>>> response) {
                isLoadingMore = false;
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<Event> items = response.body().getData();
                    AppLogger.d(TAG, "getSchoolEvents onResponse: Respuesta exitosa con " + (items != null ? items.size() : 0) + " eventos");
                    updateState(items, isRefresh);
                } else {
                    String err = response.body() != null ? response.body().getError() : ("HTTP " + response.code());
                    AppLogger.w(TAG, "getSchoolEvents error: " + err + ", buscando en Room local...");
                    if (isRefresh) {
                        repository.getLocalEvents("school", localData -> {
                            if (localData != null && !localData.isEmpty()) {
                                paginationState.setValue(PaginationState.content(localData, false));
                            } else {
                                paginationState.setValue(PaginationState.empty());
                            }
                        });
                    } else {
                        paginationState.setValue(PaginationState.error(err));
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<List<Event>>> call, @NonNull Throwable t) {
                isLoadingMore = false;
                AppLogger.e(TAG, "getSchoolEvents onFailure: " + t.getMessage(), t);
                if (isRefresh) {
                    repository.getLocalEvents("school", localData -> {
                        if (localData != null && !localData.isEmpty()) {
                            paginationState.setValue(PaginationState.content(localData, false));
                        } else {
                            paginationState.setValue(PaginationState.empty());
                        }
                    });
                }
            }
        });
    }

    private void updateState(List<Event> newItems, boolean isRefresh) {
        PaginationState<Event> current = paginationState.getValue();
        List<Event> allItems = (isRefresh || current == null) ? new ArrayList<>() : new ArrayList<>(current.items);
        if (newItems != null) {
            allItems.addAll(newItems);
        }
        if (isRefresh) currentPage = 1; else currentPage++;
        
        if (allItems.isEmpty()) {
            AppLogger.d(TAG, "updateState: Lista vacía, emitiendo PaginationState.empty()");
            paginationState.setValue(PaginationState.empty());
        } else {
            boolean hasMore = newItems != null && newItems.size() >= PAGE_SIZE;
            AppLogger.d(TAG, "updateState: Emitiendo PaginationState.content con " + allItems.size() + " items. HasMore: " + hasMore);
            paginationState.setValue(PaginationState.content(allItems, hasMore));
        }
    }
}
