/**
 * ============================================================================
 * Proyecto: PreuSync
 * Clase: AuthViewModel.java
 * Versión: v8.1.0
 * Descripción: ViewModel de Autenticación con sistema de caché inteligente
 *              para la jerarquía nacional (Multi-Tenancy).
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

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import binaryqva.educative.preusync.R;
import binaryqva.educative.preusync.data.repositories.AuthRepository;
import binaryqva.educative.preusync.data.repositories.SchoolLocationRepository;
import binaryqva.educative.preusync.debug.AppLogger;
import binaryqva.educative.preusync.network.models.ApiResponse;
import binaryqva.educative.preusync.network.models.AuthResponse;
import binaryqva.educative.preusync.network.models.Municipality;
import binaryqva.educative.preusync.network.models.Province;
import binaryqva.educative.preusync.network.models.School;
import binaryqva.educative.preusync.network.models.SchoolGroup;
import binaryqva.educative.preusync.network.requests.SignupRequest;
import binaryqva.educative.preusync.utils.common.CacheManager;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AuthViewModel extends AndroidViewModel {

    private static final String TAG = "AuthViewModel";
    private final AuthRepository authRepository;
    private final SchoolLocationRepository locationRepository;
    private final CacheManager cache;

    private final MutableLiveData<List<Province>> provinces = new MutableLiveData<>();
    private final MutableLiveData<List<Municipality>> municipalities = new MutableLiveData<>();
    private final MutableLiveData<List<School>> schools = new MutableLiveData<>();
    private final MutableLiveData<List<SchoolGroup>> groups = new MutableLiveData<>();
    private final MutableLiveData<List<String>> responsibilities = new MutableLiveData<>();

    private final MutableLiveData<ApiResponse<AuthResponse>> loginResult = new MutableLiveData<>();
    private final MutableLiveData<ApiResponse<AuthResponse>> signupResult = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> isLocationLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<Boolean> userExistsResult = new MutableLiveData<>(null);

    public AuthViewModel(@NonNull Application application) {
        super(application);
        this.authRepository = new AuthRepository(application);
        this.locationRepository = new SchoolLocationRepository(application);
        this.cache = CacheManager.getInstance();
        this.cache.init(application);
    }

    // Getters
    public LiveData<List<Province>> getProvinces() { return provinces; }
    public LiveData<List<Municipality>> getMunicipalities() { return municipalities; }
    public LiveData<List<School>> getSchools() { return schools; }
    public LiveData<List<SchoolGroup>> getGroups() { return groups; }
    public LiveData<List<String>> getResponsibilities() { return responsibilities; }
    public LiveData<ApiResponse<AuthResponse>> getLoginResult() { return loginResult; }
    public LiveData<ApiResponse<AuthResponse>> getSignupResult() { return signupResult; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<Boolean> getIsLocationLoading() { return isLocationLoading; }
    public LiveData<String> getError() { return error; }
    public LiveData<Boolean> getUserExistsResult() { return userExistsResult; }

    // --- Lógica de Ubicación con Caché ---

    public void loadProvinces() {
        String cacheKey = "cache_provinces";
        List<Province> cached = cache.loadCache(cacheKey, CacheManager.CacheSecurity.SENSITIVE, new TypeToken<List<Province>>(){}.getType());
        if (cached != null) provinces.setValue(cached);

        AppLogger.d(TAG, "Cargando provincias...");
        isLocationLoading.setValue(true);
        locationRepository.getProvinces(new Callback<ApiResponse<List<Province>>>() {
            @Override public void onResponse(@NonNull Call<ApiResponse<List<Province>>> call, @NonNull Response<ApiResponse<List<Province>>> response) {
                isLocationLoading.setValue(false);
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<Province> data = response.body().getData();
                    if (data != null && !data.equals(cached)) {
                        provinces.setValue(data);
                        cache.saveCache(cacheKey, data, CacheManager.CacheSecurity.SENSITIVE);
                    }
                }
            }
            @Override public void onFailure(@NonNull Call<ApiResponse<List<Province>>> call, @NonNull Throwable t) {
                AppLogger.e(TAG, "Error red provincias", t);
                isLocationLoading.setValue(false);
                if (provinces.getValue() == null) handleError(t);
            }
        });
    }

    public void loadMunicipalities(String provinceId) {
        String cacheKey = "cache_mun_" + provinceId;
        List<Municipality> cached = cache.loadCache(cacheKey, CacheManager.CacheSecurity.SENSITIVE, new TypeToken<List<Municipality>>(){}.getType());
        if (cached != null) municipalities.setValue(cached);

        AppLogger.d(TAG, "Cargando municipios: " + provinceId);
        isLocationLoading.setValue(true);
        locationRepository.getMunicipalities(provinceId, new Callback<ApiResponse<List<Municipality>>>() {
            @Override public void onResponse(@NonNull Call<ApiResponse<List<Municipality>>> call, @NonNull Response<ApiResponse<List<Municipality>>> response) {
                isLocationLoading.setValue(false);
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<Municipality> data = response.body().getData();
                    if (data != null && !data.equals(cached)) {
                        municipalities.setValue(data);
                        cache.saveCache(cacheKey, data, CacheManager.CacheSecurity.SENSITIVE);
                    }
                }
            }
            @Override public void onFailure(@NonNull Call<ApiResponse<List<Municipality>>> call, @NonNull Throwable t) {
                isLocationLoading.setValue(false);
                if (municipalities.getValue() == null) handleError(t);
            }
        });
    }

    public void loadSchools(String municipalityId) {
        String cacheKey = "cache_sch_" + municipalityId;
        List<School> cached = cache.loadCache(cacheKey, CacheManager.CacheSecurity.SENSITIVE, new TypeToken<List<School>>(){}.getType());
        if (cached != null) schools.setValue(cached);

        AppLogger.d(TAG, "Cargando escuelas: " + municipalityId);
        isLocationLoading.setValue(true);
        locationRepository.getSchools(municipalityId, new Callback<ApiResponse<List<School>>>() {
            @Override public void onResponse(@NonNull Call<ApiResponse<List<School>>> call, @NonNull Response<ApiResponse<List<School>>> response) {
                isLocationLoading.setValue(false);
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<School> data = response.body().getData();
                    if (data != null && !data.equals(cached)) {
                        schools.setValue(data);
                        cache.saveCache(cacheKey, data, CacheManager.CacheSecurity.SENSITIVE);
                    }
                }
            }
            @Override public void onFailure(@NonNull Call<ApiResponse<List<School>>> call, @NonNull Throwable t) {
                isLocationLoading.setValue(false);
                if (schools.getValue() == null) handleError(t);
            }
        });
    }

    public void loadGroups(String schoolId) {
        String cacheKey = "cache_grp_" + schoolId;
        List<SchoolGroup> cached = cache.loadCache(cacheKey, CacheManager.CacheSecurity.SENSITIVE, new TypeToken<List<SchoolGroup>>(){}.getType());
        if (cached != null) groups.setValue(cached);

        AppLogger.d(TAG, "Cargando grupos: " + schoolId);
        isLocationLoading.setValue(true);
        locationRepository.getGroups(schoolId, new Callback<ApiResponse<List<SchoolGroup>>>() {
            @Override public void onResponse(@NonNull Call<ApiResponse<List<SchoolGroup>>> call, @NonNull Response<ApiResponse<List<SchoolGroup>>> response) {
                isLocationLoading.setValue(false);
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<SchoolGroup> data = response.body().getData();
                    if (data != null && !data.equals(cached)) {
                        groups.setValue(data);
                        cache.saveCache(cacheKey, data, CacheManager.CacheSecurity.SENSITIVE);
                    }
                }
            }
            @Override public void onFailure(@NonNull Call<ApiResponse<List<SchoolGroup>>> call, @NonNull Throwable t) {
                isLocationLoading.setValue(false);
                if (groups.getValue() == null) handleError(t);
            }
        });
    }

    public void loadResponsibilities() {
        locationRepository.getResponsibilities(new Callback<ApiResponse<List<String>>>() {
            @Override public void onResponse(@NonNull Call<ApiResponse<List<String>>> call, @NonNull Response<ApiResponse<List<String>>> response) {
                if (response.isSuccessful() && response.body() != null) responsibilities.setValue(response.body().getData());
            }
            @Override public void onFailure(@NonNull Call<ApiResponse<List<String>>> call, @NonNull Throwable t) { error.setValue("Fallo al cargar responsabilidades"); }
        });
    }

    // --- Lógica de Auth ---

    public void checkUserExists(String username) {
        authRepository.checkUserExists(username, new Callback<ApiResponse<Boolean>>() {
            @Override public void onResponse(@NonNull Call<ApiResponse<Boolean>> call, @NonNull Response<ApiResponse<Boolean>> response) {
                if (response.isSuccessful() && response.body() != null) userExistsResult.setValue(response.body().getData());
            }
            @Override public void onFailure(@NonNull Call<ApiResponse<Boolean>> call, @NonNull Throwable t) { userExistsResult.setValue(false); }
        });
    }

    private void handleError(Throwable t) {
        String msg = t.getMessage();
        if (t instanceof UnknownHostException) {
            msg = getApplication().getString(R.string.error_network_connection) + " (DNS Error)";
        } else if (t instanceof ConnectException) {
            msg = getApplication().getString(R.string.error_server) + " (Connection Refused)";
        } else if (t instanceof SocketTimeoutException) {
            msg = getApplication().getString(R.string.error_connection) + " (Timeout)";
        }
        error.setValue(msg);
    }

    public void login(String username, String password) {
        isLoading.setValue(true);
        authRepository.login(username, password, new Callback<ApiResponse<AuthResponse>>() {
            @Override public void onResponse(@NonNull Call<ApiResponse<AuthResponse>> call, @NonNull Response<ApiResponse<AuthResponse>> response) {
                isLoading.setValue(false);
                if (response.isSuccessful() && response.body() != null) {
                    loginResult.setValue(response.body());
                } else {
                    String msg = "";
                    try (ResponseBody errorBody = response.errorBody()) {
                        if (errorBody != null) {
                            String errorJson = errorBody.string();
                            ApiResponse<?> errorResp = new Gson().fromJson(errorJson, ApiResponse.class);
                            if (errorResp != null && errorResp.getError() != null) msg = errorResp.getError();
                        }
                    } catch (Exception ignored) {}
                    
                    if (response.code() == 404 || "AUTH_USER_NOT_FOUND".equals(msg) || "Usuario no encontrado".equals(msg)) {
                        error.setValue("AUTH_USER_NOT_FOUND");
                    } else if (response.code() == 401 || "AUTH_INVALID_PASSWORD".equals(msg) || "Credenciales inválidas".equals(msg)) {
                        error.setValue("AUTH_INVALID_PASSWORD");
                    } else {
                        error.setValue(msg.isEmpty() ? "Error del servidor: " + response.code() : msg);
                    }
                }
            }
            @Override public void onFailure(@NonNull Call<ApiResponse<AuthResponse>> call, @NonNull Throwable t) { isLoading.setValue(false); handleError(t); }
        });
    }

    public void signup(String username, String password, String firstName, String lastName, String idCard, String role, HashMap<String, Object> extraData) {
        isLoading.setValue(true);
        SignupRequest request = new SignupRequest(username, password, firstName, lastName);
        request.setIdCard(idCard); request.setRole(role);
        
        if (extraData.containsKey("schoolId")) request.setSchoolId((String) extraData.get("schoolId"));
        if (extraData.containsKey("groupId")) request.setGroupId((String) extraData.get("groupId"));
        if (extraData.containsKey("groupName")) request.setGroupName((String) extraData.get("groupName"));
        if (extraData.containsKey("tutee")) request.setTutee((String) extraData.get("tutee"));
        if (extraData.containsKey("responsibilities")) request.setResponsibilities((String) extraData.get("responsibilities"));
        if (extraData.containsKey("avatar")) request.setAvatar((String) extraData.get("avatar"));
        
        authRepository.signup(request, new Callback<ApiResponse<AuthResponse>>() {
            @Override public void onResponse(@NonNull Call<ApiResponse<AuthResponse>> call, @NonNull Response<ApiResponse<AuthResponse>> response) {
                isLoading.setValue(false);
                if (response.isSuccessful() && response.body() != null) signupResult.setValue(response.body());
                else error.setValue("Error en el registro");
            }
            @Override public void onFailure(@NonNull Call<ApiResponse<AuthResponse>> call, @NonNull Throwable t) { isLoading.setValue(false); error.setValue(t.getMessage()); }
        });
    }
}
