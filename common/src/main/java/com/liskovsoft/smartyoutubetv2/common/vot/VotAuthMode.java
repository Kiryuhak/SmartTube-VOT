package com.liskovsoft.smartyoutubetv2.common.vot;

public enum VotAuthMode {
    /**
     * Attempts anonymous session by default; if session/auth is required and OAuth token is present, falls back to Yandex ID.
     */
    AUTO,

    /**
     * Strictly forces anonymous browser session without using any user OAuth token.
     */
    ANONYMOUS,

    /**
     * Strictly requires Yandex ID OAuth authorization.
     */
    YANDEX_ID
}
