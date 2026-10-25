/**
 * ============================================================================
 * Proyecto: PreuSync
 * Clase: PostsViewModel.java
 * Versión: v8.1.0
 * Descripción: ViewModel encargado de gestionar el feed de la comunidad.
 *              Soporte de logs estratégicos y manejo unificado de estados.
 * Autor: JiroxDEV
 * Licensed under the GNU Affero General Public License v3
 * ============================================================================
 */

package binaryqva.educative.preusync.ui.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.ArrayList;
import java.util.List;

import binaryqva.educative.preusync.data.repositories.PostRepository;
import binaryqva.educative.preusync.debug.AppLogger;
import binaryqva.educative.preusync.network.models.ApiResponse;
import binaryqva.educative.preusync.network.models.Post;
import binaryqva.educative.preusync.network.requests.CreatePostRequest;
import binaryqva.educative.preusync.utils.common.AppUtils;
import binaryqva.educative.preusync.utils.common.PaginationState;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PostsViewModel extends BaseViewModel<Post> {

    private static final String TAG = "PreuSync_PostsVM";
    private static final int PAGE_SIZE = 10;
    private final PostRepository repository;
    private int currentPage = 0;
    private boolean isLoadingMore = false;
    private final MutableLiveData<Boolean> operationSuccess = new MutableLiveData<>();

    public PostsViewModel(@NonNull Application application) {
        super(application);
        this.repository = new PostRepository(application);
    }

    public LiveData<Boolean> getOperationSuccess() { return operationSuccess; }

    @Override
    public void refresh() {
        AppLogger.d(TAG, "refresh: Iniciando recarga de posts...");
        currentPage = 0;
        isLoadingMore = false;
        paginationState.setValue(PaginationState.loading());
        loadPage(0, true);
    }

    public void loadMore() {
        PaginationState<Post> current = paginationState.getValue();
        if (current == null || isLoadingMore || !current.hasMore || current.state == PaginationState.STATE_ERROR) return;
        AppLogger.d(TAG, "loadMore: Cargando página de posts: " + currentPage);
        isLoadingMore = true;
        loadPage(currentPage, false);
    }

    private void loadPage(int page, boolean isRefresh) {
        AppLogger.d(TAG, "loadPage: page=" + page + ", isRefresh=" + isRefresh);

        if (!AppUtils.isConnected(getApplication())) {
            AppLogger.w(TAG, "loadPage: Sin conexión a internet, consultando Room local...");
            if (isRefresh) {
                repository.getLocalPosts(localData -> {
                    if (localData != null && !localData.isEmpty()) {
                        AppLogger.d(TAG, "loadPage: Obtenidos " + localData.size() + " posts locales de Room");
                        paginationState.setValue(PaginationState.content(localData, false));
                    } else {
                        AppLogger.w(TAG, "loadPage: Room local vacío para posts");
                        paginationState.setValue(PaginationState.empty());
                    }
                });
            }
            isLoadingMore = false;
            return;
        }

        int start = page * PAGE_SIZE;
        repository.getPostsRange(start, PAGE_SIZE, new Callback<ApiResponse<List<Post>>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<List<Post>>> call, @NonNull Response<ApiResponse<List<Post>>> response) {
                isLoadingMore = false;
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<Post> items = response.body().getData();
                    AppLogger.d(TAG, "getPostsRange onResponse: Respuesta exitosa con " + (items != null ? items.size() : 0) + " posts");
                    updateState(items, isRefresh);
                } else {
                    String err = response.body() != null ? response.body().getError() : ("HTTP " + response.code());
                    AppLogger.w(TAG, "getPostsRange error: " + err + ", consultando Room local...");
                    if (isRefresh) {
                        repository.getLocalPosts(localData -> {
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
            public void onFailure(@NonNull Call<ApiResponse<List<Post>>> call, @NonNull Throwable t) {
                isLoadingMore = false;
                AppLogger.e(TAG, "getPostsRange onFailure: " + t.getMessage(), t);
                if (isRefresh) {
                    repository.getLocalPosts(localData -> {
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

    private void updateState(List<Post> newItems, boolean isRefresh) {
        PaginationState<Post> current = paginationState.getValue();
        List<Post> allItems = (isRefresh || current == null) ? new ArrayList<>() : new ArrayList<>(current.items);
        if (newItems != null) {
            allItems.addAll(newItems);
        }
        if (isRefresh) currentPage = 1; else currentPage++;
        
        if (allItems.isEmpty()) {
            AppLogger.d(TAG, "updateState: Lista vacía de posts, emitiendo PaginationState.empty()");
            paginationState.setValue(PaginationState.empty());
        } else {
            boolean hasMore = newItems != null && newItems.size() >= PAGE_SIZE;
            AppLogger.d(TAG, "updateState: Emitiendo PaginationState.content con " + allItems.size() + " posts. HasMore: " + hasMore);
            paginationState.setValue(PaginationState.content(allItems, hasMore));
        }
    }

    public void createPost(String title, String details, String imageBase64) {
        isLoading.setValue(true);
        AppLogger.d(TAG, "createPost: Publicando post '" + title + "'...");
        CreatePostRequest request = new CreatePostRequest(title, details);
        if (imageBase64 != null && !imageBase64.isEmpty()) request.setImageBase64(imageBase64);

        repository.createPost(request, new Callback<ApiResponse<Post>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<Post>> call, @NonNull Response<ApiResponse<Post>> response) {
                isLoading.setValue(false);
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    AppLogger.d(TAG, "createPost onResponse: Post creado exitosamente!");
                    operationSuccess.setValue(true);
                    refresh();
                } else {
                    String err = response.body() != null ? response.body().getError() : ("HTTP " + response.code());
                    AppLogger.w(TAG, "createPost error: " + err);
                    operationSuccess.setValue(false);
                    paginationState.setValue(PaginationState.error(err));
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<Post>> call, @NonNull Throwable t) {
                isLoading.setValue(false);
                AppLogger.e(TAG, "createPost onFailure: " + t.getMessage(), t);
                operationSuccess.setValue(false);
                paginationState.setValue(PaginationState.error(t.getMessage()));
            }
        });
    }

    public void deletePost(String id) {
        repository.deletePost(id, new Callback<ApiResponse<Void>>() {
            @Override public void onResponse(@NonNull Call<ApiResponse<Void>> call, @NonNull Response<ApiResponse<Void>> response) {
                operationSuccess.setValue(response.isSuccessful());
            }
            @Override public void onFailure(@NonNull Call<ApiResponse<Void>> call, @NonNull Throwable t) {
                operationSuccess.setValue(false);
            }
        });
    }

    public void reportPost(String id) {
        repository.reportPost(id, new Callback<ApiResponse<Void>>() {
            @Override public void onResponse(@NonNull Call<ApiResponse<Void>> call, @NonNull Response<ApiResponse<Void>> response) {
                operationSuccess.setValue(response.isSuccessful());
            }
            @Override public void onFailure(@NonNull Call<ApiResponse<Void>> call, @NonNull Throwable t) {
                operationSuccess.setValue(false);
            }
        });
    }
}
