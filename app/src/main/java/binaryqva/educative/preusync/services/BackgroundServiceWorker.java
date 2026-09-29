/**
 * ============================================================================
 * Proyecto: PreuSync
 * Clase: BackgroundServiceWorker.java
 * Versión: v6.1.0
 * Descripción: Trabajador de WorkManager encargado de la sincronización en segundo
 *              plano. Refactorizado para usar Retrofit 2 y Glide síncrono.
 * Autor: JiroxDEV
 * Licensed under the GNU Affero General Public License v3
 * ============================================================================
 */

package binaryqva.educative.preusync.services;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.view.View;
import android.widget.RemoteViews;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.bumptech.glide.Glide;
import com.google.gson.Gson;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import binaryqva.educative.preusync.R;
import binaryqva.educative.preusync.debug.AppLogger;
import binaryqva.educative.preusync.network.models.ApiResponse;
import binaryqva.educative.preusync.network.models.Ephemeris;
import binaryqva.educative.preusync.network.models.News;
import binaryqva.educative.preusync.network.models.Post;
import binaryqva.educative.preusync.network.models.Schedule;
import binaryqva.educative.preusync.network.models.Event;
import binaryqva.educative.preusync.network.retrofit.ApiService;
import binaryqva.educative.preusync.network.retrofit.RetrofitClient;
import binaryqva.educative.preusync.ui.activities.HomeActivity;
import binaryqva.educative.preusync.utils.common.AuthManager;
import binaryqva.educative.preusync.utils.common.CacheManager;
import binaryqva.educative.preusync.utils.common.PreferenceConstants;
import binaryqva.educative.preusync.utils.common.PreferenceManager;
import binaryqva.educative.preusync.utils.notification.NotificationHelper;
import retrofit2.Response;

/**
 * Sincroniza datos en segundo plano utilizando el cliente unificado de Retrofit.
 */
public class BackgroundServiceWorker extends Worker {

    private static final String TAG = "BackgroundServiceWorker";
    private static final int FOREGROUND_NOTIFICATION_ID = 1001;

    private final Context context;
    private PreferenceManager prefsManager;
    private CacheManager cacheManager;
    private AuthManager authManager;
    private ApiService apiService;

    private int newNewsCount = 0;
    private int newPostsCount = 0;
    private int newSchoolEventsCount = 0;
    private int newExternalEventsCount = 0;
    private int ephemeridesCount = 0;
    private boolean scheduleChanged = false;

    public BackgroundServiceWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        this.context = context;
    }

    @NonNull
    @Override
    public Result doWork() {
        initialize();

        if (!prefsManager.isBgSyncEnabled() || (prefsManager.isWifiOnlyBackground() && !isWifiConnected())) {
            return Result.success();
        }

        if (!authManager.isSessionValid()) {
            final CountDownLatch latch = new CountDownLatch(1);
            authManager.verifySession(new AuthManager.SessionCallback() {
                @Override public void onSessionValid() { latch.countDown(); }
                @Override public void onSessionInvalid() { authManager.handleExpiredSession(context); latch.countDown(); }
            });
            try { 
                if (!latch.await(30, TimeUnit.SECONDS)) AppLogger.w(TAG, "Auth latch timeout");
            } catch (InterruptedException ignored) {}
        }

        if (authManager.isSessionValid()) {
            executeBackgroundTasks();
        }

        return Result.success();
    }

    private void initialize() {
        prefsManager = PreferenceManager.getInstance(context);
        cacheManager = CacheManager.getInstance();
        authManager = AuthManager.getInstance(context);
        apiService = RetrofitClient.getApiService(context);
    }

    private void executeBackgroundTasks() {
        long now = System.currentTimeMillis();
        resetCounters();

        if (shouldExecute(PreferenceConstants.KEY_LAST_NEWS, prefsManager.getSyncFreqNews())) {
            processNews(); prefsManager.setLastNewsTimestamp(now);
        }
        if (shouldExecute(PreferenceConstants.KEY_LAST_POSTS, prefsManager.getSyncFreqPosts())) {
            processPosts(); prefsManager.setLastPostsTimestamp(now);
        }
        if (shouldExecute(PreferenceConstants.KEY_LAST_EVENTS, prefsManager.getSyncFreqSchoolEvents())) {
            processSchoolEvents(); prefsManager.setLastEventsTimestamp(now);
        }
        if (shouldExecute(PreferenceConstants.KEY_LAST_EVENTS, prefsManager.getSyncFreqExternalEvents())) {
            processExternalEvents();
        }
        if (shouldExecute(PreferenceConstants.KEY_LAST_EPHEMERIDES, prefsManager.getSyncFreqEphemerides())) {
            processEphemerides(); prefsManager.setLastEphemeridesTimestamp(now);
        }
        if (shouldExecute(PreferenceConstants.KEY_LAST_SCHEDULE, prefsManager.getSyncFreqSchedule())) {
            processSchedule(); prefsManager.setLastScheduleTimestamp(now);
        }

        updateSummaryNotification();
    }

    private void processNews() {
        try {
            Response<ApiResponse<List<News>>> response = apiService.getTopNews(5).execute();
            if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                List<News> newsList = response.body().getData();
                cacheManager.saveCache("news_feed", newsList, CacheManager.CacheSecurity.PUBLIC);
                
                if (!prefsManager.isNotifNewsEnabled()) return;

                List<String> notifiedIds = getNotifiedIds(prefsManager.getNotifiedIdsNews());
                for (News news : newsList) {
                    if (!notifiedIds.contains(news.getId())) {
                        notifiedIds.add(news.getId()); newNewsCount++;
                        showSimpleNotification(news.getHeadline(), news.getDetails(), news.getId().hashCode(), 
                            NotificationHelper.CHANNEL_NEWS, HomeActivity.ACTION_OPEN_NEWS, news.getId(), news.getImageUrl());
                    }
                }
                prefsManager.setNotifiedIdsNews(new com.google.gson.Gson().toJson(notifiedIds));
            }
        } catch (Exception e) { AppLogger.e(TAG, "Error sync News", e); }
    }

    private void processPosts() {
        try {
            Response<ApiResponse<List<Post>>> response = apiService.getTopPosts(5).execute();
            if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                List<Post> postsList = response.body().getData();
                cacheManager.saveCache("posts_feed", postsList, CacheManager.CacheSecurity.PUBLIC);

                if (!prefsManager.isNotifPostsEnabled()) return;

                List<String> notifiedIds = getNotifiedIds(prefsManager.getNotifiedIdsPosts());
                for (Post post : postsList) {
                    if (!notifiedIds.contains(post.getId())) {
                        notifiedIds.add(post.getId()); newPostsCount++;
                        showSimpleNotification(post.getTitle(), context.getString(R.string.notification_post_text, post.getAuthor()), 
                            post.getId().hashCode(), NotificationHelper.CHANNEL_POSTS, HomeActivity.ACTION_OPEN_POST, post.getId(), post.getImageUrl());
                    }
                }
                prefsManager.setNotifiedIdsPosts(new com.google.gson.Gson().toJson(notifiedIds));
            }
        } catch (Exception e) { AppLogger.e(TAG, "Error sync Posts", e); }
    }

    private void processSchoolEvents() { processEventsByType("school"); }
    private void processExternalEvents() { processEventsByType("external"); }

    private void processEventsByType(String type) {
        try {
            boolean isSchool = "school".equals(type);
            String schoolId = prefsManager.getSchoolId();
            Response<ApiResponse<List<Event>>> response = isSchool ? 
                apiService.getSchoolEvents(0, 5, schoolId).execute() : apiService.getExternalEvents(0, 5, schoolId).execute();

            if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                List<Event> events = response.body().getData();
                String cacheKey = isSchool ? "school_events_feed" : "external_events_feed";
                cacheManager.saveCache(cacheKey, events, CacheManager.CacheSecurity.PUBLIC);

                if ((isSchool && !prefsManager.isNotifSchoolEventsEnabled()) || (!isSchool && !prefsManager.isNotifExternalEventsEnabled())) return;

                List<String> notifiedIds = getNotifiedIds(prefsManager.getNotifiedIdsEvents());

                Calendar calTomorrow = Calendar.getInstance();
                calTomorrow.add(Calendar.DAY_OF_MONTH, 1);
                String tomorrowDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calTomorrow.getTime());

                for (Event event : events) {
                    String eventDate = event.getDate();
                    if (eventDate == null || eventDate.isEmpty() || eventDate.equals(tomorrowDate)) {
                        if (!notifiedIds.contains(event.getId())) {
                            notifiedIds.add(event.getId());
                            if (isSchool) newSchoolEventsCount++; else newExternalEventsCount++;
                            showSimpleNotification(event.getTitle(), context.getString(R.string.notification_event_text, event.getLocation(), event.getTime()),
                                event.getId().hashCode(), NotificationHelper.CHANNEL_EVENTS, HomeActivity.ACTION_OPEN_EVENT, event.getId(), event.getImageUrl());
                        }
                    }
                }
                prefsManager.setNotifiedIdsEvents(new Gson().toJson(notifiedIds));
            }
        } catch (Exception e) { AppLogger.e(TAG, "Error sync Events " + type, e); }
    }

    private void processEphemerides() {
        try {
            String date = new java.text.SimpleDateFormat("dd/MM", Locale.getDefault()).format(Calendar.getInstance().getTime());
            if (date.equals(prefsManager.getLastEphemerisDate())) return;

            Response<ApiResponse<List<Ephemeris>>> response = apiService.getEphemeris(date).execute();
            if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                List<Ephemeris> list = response.body().getData();
                cacheManager.saveCache("ephemeris", list, CacheManager.CacheSecurity.PUBLIC);
                if (!prefsManager.isNotifEphemeridesEnabled()) return;

                ephemeridesCount = list.size();
                for (Ephemeris eph : list) {
                    showSimpleNotification(context.getString(R.string.notification_ephemeris_title, eph.getTitle()), eph.getDetails(), 
                        eph.getId().hashCode(), NotificationHelper.CHANNEL_EPHEMERIDES, HomeActivity.ACTION_OPEN_EPHEMERIS, eph.getId(), eph.getImageUrl());
                }
                prefsManager.setLastEphemerisDate(date);
            }
        } catch (Exception e) { AppLogger.e(TAG, "Error sync Ephemeris", e); }
    }

    private void processSchedule() {
        try {
            String group = prefsManager.getUserGroup();
            String schoolId = prefsManager.getSchoolId();
            if (group == null || group.isEmpty() || schoolId == null) return;

            Response<ApiResponse<List<Schedule>>> response = apiService.getSchedule(group, schoolId).execute();
            if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                List<Schedule> list = response.body().getData();
                String newHash = String.valueOf(new Gson().toJson(list).hashCode());
                if (newHash.equals(prefsManager.getScheduleHash())) return;

                cacheManager.saveCache("schedule", list, CacheManager.CacheSecurity.PUBLIC);
                prefsManager.setScheduleHash(newHash); scheduleChanged = true;

                if (prefsManager.isNotifScheduleEnabled()) {
                    showSimpleNotification(context.getString(R.string.notification_schedule_title), 
                        context.getString(R.string.notification_schedule_text, group), 2001, 
                        NotificationHelper.CHANNEL_SCHEDULE, HomeActivity.ACTION_OPEN_SCHEDULE, null, null);
                }
            }
        } catch (Exception e) { AppLogger.e(TAG, "Error sync Schedule", e); }
    }

    private void showSimpleNotification(String title, String text, int id, String channel, String action, String dataId, String imageUrl) {
        Intent intent = new Intent(context, HomeActivity.class);
        intent.setAction(action);
        if (dataId != null) intent.putExtra("id", dataId);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        final NotificationHelper.NotificationOptions options = new NotificationHelper.NotificationOptions().setPriority(NotificationCompat.PRIORITY_DEFAULT);
        
        if (imageUrl != null && !imageUrl.isEmpty() && !"null".equals(imageUrl)) {
            try {
                Bitmap bitmap = Glide.with(context).asBitmap().load(imageUrl).submit(512, 512).get();
                options.setBigPicture(bitmap);
            } catch (Exception ignored) {}
        }

        NotificationHelper.showNotification(context, title, text, id, channel, intent, options);
    }

    private void resetCounters() {
        newNewsCount = 0; newPostsCount = 0; newSchoolEventsCount = 0; newExternalEventsCount = 0;
        ephemeridesCount = 0; scheduleChanged = false;
    }

    private boolean shouldExecute(String key, long freq) {
        long last = 0;
        switch (key) {
            case PreferenceConstants.KEY_LAST_NEWS: last = prefsManager.getLastNewsTimestamp(); break;
            case PreferenceConstants.KEY_LAST_POSTS: last = prefsManager.getLastPostsTimestamp(); break;
            case PreferenceConstants.KEY_LAST_EVENTS: last = prefsManager.getLastEventsTimestamp(); break;
            case PreferenceConstants.KEY_LAST_EPHEMERIDES: last = prefsManager.getLastEphemeridesTimestamp(); break;
            case PreferenceConstants.KEY_LAST_SCHEDULE: last = prefsManager.getLastScheduleTimestamp(); break;
        }
        return (System.currentTimeMillis() - last) >= (freq * 60 * 1000L);
    }

    private boolean isWifiConnected() {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        NetworkCapabilities caps = cm.getNetworkCapabilities(cm.getActiveNetwork());
        return caps != null && caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI);
    }

    private List<String> getNotifiedIds(String json) {
        if (json == null || json.isEmpty()) return new ArrayList<>();
        return new com.google.gson.Gson().fromJson(json, new com.google.gson.reflect.TypeToken<List<String>>(){}.getType());
    }

    private void updateSummaryNotification() {
        if (!isServiceRunning(PersistentService.class)) return;

        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.notification_summary_layout);
        
        int totalEvents = newSchoolEventsCount + newExternalEventsCount;
        remoteViews.setViewVisibility(R.id.newsTextView, newNewsCount > 0 ? View.VISIBLE : View.GONE);
        remoteViews.setViewVisibility(R.id.postsTextView, newPostsCount > 0 ? View.VISIBLE : View.GONE);
        remoteViews.setViewVisibility(R.id.eventsTextView, totalEvents > 0 ? View.VISIBLE : View.GONE);
        remoteViews.setViewVisibility(R.id.ephemerisTextView, ephemeridesCount > 0 ? View.VISIBLE : View.GONE);
        remoteViews.setViewVisibility(R.id.scheduleTextView, scheduleChanged ? View.VISIBLE : View.GONE);

        if (newNewsCount > 0) remoteViews.setTextViewText(R.id.newsTextView, String.valueOf(newNewsCount));
        if (newPostsCount > 0) remoteViews.setTextViewText(R.id.postsTextView, String.valueOf(newPostsCount));
        if (totalEvents > 0) remoteViews.setTextViewText(R.id.eventsTextView, String.valueOf(totalEvents));
        if (ephemeridesCount > 0) remoteViews.setTextViewText(R.id.ephemerisTextView, String.valueOf(ephemeridesCount));
        if (scheduleChanged) remoteViews.setTextViewText(R.id.scheduleTextView, "•");

        Intent homeIntent = new Intent(context, HomeActivity.class);
        homeIntent.putExtra("page", "0");
        PendingIntent piHome = PendingIntent.getActivity(context, 0, homeIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new NotificationCompat.Builder(context, NotificationHelper.CHANNEL_FOREGROUND_SERVICE)
                .setSmallIcon(R.drawable.logo)
                .setStyle(new NotificationCompat.DecoratedCustomViewStyle())
                .setCustomContentView(remoteViews)
                .setContentIntent(piHome)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();

        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) manager.notify(FOREGROUND_NOTIFICATION_ID, notification);
    }

    private boolean isServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) return true;
        }
        return false;
    }
}


