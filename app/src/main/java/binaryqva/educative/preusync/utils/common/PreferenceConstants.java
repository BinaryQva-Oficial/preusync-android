/**
 * ============================================================================
 * Proyecto: PreuSync
 * Clase: PreferenceConstants.java
 * Versión: v2.1.0
 * Descripción: Repositorio centralizado de claves para SharedPreferences.
 * Autor: JiroxDEV
 * Licensed under the GNU Affero General Public License v3
 * ============================================================================
 */

package binaryqva.educative.preusync.utils.common;

/**
 * Define las constantes estáticas utilizadas para el almacenamiento persistente.
 * Garantiza la integridad de las claves a través de toda la arquitectura.
 */
public final class PreferenceConstants {
	
	// ARCHIVOS DE PREFERENCIAS:
	public static final String FILE_SETTINGS = "prefs_settings";
	public static final String FILE_ACCOUNT = "prefs_account";
	public static final String FILE_POST_VOTES_PREFIX = "prefs_post_votes_";
	public static final String FILE_BACKGROUND = "prefs_background";
	public static final String FILE_SCHEDULE = "prefs_schedule";
	
	// CLAVES DE CONFIGURACIÓN GENERAL:
	public static final String KEY_LANGUAGE = "language";
	public static final String KEY_DYNAMIC_COLORS = "dynamic_colors";
	public static final String KEY_THEME = "theme";
	public static final String KEY_ONBOARDING_COMPLETED = "onboarding_completed";
	public static final String KEY_HAS_ACCOUNT = "has_account";
	public static final String KEY_APP_OPEN_COUNT = "app_open_count";
	public static final String KEY_LOGIN_SESSION_COUNT = "login_session_count";
	public static final String KEY_FONT_SCALE = "font_scale";
	public static final String KEY_FONT_FAMILY = "font_family";
	public static final String KEY_BLUR_ENABLED = "blur_enabled";
	public static final String KEY_SHOW_FEATURED_NEWS = "show_featured_news";
	public static final String KEY_SHOW_FEATURED_POSTS = "show_featured_posts";
	public static final String KEY_SHOW_DAILY_EPHEMERIS = "show_daily_ephemeris";
	public static final String KEY_SHOW_CURRENT_SHIFT = "show_current_shift";
	public static final String KEY_ANIMATION_SPEED = "animation_speed";
	public static final String KEY_DEFAULT_START_TAB = "default_start_tab";
	public static final String KEY_CONFIRM_EXIT = "confirm_exit";
	public static final String KEY_DATA_SAVER_MODE = "data_saver_mode";
	
	// SINCRONIZACIÓN Y NOTIFICACIONES:
	public static final String KEY_BG_SYNC_ENABLED = "bg_sync_enabled";
	public static final String KEY_WIFI_ONLY_BACKGROUND = "wifi_only_background";
	public static final String KEY_SYNC_FREQ_NEWS = "sync_freq_news";
	public static final String KEY_SYNC_FREQ_POSTS = "sync_freq_posts";
	public static final String KEY_SYNC_FREQ_SCHOOL_EVENTS = "sync_freq_school_events";
	public static final String KEY_SYNC_FREQ_EXTERNAL_EVENTS = "sync_freq_external_events";
	public static final String KEY_SYNC_FREQ_EPHEMERIDES = "sync_freq_ephemerides";
	public static final String KEY_SYNC_FREQ_SCHEDULE = "sync_freq_schedule";
	
	public static final String KEY_NOTIF_NEWS_ENABLED = "notif_news_enabled";
	public static final String KEY_NOTIF_POSTS_ENABLED = "notif_posts_enabled";
	public static final String KEY_NOTIF_SCHOOL_EVENTS_ENABLED = "notif_school_events_enabled";
	public static final String KEY_NOTIF_EXTERNAL_EVENTS_ENABLED = "notif_external_events_enabled";
	public static final String KEY_NOTIF_EPHEMERIDES_ENABLED = "notif_ephemerides_enabled";
	public static final String KEY_NOTIF_SCHEDULE_ENABLED = "notif_schedule_enabled";
	
	public static final String KEY_NOTIF_SOUND_NEWS = "notif_sound_news";
	public static final String KEY_NOTIF_SOUND_POSTS = "notif_sound_posts";
	public static final String KEY_NOTIF_SOUND_SCHOOL_EVENTS = "notif_sound_school_events";
	public static final String KEY_NOTIF_SOUND_EXTERNAL_EVENTS = "notif_sound_external_events";
	public static final String KEY_NOTIF_SOUND_EPHEMERIDES = "notif_sound_ephemerides";
	public static final String KEY_NOTIF_SOUND_SCHEDULE = "notif_sound_schedule";
	
	public static final String KEY_REMINDER_ENABLED = "reminder_enabled";
	public static final String KEY_REMINDER_LEAD_TIME_HOURS = "reminder_lead_time_hours";
	public static final String KEY_REMINDER_SOUND = "reminder_sound";
	
	public static final String KEY_AUTO_UPDATES_ENABLED = "auto_updates_enabled";
	public static final String KEY_AUTO_UPDATES_FREQ_DAYS = "auto_updates_freq_days";
	public static final String KEY_AUTO_UPDATES_WIFI_ONLY = "auto_updates_wifi_only";
	public static final String KEY_AUTO_UPDATES_SOUND = "auto_updates_sound";
	
	public static final String KEY_INTERVAL_MINUTES = "interval_minutes";
	
	// TIMESTAMPS DE ÚLTIMA SINCRONIZACIÓN:
	public static final String KEY_LAST_NEWS = "last_news_timestamp";
	public static final String KEY_LAST_POSTS = "last_posts_timestamp";
	public static final String KEY_LAST_EVENTS = "last_events_timestamp";
	public static final String KEY_LAST_EPHEMERIDES = "last_ephemerides_timestamp";
	public static final String KEY_LAST_SCHEDULE = "last_schedule_timestamp";
	
	public static final String KEY_NOTIFIED_IDS_NEWS = "notified_ids_news";
	public static final String KEY_NOTIFIED_IDS_POSTS = "notified_ids_posts";
	public static final String KEY_NOTIFIED_IDS_EVENTS = "notified_ids_events";
	public static final String KEY_LAST_EPHEMERIS_DATE = "last_ephemeris_date";
	public static final String KEY_SCHEDULE_HASH = "schedule_hash";
	
	// DATOS DE CUENTA:
	public static final String KEY_USERNAME = "username";
	public static final String KEY_USER_ID = "user_id";
	public static final String KEY_USER_GROUP = "user_group";
	public static final String KEY_SUPABASE_ACCESS_TOKEN = "supabase_access_token";
	public static final String KEY_SUPABASE_REFRESH_TOKEN = "supabase_refresh_token";
	public static final String KEY_SUPABASE_USER_ID = "supabase_user_id";
	public static final String KEY_SCHOOL_ID = "school_id";
	
	public static final String KEY_LAST_SCHEDULE_GROUP = "last_schedule_group";
	
	// CONFIGURACIÓN DE API:
	public static final String API_BASE_URL = "https://preusync-api-node1.onrender.com/api";
	
	private PreferenceConstants() {}
}


