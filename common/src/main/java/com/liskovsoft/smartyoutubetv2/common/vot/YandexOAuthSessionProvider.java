package com.liskovsoft.smartyoutubetv2.common.vot;

import com.liskovsoft.smartyoutubetv2.common.prefs.VotData;

import java.io.IOException;
import java.util.Map;

public class YandexOAuthSessionProvider implements VotSessionProvider {
    private final VotData mVotData;
    private final AnonymousVotSessionProvider mSessionDelegate;

    public YandexOAuthSessionProvider(VotData votData, AnonymousVotSessionProvider sessionDelegate) {
        mVotData = votData;
        mSessionDelegate = sessionDelegate;
    }

    @Override
    public boolean hasValidSession() {
        return mSessionDelegate.hasValidSession();
    }

    @Override
    public VotSession getOrCreateSession() throws IOException {
        return mSessionDelegate.getOrCreateSession();
    }

    @Override
    public VotSession refreshSession() throws IOException {
        return mSessionDelegate.refreshSession();
    }

    @Override
    public void invalidateSession() {
        mSessionDelegate.invalidateSession();
    }

    @Override
    public Map<String, String> getTranslateHeaders(byte[] body, String path, boolean useLively) {
        Map<String, String> headers = mSessionDelegate.getTranslateHeaders(body, path, useLively);
        String token = mVotData.getOAuthToken();
        if (useLively && token != null && !token.isEmpty()) {
            headers = VotHeaders.merge(headers, VotHeaders.oauthHeader(token));
        }
        return headers;
    }

    @Override
    public Map<String, String> getAudioUploadHeaders(byte[] body, String path) {
        return mSessionDelegate.getAudioUploadHeaders(body, path);
    }
}
