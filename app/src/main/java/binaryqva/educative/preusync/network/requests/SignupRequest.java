/**
 * ============================================================================
 * Proyecto: PreuSync
 * Clase: SignupRequest.java
 * Versión: v1.1.0
 * Descripción: Modelo de petición para el registro de nuevos usuarios.
 * Autor: JiroxDEV
 * Licensed under the GNU Affero General Public License v3
 * ============================================================================
 */
package binaryqva.educative.preusync.network.requests;

import com.google.gson.annotations.SerializedName;

public class SignupRequest {
    @SerializedName("username")
    private String username;

    @SerializedName("password")
    private String password;

    @SerializedName("firstName")
    private String firstName;

    @SerializedName("lastName")
    private String lastName;

    @SerializedName("idCard")
    private String idCard;

    @SerializedName("role")
    private String role;

    @SerializedName("schoolId")
    private String schoolId;

    @SerializedName("groupId")
    private String groupId;

    @SerializedName("groupName")
    private String groupName;

    @SerializedName("tutee")
    private String tutee;

    @SerializedName("responsibilities")
    private String responsibilities;

    @SerializedName("avatar")
    private String avatar;

    public SignupRequest(String username, String password, String firstName, String lastName) {
        this.username = username;
        this.password = password;
        this.firstName = firstName;
        this.lastName = lastName;
    }

    public void setIdCard(String idCard) { this.idCard = idCard; }
    public void setRole(String role) { this.role = role; }
    public void setSchoolId(String schoolId) { this.schoolId = schoolId; }
    public void setGroupId(String groupId) { this.groupId = groupId; }
    public void setGroupName(String groupName) { this.groupName = groupName; }
    public void setTutee(String tutee) { this.tutee = tutee; }
    public void setResponsibilities(String responsibilities) { this.responsibilities = responsibilities; }
    public void setAvatar(String avatar) { this.avatar = avatar; }
}
