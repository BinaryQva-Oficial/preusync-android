/**
 * ============================================================================
 * Proyecto: PreuSync
 * Archivo: ProfileViewModel.java
 * Versión: v6.1.0
 * Descripción: ViewModel para perfil de usuario y sus publicaciones personales,
 *              con soporte para caché Room/SENSITIVE y logs estratégicos.
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

import binaryqva.educative.preusync.data.repositories.AuthRepository;
import binaryqva.educative.preusync.data.repositories.PostRepository;
import binaryqva.educative.preusync.debug.AppLogger;
import binaryqva.educative.preusync.network.models.ApiResponse;
import binaryqva.educative.preusync.network.models.AuthResponse;
import binaryqva.educative.preusync.network.models.Post;
import binaryqva.educative.preusync.network.models.Profile;
import binaryqva.educative.preusync.network.requests.UpdateProfileRequest;
import binaryqva.educative.preusync.utils.common.AppUtils;
import binaryqva.educative.preusync.utils.common.CacheManager;
import binaryqva.educative.preusync.utils.common.PaginationState;
import binaryqva.educative.preusync.utils.common.PreferenceManager;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileViewModel extends AndroidViewModel {

    private static final String TAG = "PreuSync_ProfileVM";
    private static final int PAGE_SIZE = 10;
    private static final String CACHE_KEY_PROFILE = "profile_data";

    private final MutableLiveData<Profile> userData = new MutableLiveData<>(null);
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> isError = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorCode = new MutableLiveData<>(null);
    private final MutableLiveData<Boolean> isLoggedOut = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> isUpdated = new MutableLiveData<>(false);

    private final MutableLiveData<PaginationState<Post>> postsPaginationState = new MutableLiveData<>(PaginationState.loading());
    private final MutableLiveData<Integer> postCount = new MutableLiveData<>(0);
    private final MutableLiveData<Long> totalLikes = new MutableLiveData<>(0L);

    private final AuthRepository authRepository;
    private final PostRepository postRepository;
    private final PreferenceManager preferenceManager;
    private final CacheManager cacheManager;

    private int currentPage = 0;

    public ProfileViewModel(@NonNull Application application) {
        super(application);
        this.authRepository = new AuthRepository(application);
        this.postRepository = new PostRepository(application);
        this.preferenceManager = PreferenceManager.getInstance(application);
        this.cacheManager = CacheManager.getInstance();
    }

    public LiveData<Profile> getUserData() { return userData; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<Boolean> getIsError() { return isError; }
    public LiveData<String> getErrorCode() { return errorCode; }
    public LiveData<Boolean> getIsLoggedOut() { return isLoggedOut; }
    public LiveData<Boolean> getIsUpdated() { return isUpdated; }
    public LiveData<PaginationState<Post>> getPostsPaginationState() { return postsPaginationState; }
    public LiveData<Integer> getPostCount() { return postCount; }
    public LiveData<Long> getTotalLikes() { return totalLikes; }

    public void loadUserData() {
        AppLogger.d(TAG, "loadUserData: Iniciando carga de datos del perfil...");
        isLoading.setValue(true);
        isError.setValue(false);

        if (!AppUtils.isConnected(getApplication())) {
            AppLogger.w(TAG, "loadUserData: Sin conexión a internet, probando caché local...");
            if (!loadProfileFromCache()) {
                isError.setValue(true);
                errorCode.setValue("Sin conexión");
            }
            isLoading.setValue(false);
            return;
        }

        authRepository.getProfile(new Callback<ApiResponse<Profile>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<Profile>> call, @NonNull Response<ApiResponse<Profile>> response) {
                isLoading.setValue(false);
                AppLogger.d(TAG, "getProfile onResponse: code=" + response.code() + ", isSuccessful=" + response.isSuccessful());

                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    Profile data = response.body().getData();
                    if (data == null) {
                        AppLogger.w(TAG, "getProfile: Respuesta exitosa pero data es null. Probando caché...");
                        if (!loadProfileFromCache()) {
                            isError.setValue(true);
                            errorCode.setValue("Perfil no disponible");
                        }
                        return;
                    }
                    AppLogger.d(TAG, "getProfile: Perfil recibido exitosamente para: " + data.getUsername());
                    userData.setValue(data);
                    isError.setValue(false);

                    if (data.getSchoolId() != null) preferenceManager.setSchoolId(data.getSchoolId());
                    if (data.getUsername() != null) preferenceManager.setUsername(data.getUsername());
                    if (data.getGroup() != null) preferenceManager.setUserGroup(data.getGroup());

                    cacheManager.saveCache(CACHE_KEY_PROFILE, data, CacheManager.CacheSecurity.SENSITIVE);
                } else {
                    String err = response.body() != null ? response.body().getError() : ("HTTP " + response.code());
                    AppLogger.w(TAG, "getProfile error: " + err + ", probando caché local...");
                    if (!loadProfileFromCache()) {
                        isError.setValue(true);
                        errorCode.setValue(err);
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<Profile>> call, @NonNull Throwable t) {
                isLoading.setValue(false);
                AppLogger.e(TAG, "getProfile onFailure: " + t.getMessage(), t);
                if (!loadProfileFromCache()) {
                    isError.setValue(true);
                    errorCode.setValue(t.getMessage());
                }
            }
        });
    }

    public void loadUserPosts(boolean refresh) {
        if (refresh) { currentPage = 0; postsPaginationState.setValue(PaginationState.loading()); }
        if (!refresh && (postsPaginationState.getValue() == null || !postsPaginationState.getValue().hasMore)) return;

        int start = currentPage * PAGE_SIZE;
        AppLogger.d(TAG, "loadUserPosts: start=" + start + ", refresh=" + refresh);

        authRepository.getUserPosts(start, PAGE_SIZE, new Callback<ApiResponse<List<Post>>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<List<Post>>> call, @NonNull Response<ApiResponse<List<Post>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<Post> newItems = response.body().getData();
                    AppLogger.d(TAG, "getUserPosts onResponse: Obtenidos " + (newItems != null ? newItems.size() : 0) + " posts del usuario");
                    updatePostsState(newItems, refresh);
                } else {
                    AppLogger.w(TAG, "getUserPosts error: HTTP " + response.code() + ", intentando fallback a Posts genéricos...");
                    postRepository.getPostsRange(start, PAGE_SIZE, new Callback<ApiResponse<List<Post>>>() {
                        @Override
                        public void onResponse(@NonNull Call<ApiResponse<List<Post>>> call, @NonNull Response<ApiResponse<List<Post>>> response) {
                            if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                                updatePostsState(response.body().getData(), refresh);
                            } else {
                                postsPaginationState.setValue(PaginationState.empty());
                            }
                        }
                        @Override
                        public void onFailure(@NonNull Call<ApiResponse<List<Post>>> call, @NonNull Throwable t) {
                            postsPaginationState.setValue(PaginationState.empty());
                        }
                    });
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<List<Post>>> call, @NonNull Throwable t) {
                AppLogger.e(TAG, "getUserPosts onFailure: " + t.getMessage(), t);
                postsPaginationState.setValue(PaginationState.empty());
            }
        });
    }

    private void updatePostsState(List<Post> newItems, boolean refresh) {
        PaginationState<Post> current = postsPaginationState.getValue();
        List<Post> allItems = (refresh || current == null) ? new ArrayList<>() : new ArrayList<>(current.items);
        if (newItems != null) {
            allItems.addAll(newItems);
        }
        if (refresh) currentPage = 1; else currentPage++;
        
        if (allItems.isEmpty()) {
            postsPaginationState.setValue(PaginationState.empty());
        } else {
            boolean hasMore = newItems != null && newItems.size() >= PAGE_SIZE;
            postsPaginationState.setValue(PaginationState.content(allItems, hasMore));
        }
        postCount.setValue(allItems.size());
    }

    public void logout() {
        authRepository.logout(new Callback<ApiResponse<Void>>() {
            @Override public void onResponse(@NonNull Call<ApiResponse<Void>> call, @NonNull Response<ApiResponse<Void>> response) {
                preferenceManager.clearAccount(); isLoggedOut.setValue(true);
            }
            @Override public void onFailure(@NonNull Call<ApiResponse<Void>> call, @NonNull Throwable t) {
                preferenceManager.clearAccount(); isLoggedOut.setValue(true);
            }
        });
    }

    public void updateUser(HashMap<String, Object> updateData) {
        isLoading.setValue(true);
        UpdateProfileRequest request = new UpdateProfileRequest();
        if (updateData.containsKey("first_name")) request.setFirstName((String) updateData.get("first_name"));
        if (updateData.containsKey("last_name")) request.setLastName((String) updateData.get("last_name"));
        if (updateData.containsKey("role")) request.setRole((String) updateData.get("role"));
        if (updateData.containsKey("school_id")) request.setSchoolId((String) updateData.get("school_id"));
        if (updateData.containsKey("group")) request.setGroup((String) updateData.get("group"));
        if (updateData.containsKey("tutee")) request.setTutee((String) updateData.get("tutee"));
        if (updateData.containsKey("responsibilities")) request.setResponsibilities((String) updateData.get("responsibilities"));
        if (updateData.containsKey("username")) request.setUsername((String) updateData.get("username"));
        if (updateData.containsKey("id_card")) request.setIdCard((String) updateData.get("id_card"));
        if (updateData.containsKey("password")) request.setPassword((String) updateData.get("password"));
        if (updateData.containsKey("avatar_url")) request.setAvatarUrl((String) updateData.get("avatar_url"));

        authRepository.updateProfile(request, new Callback<ApiResponse<Profile>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<Profile>> call, @NonNull Response<ApiResponse<Profile>> response) {
                isLoading.setValue(false);
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    userData.setValue(response.body().getData());
                    isUpdated.setValue(true);
                } else { isError.setValue(true); errorCode.setValue("update"); }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<Profile>> call, @NonNull Throwable t) {
                isLoading.setValue(false);
                isError.setValue(true);
                errorCode.setValue("network");
            }
        });
    }

    private boolean loadProfileFromCache() {
        Profile cached = cacheManager.loadCache(CACHE_KEY_PROFILE, CacheManager.CacheSecurity.SENSITIVE, Profile.class);
        if (cached != null) {
            AppLogger.d(TAG, "loadProfileFromCache: Perfil recuperado de la caché local para " + cached.getUsername());
            userData.setValue(cached);
            isError.setValue(false);
            return true;
        }
        return false;
    }

    public void refresh() { loadUserData(); loadUserPosts(true); }
    public void clearUpdatedState() { isUpdated.setValue(false); }
    public void clearError() { isError.setValue(false); errorCode.setValue(null); }

    public void removePostById(String postId) {
        PaginationState<Post> current = postsPaginationState.getValue();
        if (current != null && current.items != null) {
            List<Post> items = new ArrayList<>(current.items);
            int index = -1;
            for (int i = 0; i < items.size(); i++) {
                if (items.get(i).getId().equals(postId)) { index = i; break; }
            }
            if (index != -1) {
                items.remove(index);
                if (items.isEmpty()) postsPaginationState.setValue(PaginationState.empty());
                else postsPaginationState.setValue(PaginationState.content(items, current.hasMore));
            }
        }
    }

    public void verifyPassword(String username, String password, OnPasswordVerifiedListener listener) {
        authRepository.login(username, password, new Callback<ApiResponse<AuthResponse>>() {
            @Override public void onResponse(@NonNull Call<ApiResponse<AuthResponse>> call, @NonNull Response<ApiResponse<AuthResponse>> response) {
                if (response.isSuccessful()) listener.onVerificationSuccess(); else listener.onVerificationFailed("Error");
            }
            @Override public void onFailure(@NonNull Call<ApiResponse<AuthResponse>> call, @NonNull Throwable t) { listener.onVerificationFailed(t.getMessage()); }
        });
    }

    public interface OnPasswordVerifiedListener { void onVerificationSuccess(); void onVerificationFailed(String error); }
}
