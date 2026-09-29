/**
 * ============================================================================
 * Proyecto: PreuSync
 * Clase: RetrofitClient.java
 * Versión: v1.0.2
 * Descripción: Cliente singleton para Retrofit. Configura OkHttp con 
 *              interceptores de red y autenticación.
 * Autor: JiroxDEV
 * Licensed under the GNU Affero General Public License v3
 * ============================================================================
 */

package binaryqva.educative.preusync.network.retrofit;

import android.content.Context;

import androidx.annotation.NonNull;

import binaryqva.educative.preusync.utils.common.PreferenceConstants;
import binaryqva.educative.preusync.utils.common.PreferenceManager;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Gestiona la instancia única del cliente API y la configuración de red.
 */
public class RetrofitClient {

    private static ApiService apiService;
    // La URL base se obtiene de las constantes de preferencias del sistema.
    private static final String BASE_URL = PreferenceConstants.API_BASE_URL + "/";

    /**
     * Proporciona la instancia del servicio API, inicializándola si es necesario.
     * @param context Contexto de la aplicación para acceder a preferencias.
     * @return Instancia única de ApiService.
     */
    public static synchronized ApiService getApiService(Context context) {
        if (apiService == null) {
            // Configuración de timeouts optimizada para conexiones móviles inestables.
            OkHttpClient.Builder httpClient = new OkHttpClient.Builder()
                    .connectTimeout(25, TimeUnit.SECONDS)
                    .readTimeout(35, TimeUnit.SECONDS)
                    .writeTimeout(35, TimeUnit.SECONDS);

            // Interceptor para depuración de peticiones HTTP en consola.
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);
            httpClient.addInterceptor(logging);

            // Interceptor de autenticación: inyecta automáticamente el JWT en cada petición.
            httpClient.addInterceptor(new Interceptor() {
                @NonNull
                @Override
                public Response intercept(@NonNull Chain chain) throws IOException {
                    Request original = chain.request();
                    PreferenceManager prefs = PreferenceManager.getInstance(context);
                    String token = prefs.getSupabaseAccessToken();

                    // Construcción de la nueva petición con cabeceras estándar.
                    Request.Builder requestBuilder = original.newBuilder()
                            .header("Content-Type", "application/json")
                            .method(original.method(), original.body());

                    // Si el token existe (usuario logueado), se añade a la cabecera Authorization.
                    if (token != null && !token.isEmpty()) {
                        requestBuilder.header("Authorization", "Bearer " + token);
                    }

                    Request request = requestBuilder.build();
                    return chain.proceed(request);
                }
            });

            // Inicialización de Retrofit con el conversor Gson y el cliente OkHttp configurado.
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create())
                    .client(httpClient.build())
                    .build();

            apiService = retrofit.create(ApiService.class);
        }
        return apiService;
    }
}


