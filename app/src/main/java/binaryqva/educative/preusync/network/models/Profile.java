/**
 * ============================================================================
 * Proyecto: PreuSync
 * Clase: Profile.java
 * Versión: v1.1.0
 * Descripción: Modelo detallado del perfil de usuario, incluyendo roles 
 *              académicos, ubicación e información personal.
 * Autor: JiroxDEV
 * Licensed under the GNU Affero General Public License v3
 * ============================================================================
 */
package binaryqva.educative.preusync.network.models;

import com.google.gson.annotations.SerializedName;

public class Profile {
    @SerializedName("id")
    private String id;

    @SerializedName("username")
    private String username;

    @SerializedName("first_name")
    private String firstName;

    @SerializedName("last_name")
    private String lastName;

    @SerializedName("full_name")
    private String fullName;

    @SerializedName("id_card")
    private String idCard;

    @SerializedName("role")
    private String role;

    @SerializedName("status")
    private String status;

    @SerializedName("avatar_url")
    private String avatarUrl;

    @SerializedName("school")
    private String school;

    @SerializedName("school_id")
    private String schoolId;

    @SerializedName("group")
    private String group;

    @SerializedName("province")
    private String province;

    @SerializedName("municipality")
    private String municipality;

    @SerializedName("tutee")
    private String tutee;

    @SerializedName("responsibilities")
    private String responsibilities;

    // Getters
    public String getId() { return id; }
    public String getUsername() { return username; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getFullName() { return fullName; }
    public String getIdCard() { return idCard; }
    public String getRole() { return role; }
    public String getStatus() { return status; }
    public String getAvatarUrl() { return avatarUrl; }
    public String getSchool() { return school; }
    public String getSchoolId() { return schoolId; }
    public String getGroup() { return group; }
    public String getProvince() { return province; }
    public String getMunicipality() { return municipality; }
    public String getTutee() { return tutee; }
    public String getResponsibilities() { return responsibilities; }
}
