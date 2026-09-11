package com.liskovsoft.smartyoutubetv2.common.vot;

import android.app.Activity;
import android.content.Context;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.liskovsoft.sharedutils.mylogger.Log;
import com.liskovsoft.smartyoutubetv2.common.R;
import com.liskovsoft.smartyoutubetv2.common.utils.Utils;

public class VotProgressOverlay {
    private static final String TAG = "VotProgressOverlay";

    public static final int STATE_IDLE = 0;
    public static final int STATE_PREPARING = 1;
    public static final int STATE_WAITING_WITH_ETA = 2;
    public static final int STATE_ETA_EXPIRED_STILL_WAITING = 3;
    public static final int STATE_READY = 4;
    public static final int STATE_ERROR = 5;
    public static final int STATE_TIMEOUT = 6;

    private static final long DISMISS_DELAY_READY_MS = 2500L;
    private static final long DISMISS_DELAY_ERROR_MS = 3500L;

    private final Context mContext;
    private View mOverlayView;
    private ProgressBar mSpinner;
    private ImageView mIcon;
    private TextView mText;
    private int mCurrentState = STATE_IDLE;
    private ViewGroup mParentView;

    private final Runnable mAutoDismissRunnable = this::hide;

    public VotProgressOverlay(Context context) {
        mContext = context;
    }

    public int getState() {
        return mCurrentState;
    }

    public boolean isShown() {
        return mOverlayView != null && mOverlayView.getVisibility() == View.VISIBLE && mCurrentState != STATE_IDLE;
    }

    public void showPreparing(@Nullable Activity activity) {
        if (!ensureAttached(activity)) return;
        Utils.removeCallbacks(mAutoDismissRunnable);
        mCurrentState = STATE_PREPARING;
        showSpinnerMode();
        setText(mContext.getString(R.string.vot_progress_preparing));
        fadeIn();
    }

    public void showWaitingWithEta(@Nullable Activity activity, String timeRemainingFormatted) {
        if (!ensureAttached(activity)) return;
        Utils.removeCallbacks(mAutoDismissRunnable);
        mCurrentState = STATE_WAITING_WITH_ETA;
        showSpinnerMode();
        String formatted = mContext.getString(R.string.vot_progress_waiting_eta, timeRemainingFormatted);
        setText(formatted);
        fadeIn();
    }

    public void showStillWaiting(@Nullable Activity activity, String timeElapsedFormatted) {
        if (!ensureAttached(activity)) return;
        Utils.removeCallbacks(mAutoDismissRunnable);
        mCurrentState = STATE_ETA_EXPIRED_STILL_WAITING;
        showSpinnerMode();
        String formatted = mContext.getString(R.string.vot_progress_still_waiting, timeElapsedFormatted);
        setText(formatted);
        fadeIn();
    }

    public void showReady(@Nullable Activity activity) {
        if (!ensureAttached(activity)) return;
        Utils.removeCallbacks(mAutoDismissRunnable);
        mCurrentState = STATE_READY;
        showIconMode(R.drawable.ic_vot_ready);
        setText(mContext.getString(R.string.vot_progress_ready));
        fadeIn();
        Utils.postDelayed(mAutoDismissRunnable, DISMISS_DELAY_READY_MS);
    }

    public void showError(@Nullable Activity activity) {
        if (!ensureAttached(activity)) return;
        Utils.removeCallbacks(mAutoDismissRunnable);
        mCurrentState = STATE_ERROR;
        showIconMode(R.drawable.ic_vot_error);
        setText(mContext.getString(R.string.vot_progress_error));
        fadeIn();
        Utils.postDelayed(mAutoDismissRunnable, DISMISS_DELAY_ERROR_MS);
    }

    public void showTimeout(@Nullable Activity activity) {
        if (!ensureAttached(activity)) return;
        Utils.removeCallbacks(mAutoDismissRunnable);
        mCurrentState = STATE_TIMEOUT;
        showIconMode(R.drawable.ic_vot_timeout);
        setText(mContext.getString(R.string.vot_progress_timeout));
        fadeIn();
        Utils.postDelayed(mAutoDismissRunnable, DISMISS_DELAY_ERROR_MS);
    }

    public void hide() {
        Utils.removeCallbacks(mAutoDismissRunnable);
        mCurrentState = STATE_IDLE;
        if (mOverlayView != null) {
            mOverlayView.animate()
                    .alpha(0f)
                    .setDuration(200)
                    .withEndAction(() -> {
                        if (mOverlayView != null) {
                            mOverlayView.setVisibility(View.GONE);
                        }
                    })
                    .start();
        }
    }

    public void dismissImmediately() {
        Utils.removeCallbacks(mAutoDismissRunnable);
        mCurrentState = STATE_IDLE;
        if (mOverlayView != null) {
            mOverlayView.animate().cancel();
            mOverlayView.setVisibility(View.GONE);
            mOverlayView.setAlpha(0f);
        }
    }

    public void destroy() {
        dismissImmediately();
        if (mOverlayView != null && mParentView != null) {
            mParentView.removeView(mOverlayView);
        }
        mOverlayView = null;
        mParentView = null;
        mSpinner = null;
        mIcon = null;
        mText = null;
    }

    private void showSpinnerMode() {
        if (mSpinner != null) mSpinner.setVisibility(View.VISIBLE);
        if (mIcon != null) mIcon.setVisibility(View.GONE);
    }

    private void showIconMode(int iconResId) {
        if (mSpinner != null) mSpinner.setVisibility(View.GONE);
        if (mIcon != null) {
            mIcon.setImageDrawable(ContextCompat.getDrawable(mContext, iconResId));
            mIcon.setVisibility(View.VISIBLE);
        }
    }

    private void setText(String text) {
        if (mText != null) {
            mText.setText(text);
        }
    }

    private void fadeIn() {
        if (mOverlayView != null) {
            if (mOverlayView.getVisibility() != View.VISIBLE) {
                mOverlayView.setAlpha(0f);
                mOverlayView.setVisibility(View.VISIBLE);
            }
            mOverlayView.animate().alpha(1f).setDuration(200).start();
        }
    }

    private boolean ensureAttached(@Nullable Activity activity) {
        if (mOverlayView != null && mOverlayView.getParent() != null) {
            return true;
        }
        if (activity == null || activity.isFinishing()) {
            return false;
        }

        ViewGroup targetContainer = findTargetContainer(activity);
        if (targetContainer == null) {
            Log.w(TAG, "Cannot attach overlay: no target container found");
            return false;
        }

        try {
            LayoutInflater inflater = LayoutInflater.from(mContext);
            mOverlayView = inflater.inflate(R.layout.vot_progress_overlay, targetContainer, false);
            mSpinner = mOverlayView.findViewById(R.id.vot_progress_spinner);
            mIcon = mOverlayView.findViewById(R.id.vot_progress_icon);
            mText = mOverlayView.findViewById(R.id.vot_progress_text);

            mOverlayView.setFocusable(false);
            mOverlayView.setFocusableInTouchMode(false);
            mOverlayView.setClickable(false);

            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            lp.gravity = Gravity.BOTTOM | Gravity.END;
            lp.rightMargin = dpToPx(36);
            lp.bottomMargin = dpToPx(80);

            mOverlayView.setLayoutParams(lp);
            mOverlayView.setVisibility(View.GONE);
            mOverlayView.setAlpha(0f);

            targetContainer.addView(mOverlayView);
            mParentView = targetContainer;
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Failed to inflate VOT progress overlay: %s", e.getMessage());
            return false;
        }
    }

    private ViewGroup findTargetContainer(Activity activity) {
        int rootId = activity.getResources().getIdentifier("playback_fragment_root", "id", activity.getPackageName());
        if (rootId != 0) {
            View root = activity.findViewById(rootId);
            if (root instanceof ViewGroup) {
                return (ViewGroup) root;
            }
        }
        View content = activity.findViewById(android.R.id.content);
        if (content instanceof ViewGroup) {
            return (ViewGroup) content;
        }
        View decor = activity.getWindow() != null ? activity.getWindow().getDecorView() : null;
        if (decor instanceof ViewGroup) {
            return (ViewGroup) decor;
        }
        return null;
    }

    private int dpToPx(float dp) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp,
                mContext.getResources().getDisplayMetrics()
        );
    }
}
