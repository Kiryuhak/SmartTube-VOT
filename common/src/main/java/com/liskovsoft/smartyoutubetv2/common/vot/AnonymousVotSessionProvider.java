package com.liskovsoft.smartyoutubetv2.common.vot;

import androidx.annotation.Nullable;
import com.liskovsoft.sharedutils.mylogger.Log;

import java.io.IOException;
import java.util.Map;

public class AnonymousVotSessionProvider implements VotSessionProvider {
    private static final String TAG = "ANON_VOT";

    private final VotHttp mHttp;
    private final Object mLock = new Object();
    @Nullable
    private volatile VotSession mSession;

    public AnonymousVotSessionProvider(VotHttp http) {
        mHttp = http;
    }

    @Override
    public boolean hasValidSession() {
        VotSession s = mSession;
        return s != null && s.isValid();
    }

    @Override
    public VotSession getOrCreateSession() throws IOException {
        VotSession current = mSession;
        if (current != null && current.isValid()) {
            return current;
        }

        synchronized (mLock) {
            current = mSession;
            if (current != null && current.isValid()) {
                return current;
            }
            return createSessionInternal();
        }
    }

    @Override
    public VotSession refreshSession() throws IOException {
        synchronized (mLock) {
            mSession = null;
            return createSessionInternal();
        }
    }

    @Override
    public void invalidateSession() {
        synchronized (mLock) {
            mSession = null;
        }
    }

    @Override
    public Map<String, String> getTranslateHeaders(byte[] body, String path, boolean useLively) {
        VotSession session = mSession;
        if (session != null && session.isValid()) {
            Log.d(TAG, "ANON_VOT session age: " + session.getAgeSec() + "s");
            return VotHeaders.sessionTranslate(session, body, path);
        }
        return VotHeaders.simpleTranslate(body);
    }

    @Override
    public Map<String, String> getAudioUploadHeaders(byte[] body, String path) {
        VotSession session = mSession;
        if (session != null && session.isValid()) {
            return VotHeaders.sessionTranslate(session, body, path);
        }
        return VotHeaders.simpleTranslate(body);
    }

    private VotSession createSessionInternal() throws IOException {
        Log.d(TAG, "ANON_VOT session_create start");
        String uuid = VotSignature.randomToken();
        byte[] body = VotProtobuf.encodeSessionRequest(uuid, "video-translation");
        byte[] raw = mHttp.postProtobuf("/session/create", body, VotHeaders.simpleTranslate(body));

        if (raw == null || raw.length == 0) {
            throw new IOException("ANON_VOT session_create failed: empty response");
        }

        VotSession decoded = VotProtobuf.decodeSessionResponse(raw);
        decoded.uuid = uuid;
        decoded.createdAtMs = System.currentTimeMillis();
        mSession = decoded;

        Log.d(TAG, "ANON_VOT session_create success (expires=" + decoded.expiresSec + "s)");
        return decoded;
    }
}
