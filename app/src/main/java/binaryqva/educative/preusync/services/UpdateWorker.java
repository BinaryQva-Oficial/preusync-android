/**
 * ============================================================================
 * Proyecto: PreuSync
 * Clase: UpdateWorker.java
 * Versión: v2.0.0
 * Descripción: Trabajador de WorkManager encargado de comprobar periódicamente
 *              si existe una nueva versión de la aplicación en el servidor.
 *              Refactorizado para utilizar el cliente unificado de Retrofit.
 * Autor: JiroxDEV
 * Licensed under the GNU Affero General Public License v3
 * ============================================================================
 */

package binaryqva.educative.preusync.services;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.Uri;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import binaryqva.educative.preusync.R;
import binaryqva.educative.preusync.debug.AppLogger;
import binaryqva.educative.preusync.network.models.ApiResponse;
import binaryqva.educative.preusync.network.models.AppVersion;
import binaryqva.educative.preusync.network.retrofit.ApiService;
import binaryqva.educative.preusync.network.retrofit.RetrofitClient;
import binaryqva.educative.preusync.utils.common.AuthManager;
import binaryqva.educative.preusync.utils.common.PreferenceManager;
import binaryqva.educative.preusync.utils.notification.NotificationHelper;
import retrofit2.Response;

/**
 * Ejecuta la comprobación de actualizaciones comparando la versión instalada
 * con la información proporcionada por la API.
 */
public class UpdateWorker extends Worker {

    private static final String TAG = "UpdateWorker";
    private static final String CHANNEL_ID = "update_channel";
    private static final int NOTIFICATION_ID = 1001;

    private AuthManager authManager;
    private PreferenceManager prefsManager;
    private ApiService apiService;

    public UpdateWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        AppLogger.d(TAG, "Iniciando comprobación de actualización...");

        Context context = getApplicationContext();
        prefsManager = PreferenceManager.getInstance(context);
        authManager = AuthManager.getInstance(context);
        apiService = RetrofitClient.getApiService(context);

        // VALIDACIÓN DE SESIÓN: Comprobamos si el token es válido antes de consultar la API.
        if (!authManager.isSessionValid()) {
            AppLogger.w(TAG, "Sesión expirada o inválida, reintentando verificación...");
            authManager.verifySession(new AuthManager.SessionCallback() {
                @Override
                public void onSessionValid() {
                    AppLogger.d(TAG, "Sesión verificada, procediendo con la comprobación");
                    performUpdateCheck(context);
                }

                @Override
                public void onSessionInvalid() {
                    AppLogger.w(TAG, "Sesión inválida, cancelando comprobación de actualización");
                    authManager.handleExpiredSession(context);
                }
            });
            return Result.success();
        }

        performUpdateCheck(context);
        return Result.success();
    }

    /**
     * Realiza la lógica de comparación de versiones tras verificar las preferencias del usuario.
     */
    private void performUpdateCheck(Context context) {
        // Verifica si el usuario tiene activadas las actualizaciones automáticas.
        if (!prefsManager.isAutoUpdatesEnabled()) {
            AppLogger.d(TAG, "Actualizaciones automáticas desactivadas por el usuario");
            return;
        }

        // Verifica si se debe usar solo WiFi según la configuración.
        if (prefsManager.isAutoUpdatesWifiOnly() && !isWifiConnected()) {
            AppLogger.d(TAG, "Modo 'Solo WiFi' activo para actualizaciones, pero no hay WiFi disponible");
            return;
        }

        try {
            // Obtenemos el código de versión (versionCode) de la aplicación instalada.
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            long installedVersionCode = packageInfo.versionCode;
            AppLogger.d(TAG, "Versión instalada (Code): " + installedVersionCode);

            // Consulta el endpoint de versión en el backend vía Retrofit.
            Response<ApiResponse<AppVersion>> response = apiService.getLatestVersion().execute();

            if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                AppVersion serverVersion = response.body().getData();
                if (serverVersion != null) {
                    long serverVersionCode = serverVersion.getVersionCode();
                    String serverVersionName = serverVersion.getVersionName();
                    String details = serverVersion.getDetails();

                    AppLogger.d(TAG, "Versión en servidor: " + serverVersionName + " (" + serverVersionCode + ")");

                    // Si la versión del servidor es superior a la instalada, se notifica al usuario.
                    if (serverVersionCode > installedVersionCode) {
                        AppLogger.d(TAG, "¡Nueva versión disponible detectada!");
                        showUpdateNotification(context, serverVersionName, details);
                    } else {
                        AppLogger.d(TAG, "La aplicación está actualizada a la última versión");
                    }
                }
            } else if (response.code() == 401) {
                AppLogger.w(TAG, "Error 401 - Sesión expirada");
                authManager.handleExpiredSession(context);
            } else {
                AppLogger.w(TAG, "Error HTTP en actualización: " + response.code());
            }
        } catch (Exception e) {
            AppLogger.e(TAG, "Excepción durante la comprobación de actualización", e);
        }
    }

    /**
     * Crea el canal de notificación (si es necesario) y muestra el aviso de actualización.
     */
    private void showUpdateNotification(Context context, String versionName, String details) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        // Creación del canal para Android 8.0 o superior.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = notificationManager.getNotificationChannel(CHANNEL_ID);
            if (channel == null) {
                channel = new NotificationChannel(
                        CHANNEL_ID,
                        context.getString(R.string.notification_channel_updates_name),
                        NotificationManager.IMPORTANCE_HIGH
                );
                channel.setDescription(context.getString(R.string.notification_channel_updates_desc));
                channel.setSound(null, null);
                notificationManager.createNotificationChannel(channel);
            }
        }

        // Selección de sonido personalizado según configuración.
        String soundPref = prefsManager.getAutoUpdatesSound();
        Uri soundUri = NotificationHelper.getSoundUri(context, soundPref);

        // Al tocar la notificación, abre el navegador con la URL de descarga configurada.
        String downloadUrl = context.getString(R.string.update_download_url);
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl));
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.logo)
                .setContentTitle(context.getString(R.string.notification_new_version_title, versionName))
                .setContentText(details)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        if (soundUri != null) {
            builder.setSound(soundUri);
        }

        notificationManager.notify(NOTIFICATION_ID, builder.build());
    }

    /**
     * Verifica la disponibilidad de una red WiFi activa.
     */
    private boolean isWifiConnected() {
        ConnectivityManager cm = (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        NetworkCapabilities capabilities = cm.getNetworkCapabilities(cm.getActiveNetwork());
        return capabilities != null && capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI);
    }
}
