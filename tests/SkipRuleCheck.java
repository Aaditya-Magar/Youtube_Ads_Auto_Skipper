package io.github.adityamagar.autoskip;

public final class SkipRuleCheck {
    public static void main(String[] args) {
        String youtube = "com.google.android.youtube";
        assert SkipRule.matches(youtube, youtube + ":id/skip_ad_button");
        assert SkipRule.matches(youtube, youtube + ":id/skip_ad_button_text");
        assert SkipRule.matches(youtube, youtube + ":id/skip_ad_button_container");
        assert SkipRule.matches(youtube, youtube + ":id/modern_skip_ad_button");
        assert SkipRule.matches(youtube, youtube + ":id/modern_skip_ad_button_container");
        assert SkipRule.matches(youtube, youtube + ":id/modern_miniplayer_skip_ad_button");
        assert !SkipRule.matches("other.app", youtube + ":id/skip_ad_button");
        assert !SkipRule.matches(youtube, "other.app:id/skip_ad_button");
        assert !SkipRule.matches(youtube, youtube + ":id/skip_ad_button_countdown");
        assert !SkipRule.matches(youtube, "Skip ad");
        assert !SkipRule.matches(youtube, null);
        assert !SkipRule.matches(null, youtube + ":id/skip_ad_button");
        System.out.println("Skip rule checks passed (12 assertions).");
    }
}
