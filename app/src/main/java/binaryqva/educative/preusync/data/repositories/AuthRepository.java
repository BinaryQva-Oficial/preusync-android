/**
 * ============================================================================
 * Proyecto: PreuSync
 * Clase: AuthRepository.java
 * Versión: v1.1.0
 * Descripción: Repositorio encargado de centralizar las operaciones de 
 *              autenticación, registro y gestión de perfiles de usuario.
 * Autor: JiroxDEV
 * Licensed under the GNU Affero General Public License v3
 * ============================================================================
 */
package binaryqva.educative.preusync.data.repositories;

import android.content.Context;

import java.util.List;

import binaryqva.educative.preusync.network.models.ApiResponse;
import binaryqva.educative.preusync.network.models.AuthResponse;
import binaryqva.educative.preusync.network.models.Post;
import binaryqva.educative.preusync.network.models.Profile;
import binaryqva.educative.preusync.network.requests.LoginRequest;
import binaryqva.educative.preusync.network.requests.RefreshSessionRequest;
import binaryqva.educative.preusync.network.requests.SignupRequest;
import binaryqva.educative.preusync.network.requests.UpdateProfileRequest;
import binaryqva.educative.preusync.network.requests.VerifyPasswordRequest;
import binaryqva.educative.preusync.network.retrofit.ApiService;
import binaryqva.educative.preusync.network.retrofit.RetrofitClient;
import retrofit2.Callback;

/**
 * Repositorio encargado de la gestión de autenticación y perfiles de usuario.
 */
public class AuthRepository {
    
    private final ApiService apiService;
    
    public AuthRepository(Context context) {
        this.apiService = RetrofitClient.getApiService(context);
    }
    
    public void login(String username, String password, Callback<ApiResponse<AuthResponse>> callback) {
        apiService.login(new LoginRequest(username, password)).enqueue(callback);
    }

    public void signup(SignupRequest request, Callback<ApiResponse<AuthResponse>> callback) {
        apiService.signup(request).enqueue(callback);
    }

    public void refreshSession(String refreshToken, Callback<ApiResponse<AuthResponse>> callback) {
        apiService.refreshSession(new RefreshSessionRequest(refreshToken)).enqueue(callback);
    }

    public void getProfile(Callback<ApiResponse<Profile>> callback) {
        apiService.getProfile().enqueue(callback);
    }

    public void getUserPosts(int start, int count, Callback<ApiResponse<List<Post>>> callback) {
        apiService.getUserPosts(start, count).enqueue(callback);
    }

    public void updateProfile(UpdateProfileRequest request, Callback<ApiResponse<Profile>> callback) {
        apiService.updateProfile(request).enqueue(callback);
    }

    public void verifyPassword(String password, Callback<ApiResponse<Boolean>> callback) {
        apiService.verifyPassword(new VerifyPasswordRequest(password)).enqueue(callback);
    }

    public void checkUserExists(String username, Callback<ApiResponse<Boolean>> callback) {
        apiService.checkUserExists(username).enqueue(callback);
    }

    public void logout(Callback<ApiResponse<Void>> callback) {
        apiService.logout().enqueue(callback);
    }
}


