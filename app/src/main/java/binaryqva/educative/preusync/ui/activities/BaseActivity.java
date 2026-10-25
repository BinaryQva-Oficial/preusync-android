/**
 * ============================================================================
 * Proyecto: PreuSync
 * Clase Abstracta: BaseActivity.java
 * Versión: v1.8.1
 * Descripción: Clase base para todas las actividades. Centraliza la gestión 
 *              de temas, idioma y animaciones globales.
 * Autor: JiroxDEV
 * Licensed under the GNU Affero General Public License v3
 * ============================================================================
 */

package binaryqva.educative.preusync.ui.activities;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Build;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

import binaryqva.educative.preusync.R;
import binaryqva.educative.preusync.utils.common.LocaleManager;
import binaryqva.educative.preusync.utils.common.PreferenceManager;
import binaryqva.educative.preusync.utils.common.PreferenceConstants;
import binaryqva.educative.preusync.utils.ui.ButtonAnimator;
import binaryqva.educative.preusync.utils.theme.ThemeManager;

/**
 * Provee la infraestructura común para las pantallas de la aplicación.
 * Asegura que las preferencias del usuario (idioma, fuente, tema) se apliquen
 * correctamente en cada ciclo de vida de la actividad.
 */
public abstract class BaseActivity extends AppCompatActivity {
	
	@Override
	protected void attachBaseContext(Context newBase) {
		PreferenceManager prefs = PreferenceManager.getInstance(newBase);
		
		// 1. APLICACIÓN DINÁMICA DE IDIOMA:
		String language = prefs.getLanguage();
		if (language == null || "system".equalsIgnoreCase(language)) language = LocaleManager.getCurrentLanguage(newBase);
		Context contextWithLocale = LocaleManager.setLocale(newBase, language);
		
		// 2. ESCALADO DE FUENTES PERSONALIZADO:
		float fontScale = prefs.getFontScale();
		Configuration config = new Configuration(contextWithLocale.getResources().getConfiguration());
		config.fontScale = fontScale;
		
		// Inyectamos el nuevo contexto configurado para que afecte a toda la actividad.
		super.attachBaseContext(contextWithLocale.createConfigurationContext(config));
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		PreferenceManager prefs = PreferenceManager.getInstance(getApplicationContext());
		
		boolean useDynamic = prefs.isDynamicColorsEnabled();
		String fontFamily = prefs.getFontFamily();
		
		// 1. INTEGRACIÓN DE FAMILIAS TIPOGRÁFICAS Y TEMAS (Soporte para Material You Android 12+):
		applyFontTheme(fontFamily, useDynamic);
		
		// 3. PERSISTENCIA DE MODO OSCURO/CLARO:
		ThemeManager.applyTheme(prefs.getTheme());
		
		super.onCreate(savedInstanceState);
	}
	
	private void applyFontTheme(String fontFamily, boolean useDynamic) {
		boolean isS = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S;
		int themeId;
		switch (fontFamily) {
			case "sans-serif":
				themeId = (useDynamic && isS ? R.style.Theme_PreuSync_Dynamic_Sans : R.style.Theme_PreuSync_Static_Sans);
				break;
			case "serif":
				themeId = (useDynamic && isS ? R.style.Theme_PreuSync_Dynamic_Serif : R.style.Theme_PreuSync_Static_Serif);
				break;
			case "monospace":
				themeId = (useDynamic && isS ? R.style.Theme_PreuSync_Dynamic_Mono : R.style.Theme_PreuSync_Static_Mono);
				break;
			default:
				themeId = (useDynamic && isS ? R.style.Theme_PreuSync_Dynamic : R.style.Theme_PreuSync_Static);
				break;
		}
		setTheme(themeId);
	}
	
	@Override
	public void setContentView(int layoutResID) {
		super.setContentView(layoutResID);
		// APLICACIÓN OPTIMIZADA DE ANIMACIONES:
		// Se delega al ButtonAnimator, el cual ahora gestiona mejor la recursividad y tags.
		ButtonAnimator.applyTo(findViewById(android.R.id.content));
	}
}


