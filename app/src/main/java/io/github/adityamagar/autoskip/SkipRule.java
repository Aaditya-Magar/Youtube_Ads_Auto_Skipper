package io.github.adityamagar.autoskip;

final class SkipRule {
    static final String YOUTUBE = "com.google.android.youtube";

    static boolean matches(CharSequence packageName, String viewId) {
        if (!YOUTUBE.contentEquals(packageName == null ? "" : packageName)) return false;
        // ponytail: exact IDs miss new YouTube layouts; add IDs after device verification.
        return (YOUTUBE + ":id/skip_ad_button").equals(viewId)
                || (YOUTUBE + ":id/skip_ad_button_text").equals(viewId)
                || (YOUTUBE + ":id/skip_ad_button_container").equals(viewId)
                || (YOUTUBE + ":id/modern_skip_ad_button").equals(viewId)
                || (YOUTUBE + ":id/modern_skip_ad_button_container").equals(viewId)
                || (YOUTUBE + ":id/modern_miniplayer_skip_ad_button").equals(viewId);
    }
}
