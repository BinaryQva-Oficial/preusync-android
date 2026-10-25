/**
 * ============================================================================
 * Proyecto: PreuSync
 * Clase: GeneralSettingsActivity.java
 * Versión: v1.4.7
 * Descripción: Gestión de ajustes generales: idioma, caché y comportamiento inicial.
 * Autor: JiroxDEV
 * Licensed under the GNU Affero General Public License v3
 * ============================================================================
 */

package binaryqva.educative.preusync.ui.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import binaryqva.educative.preusync.R;
import binaryqva.educative.preusync.utils.common.DialogHelper;
import binaryqva.educative.preusync.utils.common.FileUtils;
import binaryqva.educative.preusync.utils.common.PreferenceConstants;
import binaryqva.educative.preusync.utils.common.PreferenceManager;
import binaryqva.educative.preusync.utils.settings.ButtonItem;
import binaryqva.educative.preusync.utils.settings.HeaderItem;
import binaryqva.educative.preusync.utils.settings.SelectItem;
import binaryqva.educative.preusync.utils.settings.SettingsItem;
import binaryqva.educative.preusync.utils.settings.SwitchItem;
import binaryqva.educative.preusync.utils.theme.ThemeManager;

/**
 * Administra preferencias de comportamiento global y herramientas de limpieza.
 */
public class GeneralSettingsActivity extends BaseSettingsActivity {

    private PreferenceManager prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        prefs = PreferenceManager.getInstance(getApplicationContext());
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
    }

    @Override
    protected int getTitleResId() {
        return R.string.title_general;
    }

    /**
     * Organiza los bloques de configuración general.
     */
    @Override
    protected List<SettingsItem> buildItems() {
        List<SettingsItem> items = new ArrayList<>();

        // BLOQUE: PREFERENCIAS DE USO
        items.add(new HeaderItem(getString(R.string.section_general_prefs)));
        items.add(buildLanguageSelectItem());
        items.add(buildDefaultStartTabSelectItem());
        items.add(buildConfirmExitSwitchItem());

        // BLOQUE: ALMACENAMIENTO Y SISTEMA
        items.add(new HeaderItem(getString(R.string.section_data_storage)));
        items.add(buildDataSaverSwitchItem());
        items.add(buildClearCacheButtonItem());
        items.add(buildResetSettingsButtonItem());

        return items;
    }

    // ==================== CONSTRUCCIÓN DE OPCIONES ====================

    private SelectItem buildLanguageSelectItem() {
        String savedLang = prefs.getLanguage();
        final String currentLang = (savedLang != null && !savedLang.isEmpty()) ? savedLang : "system";

        List<SelectItem.SelectOption> options = Arrays.asList(
                new SelectItem.SelectOption(getString(R.string.language_system_default), "system"),
                new SelectItem.SelectOption("Español", "es"),
                new SelectItem.SelectOption("English", "en")
        );

        return new SelectItem(getString(R.string.title_language), options, currentLang, selected -> {
            if (!selected.equals(currentLang)) {
                if ("system".equalsIgnoreCase(selected)) {
                    prefs.setLanguage(null);
                } else {
                    prefs.setLanguage(selected);
                }
                ThemeManager.restartApp(GeneralSettingsActivity.this);
            }
        });
    }

    private SelectItem buildDefaultStartTabSelectItem() {
        int currentTab = prefs.getDefaultStartTab();
        String[] tabNames = {
                getString(R.string.tab_home), getString(R.string.tab_news),
                getString(R.string.tab_events), getString(R.string.tab_posts),
                getString(R.string.tab_profile)
        };
        List<SelectItem.SelectOption> options = new ArrayList<>();
        for (int i = 0; i < tabNames.length; i++) options.add(new SelectItem.SelectOption(tabNames[i], String.valueOf(i)));
        
        return new SelectItem(getString(R.string.default_start_tab), options, String.valueOf(currentTab), selected -> {
            int newTab = Integer.parseInt(selected);
            if (newTab != currentTab) {
                prefs.setDefaultStartTab(newTab);
                Toast.makeText(this, R.string.default_tab_changed_restart, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private SwitchItem buildConfirmExitSwitchItem() {
        return new SwitchItem(getString(R.string.confirm_exit), prefs.isConfirmExit(), isChecked -> prefs.setConfirmExit(isChecked));
    }

    private SwitchItem buildDataSaverSwitchItem() {
        return new SwitchItem(getString(R.string.data_saver_mode), prefs.isDataSaverMode(), isChecked -> {
            prefs.setDataSaverMode(isChecked);
            Toast.makeText(this, isChecked ? R.string.data_saver_enabled : R.string.data_saver_disabled, Toast.LENGTH_SHORT).show();
        });
    }

    private ButtonItem buildClearCacheButtonItem() {
        return new ButtonItem(getString(R.string.clear_cache), v -> showClearCacheDialog());
    }

    private ButtonItem buildResetSettingsButtonItem() {
        return new ButtonItem(getString(R.string.reset_settings), v -> showResetSettingsDialog());
    }

    // ==================== LÓGICA DE MANTENIMIENTO ====================

    private void showClearCacheDialog() {
        DialogHelper.showConfirmDialog(this,
                getString(R.string.clear_cache_title), getString(R.string.clear_cache_message),
                getString(R.string.button_clear), getString(R.string.button_cancel),
                () -> {
                    if (clearAppCache()) Toast.makeText(this, R.string.cache_cleared, Toast.LENGTH_SHORT).show();
                    else Toast.makeText(this, R.string.cache_clear_error, Toast.LENGTH_SHORT).show();
                }, null);
    }

    /**
     * Elimina los directorios de caché interna y externa de la aplicación.
     */
    private boolean clearAppCache() {
        try {
            deleteDir(getCacheDir());
            deleteDir(getExternalCacheDir());
            deleteDir(new File(FileUtils.getPackageDataDir(this) + "/cache"));
            return true;
        } catch (Exception e) { return false; }
    }

    private boolean deleteDir(File dir) {
        if (dir != null && dir.isDirectory()) {
            String[] children = dir.list();
            if (children != null) {
                for (String child : children) if (!deleteDir(new File(dir, child))) return false;
            }
            return dir.delete();
        } else if (dir != null && dir.isFile()) return dir.delete();
        return false;
    }

    private void showResetSettingsDialog() {
        DialogHelper.showConfirmDialog(this,
                getString(R.string.reset_settings_title), getString(R.string.reset_settings_message),
                getString(R.string.button_reset), getString(R.string.button_cancel),
                () -> {
                    resetAllSettings();
                    ThemeManager.restartApp(this);
                }, null);
    }

    /**
     * Limpia todos los archivos de SharedPreferences para restaurar la App a fábrica.
     */
    private void resetAllSettings() {
        getSharedPreferences(PreferenceConstants.FILE_SETTINGS, MODE_PRIVATE).edit().clear().apply();
        getSharedPreferences(PreferenceConstants.FILE_BACKGROUND, MODE_PRIVATE).edit().clear().apply();
        getSharedPreferences(PreferenceConstants.FILE_SCHEDULE, MODE_PRIVATE).edit().clear().apply();
    }
}


