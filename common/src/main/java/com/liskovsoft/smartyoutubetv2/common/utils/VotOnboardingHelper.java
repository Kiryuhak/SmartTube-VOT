package com.liskovsoft.smartyoutubetv2.common.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.widget.Button;

import androidx.appcompat.app.AlertDialog;

import com.liskovsoft.mediaserviceinterfaces.SignInService;
import com.liskovsoft.sharedutils.helpers.MessageHelpers;
import com.liskovsoft.sharedutils.mylogger.Log;
import com.liskovsoft.smartyoutubetv2.common.R;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.AppDialogPresenter;
import com.liskovsoft.smartyoutubetv2.common.app.views.ViewManager;
import com.liskovsoft.smartyoutubetv2.common.prefs.VotData;
import com.liskovsoft.youtubeapi.service.YouTubeServiceManager;

public final class VotOnboardingHelper {
    private static final String TAG = "VotOnboardingHelper";
    private static boolean sIsDialogShowing;
    private static AlertDialog sDialogInstance;

    private VotOnboardingHelper() {
    }

    public static boolean isStvot(Context context) {
        if (context == null) {
            return false;
        }
        String pkg = context.getPackageName();
        if (pkg != null && pkg.contains("smarttubevot")) {
            return true;
        }
        try {
            String title = context.getString(R.string.vot_onboarding_title);
            return title != null && !title.isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    public static void checkOnboarding(Activity activity) {
        if (!Utils.checkActivity(activity) || !Utils.isAppInForegroundFixed()) {
            return;
        }

        if (!isStvot(activity)) {
            return;
        }

        SignInService signInService = YouTubeServiceManager.instance().getSignInService();
        if (signInService == null || !signInService.isSigned()) {
            return;
        }

        VotData votData = VotData.instance(activity);
        if (votData.hasOAuthToken() || votData.isOnboardingShown()) {
            return;
        }

        if (sIsDialogShowing) {
            if (sDialogInstance != null && sDialogInstance.isShowing()) {
                return;
            } else {
                sIsDialogShowing = false;
                sDialogInstance = null;
            }
        }

        if (ViewManager.instance(activity).isPlayerInForeground()) {
            return;
        }

        if (AppDialogPresenter.instance(activity).isDialogShown()) {
            return;
        }

        showOnboardingDialog(activity);
    }

    private static void showOnboardingDialog(Activity activity) {
        sIsDialogShowing = true;

        AlertDialog.Builder builder = new AlertDialog.Builder(activity, R.style.AppDialog);
        builder.setTitle(R.string.vot_onboarding_title)
                .setMessage(R.string.vot_onboarding_message)
                .setCancelable(true)
                .setPositiveButton(R.string.vot_onboarding_login, (dialog, which) -> {
                    VotData.instance(activity).setOnboardingShown(true);
                    startYandexOAuth(activity);
                })
                .setNegativeButton(R.string.vot_onboarding_later, (dialog, which) -> {
                    VotData.instance(activity).setOnboardingShown(true);
                    dialog.dismiss();
                })
                .setOnCancelListener(dialog -> {
                    VotData.instance(activity).setOnboardingShown(true);
                })
                .setOnDismissListener(dialog -> {
                    VotData.instance(activity).setOnboardingShown(true);
                    sIsDialogShowing = false;
                    sDialogInstance = null;
                });

        try {
            AlertDialog dialog = builder.create();
            sDialogInstance = dialog;

            dialog.setOnShowListener(d -> {
                Button positive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                if (positive != null) {
                    positive.requestFocus();
                }
            });

            dialog.show();
        } catch (Exception e) {
            Log.e(TAG, "Failed to show onboarding dialog: " + e.getMessage());
            sIsDialogShowing = false;
            sDialogInstance = null;
        }
    }

    public static void startYandexOAuth(Context context) {
        if (context == null) {
            return;
        }

        Intent intent = new Intent();
        intent.setClassName(
                context,
                "com.liskovsoft.smartyoutubetv2.tv.ui.oauth.YandexOAuthActivity"
        );

        try {
            context.startActivity(intent);
        } catch (Exception e) {
            MessageHelpers.showMessage(context, e.getMessage());
        }
    }

    public static void dismiss() {
        if (sDialogInstance != null) {
            try {
                sDialogInstance.dismiss();
            } catch (Exception ignored) {
            }
            sDialogInstance = null;
            sIsDialogShowing = false;
        }
    }
}
