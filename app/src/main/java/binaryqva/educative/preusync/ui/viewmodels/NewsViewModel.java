/**
 * ============================================================================
 * Proyecto: PreuSync
 * Clase: NewsViewModel.java
 * Versión: v8.1.0
 * Descripción: ViewModel encargado de gestionar el feed de noticias.
 *              Soporte de logs estratégicos y manejo unificado de estados.
 * Autor: JiroxDEV
 * Licensed under the GNU Affero General Public License v3
 * ============================================================================
 */

package binaryqva.educative.preusync.ui.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

import binaryqva.educative.preusync.data.repositories.NewsRepository;
import binaryqva.educative.preusync.debug.AppLogger;
import binaryqva.educative.preusync.network.models.ApiResponse;
import binaryqva.educative.preusync.network.models.News;
import binaryqva.educative.preusync.utils.common.AppUtils;
import binaryqva.educative.preusync.utils.common.PaginationState;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NewsViewModel extends BaseViewModel<News> {

    private static final String TAG = "PreuSync_NewsVM";
    private static final int PAGE_SIZE = 10;
    private final NewsRepository repository;
    private int currentPage = 0;
    private boolean isLoadingMore = false;

    public NewsViewModel(@NonNull Application application) {
        super(application);
        this.repository = new NewsRepository(application);
    }

    @Override
    public void refresh() {
        AppLogger.d(TAG, "refresh: Iniciando recarga de noticias...");
        currentPage = 0;
        isLoadingMore = false;
        paginationState.setValue(PaginationState.loading());
        loadPage(0, true);
    }

    public void loadMore() {
        PaginationState<News> current = paginationState.getValue();
        if (current == null || isLoadingMore || !current.hasMore || current.state == PaginationState.STATE_ERROR) return;
        AppLogger.d(TAG, "loadMore: Cargando página de noticias: " + currentPage);
        isLoadingMore = true;
        loadPage(currentPage, false);
    }

    private void loadPage(int page, boolean isRefresh) {
        AppLogger.d(TAG, "loadPage: page=" + page + ", isRefresh=" + isRefresh);

        if (!AppUtils.isConnected(getApplication())) {
            AppLogger.w(TAG, "loadPage: Sin conexión a internet, consultando Room local...");
            if (isRefresh) {
                repository.getLocalNews(localData -> {
                    if (localData != null && !localData.isEmpty()) {
                        AppLogger.d(TAG, "loadPage: Obtenidas " + localData.size() + " noticias locales de Room");
                        paginationState.setValue(PaginationState.content(localData, false));
                    } else {
                        AppLogger.w(TAG, "loadPage: Room local vacío para noticias");
                        paginationState.setValue(PaginationState.empty());
                    }
                });
            }
            isLoadingMore = false;
            return;
        }

        int start = page * PAGE_SIZE;
        repository.getNewsRange(start, PAGE_SIZE, new Callback<ApiResponse<List<News>>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<List<News>>> call, @NonNull Response<ApiResponse<List<News>>> response) {
                isLoadingMore = false;
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<News> items = response.body().getData();
                    AppLogger.d(TAG, "getNewsRange onResponse: Respuesta exitosa con " + (items != null ? items.size() : 0) + " noticias");
                    updateState(items, isRefresh);
                } else {
                    String err = response.body() != null ? response.body().getError() : ("HTTP " + response.code());
                    AppLogger.w(TAG, "getNewsRange error: " + err + ", consultando Room local...");
                    if (isRefresh) {
                        repository.getLocalNews(localData -> {
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
            public void onFailure(@NonNull Call<ApiResponse<List<News>>> call, @NonNull Throwable t) {
                isLoadingMore = false;
                AppLogger.e(TAG, "getNewsRange onFailure: " + t.getMessage(), t);
                if (isRefresh) {
                    repository.getLocalNews(localData -> {
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

    private void updateState(List<News> newItems, boolean isRefresh) {
        PaginationState<News> current = paginationState.getValue();
        List<News> allItems = (isRefresh || current == null) ? new ArrayList<>() : new ArrayList<>(current.items);
        if (newItems != null) {
            allItems.addAll(newItems);
        }
        if (isRefresh) currentPage = 1; else currentPage++;
        
        if (allItems.isEmpty()) {
            AppLogger.d(TAG, "updateState: Lista vacía de noticias, emitiendo PaginationState.empty()");
            paginationState.setValue(PaginationState.empty());
        } else {
            boolean hasMore = newItems != null && newItems.size() >= PAGE_SIZE;
            AppLogger.d(TAG, "updateState: Emitiendo PaginationState.content con " + allItems.size() + " noticias. HasMore: " + hasMore);
            paginationState.setValue(PaginationState.content(allItems, hasMore));
        }
    }
}
