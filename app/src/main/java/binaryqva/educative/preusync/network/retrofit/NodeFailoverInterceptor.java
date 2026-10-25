/**
 * ============================================================================
 * Proyecto: PreuSync
 * Clase: NodeFailoverInterceptor.java
 * Versión: v1.0.0
 * Descripción: Interceptor OkHttp para la conmutación automática (Failover)
 *              entre los Nodos de Render del cluster API de PreuSync.
 * Autor: JiroxDEV
 * Licensed under the GNU Affero General Public License v3
 * ============================================================================
 */

package binaryqva.educative.preusync.network.retrofit;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import binaryqva.educative.preusync.debug.AppLogger;
import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class NodeFailoverInterceptor implements Interceptor {

    private static final String TAG = "PreuSync_Failover";

    private static final List<String> NODE_HOSTS = Arrays.asList(
        "preusync-api-node1.onrender.com",
        "preusync-api-node2.onrender.com",
        "preusync-api-node3.onrender.com"
    );

    @NonNull
    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {
        Request request = chain.request();
        HttpUrl originalUrl = request.url();

        // Intento primario con la URL solicitada
        try {
            Response response = chain.proceed(request);
            if (response.isSuccessful() || response.code() < 500) {
                return response;
            }
            response.close();
        } catch (IOException e) {
            AppLogger.w(TAG, "Nodo primario no respondió: " + e.getMessage() + ". Conmutando cluster...");
        }

        // Conmutación automática a Nodos Hermanos
        for (String host : NODE_HOSTS) {
            if (host.equals(originalUrl.host())) continue;

            HttpUrl newUrl = originalUrl.newBuilder()
                    .host(host)
                    .build();

            Request newRequest = request.newBuilder()
                    .url(newUrl)
                    .build();

            try {
                AppLogger.d(TAG, "Reintentando llamada en nodo hermano: " + host);
                Response response = chain.proceed(newRequest);
                if (response.isSuccessful() || response.code() < 500) {
                    return response;
                }
                response.close();
            } catch (IOException e) {
                AppLogger.w(TAG, "Nodo " + host + " falló: " + e.getMessage());
            }
        }

        return chain.proceed(request);
    }
}
