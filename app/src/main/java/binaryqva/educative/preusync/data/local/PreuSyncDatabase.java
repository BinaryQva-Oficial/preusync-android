/**
 * ============================================================================
 * Proyecto: PreuSync
 * Clase abstracta: PreuSyncDatabase.java
 * Versión: v2.0.0
 * Descripción: Base de datos centralizada de Room. Registra todas las entidades
 *              y provee acceso a los DAOs para la persistencia local.
 * Autor: JiroxDEV
 * Licensed under the GNU Affero General Public License v3
 * ============================================================================
 */

package binaryqva.educative.preusync.data.local;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import binaryqva.educative.preusync.data.local.dao.EphemerisDao;
import binaryqva.educative.preusync.data.local.dao.EventDao;
import binaryqva.educative.preusync.data.local.dao.NewsDao;
import binaryqva.educative.preusync.data.local.dao.PostDao;
import binaryqva.educative.preusync.data.local.dao.ScheduleDao;
import binaryqva.educative.preusync.data.local.entities.EphemerisEntity;
import binaryqva.educative.preusync.data.local.entities.EventEntity;
import binaryqva.educative.preusync.data.local.entities.NewsEntity;
import binaryqva.educative.preusync.data.local.entities.PostEntity;
import binaryqva.educative.preusync.data.local.entities.ScheduleEntity;

@Database(entities = {
        NewsEntity.class, 
        PostEntity.class, 
        EphemerisEntity.class, 
        EventEntity.class, 
        ScheduleEntity.class
}, version = 2, exportSchema = false)
public abstract class PreuSyncDatabase extends RoomDatabase {
    private static volatile PreuSyncDatabase instance;

    public abstract NewsDao newsDao();
    public abstract PostDao postDao();
    public abstract EphemerisDao ephemerisDao();
    public abstract EventDao eventDao();
    public abstract ScheduleDao scheduleDao();

    public static PreuSyncDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (PreuSyncDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(context.getApplicationContext(),
                            PreuSyncDatabase.class, "PreuSync_db")
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return instance;
    }
}


