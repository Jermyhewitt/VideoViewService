package snaplic.com.videoviewservice;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.media.Image;
import android.media.MediaPlayer;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.accessibility.AccessibilityManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import java.util.Formatter;
import java.util.Locale;

/**
 * Created by Jermaine on 11/1/2017.
 */

public class media_controller {
    private MediaController.MediaPlayerControl mPlayer;
    private ProgressBar mProgress;
    private TextView mEndTime, mCurrentTime;
    private boolean mShowing;
    private boolean mDragging;
    private static final int sDefaultTimeout = 3000;
    private boolean mListenersSet;
    private View.OnClickListener mNextListener, mPrevListener;
    StringBuilder mFormatBuilder;
    Formatter mFormatter;
    private ImageView mPauseButton;
    private ImageView mNextButton;
    private ImageView mPrevButton;
    private CharSequence mPlayDescription;
    private CharSequence mPauseDescription;
    private final AccessibilityManager mAccessibilityManager;
    private final Context mContext;
    Activity activity;
    Handler handle;



    public media_controller(RelativeLayout layout, final Activity activity,VideoViewService service)
    {
        mAccessibilityManager =  (AccessibilityManager) activity.getSystemService(Context.ACCESSIBILITY_SERVICE);
        mContext=activity;
        activity.setVolumeControlStream(AudioManager.STREAM_MUSIC);
        initControllerView(layout,activity);
        this.activity=activity;
        mPlayer=service;
        handle= new Handler(activity.getMainLooper());

    }

    public void initControllerView(RelativeLayout layout,final  Activity activity)
    {


        mPauseButton = (ImageView) layout.findViewById(R.id.pause);
        if (mPauseButton != null) {
            mPauseButton.requestFocus();
            mPauseButton.setOnClickListener(mPauseListener);
        }



        layout.findViewById(R.id.full).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (activity.getResources().getConfiguration().orientation== Configuration.ORIENTATION_PORTRAIT)
                {
                    activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                    ((VideoViewService)mPlayer).setDisplayNull();
                }
                else
                {
                    activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                    ((VideoViewService)mPlayer).setDisplayNull();
                }

            }
        });


        // By default these are hidden. They will be enabled when setPrevNextListeners() is called
        mNextButton = (ImageView) layout.findViewById(R.id.next);

        mPrevButton = (ImageView) layout.findViewById(R.id.prev);

        mProgress = (ProgressBar) layout.findViewById(R.id.seekbar);
        if (mProgress != null) {
            if (mProgress instanceof SeekBar) {
                SeekBar seeker = (SeekBar) mProgress;
                seeker.setOnSeekBarChangeListener(mSeekListener);
            }
            mProgress.setMax(1000);
        }
        mEndTime = (TextView) layout.findViewById(R.id.end);
        mCurrentTime = (TextView) layout.findViewById(R.id.start);
        mFormatBuilder = new StringBuilder();
        mFormatter = new Formatter(mFormatBuilder, Locale.getDefault());

        installPrevNextListeners();

    }

    public void show() {
        show(sDefaultTimeout);
    }

    public void show(int timeout) {
        if (!mShowing ) {
            setProgress();
            if (mPauseButton != null) {
                mPauseButton.requestFocus();
            }
            mShowing = true;
        }
        updatePausePlay();
        // cause the progress bar to be updated even if mShowing
        // was already true.  This happens, for example, if we're
        // paused with the progress bar showing the user hits play.
        handle.post(mShowProgress);
        if (timeout != 0 && !mAccessibilityManager.isTouchExplorationEnabled()) {
            handle.removeCallbacks(mFadeOut);
            handle.postDelayed(mFadeOut, timeout);
        }
    }

    public boolean isShowing() {
        return mShowing;
    }


    public void hide() {
        if (mShowing) {
            try {
                handle.removeCallbacks(mShowProgress);
            } catch (IllegalArgumentException ex) {
                Log.w("MediaController", "already removed");
            }
            mShowing = false;
        }
    }

    private final Runnable mFadeOut = new Runnable() {
        @Override
        public void run() {
            hide();
        }
    };

    private final Runnable mShowProgress = new Runnable() {
        @Override
        public void run() {
            int pos = setProgress();
            if (!mDragging && mShowing && mPlayer.isPlaying()) {
                handle.postDelayed(mShowProgress, 1000 - (pos % 1000));
            }
        }
    };

    private String stringForTime(int timeMs) {
        int totalSeconds = timeMs / 1000;
        int seconds = totalSeconds % 60;
        int minutes = (totalSeconds / 60) % 60;
        int hours   = totalSeconds / 3600;
        mFormatBuilder.setLength(0);
        if (hours > 0) {
            return mFormatter.format("%d:%02d:%02d", hours, minutes, seconds).toString();
        } else {
            return mFormatter.format("%02d:%02d", minutes, seconds).toString();
        }
    }

    private int setProgress() {
        if (mPlayer == null || mDragging) {
            return 0;
        }
       int position = mPlayer.getCurrentPosition();
        int duration =mPlayer.getDuration();
        if (mProgress != null) {
            if (duration > 0) {
                // use long to avoid overflow
                long pos = 1000L * position / duration;
                mProgress.setProgress( (int) pos);
            }
            int percent = mPlayer.getBufferPercentage();
           mProgress.setSecondaryProgress(percent * 10);
        }
        if (mEndTime != null)
            mEndTime.setText(stringForTime(duration));
        if (mCurrentTime != null)
            mCurrentTime.setText(stringForTime(position)+"/");
        return position;
    }




    private final View.OnClickListener mPauseListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            doPauseResume();
            show(sDefaultTimeout);
        }
    };

    private void updatePausePlay() {
        if (mPauseButton == null)
            return;
        if (mPlayer.isPlaying()) {
            //Change the drawable here
            //mPauseButton.setImageResource(com.android.internal.R.drawable.ic_media_pause);
            //mPauseButton.setContentDescription(mPauseDescription);
        } else {

            //mPauseButton.setImageResource(com.android.internal.R.drawable.ic_media_play);
            //mPauseButton.setContentDescription(mPlayDescription);
        }
    }

    public void doPauseResume() {
        if (mPlayer.isPlaying()) {
            mPlayer.pause();
        } else {
            mPlayer.start();
        }
        updatePausePlay();
    }

    private final SeekBar.OnSeekBarChangeListener mSeekListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onStartTrackingTouch(SeekBar bar) {
            show(3600000);
            mDragging = true;
            // By removing these pending progress messages we make sure
            // that a) we won't update the progress while the user adjusts
            // the seekbar and b) once the user is done dragging the thumb
            // we will post one of these messages to the queue again and
            // this ensures that there will be exactly one message queued up.
            handle.removeCallbacks(mShowProgress);
        }
        @Override
        public void onProgressChanged(SeekBar bar, int progress, boolean fromuser) {
            if (!fromuser) {
                // We're not interested in programmatically generated changes to
                // the progress bar's position.
                return;
            }
            long duration = mPlayer.getDuration();
            long newposition = (duration * progress) / 1000L;
            mPlayer.seekTo( (int) newposition);
            if (mCurrentTime != null)
                mCurrentTime.setText(stringForTime( (int) newposition)+"/");
        }
        @Override
        public void onStopTrackingTouch(SeekBar bar) {
            mDragging = false;
            setProgress();
            updatePausePlay();
            show(sDefaultTimeout);
            // Ensure that progress is properly updated in the future,
            // the call to show() does not guarantee this because it is a
            // no-op if we are already showing.
            handle.post(mShowProgress);
        }
    };



    private void installPrevNextListeners() {
        if (mNextButton != null) {
            mNextButton.setOnClickListener(mNextListener);
            mNextButton.setEnabled(mNextListener != null);
        }
        if (mPrevButton != null) {
            mPrevButton.setOnClickListener(mPrevListener);
            mPrevButton.setEnabled(mPrevListener != null);
        }
    }


    public void setPrevNextListeners(View.OnClickListener next, View.OnClickListener prev) {
        mNextListener = next;
        mPrevListener = prev;
        mListenersSet = true;

            installPrevNextListeners();
            if (mNextButton != null ) {
                mNextButton.setVisibility(View.VISIBLE);
            }
            if (mPrevButton != null) {
                mPrevButton.setVisibility(View.VISIBLE);
            }

    }





}
