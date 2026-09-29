/**
 * ============================================================================
 * Proyecto: PreuSync
 * Clase: App.java
 * Versión: v6.3.0
 * Descripción: Clase principal de la aplicación.
 * Autor: JiroxDEV
 * Licensed under the GNU Affero General Public License v3
 * ============================================================================
 */

package binaryqva.educative.preusync;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.os.Process;

import binaryqva.educative.preusync.debug.AppLogger;
import binaryqva.educative.preusync.debug.CrashReportActivity;
import binaryqva.educative.preusync.utils.common.CacheManager;
import binaryqva.educative.preusync.utils.common.PreferenceManager;
import binaryqva.educative.preusync.utils.theme.ThemeManager;

public class App extends Application {

    private static final String TAG = "PreuSync_App";
    private static Context applicationContext;

    public static Context getContext() { return applicationContext; }

    @Override
    public void onCreate() {
        super.onCreate();
        applicationContext = getApplicationContext();

        PreferenceManager prefs = PreferenceManager.getInstance(this);
        prefs.initDefaultsIfNeeded();
        ThemeManager.applyTheme(prefs.getTheme());

        CacheManager.getInstance().init(this);

        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
        String trace = AppLogger.getStackTraceString(throwable);
        AppLogger.e(TAG, "ERROR CRÍTICO DETECTADO: " + trace);

        Intent intent = new Intent(getApplicationContext(), CrashReportActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.putExtra("error", trace);
        startActivity(intent);

        Process.killProcess(Process.myPid());
        System.exit(1);
    });

        AppLogger.i(TAG, "Entorno PreuSync inicializado correctamente");
    }
}
