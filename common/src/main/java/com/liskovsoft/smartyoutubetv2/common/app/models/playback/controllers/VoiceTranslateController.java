package com.liskovsoft.smartyoutubetv2.common.app.models.playback.controllers;

import com.liskovsoft.sharedutils.helpers.MessageHelpers;
import com.liskovsoft.sharedutils.mylogger.Log;
import com.liskovsoft.smartyoutubetv2.common.R;
import com.liskovsoft.mediaserviceinterfaces.data.MediaItemMetadata;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.Video;
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.BasePlayerController;
import com.liskovsoft.smartyoutubetv2.common.exoplayer.selector.FormatItem;
import com.liskovsoft.smartyoutubetv2.common.prefs.VotData;
import com.liskovsoft.smartyoutubetv2.common.utils.AppDialogUtil;
import com.liskovsoft.smartyoutubetv2.common.utils.Utils;
import com.liskovsoft.smartyoutubetv2.common.vot.TranslationAudioPlayer;
import com.liskovsoft.smartyoutubetv2.common.vot.VotAudioTrackHelper;
import com.liskovsoft.smartyoutubetv2.common.vot.VotAudioTrackHelper.TrackInfo;
import com.liskovsoft.smartyoutubetv2.common.vot.VotClient;
import com.liskovsoft.smartyoutubetv2.common.vot.VotProgress;
import com.liskovsoft.smartyoutubetv2.common.vot.VotProgressOverlay;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;

/**
 * Yandex voice-over translation (EN→RU) alongside the main player.
 */
public class VoiceTranslateController extends BasePlayerController {
    private static final String TAG = "SmartTubeVOT";
    private static final int ACTION_VOICE_TRANSLATE = R.id.action_voice_translate;

    public static final int BTN_OFF = 0;
    public static final int BTN_PENDING = 1;
    public static final int BTN_ON = 2;

    private static final int STATE_OFF = 0;
    private static final int STATE_PENDING = 1;
    private static final int STATE_ACTIVE = 2;

    private static final long SYNC_INTERVAL_MS = 1000;
    private static final long SYNC_THRESHOLD_MS = 800;
    private static final long AUTO_TRANSLATE_RETRY_MS = 1000;
    private static final int AUTO_TRANSLATE_MAX_RETRIES = 20;
    private static final long MAX_TOTAL_WAIT_MS = 25 * 60 * 1000L;
    private static final long PENDING_HEARTBEAT_TIMEOUT_MS = 90 * 1000L;
    private static final long PROGRESS_TICK_INTERVAL_MS = 1000L;

    private VotData mVotData;
    private VotClient mVotClient;
    private TranslationAudioPlayer mTranslationPlayer;
    private Disposable mTranslationDisposable;
    private VotProgressOverlay mProgressOverlay;
    private float mSavedMainVolume = 1f;
    private boolean mIsAudioDucked;
    private FormatItem mSavedAudioFormat;
    private boolean mUserArmed;
    private boolean mArmed;
    private int mState = STATE_OFF;
    private int mPendingEtaSec;
    private boolean mPendingToastShown;
    private String mPendingVideoUrl;
    private String mCurrentVideoId;
    private int mAutoTranslateRetryCount;

    private long mRequestStartTimestamp;
    private long mExpectedReadyTimestamp;
    private long mLastBackendPendingTimestamp;

    private final Runnable mSyncRunnable = new Runnable() {
        @Override
        public void run() {
            if (mState == STATE_ACTIVE) {
                syncTranslationPositionIfNeeded();
                Utils.postDelayed(mSyncRunnable, SYNC_INTERVAL_MS);
            }
        }
    };

    private final Runnable mAutoTranslateRetryRunnable = new Runnable() {
        @Override
        public void run() {
            tryApplyAutoTranslate(false);
        }
    };

    private final Runnable mProgressTickRunnable = new Runnable() {
        @Override
        public void run() {
            if (mState != STATE_PENDING) {
                return;
            }
            long now = System.currentTimeMillis();
            long elapsedSec = Math.max(0, (now - mRequestStartTimestamp) / 1000);

            if (mRequestStartTimestamp > 0 && (now - mRequestStartTimestamp > MAX_TOTAL_WAIT_MS)) {
                Log.w(TAG, "VOT timeout: exceeded absolute maximum wait (%d ms) for video=%s", MAX_TOTAL_WAIT_MS, mCurrentVideoId);
                onTranslationTimeout();
                return;
            }

            if (mLastBackendPendingTimestamp > 0 && (now - mLastBackendPendingTimestamp > PENDING_HEARTBEAT_TIMEOUT_MS)) {
                Log.w(TAG, "VOT timeout: backend unresponsive for %d ms (video=%s)", now - mLastBackendPendingTimestamp, mCurrentVideoId);
                onTranslationTimeout();
                return;
            }

            long remainingSec = 0;
            if (mExpectedReadyTimestamp > now) {
                remainingSec = (mExpectedReadyTimestamp - now + 999) / 1000;
            }

            if (mUserArmed && progressOverlay() != null) {
                if (remainingSec > 0) {
                    progressOverlay().showWaitingWithEta(getActivity(), formatMmSs(remainingSec));
                } else {
                    Log.d(TAG, "VOT ETA expired, polling continues: elapsed=%ds, video=%s", elapsedSec, mCurrentVideoId);
                    progressOverlay().showStillWaiting(getActivity(), formatMmSs(elapsedSec));
                }
            }

            if (getPlayer() != null) {
                getPlayer().updateVoiceTranslatePendingEta((int) remainingSec);
            }

            Utils.postDelayed(mProgressTickRunnable, PROGRESS_TICK_INTERVAL_MS);
        }
    };

    private VotProgressOverlay progressOverlay() {
        if (mProgressOverlay == null && getContext() != null) {
            mProgressOverlay = new VotProgressOverlay(getContext());
        }
        return mProgressOverlay;
    }

    private static String formatMmSs(long totalSec) {
        long minutes = totalSec / 60;
        long seconds = totalSec % 60;
        return String.format(Locale.US, "%02d:%02d", minutes, seconds);
    }

    public VoiceTranslateController() {
    }

    private VotData votData() {
        if (mVotData == null) {
            mVotData = VotData.instance(getContext());
        }
        return mVotData;
    }

    private VotClient votClient() {
        if (mVotClient == null) {
            mVotClient = new VotClient(getContext());
        }
        return mVotClient;
    }

    @Override
    public void onNewVideo(Video item) {
        Log.d(TAG, "VOT reset reason: new video (%s)", item != null ? item.videoId : "null");
        Utils.removeCallbacks(mAutoTranslateRetryRunnable);
        Utils.removeCallbacks(mProgressTickRunnable);
        mAutoTranslateRetryCount = 0;
        cancelTranslationJob();
        releaseTranslationPlayer();
        restoreMainVolume();
        restoreSavedAudioFormat();
        if (mProgressOverlay != null) {
            mProgressOverlay.dismissImmediately();
        }
        mPendingToastShown = false;
        mPendingVideoUrl = null;
        mCurrentVideoId = item != null ? item.videoId : null;

        if (mUserArmed) {
            mArmed = true;
            setState(STATE_PENDING);
        } else {
            mArmed = false;
            setState(STATE_OFF);
        }
    }

    @Override
    public void onVideoLoaded(Video item) {
        tryApplyAutoTranslate(false);
    }

    @Override
    public void onMetadata(MediaItemMetadata metadata) {
        tryApplyAutoTranslate(false);
    }

    @Override
    public void onTrackChanged(FormatItem track) {
        if (track != null && track.getType() == FormatItem.TYPE_AUDIO) {
            tryApplyAutoTranslate(true);
        }
    }

    @Override
    public void onPlay() {
        if (mState == STATE_ACTIVE && mTranslationPlayer != null) {
            mTranslationPlayer.resume();
            syncTranslationPositionIfNeeded();
        }
    }

    @Override
    public void onPause() {
        if (mState == STATE_ACTIVE && mTranslationPlayer != null) {
            mTranslationPlayer.pause();
        }
    }

    @Override
    public void onSeekEnd() {
        if (mState == STATE_ACTIVE && mTranslationPlayer != null && mTranslationPlayer.isReady()) {
            mTranslationPlayer.seekTo(getPlayer().getPositionMs());
        }
    }

    @Override
    public void onSpeedChanged(float speed) {
        if (mState == STATE_ACTIVE && mTranslationPlayer != null) {
            mTranslationPlayer.setPlaybackSpeed(speed);
        }
    }

    @Override
    public void onEngineReleased() {
        Log.d(TAG, "VOT reset reason: engine released");
        if (mProgressOverlay != null) {
            mProgressOverlay.destroy();
            mProgressOverlay = null;
        }
        disarm();
    }

    @Override
    public void onViewDestroyed() {
        Log.d(TAG, "VOT reset reason: view destroyed");
        if (mProgressOverlay != null) {
            mProgressOverlay.destroy();
            mProgressOverlay = null;
        }
    }

    @Override
    public void onButtonClicked(int buttonId, int buttonState) {
        if (buttonId != ACTION_VOICE_TRANSLATE) {
            return;
        }
        if (buttonState == BTN_OFF) {
            armAndStart();
        } else {
            Log.d(TAG, "Trigger: manual stop");
            disarm();
            MessageHelpers.showMessage(getContext(), R.string.vot_disabled);
        }
    }

    @Override
    public void onButtonLongClicked(int buttonId, int buttonState) {
         if (buttonId == ACTION_VOICE_TRANSLATE) {
            AppDialogUtil.showVotMixDialog(getContext(), this::applyCurrentMix);
        }
    }

    private void armAndStart() {
        Log.d(TAG, "Trigger: manual start (Yandex authorized=%b)", votData().hasOAuthToken());
        if (votData().isPreferYoutubeAutoDub()) {
            MessageHelpers.showMessage(getContext(), R.string.vot_disable_google_for_yandex);
            return;
        }
        TrackInfo info = resolveAudioInfo();
        if (VotAudioTrackHelper.isRussianOriginal(info)
                || (info != null && VotAudioTrackHelper.isRussianLang(info.langCode))) {
            MessageHelpers.showMessage(getContext(), R.string.vot_skip_russian);
            return;
        }
        mUserArmed = true;
        mArmed = true;
        if (progressOverlay() != null) {
            progressOverlay().showPreparing(getActivity());
        }
        startYandexTranslation();
    }

    private void tryApplyAutoTranslate(boolean fromTrackChange) {
        if (getPlayer() == null || getPlayer().getVideo() == null) {
            return;
        }
        String videoId = getPlayer().getVideo().videoId;
        if (videoId != null && !videoId.equals(mCurrentVideoId)) {
            mCurrentVideoId = videoId;
            mAutoTranslateRetryCount = 0;
        }

        boolean autoEnabled = votData().isAutoTranslateEnabled();
        if (!autoEnabled && !mUserArmed) {
            return;
        }

        if (mState == STATE_ACTIVE) {
            return;
        }

        if (votData().isPreferYoutubeAutoDub()) {
            if (tryApplyYoutubeAutoDub(autoEnabled)) {
                return;
            }
        }

        TrackInfo info = resolveAudioInfo();
        String langCode = info != null ? info.langCode : null;

        if (!VotAudioTrackHelper.isKnownLanguage(langCode)) {
            if ((autoEnabled || mUserArmed) && mAutoTranslateRetryCount < AUTO_TRANSLATE_MAX_RETRIES) {
                mAutoTranslateRetryCount++;
                Utils.removeCallbacks(mAutoTranslateRetryRunnable);
                Utils.postDelayed(mAutoTranslateRetryRunnable, AUTO_TRANSLATE_RETRY_MS);
                return;
            }
            Utils.removeCallbacks(mAutoTranslateRetryRunnable);
            if (autoEnabled) {
                Log.d(TAG, "VOT auto: skip, audio language unknown (video=%s)", videoId);
            }
            return;
        }

        Utils.removeCallbacks(mAutoTranslateRetryRunnable);
        mAutoTranslateRetryCount = 0;

        if (VotAudioTrackHelper.isRussianLang(langCode)) {
            if (mUserArmed) {
                disarmWithMessage(R.string.vot_skip_russian);
                mUserArmed = false;
            } else if (autoEnabled) {
                Log.d(TAG, "VOT auto: skip, current audio language=%s (video=%s)", langCode, videoId);
                if (mArmed || mState != STATE_OFF) {
                    disarmQuiet();
                }
            }
            return;
        }

        if (VotAudioTrackHelper.isExplicitNonRussian(info)) {
            if (autoEnabled) {
                Log.d(TAG, "VOT auto: start, current audio language=%s (video=%s)", langCode, videoId);
            }
            if (!mArmed || (mState == STATE_OFF && mTranslationDisposable == null)) {
                mArmed = true;
                startYandexTranslation();
            }
            return;
        }

        if (autoEnabled) {
            Log.d(TAG, "VOT auto: skip, audio language=%s (video=%s)", langCode, videoId);
        }
    }

    /** @return true if YouTube dub was applied and Yandex should not run */
    private boolean tryApplyYoutubeAutoDub(boolean showToast) {
        FormatItem dub = VotAudioTrackHelper.findYoutubeRussianAutoDub(getAudioFormats());
        if (dub == null) {
            return false;
        }
        saveCurrentAudioFormatBeforeSwitch(dub);
        getPlayer().setFormat(dub);
        mArmed = false;
        mUserArmed = false;
        cancelTranslationJob();
        releaseTranslationPlayer();
        restoreMainVolume();
        setState(STATE_OFF);
        if (showToast) {
            MessageHelpers.showMessage(getContext(), R.string.vot_using_youtube_dub);
        }
        return true;
    }

    private void startYandexTranslation() {
        if (getPlayer() == null || getPlayer().getVideo() == null) {
            MessageHelpers.showMessage(getContext(), R.string.vot_error_no_video);
            return;
        }

        ensureOriginalAudioForYandex();

        TrackInfo info = resolveAudioInfo();
        if (VotAudioTrackHelper.isRussianOriginal(info)) {
            disarmWithMessage(R.string.vot_skip_russian);
            return;
        }

        String videoUrl = getPlayer().getVideo().videoId != null
                ? "https://www.youtube.com/watch?v=" + getPlayer().getVideo().videoId
                : null;
        if (videoUrl == null) {
            MessageHelpers.showMessage(getContext(), R.string.vot_error_no_video);
            return;
        }

        if (mTranslationDisposable != null && !mTranslationDisposable.isDisposed()
                && videoUrl.equals(mPendingVideoUrl)) {
            return;
        }

        cancelTranslationJob();
        mPendingToastShown = false;
        mPendingVideoUrl = videoUrl;
        mRequestStartTimestamp = System.currentTimeMillis();
        mLastBackendPendingTimestamp = System.currentTimeMillis();
        mExpectedReadyTimestamp = 0;
        setState(STATE_PENDING);

        Utils.removeCallbacks(mProgressTickRunnable);
        Utils.postDelayed(mProgressTickRunnable, PROGRESS_TICK_INTERVAL_MS);

        long durationSec = Math.max(1, getPlayer().getDurationMs() / 1000);
        Log.d(TAG, "VOT request started: url=%s, duration=%ds, userArmed=%b", videoUrl, durationSec, mUserArmed);
        mTranslationDisposable = votClient().observeTranslation(videoUrl, durationSec)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        this::onVotProgress,
                        this::onVotError
                );
    }

    private void ensureOriginalAudioForYandex() {
        if (getPlayer() == null) {
            return;
        }
        TrackInfo current = resolveAudioInfo();
        if (VotAudioTrackHelper.isOriginalTrack(current) && !VotAudioTrackHelper.isYoutubeAutoDub(current)) {
            return;
        }
        FormatItem original = VotAudioTrackHelper.findBestOriginalForYandex(getAudioFormats());
        if (original == null) {
            return;
        }
        FormatItem active = getPlayer().getAudioFormat();
        if (!VotAudioTrackHelper.isSameFormat(active, original)) {
            saveCurrentAudioFormatBeforeSwitch(original);
            getPlayer().setFormat(original);
        }
    }

    private void saveCurrentAudioFormatBeforeSwitch(FormatItem target) {
        if (getPlayer() == null || target == null || mSavedAudioFormat != null) {
            return;
        }
        FormatItem current = getPlayer().getAudioFormat();
        if (current != null && !VotAudioTrackHelper.isSameFormat(current, target)) {
            mSavedAudioFormat = current;
        }
    }

    private void restoreSavedAudioFormat() {
        if (mSavedAudioFormat != null && getPlayer() != null) {
            getPlayer().setFormat(mSavedAudioFormat);
            mSavedAudioFormat = null;
        }
    }

    private TrackInfo resolveAudioInfo() {
        if (getPlayer() == null) {
            return VotAudioTrackHelper.from(null);
        }
        return VotAudioTrackHelper.resolveCurrent(getPlayer().getAudioFormat(), getAudioFormats());
    }

    private List<FormatItem> getAudioFormats() {
        return getPlayer() != null ? getPlayer().getAudioFormats() : null;
    }

    private void onVotProgress(VotProgress progress) {
        if (getPlayer() == null || getPlayer().getVideo() == null) {
            return;
        }
        String currentUrl = "https://www.youtube.com/watch?v=" + getPlayer().getVideo().videoId;
        if (mPendingVideoUrl != null && !mPendingVideoUrl.equals(currentUrl)) {
            return;
        }
        if (!mArmed) {
            return;
        }

        mLastBackendPendingTimestamp = System.currentTimeMillis();

        switch (progress.type) {
            case VotProgress.TYPE_WAITING:
                mPendingEtaSec = progress.remainingTimeSec;
                if (progress.remainingTimeSec > 0) {
                    mExpectedReadyTimestamp = System.currentTimeMillis() + (progress.remainingTimeSec * 1000L);
                    Log.d(TAG, "VOT ETA received: %ds, expected ready at +%ds", progress.remainingTimeSec, progress.remainingTimeSec);
                } else {
                    Log.d(TAG, "VOT pending (status=%d, remainingTime=%d)", progress.status, progress.remainingTimeSec);
                }
                setState(STATE_PENDING);
                mProgressTickRunnable.run();
                break;
            case VotProgress.TYPE_READY:
                Log.d(TAG, "VOT translation ready");
                Utils.removeCallbacks(mProgressTickRunnable);
                if (mUserArmed && progressOverlay() != null) {
                    progressOverlay().showReady(getActivity());
                }
                if (progress.audioUrl != null) {
                    playTranslation(progress.audioUrl);
                }
                break;
            case VotProgress.TYPE_FAILED:
                Log.e(TAG, "VOT translation failed: %s", progress.message);
                Utils.removeCallbacks(mProgressTickRunnable);
                if (mUserArmed && progressOverlay() != null) {
                    progressOverlay().showError(getActivity());
                }
                handleTranslationError(progress.message, false);
                break;
        }
    }

    private void onVotError(Throwable e) {
        Log.e(TAG, "VOT error callback: %s", e != null ? e.getMessage() : "unknown");
        Utils.removeCallbacks(mProgressTickRunnable);
        if (mUserArmed && progressOverlay() != null) {
            progressOverlay().showError(getActivity());
        }
        String msg = e != null ? e.getMessage() : null;
        boolean isNetwork = e instanceof IOException;
        handleTranslationError(msg, isNetwork);
    }

    private void playTranslation(String audioUrl) {
        if (getPlayer() == null) {
            return;
        }
        releaseTranslationPlayer();
        mTranslationPlayer = new TranslationAudioPlayer(getContext());
        mTranslationPlayer.setOnReadyListener(() -> {
            if (getPlayer() != null && mTranslationPlayer != null) {
                mTranslationPlayer.seekTo(getPlayer().getPositionMs());
            }
        });
        mTranslationPlayer.setOnErrorListener(e -> {
            Utils.post(() -> onTranslationPlaybackError(e));
        });
        float speed = getPlayer().getSpeed();
        mTranslationPlayer.play(
                audioUrl,
                getPlayer().getPositionMs(),
                votData().getTranslationVolumeMultiplier(),
                speed > 0 ? speed : 1f
        );
        duckMainAudio();
        setState(STATE_ACTIVE);
        Utils.removeCallbacks(mSyncRunnable);
        Utils.postDelayed(mSyncRunnable, SYNC_INTERVAL_MS);
    }

    private void syncTranslationPositionIfNeeded() {
        if (mTranslationPlayer == null || getPlayer() == null || !mTranslationPlayer.isReady()) {
            return;
        }
        long mainPos = getPlayer().getPositionMs();
        long transPos = mTranslationPlayer.getPositionMs();
        if (Math.abs(mainPos - transPos) > SYNC_THRESHOLD_MS) {
            mTranslationPlayer.seekTo(mainPos);
        }
    }

	private void applyCurrentMix() {
		if (getPlayer() != null && mState == STATE_ACTIVE) {
			getPlayer().setVolume(votData().getOriginalVolumeMultiplier());
		}
		
		if (mTranslationPlayer != null) {
			mTranslationPlayer.setVolume(votData().getTranslationVolumeMultiplier());
        }
    }

    private void duckMainAudio() {
        if (getPlayer() == null || mIsAudioDucked) {
            return;
        }
        mSavedMainVolume = getPlayer().getVolume();
        getPlayer().setVolume(votData().getOriginalVolumeMultiplier());
        mIsAudioDucked = true;
    }

    private void restoreMainVolume() {
        if (getPlayer() != null && mIsAudioDucked) {
            getPlayer().setVolume(mSavedMainVolume);
            mIsAudioDucked = false;
        }
    }

    private void cancelTranslationJob() {
        Utils.removeCallbacks(mProgressTickRunnable);
        if (mTranslationDisposable != null && !mTranslationDisposable.isDisposed()) {
            mTranslationDisposable.dispose();
        }
        mTranslationDisposable = null;
    }

    private void releaseTranslationPlayer() {
        Utils.removeCallbacks(mSyncRunnable);
        if (mTranslationPlayer != null) {
            mTranslationPlayer.release();
            mTranslationPlayer = null;
        }
    }

    private void disarm() {
        disarmQuiet();
    }

    private void disarmQuiet() {
        Log.d(TAG, "VOT reset reason: disarmQuiet");
        mUserArmed = false;
        mArmed = false;
        Utils.removeCallbacks(mAutoTranslateRetryRunnable);
        Utils.removeCallbacks(mProgressTickRunnable);
        cancelTranslationJob();
        releaseTranslationPlayer();
        restoreMainVolume();
        restoreSavedAudioFormat();
        if (mProgressOverlay != null) {
            mProgressOverlay.dismissImmediately();
        }
        setState(STATE_OFF);
        mPendingVideoUrl = null;
    }

    private void disarmWithMessage(int msgResId) {
        disarm();
        MessageHelpers.showMessage(getContext(), msgResId);
    }

    private void onTranslationPlaybackError(Exception e) {
        Log.e(TAG, "Translation playback error: %s", e != null ? e.getMessage() : "unknown");
        if (mState == STATE_OFF) {
            return;
        }
        boolean wasUserArmed = mUserArmed;
        disarmQuiet();
        if (wasUserArmed) {
            MessageHelpers.showMessage(getContext(), R.string.vot_error_playback);
        }
    }

    private void onTranslationTimeout() {
        Log.w(TAG, "VOT translation timeout (video=%s)", mCurrentVideoId);
        Utils.removeCallbacks(mProgressTickRunnable);
        if (mUserArmed && progressOverlay() != null) {
            progressOverlay().showTimeout(getActivity());
        }
        boolean wasUserArmed = mUserArmed;
        disarmQuiet();
        if (wasUserArmed) {
            MessageHelpers.showMessage(getContext(), R.string.vot_error_timeout);
        }
    }

    private void handleTranslationError(String message, boolean isNetworkError) {
        Log.e(TAG, "Translation error: %s (network=%b)", message, isNetworkError);
        Utils.removeCallbacks(mProgressTickRunnable);
        boolean wasUserArmed = mUserArmed;
        boolean isAuthRequired = message != null && message.contains("auth required");

        if (isAuthRequired) {
            Log.w(TAG, "Yandex session required/invalid, clearing token and disabling lively voice");
            votData().clearOAuthToken();
            votData().setLivelyVoiceEnabled(false);
            if (wasUserArmed) {
                MessageHelpers.showMessage(getContext(), R.string.vot_error_auth_required);
            }
        } else if (wasUserArmed) {
            if (isNetworkError) {
                MessageHelpers.showMessage(getContext(), R.string.vot_error_network);
            } else {
                MessageHelpers.showMessage(getContext(), R.string.vot_error_generic);
            }
        }
        disarmQuiet();
    }

    private void setState(int state) {
        if (mState != state) {
            Log.d(TAG, "State transition: " + stateToString(mState) + " -> " + stateToString(state));
        }
        mState = state;
        int btnIndex;
        switch (state) {
            case STATE_PENDING:
                btnIndex = BTN_PENDING;
                break;
            case STATE_ACTIVE:
                btnIndex = BTN_ON;
                break;
            default:
                btnIndex = BTN_OFF;
                break;
        }
        updateVoiceButton(btnIndex);
    }

    private static String stateToString(int state) {
        switch (state) {
            case STATE_OFF:
                return "OFF";
            case STATE_PENDING:
                return "PENDING";
            case STATE_ACTIVE:
                return "ACTIVE";
            default:
                return "UNKNOWN";
        }
    }

    private void updateVoiceButton(int index) {
        if (getPlayer() == null) {
            return;
        }
        getPlayer().setButtonState(ACTION_VOICE_TRANSLATE, index);
        if (index == BTN_PENDING) {
            getPlayer().updateVoiceTranslatePendingEta(mPendingEtaSec);
        }
    }
}
