package io.github.adityamagar.autoskip;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.accessibility.AccessibilityNodeInfo;


/** Dependency-free emulator checks. Run on a fresh install with accessibility off. */
public final class SmokeInstrumentation extends Instrumentation {
    @Override public void onCreate(Bundle arguments) {
        super.onCreate(arguments);
        start();
    }

    @Override public void onStart() {
        Bundle result = new Bundle();
        Activity activity = null;
        int resultCode = Activity.RESULT_OK;
        try {
            checkNodes();
            getTargetContext().getSharedPreferences("settings", 0).edit().clear().commit();
            Intent intent = new Intent(getTargetContext(), MainActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            activity = startActivitySync(intent);
            Activity screen = activity;
            runOnMainSync(() -> {
                require(screen.findViewById(R.id.auto_skip).isShown(), "Central control missing");
                require(!screen.findViewById(R.id.auto_skip).isSelected(), "Skipping must stay off without access");
                require(screen.findViewById(R.id.open_settings) == null, "Permission request must not be embedded in main screen");
            });
            result.putString("stream", "\nPASS: node safety, first launch, permission denial, persistent onboarding choice.\n");
        } catch (Throwable failure) {
            result.putString("stream", "\nFAIL: " + failure + "\n");
            resultCode = Activity.RESULT_CANCELED;
        } finally {
            if (activity != null) {
                Activity screen = activity;
                runOnMainSync(screen::finish);
            }
        }
        finish(resultCode, result);
    }

    private void checkNodes() {
        AccessibilityNodeInfo node = AccessibilityNodeInfo.obtain();
        try {
            node.setPackageName(SkipRule.YOUTUBE);
            node.setViewIdResourceName(SkipRule.YOUTUBE + ":id/skip_ad_button");
            node.setEnabled(true);
            node.setVisibleToUser(true);
            node.setClickable(true);
            AccessibilityNodeInfo target = SkipService.findSkipTarget(node);
            require(target != null, "Available Skip button was missed");
            target.recycle();
            node.setEnabled(false);
            require(SkipService.findSkipTarget(node) == null, "Disabled Skip button selected");
            node.setEnabled(true);
            node.setVisibleToUser(false);
            require(SkipService.findSkipTarget(node) == null, "Hidden Skip button selected");
            node.setVisibleToUser(true);
            node.setClickable(false);
            require(SkipService.findSkipTarget(node) == null, "Non-clickable control selected");
            node.setClickable(true);
            node.setPackageName("other.app");
            require(SkipService.findSkipTarget(node) == null, "Other app selected");
            node.setPackageName(SkipRule.YOUTUBE);
            node.setViewIdResourceName(SkipRule.YOUTUBE + ":id/video_title");
            node.setText("Skip ad");
            require(SkipService.findSkipTarget(node) == null, "Video title selected as Skip button");
        } finally { node.recycle(); }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
