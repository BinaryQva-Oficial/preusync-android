/**
 * ============================================================================
 * Proyecto: PreuSync
 * Clase: DialogHelper.java
 * Versión: v7.2.0
 * Descripción: Centralizador de cuadros de diálogo. Estandariza la visualización
 *              de alertas, estados de carga y selectores con estilo Material 3.
 *              Incluye protección contra diálogos duplicados.
 * Autor: JiroxDEV
 * Licensed under the GNU Affero General Public License v3
 * ============================================================================
 */

package binaryqva.educative.preusync.utils.common;

import android.app.Activity;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.NumberPicker;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.text.DateFormatSymbols;
import java.util.List;

import binaryqva.educative.preusync.R;
import binaryqva.educative.preusync.utils.theme.ThemeManager;

import androidx.annotation.Nullable;

public class DialogHelper {

    private static String lastShownTitle = "";
    private static String lastShownMessage = "";
    private static AlertDialog currentActiveDialog = null;
	
	// ==================== DIÁLOGOS DE PROGRESO (SPINNER) ====================

	public static AlertDialog showProgressDialog(Activity activity, String message) {
		return showProgressDialog(activity, message, false);
	}
	
	public static AlertDialog showProgressDialog(Activity activity, String message, boolean cancelable) {
		MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(activity);
		View inflate = LayoutInflater.from(activity).inflate(R.layout.dialog_progress_circular, null);
		builder.setView(inflate);
		TextView text = inflate.findViewById(R.id.progressMessage);
		text.setText(message);
		builder.setCancelable(cancelable);
		AlertDialog dialog = builder.create();
		dialog.show();
		return dialog;
	}
	
	public static AlertDialog showProgressDialog(Fragment fragment, String message, boolean cancelable) {
		if (fragment.getActivity() == null) return null;
		return showProgressDialog(fragment.getActivity(), message, cancelable);
	}
	
	// ==================== DIÁLOGOS DE ALERTA E INFORMACIÓN ====================

	public static void showAlertDialog(Activity activity, String title, String message) {
		showAlertDialog(activity, title, message, null);
	}
	
	public static AlertDialog showAlertDialog(Activity activity, String title, String message, Runnable onPositive) {
        if (isAlreadyShowing(title, message)) return currentActiveDialog;

		MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(activity);
		View inflate = LayoutInflater.from(activity).inflate(R.layout.dialog_alert, null);
		builder.setView(inflate);
		TextView tvTitle = inflate.findViewById(R.id.heading);
		TextView tvMessage = inflate.findViewById(R.id.message);
		tvTitle.setText(title);
		tvMessage.setText(message);
		builder.setPositiveButton(activity.getString(R.string.button_accept), (dialog, which) -> {
            clearTracking();
			if (onPositive != null) onPositive.run();
		});
		builder.setCancelable(true);
        builder.setOnCancelListener(dialog -> clearTracking());

		AlertDialog dialog = builder.create();
		dialog.show();
        trackDialog(title, message, dialog);
		return dialog;
	}
	
	// ==================== GESTIÓN DE ERRORES Y REINTENTOS ====================

	public static void showErrorDialog(Activity activity, String message) {
		showAlertDialog(activity, activity.getString(R.string.error_general), message);
	}
	
	public static void showRetryDialog(Activity activity, String title, String message, Runnable onRetry, Runnable onExit) {
        if (isAlreadyShowing(title, message)) return;

		MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(activity);
		View inflate = LayoutInflater.from(activity).inflate(R.layout.dialog_alert, null);
		builder.setView(inflate);
		TextView tvTitle = inflate.findViewById(R.id.heading);
		TextView tvMessage = inflate.findViewById(R.id.message);
		tvTitle.setText(title);
		tvMessage.setText(message);
		builder.setPositiveButton(activity.getString(R.string.button_retry), (dialog, which) -> {
            clearTracking();
			if (onRetry != null) onRetry.run();
		});
		if (onExit != null) {
			builder.setNegativeButton(activity.getString(R.string.button_exit), (dialog, which) -> {
                clearTracking();
                onExit.run();
            });
		} else {
			builder.setNegativeButton(activity.getString(R.string.button_cancel), (dialog, which) -> clearTracking());
		}
		builder.setCancelable(true);
        builder.setOnCancelListener(dialog -> clearTracking());
		AlertDialog dialog = builder.show();
        trackDialog(title, message, dialog);
	}
	
	public static AlertDialog showConfirmDialog(Activity activity, String title, String message,
	String positiveText, String negativeText, Runnable onPositive, Runnable onNegative) {
		MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(activity);
		builder.setTitle(title).setMessage(message);
		builder.setPositiveButton(positiveText, (dialog, which) -> { if (onPositive != null) onPositive.run(); });
		builder.setNegativeButton(negativeText, (dialog, which) -> { if (onNegative != null) onNegative.run(); });
		return builder.show();
	}
	
	// ==================== SELECTORES Y COMPONENTES UI ====================

	public static class RadioOption {
		public final String label;
		public final String value;
		public RadioOption(String label, String value) { this.label = label; this.value = value; }
	}
	
	public interface OnOptionSelectedListener { void onOptionSelected(String value); }
	
	public static void showRadioDialog(Activity activity, String title,
	List<RadioOption> options, String selectedValue,
	OnOptionSelectedListener listener) {
		View dialogView = LayoutInflater.from(activity).inflate(R.layout.dialog_radio_selector, null);
		TextView tvTitle = dialogView.findViewById(R.id.dialogTitle);
		RadioGroup radioGroup = dialogView.findViewById(R.id.radioGroup);
		
		tvTitle.setText(title);
		radioGroup.removeAllViews();
		
		int colorAccent = ThemeManager.getThemeColor(activity, R.attr.colorAccent);
		int colorControlNormal = ThemeManager.getThemeColor(activity, R.attr.colorControlNormal);
		int colorText = ThemeManager.getThemeColor(activity, R.attr.colorText);
		
		for (RadioOption option : options) {
			RadioButton radioButton = new RadioButton(activity);
			radioButton.setId(View.generateViewId());
			radioButton.setText(option.label);
			radioButton.setTag(option.value);
			radioButton.setTextColor(colorText);
			radioButton.setButtonTintList(new ColorStateList(
                new int[][]{new int[]{android.R.attr.state_checked}, new int[]{-android.R.attr.state_checked}},
                new int[]{colorAccent, colorControlNormal}
            ));
			
			if (selectedValue != null && selectedValue.equals(option.value)) radioButton.setChecked(true);
			radioGroup.addView(radioButton);
		}
		
		MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(activity);
		builder.setView(dialogView);
		builder.setPositiveButton(activity.getString(R.string.button_accept), (dialog, which) -> {
			int checkedId = radioGroup.getCheckedRadioButtonId();
			if (checkedId != -1) {
				RadioButton selected = radioGroup.findViewById(checkedId);
				if (listener != null) listener.onOptionSelected((String) selected.getTag());
			}
		});
		builder.setNegativeButton(activity.getString(R.string.button_cancel), null);
		builder.show();
	}

	public static void showRadioDialogWithPreview(Activity activity, String title,
	List<RadioOption> options, String selectedValue,
	OnOptionSelectedListener listener, OnOptionSelectedListener previewListener, Runnable onCancel) {
		showRadioDialog(activity, title, options, selectedValue, listener);
	}

	public static void showDatePickerDialog(Activity activity, String title,
	int initialDay, int initialMonth, OnDateSetListener listener) {
		View dialogView = LayoutInflater.from(activity).inflate(R.layout.dialog_date_picker, null);
		TextView tvTitle = dialogView.findViewById(R.id.dialogTitle);
		NumberPicker dayPicker = dialogView.findViewById(R.id.dayPicker);
		NumberPicker monthPicker = dialogView.findViewById(R.id.monthPicker);
		
		tvTitle.setText(title);
		dayPicker.setMinValue(1); dayPicker.setMaxValue(31); dayPicker.setValue(initialDay);
		
		String[] monthNames = new DateFormatSymbols(activity.getResources().getConfiguration().locale).getMonths();
		monthPicker.setMinValue(0); monthPicker.setMaxValue(11);
		monthPicker.setDisplayedValues(monthNames);
		monthPicker.setValue(initialMonth);
		
		MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(activity);
		builder.setView(dialogView);
		builder.setPositiveButton(activity.getString(R.string.button_accept), (dialog, which) -> {
			if (listener != null) listener.onDateSet(dayPicker.getValue(), monthPicker.getValue());
		});
		builder.setNegativeButton(activity.getString(R.string.button_cancel), null);
		builder.show();
	}

    private static boolean isAlreadyShowing(String title, String message) {
        return currentActiveDialog != null && currentActiveDialog.isShowing() 
            && lastShownTitle.equals(title) && lastShownMessage.equals(message);
    }

    private static void trackDialog(String title, String message, AlertDialog dialog) {
        lastShownTitle = title;
        lastShownMessage = message;
        currentActiveDialog = dialog;
    }

    private static void clearTracking() {
        lastShownTitle = "";
        lastShownMessage = "";
        currentActiveDialog = null;
    }

    public interface OnDateSetListener { void onDateSet(int day, int month); }
}
