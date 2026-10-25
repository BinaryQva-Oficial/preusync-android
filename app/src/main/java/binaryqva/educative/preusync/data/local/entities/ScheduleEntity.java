/**
 * ============================================================================
 * Proyecto: PreuSync
 * Clase: ScheduleEntity.java
 * Versión: v2.0.0
 * Descripción: Entidad de Room para la persistencia del horario escolar unificado.
 * Autor: JiroxDEV
 * Licensed under the GNU Affero General Public License v3
 * ============================================================================
 */

package binaryqva.educative.preusync.data.local.entities;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "schedules")
public class ScheduleEntity {
    @PrimaryKey
    @NonNull
    public String id;
    public String groupName;
    public int shift;
    public String day;
    public String subject;

    public ScheduleEntity(@NonNull String id, String groupName, int shift, String day, String subject) {
        this.id = id; this.groupName = groupName; this.shift = shift;
        this.day = day; this.subject = subject;
    }
}
