/**
 * ============================================================================
 * Proyecto: PreuSync
 * Clase: AuthManager.java
 * Versión: v2.0.1
 * Descripción: Gestor centralizado de autenticación y sesiones del usuario.
 *              Refactorizado para usar AuthRepository y modelos tipados.
 * Autor: JiroxDEV
 * Licensed under the GNU Affero General Public License v3
 * ============================================================================
 */

package binaryqva.educative.preusync.utils.common;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import androidx.appcompat.app.AlertDialog;

import org.json.JSONObject;

import binaryqva.educative.preusync.R;
import binaryqva.educative.preusync.data.repositories.AuthRepository;
import binaryqva.educative.preusync.debug.AppLogger;
import binaryqva.educative.preusync.network.models.ApiResponse;
import binaryqva.educative.preusync.network.models.AuthResponse;
import binaryqva.educative.preusync.network.models.Profile;
import binaryqva.educative.preusync.ui.activities.AuthActivity;
import binaryqva.educative.preusync.ui.activities.HomeActivity;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Centraliza las operaciones de autenticación con la API y Supabase.
 * Implementa el patrón Singleton para asegurar un único control de estado de sesión.
 */
public class AuthManager {

    private static final String TAG = "AuthManager";
    private static AuthManager instance;
    private final Context appContext;
    private final PreferenceManager preferenceManager;
    private final AuthRepository authRepository;
    private boolean isVerifying = false;

    private AlertDialog currentDialog;

    private AuthManager(Context context) {
        this.appContext = context.getApplicationContext();
        this.preferenceManager = PreferenceManager.getInstance(appContext);
        this.authRepository = new AuthRepository(appContext);
    }

    public static synchronized AuthManager getInstance(Context context) {
        if (instance == null) {
            instance = new AuthManager(context);
        }
        return instance;
    }

    public void verifySession(SessionCallback callback) {
        if (isVerifying) {
            AppLogger.d(TAG, "verifySession: Validación en curso, omitiendo petición duplicada");
            return;
        }

        if (hasValidToken()) {
            AppLogger.d(TAG, "verifySession: Token local válido, corroborando con el servidor...");
            performServerVerification(callback);
        } else {
            String refreshToken = preferenceManager.getSupabaseRefreshToken();
            if (refreshToken != null && !refreshToken.isEmpty()) {
                AppLogger.d(TAG, "verifySession: Token expirado, intentando renovación silenciosa...");
                refreshSession(new SessionCallback() {
                    @Override
                    public void onSessionValid() {
                        performServerVerification(callback);
                    }

                    @Override
                    public void onSessionInvalid() {
                        callback.onSessionInvalid();
                    }
                });
            } else {
                AppLogger.w(TAG, "verifySession: Sin token válido ni vía de renovación");
                callback.onSessionInvalid();
            }
        }
    }

    private void performServerVerification(SessionCallback callback) {
        isVerifying = true;

        authRepository.getProfile(new Callback<ApiResponse<Profile>>() {
            @Override
            public void onResponse(Call<ApiResponse<Profile>> call, Response<ApiResponse<Profile>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    isVerifying = false;
                    AppLogger.d(TAG, "verifySession: Sesión confirmada por la API");
                    callback.onSessionValid();
                } else if (response.code() == 401 || response.code() == 403 || !hasValidToken()) {
                    AppLogger.w(TAG, "verifySession: Token rechazado o expirado en servidor. Intentando renovación silenciosa...");
                    refreshSession(new SessionCallback() {
                        @Override
                        public void onSessionValid() {
                            isVerifying = false;
                            AppLogger.d(TAG, "verifySession: Token renovado exitosamente con refresh token");
                            callback.onSessionValid();
                        }

                        @Override
                        public void onSessionInvalid() {
                            isVerifying = false;
                            AppLogger.w(TAG, "verifySession: Renovación fallida, cerrando sesión");
                            callback.onSessionInvalid();
                        }
                    });
                } else {
                    isVerifying = false;
                    AppLogger.w(TAG, "verifySession: Servidor no respondió con perfil pero manteniendo sesión offline");
                    callback.onSessionValid();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Profile>> call, Throwable t) {
                isVerifying = false;
                AppLogger.e(TAG, "verifySession: Error de conectividad (" + t.getMessage() + "), manteniendo sesión offline", null);
                callback.onSessionValid();
            }
        });
    }

    public void checkAndHandleSession(Context context) {
        if (context == null) return;
        verifySession(new SessionCallback() {
            @Override
            public void onSessionValid() {
                AppLogger.d(TAG, "checkAndHandleSession: Sesión OK");
            }

            @Override
            public void onSessionInvalid() {
                AppLogger.w(TAG, "checkAndHandleSession: Sesión corrupta o expirada, forzando logout");
                handleExpiredSession(context);
            }
        });
    }

    public void handleExpiredSession(Context context) {
        if (context == null) return;
        preferenceManager.clearAccount();
        dismissCurrentDialog();

        Intent intent = new Intent(context, AuthActivity.class);
        intent.putExtra("session_expired", true);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        if (context instanceof Activity && !((Activity) context).isFinishing()) {
            Activity activity = (Activity) context;
            activity.runOnUiThread(() -> {
                currentDialog = DialogHelper.showAlertDialog(
                        activity,
                        activity.getString(R.string.title_warning),
                        activity.getString(R.string.session_expired_message),
                        () -> {
                            currentDialog = null;
                            activity.startActivity(intent);
                            activity.finish();
                        }
                );
                if (currentDialog != null) {
                    currentDialog.setCancelable(false);
                }
            });
        } else {
            context.startActivity(intent);
        }
    }

    private void dismissCurrentDialog() {
        if (currentDialog != null && currentDialog.isShowing()) {
            currentDialog.dismiss();
            currentDialog = null;
        }
    }

    public boolean hasValidToken() {
        String token = preferenceManager.getSupabaseAccessToken();
        if (token == null || token.isEmpty()) return false;

        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) return false;
            String payload = new String(android.util.Base64.decode(parts[1], 
                    android.util.Base64.URL_SAFE | android.util.Base64.NO_WRAP | android.util.Base64.NO_PADDING));
            JSONObject json = new JSONObject(payload);
            long exp = json.optLong("exp", 0);
            long now = System.currentTimeMillis() / 1000;
            return exp > (now + 30);
        } catch (Exception e) {
            AppLogger.e(TAG, "Error al parsear estructura del token", e);
            return false;
        }
    }

    public void refreshSession(SessionCallback callback) {
        String refreshToken = preferenceManager.getSupabaseRefreshToken();
        if (refreshToken == null || refreshToken.isEmpty()) {
            callback.onSessionInvalid();
            return;
        }

        authRepository.refreshSession(refreshToken, new Callback<ApiResponse<AuthResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<AuthResponse>> call, Response<ApiResponse<AuthResponse>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    AuthResponse data = response.body().getData();
                    String newToken = data.getSessionToken();
                    String newRefresh = data.getRefreshToken();
                    
                    preferenceManager.setSupabaseAccessToken(newToken);
                    preferenceManager.setSupabaseRefreshToken(newRefresh);
                    
                    callback.onSessionValid();
                } else {
                    callback.onSessionInvalid();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<AuthResponse>> call, Throwable t) {
                if (hasValidToken()) {
                    callback.onSessionValid();
                } else {
                    callback.onSessionInvalid();
                }
            }
        });
    }

    public boolean isSessionValid() {
        return hasValidToken();
    }

    public interface SessionCallback {
        void onSessionValid();
        void onSessionInvalid();
    }
}


