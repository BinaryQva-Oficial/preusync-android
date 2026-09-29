/**
 * ============================================================================
 * Proyecto: PreuSync
 * Clase: PersistentService.java
 * Versión: v4.0.4
 * Descripción: Servicio de primer plano (Foreground Service) que mantiene una
 *              notificación persistente con accesos directos a secciones clave.
 * Autor: JiroxDEV
 * Licensed under the GNU Affero General Public License v3
 * ============================================================================
 */

package binaryqva.educative.preusync.services;

import binaryqva.educative.preusync.R;
import binaryqva.educative.preusync.ui.activities.HomeActivity;
import binaryqva.educative.preusync.utils.notification.NotificationHelper;
import binaryqva.educative.preusync.utils.scheduling.BackgroundServiceScheduler;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;
import android.widget.RemoteViews;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

/**
 * Servicio encargado de gestionar la visibilidad de la App en el área de notificaciones
 * y asegurar que el sistema no mate los procesos de sincronización por inactividad.
 */
public class PersistentService extends Service {
    private static final int NOTIFICATION_ID = 1001;

    @Override
    public void onCreate() {
        super.onCreate();
        // Inicia inmediatamente el estado de primer plano para cumplir con los requisitos de Android.
        startForegroundImmediately();
        checkAndStopIfNecessary();
    }

    /**
     * Lanza la notificación inicial para evitar que el sistema detenga el servicio por ANR.
     */
    private void startForegroundImmediately() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(NOTIFICATION_ID, buildSummaryNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
            } else {
                startForeground(NOTIFICATION_ID, buildSummaryNotification());
            }
        } catch (Exception e) {
            stopSelf();
        }
    }

    /**
     * Verifica permisos de notificación y lanza la programación del primer ciclo de sincronización.
     */
    private void checkAndStopIfNecessary() {
        if (Build.VERSION.SDK_INT >= 33) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                stopSelf();
                return;
            }
        }
        updateNotification();
        // Programa la primera ejecución de los Workers de fondo.
        BackgroundServiceScheduler.scheduleFirst(this);
    }

    private void updateNotification() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(NOTIFICATION_ID, buildSummaryNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
            } else {
                startForeground(NOTIFICATION_ID, buildSummaryNotification());
            }
        } catch (Exception ignored) { }
    }

    /**
     * Crea un PendingIntent para navegar a una página y subpestaña específica dentro del Home.
     * @param page Página de destino (0-4)
     * @param requestCode Código identificador único
     * @param subtab Índice de la subpestaña (ej. en Eventos), -1 para ignorar.
     * @return PendingIntent configurado.
     */
    private PendingIntent createPendingIntent(String page, int requestCode, int subtab) {
        Intent intent = new Intent(this, HomeActivity.class);
        intent.putExtra("page", page);
        if (subtab >= 0) {
            intent.putExtra("subtab", subtab);
        }
        return PendingIntent.getActivity(
                this, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    /**
     * Construye la vista remota de la notificación con botones interactivos.
     */
    private Notification buildSummaryNotification() {
        Intent homeIntent = new Intent(this, HomeActivity.class);
        homeIntent.putExtra("page", "0");
        PendingIntent piHome = PendingIntent.getActivity(
                this, 0, homeIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // Definición de destinos para los accesos rápidos.
        PendingIntent piNews = createPendingIntent("1", 1, -1);
        PendingIntent piPosts = createPendingIntent("3", 2, -1);
        PendingIntent piEvents = createPendingIntent("2", 3, -1);
        PendingIntent piEphemerides = createPendingIntent("2", 4, 2);  // 2 = Efemérides
        PendingIntent piSchedule = createPendingIntent("2", 5, 3);     // 3 = Horario

        RemoteViews remoteViews = new RemoteViews(getPackageName(), R.layout.notification_summary_layout);

        // Configuración de iconos fijos.
        remoteViews.setImageViewResource(R.id.newsImage, R.drawable.ic_notification_news);
        remoteViews.setImageViewResource(R.id.postsImage, R.drawable.ic_notification_posts);
        remoteViews.setImageViewResource(R.id.eventsImage, R.drawable.ic_notification_events);
        remoteViews.setImageViewResource(R.id.ephemerisImage, R.drawable.ic_notification_ephemeris);
        remoteViews.setImageViewResource(R.id.scheduleImage, R.drawable.ic_notification_schedule);

        // Asociación de eventos táctiles a los iconos de la notificación.
        remoteViews.setOnClickPendingIntent(R.id.newsImage, piNews);
        remoteViews.setOnClickPendingIntent(R.id.postsImage, piPosts);
        remoteViews.setOnClickPendingIntent(R.id.eventsImage, piEvents);
        remoteViews.setOnClickPendingIntent(R.id.ephemerisImage, piEphemerides);
        remoteViews.setOnClickPendingIntent(R.id.scheduleImage, piSchedule);

        return new NotificationCompat.Builder(this, NotificationHelper.CHANNEL_FOREGROUND_SERVICE)
                .setSmallIcon(R.drawable.logo)
                .setStyle(new NotificationCompat.DecoratedCustomViewStyle())
                .setCustomContentView(remoteViews)
                .setContentIntent(piHome)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Asegura que la sincronización se programe al iniciar el servicio.
        BackgroundServiceScheduler.scheduleFirst(this);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}


