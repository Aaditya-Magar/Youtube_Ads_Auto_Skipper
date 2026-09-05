package com.ytadsskipper.app;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.view.Gravity;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityManager;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

public final class MainActivity extends Activity {
    private SharedPreferences preferences;
    private Dialog permissionSheet;
    private boolean restoreSheet;

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferences = getSharedPreferences("settings", MODE_PRIVATE);
        setContentView(R.layout.activity_main);
        // Keep controls clear of system bars on edge-to-edge Android versions.
        findViewById(R.id.page).setOnApplyWindowInsetsListener((view, insets) -> {
            view.setPadding(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(),
                    insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom());
            return insets;
        });
        findViewById(R.id.page).requestApplyInsets();
        restoreSheet = savedInstanceState != null && savedInstanceState.getBoolean("permission_sheet");
        findViewById(R.id.auto_skip).setOnClickListener(view -> {
            if (!hasAccess()) {
                showPermissionSheet();
                return;
            }
            preferences.edit().putBoolean("enabled", !preferences.getBoolean("enabled", false)).apply();
            refresh();
        });
        findViewById(R.id.auto_skip).setAccessibilityDelegate(new View.AccessibilityDelegate() {
            @Override public void onInitializeAccessibilityNodeInfo(View host, AccessibilityNodeInfo info) {
                super.onInitializeAccessibilityNodeInfo(host, info);
                info.setCheckable(true);
                info.setChecked(host.isSelected());
            }
        });
        findViewById(R.id.help).setOnClickListener(view -> new AlertDialog.Builder(this)
                .setTitle(R.string.help).setMessage(R.string.help_body)
                .setPositiveButton(R.string.close, null)
                .setNeutralButton(R.string.app_info, (dialog, which) -> openSettings(
                        new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + getPackageName()))))
                .show());
    }

    @Override protected void onResume() {
        super.onResume();
        refresh();
        if (!hasAccess() && (!preferences.getBoolean("setup_seen", false) || restoreSheet)) {
            restoreSheet = false;
            showPermissionSheet();
        }
    }

    @Override protected void onSaveInstanceState(Bundle state) {
        state.putBoolean("permission_sheet", permissionSheet != null && permissionSheet.isShowing());
        super.onSaveInstanceState(state);
    }

    @Override protected void onDestroy() {
        if (permissionSheet != null) permissionSheet.dismiss();
        super.onDestroy();
    }

    private void showPermissionSheet() {
        if (permissionSheet != null && permissionSheet.isShowing()) return;
        preferences.edit().putBoolean("setup_seen", true).apply();
        permissionSheet = new Dialog(this, R.style.PermissionSheet);
        permissionSheet.setContentView(R.layout.permission_sheet);
        permissionSheet.setCanceledOnTouchOutside(true);
        permissionSheet.findViewById(R.id.not_now).setOnClickListener(view -> permissionSheet.cancel());
        permissionSheet.setOnCancelListener(dialog ->
                preferences.edit().putBoolean("pending_enable", false).apply());
        permissionSheet.findViewById(R.id.open_settings).setOnClickListener(view -> {
            preferences.edit().putBoolean("pending_enable", true).apply();
            permissionSheet.dismiss();
            openSettings(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
        });
        permissionSheet.show();
        permissionSheet.getWindow().setGravity(Gravity.BOTTOM);
        permissionSheet.getWindow().setLayout(Math.min(getResources().getDisplayMetrics().widthPixels,
                (int) (560 * getResources().getDisplayMetrics().density)), WindowManager.LayoutParams.WRAP_CONTENT);
    }

    private boolean hasAccess() {
        AccessibilityManager manager = getSystemService(AccessibilityManager.class);
        ComponentName ours = new ComponentName(this, SkipService.class);
        for (AccessibilityServiceInfo service : manager.getEnabledAccessibilityServiceList(
                AccessibilityServiceInfo.FEEDBACK_ALL_MASK)) {
            if (ours.equals(ComponentName.unflattenFromString(service.getId()))) return true;
        }
        return false;
    }

    private void refresh() {
        boolean access = hasAccess();
        if (access && preferences.getBoolean("pending_enable", false)) {
            preferences.edit().putBoolean("enabled", true).putBoolean("pending_enable", false)
                    .putBoolean("setup_seen", true).apply();
        }
        if (access && permissionSheet != null) permissionSheet.dismiss();
        boolean enabled = access && preferences.getBoolean("enabled", false);
        ImageButton toggle = findViewById(R.id.auto_skip);
        toggle.setSelected(enabled);
        toggle.setContentDescription(getString(enabled ? R.string.turn_off : R.string.turn_on));
        ((TextView) findViewById(R.id.status)).setText(!access ? R.string.setup_needed : enabled ? R.string.ready : R.string.paused);
        ((TextView) findViewById(R.id.status_detail)).setText(!access ? R.string.setup_detail : enabled ? R.string.ready_detail : R.string.paused_detail);
    }

    private void openSettings(Intent intent) {
        try { startActivity(intent); }
        catch (ActivityNotFoundException exception) {
            Toast.makeText(this, R.string.settings_missing, Toast.LENGTH_LONG).show();
        }
    }
}
