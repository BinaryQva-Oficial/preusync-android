/**
 * ============================================================================
 * Proyecto: PreuSync
 * Clase: Schedule.java
 * Versión: v2.1.0
 * Descripción: Modelo de datos para el horario escolar dinámico unificado.
 * Autor: JiroxDEV
 * Licensed under the GNU Affero General Public License v3
 * ============================================================================
 */

package binaryqva.educative.preusync.network.models;

import com.google.gson.annotations.SerializedName;
import binaryqva.educative.preusync.data.local.entities.ScheduleEntity;

public class Schedule {

    @SerializedName("id")
    private String id;

    @SerializedName("group")
    private String group;

    @SerializedName("shift")
    private int shift;

    @SerializedName("day")
    private String day;

    @SerializedName("subject")
    private String subject;

    public Schedule() {}

    public Schedule(ScheduleEntity entity) {
        if (entity == null) return;
        this.id = entity.id;
        this.group = entity.groupName;
        this.shift = entity.shift;
        this.day = entity.day;
        this.subject = entity.subject;
    }

    public ScheduleEntity toEntity() {
        return new ScheduleEntity(id, group, shift, day, subject);
    }

    // Getters
    public String getId() { return id; }
    public String getGroup() { return group; }
    public int getShift() { return shift; }
    public String getDay() { return day; }
    public String getSubject() { return subject; }
}
