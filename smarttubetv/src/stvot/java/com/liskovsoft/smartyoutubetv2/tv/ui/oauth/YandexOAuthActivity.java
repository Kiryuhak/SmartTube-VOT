package com.liskovsoft.smartyoutubetv2.tv.ui.oauth;

import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.ComponentActivity;
import com.liskovsoft.smartyoutubetv2.common.prefs.VotData;
import com.yandex.authsdk.YandexAuthLoginOptions;
import com.yandex.authsdk.YandexAuthOptions;
import com.yandex.authsdk.YandexAuthResult;
import com.yandex.authsdk.YandexAuthSdk;
import android.util.Log;

public class YandexOAuthActivity extends ComponentActivity {
    private ActivityResultLauncher<YandexAuthLoginOptions> mLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        YandexAuthSdk sdk = YandexAuthSdk.create(new YandexAuthOptions(this, true));

        mLauncher = registerForActivityResult(
                sdk.getContract(),
                this::handleResult
        );

        if (savedInstanceState == null) {
            mLauncher.launch(new YandexAuthLoginOptions());
        }
    }

    private void handleResult(YandexAuthResult result) {
        if (result instanceof YandexAuthResult.Success) {
            String token = ((YandexAuthResult.Success) result)
                    .getToken()
                    .getValue();

            boolean tokenPresent = token != null && !token.isEmpty();
            int tokenLength = token != null ? token.length() : 0;
            Log.d("SmartTubeVOT-OAuth", "Yandex OAuth SUCCESS (tokenPresent=" + tokenPresent + ", length=" + tokenLength + ")");
            VotData.instance(this).setOAuthToken(token);
        } else if (result instanceof YandexAuthResult.Failure) {
            Exception exception =
                    ((YandexAuthResult.Failure) result).getException();

            Log.e("SmartTubeVOT-OAuth", "Yandex OAuth FAILURE", exception);
        } else {
            Log.w("SmartTubeVOT-OAuth", "Yandex OAuth CANCELLED");
        }

        finish();
    }
}