package com.liskovsoft.smartyoutubetv2.common.vot;

public class VotSession {
    public String uuid;
    public String secretKey;
    public int expiresSec;
    public long createdAtMs;

    public boolean isValid() {
        return secretKey != null && !secretKey.isEmpty() &&
                (System.currentTimeMillis() - createdAtMs < expiresSec * 1000L);
    }

    public long getAgeSec() {
        return (System.currentTimeMillis() - createdAtMs) / 1000L;
    }

    public long getRemainingSec() {
        return Math.max(0, expiresSec - getAgeSec());
    }
}
