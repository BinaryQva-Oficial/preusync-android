/**
 * ============================================================================
 * Proyecto: PreuSync
 * Clase: ProfileEditorHelper.java
 * Versión: v4.0.0
 * Descripción: Ayudante para la edición de perfiles dividida en pasos.
 * Autor: JiroxDEV
 * Licensed under the GNU Affero General Public License v3
 * ============================================================================
 */

package binaryqva.educative.preusync.ui.helpers;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ViewFlipper;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputLayout;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import binaryqva.educative.preusync.R;
import binaryqva.educative.preusync.network.models.Profile;
import binaryqva.educative.preusync.ui.viewmodels.ProfileViewModel;
import binaryqva.educative.preusync.utils.common.AvatarHelper;
import binaryqva.educative.preusync.utils.common.DialogHelper;
import binaryqva.educative.preusync.utils.common.FilePickerHelper;
import binaryqva.educative.preusync.utils.common.FileUtils;
import binaryqva.educative.preusync.utils.common.PreferenceManager;
import binaryqva.educative.preusync.utils.common.RoleHelper;
import binaryqva.educative.preusync.utils.common.Validator;
import binaryqva.educative.preusync.utils.theme.ThemeManager;
import binaryqva.educative.preusync.utils.ui.ButtonAnimator;

public class ProfileEditorHelper {

    private final Context context;
    private final Activity activity;
    private final ProfileViewModel viewModel;
    private final FilePickerHelper filePickerHelper;

    private BottomSheetDialog editBottomSheet;
    private ViewFlipper editStepFlipper;
    private View step1Ind, step2Ind, step3Ind;
    
    private EditText firstNameET, lastNameET, usernameET, oldPasswordET, newPasswordET, idET;
    private TextInputLayout firstNameTIL, lastNameTIL, usernameTIL, oldPasswordTIL, newPasswordTIL, idTIL;
    private AutoCompleteTextView roleAC;
    private EditText schoolET, groupET, tuteeET, responsibilitiesET;
    private View studentLayout, tutorLayout, teacherLayout;
    private ImageView avatarIV;
    private MaterialSwitch advancedSwitch;
    private View advancedFields;
    private Button saveButton, cancelButton;

    private int currentStep = 0;
    private String pathBase64 = "";
    private boolean isEditAvatarInitial = true;
    private String currentRoleValue = RoleHelper.ROLE_STUDENT;
    private boolean ignoreSwitchChange = false;

    public interface OnSaveListener {
        void onSave(HashMap<String, Object> updateData);
        void onCancel();
    }

    public ProfileEditorHelper(Activity activity, ProfileViewModel viewModel, FilePickerHelper filePickerHelper) {
        this.context = activity; this.activity = activity;
        this.viewModel = viewModel; this.filePickerHelper = filePickerHelper;
    }

    public void showEditBottomSheet(Profile userData, OnSaveListener listener) {
        if (userData == null) return;
        if (editBottomSheet == null) initializeEditBottomSheet();
        currentStep = 0;
        fillData(userData);
        updateStepUi();
        
        cancelButton.setOnClickListener(v -> {
            if (currentStep > 0) goBackStep();
            else { editBottomSheet.dismiss(); if (listener != null) listener.onCancel(); }
        });
        
        saveButton.setOnClickListener(v -> {
            if (validateStep(currentStep)) {
                if (currentStep < 2) goNextStep();
                else {
                    HashMap<String, Object> data = collectData();
                    if (data != null && listener != null) listener.onSave(data);
                }
            }
        });
        editBottomSheet.show();
    }

    private void initializeEditBottomSheet() {
        editBottomSheet = new BottomSheetDialog(activity);
        View v = LayoutInflater.from(context).inflate(R.layout.bottomsheet_edit_account, null);
        ButtonAnimator.applyTo(v);
        editBottomSheet.setContentView(v);
        editBottomSheet.getWindow().findViewById(com.google.android.material.R.id.design_bottom_sheet).setBackgroundResource(android.R.color.transparent);
        editBottomSheet.setCancelable(false);
        
        editStepFlipper = v.findViewById(R.id.editStepFlipper);
        step1Ind = v.findViewById(R.id.step1Indicator);
        step2Ind = v.findViewById(R.id.step2Indicator);
        step3Ind = v.findViewById(R.id.step3Indicator);
        
        firstNameET = v.findViewById(R.id.firstNameEditText); lastNameET = v.findViewById(R.id.lastNameEditText);
        firstNameTIL = v.findViewById(R.id.firstNameTextInputLayout); lastNameTIL = v.findViewById(R.id.lastNameTextInputLayout);
        usernameET = v.findViewById(R.id.registerUsernameEditText); oldPasswordET = v.findViewById(R.id.oldPasswordEditText);
        newPasswordET = v.findViewById(R.id.newPasswordEditText); idET = v.findViewById(R.id.idEditText);
        usernameTIL = v.findViewById(R.id.registerUsernameTextInputLayout); oldPasswordTIL = v.findViewById(R.id.oldPasswordTextInputLayout);
        newPasswordTIL = v.findViewById(R.id.newPasswordTextInputLayout); idTIL = v.findViewById(R.id.idTextInputLayout);
        
        roleAC = v.findViewById(R.id.roleAutoComplete);
        schoolET = v.findViewById(R.id.schoolEditText); groupET = v.findViewById(R.id.groupEditText);
        tuteeET = v.findViewById(R.id.tuteeEditText); responsibilitiesET = v.findViewById(R.id.responsibilitiesEditText);
        studentLayout = v.findViewById(R.id.studentLayout); tutorLayout = v.findViewById(R.id.tuteeTextInputLayout); teacherLayout = v.findViewById(R.id.responsibilitiesTextInputLayout);
        
        avatarIV = v.findViewById(R.id.avatarImageView);
        advancedSwitch = v.findViewById(R.id.advancedMaterialSwitch);
        advancedFields = v.findViewById(R.id.advancedLayout);
        saveButton = v.findViewById(R.id.buttonSave);
        cancelButton = v.findViewById(R.id.buttonCancel);

        setupLogic();
    }

    private void setupLogic() {
        List<String> roles = RoleHelper.getLocalizedRoles(context);
        roleAC.setAdapter(new ArrayAdapter<>(context, android.R.layout.simple_dropdown_item_1line, roles));
        roleAC.setOnItemClickListener((p, view, pos, id) -> {
            currentRoleValue = RoleHelper.getRoleValueFromLocalized(roles.get(pos), context);
            updateRoleFields(currentRoleValue);
        });
        
        advancedSwitch.setOnCheckedChangeListener((btn, isChecked) -> {
            if (ignoreSwitchChange) { ignoreSwitchChange = false; return; }
            if (isChecked) {
                DialogHelper.showConfirmDialog(activity, context.getString(R.string.profile_warning_title), context.getString(R.string.profile_warning_message),
                    context.getString(R.string.profile_continue), context.getString(R.string.button_cancel), 
                    () -> advancedFields.setVisibility(View.VISIBLE),
                    () -> { ignoreSwitchChange = true; advancedSwitch.setChecked(false); advancedFields.setVisibility(View.GONE); });
            } else advancedFields.setVisibility(View.GONE);
        });
        
        avatarIV.setOnClickListener(v -> showAvatarSelector());
    }

    private void updateStepUi() {
        editStepFlipper.setDisplayedChild(currentStep);
        cancelButton.setText(currentStep == 0 ? R.string.button_cancel : R.string.button_previous);
        saveButton.setText(currentStep == 2 ? R.string.button_save : R.string.button_next);
        
        int accent = ThemeManager.getColorAccent(context);
        int highlight = ThemeManager.getColorControlHighlight(context);
        step1Ind.setBackgroundColor(currentStep >= 0 ? accent : highlight);
        step2Ind.setBackgroundColor(currentStep >= 1 ? accent : highlight);
        step3Ind.setBackgroundColor(currentStep >= 2 ? accent : highlight);
    }

    private void goNextStep() {
        currentStep++;
        editStepFlipper.setInAnimation(context, R.anim.slide_in_right);
        editStepFlipper.setOutAnimation(context, R.anim.slide_out_left);
        updateStepUi();
    }

    private void goBackStep() {
        currentStep--;
        editStepFlipper.setInAnimation(context, R.anim.slide_in_left);
        editStepFlipper.setOutAnimation(context, R.anim.slide_out_right);
        updateStepUi();
    }

    private boolean validateStep(int step) {
        if (step == 0) {
            boolean v1 = Validator.isValidFirstName(firstNameET.getText().toString());
            firstNameTIL.setError(v1 ? null : context.getString(R.string.profile_invalid_first_name));
            boolean v2 = Validator.isValidLastName(lastNameET.getText().toString());
            lastNameTIL.setError(v2 ? null : context.getString(R.string.profile_invalid_last_name));
            return v1 && v2;
        } else if (step == 2) {
            boolean v3 = !advancedSwitch.isChecked() || Validator.isValidUsername(usernameET.getText().toString());
            usernameTIL.setError(v3 ? null : context.getString(R.string.profile_invalid_username));
            boolean v4 = !oldPasswordET.getText().toString().isEmpty();
            oldPasswordTIL.setError(v4 ? null : context.getString(R.string.profile_current_password_required));
            return v3 && v4;
        }
        return true;
    }

    private void fillData(Profile p) {
        firstNameET.setText(p.getFirstName()); lastNameET.setText(p.getLastName());
        usernameET.setText(p.getUsername()); idET.setText(p.getIdCard());
        currentRoleValue = p.getRole() != null ? p.getRole() : RoleHelper.ROLE_STUDENT;
        roleAC.setText(RoleHelper.getLocalizedFromRoleValue(currentRoleValue, context), false);
        updateRoleFields(currentRoleValue);
        schoolET.setText(p.getSchool()); groupET.setText(p.getGroup());
        tuteeET.setText(p.getTutee()); responsibilitiesET.setText(p.getResponsibilities());
        renderAvatar(p);
    }

    private void renderAvatar(Profile p) {
        String url = p.getAvatarUrl();
        if (url != null && !url.isEmpty() && !url.startsWith("{")) Glide.with(context).load(url).into(avatarIV);
        else avatarIV.setImageResource(R.drawable.ic_nav_profile);
    }

    private HashMap<String, Object> collectData() {
        LinkedHashMap<String, Object> data = new LinkedHashMap<>();
        data.put("first_name", firstNameET.getText().toString().trim());
        data.put("last_name", lastNameET.getText().toString().trim());
        data.put("role", currentRoleValue);
        if (currentRoleValue.equals(RoleHelper.ROLE_STUDENT)) {
            data.put("school_id", schoolET.getText().toString().trim());
            data.put("group", groupET.getText().toString().trim());
        }
        else if (currentRoleValue.equals(RoleHelper.ROLE_TUTOR)) data.put("tutee", tuteeET.getText().toString().trim());
        else data.put("responsibilities", responsibilitiesET.getText().toString().trim());
        
        if (advancedSwitch.isChecked()) {
            data.put("username", usernameET.getText().toString().trim());
            data.put("id_card", idET.getText().toString().trim());
            String pass = newPasswordET.getText().toString().trim();
            if (!pass.isEmpty()) data.put("password", pass);
        }
        if (!pathBase64.isEmpty()) data.put("avatar_url", pathBase64);
        data.put("current_password", oldPasswordET.getText().toString().trim());
        return data;
    }

    private void updateRoleFields(String val) {
        studentLayout.setVisibility(val.equals(RoleHelper.ROLE_STUDENT) ? View.VISIBLE : View.GONE);
        tutorLayout.setVisibility(val.equals(RoleHelper.ROLE_TUTOR) ? View.VISIBLE : View.GONE);
        teacherLayout.setVisibility(val.equals(RoleHelper.ROLE_TEACHER) ? View.VISIBLE : View.GONE);
    }

    private void showAvatarSelector() {
        BottomSheetDialog selector = new BottomSheetDialog(activity);
        View v = LayoutInflater.from(context).inflate(R.layout.bottomsheet_avatar_selector, null);
        selector.setContentView(v);
        View bs = selector.findViewById(com.google.android.material.R.id.design_bottom_sheet);
        if (bs != null) {
            bs.setBackgroundResource(android.R.color.transparent);
        }
        v.findViewById(R.id.uploadImageLayout).setOnClickListener(view -> filePickerHelper.pickFile("image/jpeg", new FilePickerHelper.OnFilePickedListener() {
            @Override public void onFilePicked(String path, String name, String mime, String base64) {
                pathBase64 = base64; avatarIV.setImageBitmap(FileUtils.decodeSampleBitmapFromPath(path, 256, 256));
                selector.dismiss();
            }
            @Override public void onPickCancelled() {}
        }));
        selector.show();
    }

    public void dismiss() { if (editBottomSheet != null) editBottomSheet.dismiss(); }
}
