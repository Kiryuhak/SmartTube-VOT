package com.liskovsoft.smartyoutubetv2.common.vot;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.Nullable;

import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;
import com.liskovsoft.sharedutils.mylogger.Log;

public class TranslationAudioPlayer implements Player.EventListener {
    private static final String TAG = TranslationAudioPlayer.class.getSimpleName();

    public interface PlaybackCallback {
        void onPrepared();
        void onSeekProcessed();
        void onError(Exception error);
    }

    private final Context mContext;
    private SimpleExoPlayer mPlayer;
    private int mSessionId;
    @Nullable
    private PlaybackCallback mCallback;
    private boolean mIsPrepared;
    private boolean mIsPlaying;

    public TranslationAudioPlayer(Context context) {
        mContext = context.getApplicationContext();
    }

    public void prepare(int sessionId, String url, float speed, PlaybackCallback callback) {
        release();
        mSessionId = sessionId;
        mCallback = callback;
        mIsPrepared = false;
        mIsPlaying = false;

        Log.d(TAG, "VOT_AUDIO session=%d create", mSessionId);
        String userAgent = Util.getUserAgent(mContext, "SmartTubeVOT");
        DefaultDataSourceFactory dataSourceFactory = new DefaultDataSourceFactory(mContext, userAgent);
        ExtractorMediaSource mediaSource = new ExtractorMediaSource.Factory(dataSourceFactory)
                .createMediaSource(Uri.parse(url));
        Log.d(TAG, "VOT_AUDIO session=%d source_set", mSessionId);

        mPlayer = ExoPlayerFactory.newSimpleInstance(mContext);
        mPlayer.addListener(this);

        // Silent pre-buffer barrier: volume is 0 until initial seek completes
        mPlayer.setVolume(0f);
        mPlayer.setPlayWhenReady(false);

        if (speed > 0f && speed != 1f) {
            mPlayer.setPlaybackParameters(new PlaybackParameters(speed, 1f));
        }

        Log.d(TAG, "VOT_AUDIO session=%d prepare_start", mSessionId);
        mPlayer.prepare(mediaSource);
    }

    public void startPlayback(float volume) {
        if (mPlayer == null) {
            return;
        }
        if (mIsPlaying) {
            Log.d(TAG, "VOT_AUDIO session=%d duplicate_play_ignored", mSessionId);
            return;
        }
        mIsPlaying = true;
        mPlayer.setVolume(volume);
        mPlayer.setPlayWhenReady(true);
        Log.d(TAG, "VOT_AUDIO session=%d play", mSessionId);
    }

    public void setVolume(float volume) {
        if (mPlayer != null && mIsPlaying) {
            mPlayer.setVolume(volume);
        }
    }

    public void setPlaybackSpeed(float speed) {
        if (mPlayer == null) {
            return;
        }
        float s = speed > 0f ? speed : 1f;
        mPlayer.setPlaybackParameters(new PlaybackParameters(s, 1f));
    }

    public boolean isReady() {
        return mPlayer != null && mPlayer.getPlaybackState() == Player.STATE_READY;
    }

    public boolean isPlaying() {
        return mIsPlaying && mPlayer != null && mPlayer.getPlayWhenReady();
    }

    public void seekTo(long positionMs) {
        if (mPlayer != null) {
            Log.d(TAG, "VOT_AUDIO session=%d target_position=%d", mSessionId, positionMs);
            Log.d(TAG, "VOT_AUDIO session=%d seek_start", mSessionId);
            mPlayer.seekTo(positionMs);
        }
    }

    public long getPositionMs() {
        return mPlayer != null ? mPlayer.getCurrentPosition() : 0;
    }

    public void pause() {
        if (mPlayer != null) {
            mPlayer.setPlayWhenReady(false);
        }
    }

    public void resume() {
        if (mPlayer != null && mIsPlaying) {
            mPlayer.setPlayWhenReady(true);
        }
    }

    public void release() {
        mCallback = null;
        if (mPlayer != null) {
            Log.d(TAG, "VOT_AUDIO session=%d release", mSessionId);
            mPlayer.removeListener(this);
            mPlayer.release();
            mPlayer = null;
        }
        mIsPrepared = false;
        mIsPlaying = false;
    }

    @Override
    public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
        if (playbackState == Player.STATE_READY && !mIsPrepared) {
            mIsPrepared = true;
            Log.d(TAG, "VOT_AUDIO session=%d prepared", mSessionId);
            if (mCallback != null) {
                mCallback.onPrepared();
            }
        }
    }

    @Override
    public void onSeekProcessed() {
        Log.d(TAG, "VOT_AUDIO session=%d seek_complete", mSessionId);
        if (mCallback != null) {
            mCallback.onSeekProcessed();
        }
    }

    @Override
    public void onPlayerError(ExoPlaybackException error) {
        Log.e(TAG, "Translation player error: %s", error != null ? error.getMessage() : "unknown");
        PlaybackCallback callback = mCallback;
        if (callback != null) {
            callback.onError(error);
        }
    }
}
