package com.liskovsoft.smartyoutubetv2.common.prefs;

import android.annotation.SuppressLint;
import android.content.Context;
import android.text.TextUtils;

import com.liskovsoft.sharedutils.prefs.SharedPreferencesBase;
import com.liskovsoft.smartyoutubetv2.common.BuildConfig;
import com.liskovsoft.smartyoutubetv2.common.vot.VotAuthMode;

public class VotData extends SharedPreferencesBase {
    private static final String PREFS_NAME = "vot_data";
    private static final String OAUTH_TOKEN = "yandex_oauth_token";
    private static final String LIVELY_VOICE = "use_lively_voice";
    private static final String PREF_ONBOARDING_SHOWN = "yandex_onboarding_shown";
    private static final String ORIGINAL_VOLUME_PERCENT = "original_volume_percent";
    private static final String TRANSLATION_VOLUME_PERCENT = "translation_volume_percent";
    private static final String AUTO_TRANSLATE = "auto_translate_enabled";
    private static final String PREFER_YOUTUBE_AUTO_DUB = "prefer_youtube_auto_dub";
    private static final int DEFAULT_ORIGINAL_VOLUME_PERCENT = 5;
    private static final int DEFAULT_TRANSLATION_VOLUME_PERCENT = 100;

    @SuppressLint("StaticFieldLeak")
    private static VotData sInstance;

    private VotData(Context context) {
        super(context.getApplicationContext(), PREFS_NAME);
    }

    public static VotData instance(Context context) {
        if (sInstance == null) {
            sInstance = new VotData(context);
        }
        return sInstance;
    }

    public String getOAuthToken() {
        return getString(OAUTH_TOKEN, "");
    }

    public void setOAuthToken(String token) {
        String normalized = normalizeToken(token);
        putString(OAUTH_TOKEN, normalized);
        if (!TextUtils.isEmpty(normalized)) {
            setLivelyVoiceEnabled(true);
        } else {
            setLivelyVoiceEnabled(false);
        }
    }

    public void clearOAuthToken() {
        putString(OAUTH_TOKEN, "");
        setLivelyVoiceEnabled(false);
    }

    public void logoutYandex() {
        clearOAuthToken();
    }

    public boolean hasOAuthToken() {
        return !TextUtils.isEmpty(getOAuthToken());
    }

    private static final String PREF_AUTH_MODE = "vot_auth_mode";

    public VotAuthMode getAuthMode() {
        if (!BuildConfig.DEBUG) {
            return VotAuthMode.AUTO;
        }
        String modeName = getString(PREF_AUTH_MODE, VotAuthMode.AUTO.name());
        try {
            return VotAuthMode.valueOf(modeName);
        } catch (Exception e) {
            return VotAuthMode.AUTO;
        }
    }

    public void setAuthMode(VotAuthMode mode) {
        if (BuildConfig.DEBUG) {
            putString(PREF_AUTH_MODE, mode != null ? mode.name() : VotAuthMode.AUTO.name());
        }
    }

    public boolean isLivelyVoiceEnabled() {
        if (BuildConfig.DEBUG && getAuthMode() == VotAuthMode.ANONYMOUS) {
            return getBoolean(LIVELY_VOICE, true);
        }
        return hasOAuthToken() && getBoolean(LIVELY_VOICE, true);
    }

    public void setLivelyVoiceEnabled(boolean enabled) {
        putBoolean(LIVELY_VOICE, enabled);
    }

    public boolean isOnboardingShown() {
        return getBoolean(PREF_ONBOARDING_SHOWN, false);
    }

    public void setOnboardingShown(boolean shown) {
        putBoolean(PREF_ONBOARDING_SHOWN, shown);
    }

    /** YouTube/original track level while translation plays (0–100%). */
    public int getOriginalVolumePercent() {
        return clampPercent(getInt(ORIGINAL_VOLUME_PERCENT, DEFAULT_ORIGINAL_VOLUME_PERCENT));
    }

    public void setOriginalVolumePercent(int percent) {
        putInt(ORIGINAL_VOLUME_PERCENT, clampPercent(percent));
    }

    /** Russian voice-over level (0–100%). */
    public int getTranslationVolumePercent() {
        return clampPercent(getInt(TRANSLATION_VOLUME_PERCENT, DEFAULT_TRANSLATION_VOLUME_PERCENT));
    }

    public void setTranslationVolumePercent(int percent) {
        putInt(TRANSLATION_VOLUME_PERCENT, clampPercent(percent));
    }

    public boolean isAutoTranslateEnabled() {
        return getBoolean(AUTO_TRANSLATE, false);
    }

    public void setAutoTranslateEnabled(boolean enabled) {
        putBoolean(AUTO_TRANSLATE, enabled);
    }

    public boolean isPreferYoutubeAutoDub() {
        return getBoolean(PREFER_YOUTUBE_AUTO_DUB, false);
    }

    public void setPreferYoutubeAutoDub(boolean enabled) {
        putBoolean(PREFER_YOUTUBE_AUTO_DUB, enabled);
    }

    public float getOriginalVolumeMultiplier() {
        return getOriginalVolumePercent() / 100f;
    }

    public float getTranslationVolumeMultiplier() {
        return getTranslationVolumePercent() / 100f;
    }

    public String getTokenPreview() {
        String token = getOAuthToken();
        if (TextUtils.isEmpty(token)) {
            return "";
        }
        if (token.length() <= 12) {
            return token.substring(0, 4) + "…";
        }
        return token.substring(0, 6) + "…" + token.substring(token.length() - 4);
    }

    private static String normalizeToken(String token) {
        if (token == null) {
            return "";
        }
        return token.trim().replaceAll("\\s+", "");
    }

    private static int clampPercent(int percent) {
        return Math.max(0, Math.min(100, percent));
    }
}
