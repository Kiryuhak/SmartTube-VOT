package com.liskovsoft.smartyoutubetv2.tv.ui.playback.actions;

import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import androidx.leanback.widget.PlaybackControlsRow.MultiAction;

import com.liskovsoft.smartyoutubetv2.tv.R;

/**
 * OFF = idle, PENDING = waiting for Yandex, ON = translation playing.
 */
public class VoiceTranslateAction extends MultiAction {
    public static final int INDEX_OFF = 0;
    public static final int INDEX_PENDING = 1;
    public static final int INDEX_ON = 2;
    public static final int INDEX_ERROR = 3;

    private final Context mContext;
    private final String mBaseLabel;
    private final String[] mLabels;

    public VoiceTranslateAction(Context context) {
        super(R.id.action_voice_translate);
        mContext = context;
        mBaseLabel = context.getString(com.liskovsoft.smartyoutubetv2.common.R.string.action_voice_translate);

        int highlightColor = ActionHelpers.getIconHighlightColor(context);
        int orangeAccent = 0xFFFFA726; // Material Orange / Amber accent for WAITING state
        int errorColor = 0xFFFF5252;   // Red accent for ERROR state

        BitmapDrawable offDrawable = ActionHelpers.getBitmapDrawable(context, R.drawable.action_voice_translate);
        BitmapDrawable rawPending = ActionHelpers.getBitmapDrawable(context, R.drawable.action_voice_translate_pending);
        BitmapDrawable pendingDrawable = rawPending == null ? null
                : ActionHelpers.createDrawable(context, rawPending, orangeAccent);
        BitmapDrawable onDrawable = offDrawable == null ? null
                : new BitmapDrawable(context.getResources(),
                ActionHelpers.createBitmap(offDrawable.getBitmap(), highlightColor));
        BitmapDrawable errorDrawable = offDrawable == null ? null
                : ActionHelpers.createDrawable(context, offDrawable, errorColor);

        Drawable[] drawables = new Drawable[4];
        drawables[INDEX_OFF] = offDrawable;
        drawables[INDEX_PENDING] = pendingDrawable;
        drawables[INDEX_ON] = onDrawable;
        drawables[INDEX_ERROR] = errorDrawable;
        setDrawables(drawables);

        mLabels = new String[4];
        mLabels[INDEX_OFF] = mBaseLabel;
        mLabels[INDEX_PENDING] = mBaseLabel;
        mLabels[INDEX_ON] = mBaseLabel;
        mLabels[INDEX_ERROR] = mBaseLabel;
        setLabels(mLabels);
        setIndex(INDEX_OFF);
    }

    public void updatePendingLabel(int remainingTimeSec) {
        if (remainingTimeSec > 0) {
            int min = Math.max(1, (remainingTimeSec + 59) / 60);
            mLabels[INDEX_PENDING] = mContext.getString(
                    com.liskovsoft.smartyoutubetv2.common.R.string.vot_pending_eta_short, min);
        } else {
            mLabels[INDEX_PENDING] = mContext.getString(
                    com.liskovsoft.smartyoutubetv2.common.R.string.vot_pending_long);
        }
        setLabels(mLabels);
    }

    public void resetLabels() {
        mLabels[INDEX_OFF] = mBaseLabel;
        mLabels[INDEX_PENDING] = mBaseLabel;
        mLabels[INDEX_ON] = mBaseLabel;
        mLabels[INDEX_ERROR] = mBaseLabel;
        setLabels(mLabels);
    }
}
