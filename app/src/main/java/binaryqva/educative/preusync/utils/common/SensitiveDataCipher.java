/**
 * ============================================================================
 * Proyecto: PreuSync
 * Clase: SensitiveDataCipher.java
 * Versión: v1.0.0
 * Descripción: Cifrado AES-GCM con clave derivada de salt único por equipo.
 * Autor: JiroxDEV
 * Licensed under the GNU Affero General Public License v3
 * ============================================================================
 */

package binaryqva.educative.preusync.utils.common;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.KeySpec;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import binaryqva.educative.preusync.debug.AppLogger;

/**
 * Provee cifrado robusto para la caché sensible (perfiles y estadísticas).
 * Utiliza PBKDF2 para derivar claves a partir de un salt generado aleatoriamente
 * durante la primera ejecución de la App.
 */
public final class SensitiveDataCipher {

    private static final String TAG = "SensitiveCipher";
    private static final String PREFS = PreferenceConstants.FILE_SETTINGS;
    private static final String SALT_KEY = "sensitive_cache_salt";

    private static final int IV_LEN = 12, TAG_LEN = 128, KEY_LEN = 256, ITERS = 10000;
    private static final String SEED = "PreuSyncSecureCaché";

    private SensitiveDataCipher() {}

    private static byte[] getSalt(Context ctx) {
        SharedPreferences p = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String s = p.getString(SALT_KEY, null);
        if (s == null) {
            byte[] salt = new byte[16]; new SecureRandom().nextBytes(salt);
            s = Base64.encodeToString(salt, Base64.DEFAULT);
            p.edit().putString(SALT_KEY, s).apply();
            AppLogger.d(TAG, "Salt único generado para este dispositivo");
        }
        return Base64.decode(s, Base64.DEFAULT);
    }

    private static SecretKey getKey(Context ctx) throws Exception {
        byte[] salt = getSalt(ctx);
        KeySpec spec = new PBEKeySpec(SEED.toCharArray(), salt, ITERS, KEY_LEN);
        SecretKeyFactory factory;
        try {
            factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        } catch (NoSuchAlgorithmException e) {
            factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
        }
        byte[] k = factory.generateSecret(spec).getEncoded();
        return new SecretKeySpec(k, "AES");
    }

    /**
     * Codifica una cadena de texto en un bloque cifrado Base64.
     */
    public static String encode(String raw, Context ctx) {
        if (raw == null) return null;
        try {
            SecretKey key = getKey(ctx);
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            byte[] iv = new byte[IV_LEN]; new SecureRandom().nextBytes(iv);
            c.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_LEN, iv));

            byte[] enc = c.doFinal(raw.getBytes(StandardCharsets.UTF_8));
            ByteBuffer buf = ByteBuffer.allocate(iv.length + enc.length);
            buf.put(iv); buf.put(enc);
            return Base64.encodeToString(buf.array(), Base64.DEFAULT);
        } catch (Exception e) { AppLogger.e(TAG, "Error de codificación sensible", e); return null; }
    }

    /**
     * Decodifica un bloque Base64 recuperando el texto original.
     */
    public static String decode(String enc, Context ctx) {
        if (enc == null) return null;
        try {
            byte[] res = Base64.decode(enc, Base64.DEFAULT);
            if (res.length < IV_LEN) return null;

            ByteBuffer buf = ByteBuffer.wrap(res);
            byte[] iv = new byte[IV_LEN]; buf.get(iv);
            byte[] cipher = new byte[buf.remaining()]; buf.get(cipher);

            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.DECRYPT_MODE, getKey(ctx), new GCMParameterSpec(TAG_LEN, iv));
            return new String(c.doFinal(cipher), StandardCharsets.UTF_8);
        } catch (Exception e) { AppLogger.e(TAG, "Error de decodificación sensible", e); return null; }
    }
}


