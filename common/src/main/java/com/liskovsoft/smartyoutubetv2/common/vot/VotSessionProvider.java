package com.liskovsoft.smartyoutubetv2.common.vot;

import androidx.annotation.Nullable;
import java.io.IOException;
import java.util.Map;

public interface VotSessionProvider {
    /**
     * @return true if currently cached session is valid and not expired.
     */
    boolean hasValidSession();

    /**
     * Returns an active session, creating or renewing it if necessary.
     * Implementations must ensure thread-safety and prevent session stampede.
     */
    VotSession getOrCreateSession() throws IOException;

    /**
     * Forces renewal of the session.
     */
    VotSession refreshSession() throws IOException;

    /**
     * Invalidates any active session in cache.
     */
    void invalidateSession();

    /**
     * Builds request headers for translation requests.
     */
    Map<String, String> getTranslateHeaders(byte[] body, String path, boolean useLively);

    /**
     * Builds request headers for audio upload requests.
     */
    Map<String, String> getAudioUploadHeaders(byte[] body, String path);
}
