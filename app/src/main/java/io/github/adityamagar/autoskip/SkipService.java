package io.github.adityamagar.autoskip;

import android.accessibilityservice.AccessibilityService;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import java.util.ArrayDeque;

public final class SkipService extends AccessibilityService {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private long lastClick;
    private SharedPreferences preferences;
    private final Runnable scan = this::scanWindow;
    private final SharedPreferences.OnSharedPreferenceChangeListener preferenceListener =
            (prefs, key) -> {
                handler.removeCallbacks(scan);
                if (prefs.getBoolean("enabled", false)) handler.postDelayed(scan, 150);
            };

    @Override protected void onServiceConnected() {
        preferences = getSharedPreferences("settings", MODE_PRIVATE);
        preferences.registerOnSharedPreferenceChangeListener(preferenceListener);
        handler.postDelayed(scan, 150);
    }

    @Override public void onAccessibilityEvent(AccessibilityEvent event) {
        if (preferences == null || !preferences.getBoolean("enabled", false)
                || !SkipRule.YOUTUBE.contentEquals(event.getPackageName() == null ? "" : event.getPackageName())) return;
        // Coalesce event bursts without continually postponing the scan.
        if (!handler.hasMessages(0)) {
            handler.postDelayed(scan, Math.max(150, 800 - (SystemClock.uptimeMillis() - lastClick)));
        }
    }

    private void scanWindow() {
        if (preferences == null || !preferences.getBoolean("enabled", false)) return;
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return;
        try {
            if (!SkipRule.YOUTUBE.contentEquals(root.getPackageName() == null ? "" : root.getPackageName())) return;
            AccessibilityNodeInfo target = findSkipTarget(root);
            if (target != null) {
                try {
                    if (target.refresh() && SkipRule.matches(target.getPackageName(), target.getViewIdResourceName())
                            && target.isVisibleToUser() && target.isEnabled() && target.isClickable()
                            && target.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                        lastClick = SystemClock.uptimeMillis();
                        handler.postDelayed(scan, 800);
                    }
                } finally { target.recycle(); }
            }
        } catch (IllegalStateException ignored) {
            // YouTube can replace a window while its accessibility nodes are being read.
        } finally { root.recycle(); }
    }

    static AccessibilityNodeInfo findSkipTarget(AccessibilityNodeInfo root) {
        ArrayDeque<AccessibilityNodeInfo> queue = new ArrayDeque<>();
        queue.add(AccessibilityNodeInfo.obtain(root));
        int visited = 0;
        try {
            // ponytail: bound work on large feeds; revisit the budget if real player trees exceed it.
            while (!queue.isEmpty() && visited++ < 400) {
                AccessibilityNodeInfo node = queue.removeFirst();
                try {
                    if (SkipRule.matches(node.getPackageName(), node.getViewIdResourceName())
                            && node.isVisibleToUser() && node.isEnabled()) {
                        if (node.isClickable()) return AccessibilityNodeInfo.obtain(node);
                    }
                    for (int i = 0; i < node.getChildCount() && queue.size() + visited < 400; i++) {
                        AccessibilityNodeInfo child = node.getChild(i);
                        if (child != null) queue.addLast(child);
                    }
                } finally { node.recycle(); }
            }
            return null;
        } finally {
            while (!queue.isEmpty()) queue.removeFirst().recycle();
        }
    }

    @Override public void onInterrupt() { handler.removeCallbacks(scan); }

    @Override public void onDestroy() {
        handler.removeCallbacks(scan);
        if (preferences != null) preferences.unregisterOnSharedPreferenceChangeListener(preferenceListener);
        super.onDestroy();
    }
}
