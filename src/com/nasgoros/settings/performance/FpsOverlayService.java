/* SPDX-FileCopyrightText: 2026 The nasgorOS Project
 * SPDX-License-Identifier: Apache-2.0 */
package com.nasgoros.settings.performance;

import android.annotation.SuppressLint;
import android.app.KeyguardManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.hardware.display.DisplayManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.view.Display;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import java.util.Locale;

/**
 * Small draggable FPS counter drawn over apps.
 *
 * Runs as a plain started service without a notification: the app is on the
 * power-save allowlist (sysconfig) and its visible overlay keeps the process
 * perceptible. Sampling stops while the screen is off or locked.
 */
public class FpsOverlayService extends Service {
    private static final long INTERVAL_MS = 1000;
    private final Handler mMain = new Handler(Looper.getMainLooper());
    private TaskFpsMonitor mFps;
    private Context mWindowContext;
    private WindowManager mWindows;
    private WindowManager.LayoutParams mParams;
    private TextView mOverlay;
    private boolean mSampling;
    private boolean mReceiverRegistered;

    private final BroadcastReceiver mScreenReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) { updateScreenState(); }
    };

    private final Runnable mTick = new Runnable() {
        @Override public void run() {
            if (mOverlay == null) return;
            if (!screenAvailable()) { updateScreenState(); return; }
            float fps = mFps.sample();
            mOverlay.setText(Float.isNaN(fps) ? "— FPS"
                    : String.format(Locale.getDefault(), "%d FPS", Math.round(fps)));
            mMain.postDelayed(this, INTERVAL_MS);
        }
    };

    @Override public void onCreate() {
        super.onCreate();
        // A window context gives the overlay correct metrics and configuration.
        Display display = getSystemService(DisplayManager.class)
                .getDisplay(Display.DEFAULT_DISPLAY);
        mWindowContext = createWindowContext(display,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, null);
        mWindows = mWindowContext.getSystemService(WindowManager.class);
        mFps = new TaskFpsMonitor(this);
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_USER_PRESENT);
        registerReceiver(mScreenReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        mReceiverRegistered = true;
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (!FpsSettings.isEnabled(this)) {
            stopSelf();
            return START_NOT_STICKY;
        }
        try {
            if (mOverlay == null) createOverlay();
            updateScreenState();
        } catch (RuntimeException e) {
            FpsSettings.setEnabled(this, false);
            stopSelf();
            return START_NOT_STICKY;
        }
        // Restarted by the system if the process is reclaimed while enabled.
        return START_STICKY;
    }

    @SuppressLint("ClickableViewAccessibility")
    private void createOverlay() {
        TextView overlay = new TextView(mWindowContext);
        overlay.setText("— FPS");
        overlay.setTextColor(Color.WHITE);
        overlay.setTextSize(12);
        overlay.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
        overlay.setPadding(dp(8), dp(4), dp(8), dp(4));
        GradientDrawable background = new GradientDrawable();
        background.setColor(0x99000000);
        background.setCornerRadius(dp(8));
        overlay.setBackground(background);

        mParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT);
        // Anchored top-right: x is the distance from the right edge.
        mParams.gravity = Gravity.TOP | Gravity.RIGHT;
        int x = FpsSettings.getX(this);
        int y = FpsSettings.getY(this);
        mParams.x = x == FpsSettings.UNSET ? dp(8) : x;
        mParams.y = y == FpsSettings.UNSET ? dp(48) : y;
        mParams.setTitle("NasgorOS FPS");
        // Settings, Files, the package installer and other system UIs set
        // HIDE_NON_SYSTEM_OVERLAY_WINDOWS, which hides normal app overlays. A system
        // application overlay (platform-signed, SYSTEM_APPLICATION_OVERLAY) stays visible.
        mParams.setSystemApplicationOverlay(true);
        overlay.setOnTouchListener(new DragListener());
        mWindows.addView(overlay, mParams);
        mOverlay = overlay;
    }

    /** Moves the window with the finger and remembers where it was dropped. */
    private final class DragListener implements View.OnTouchListener {
        private float mDownX, mDownY;
        private int mStartX, mStartY;

        @Override public boolean onTouch(View view, MotionEvent event) {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    mDownX = event.getRawX();
                    mDownY = event.getRawY();
                    mStartX = mParams.x;
                    mStartY = mParams.y;
                    return true;
                case MotionEvent.ACTION_MOVE:
                    Point size = screenSize();
                    mParams.x = clamp(mStartX - Math.round(event.getRawX() - mDownX),
                            size.x - view.getWidth());
                    mParams.y = clamp(mStartY + Math.round(event.getRawY() - mDownY),
                            size.y - view.getHeight());
                    mWindows.updateViewLayout(view, mParams);
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    FpsSettings.setPosition(FpsOverlayService.this, mParams.x, mParams.y);
                    return true;
                default:
                    return false;
            }
        }
    }

    private Point screenSize() {
        Rect bounds = mWindows.getCurrentWindowMetrics().getBounds();
        return new Point(bounds.width(), bounds.height());
    }

    private static int clamp(int value, int max) { return Math.max(0, Math.min(value, max)); }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private boolean screenAvailable() {
        return getSystemService(PowerManager.class).isInteractive()
                && !getSystemService(KeyguardManager.class).isKeyguardLocked();
    }

    private void updateScreenState() {
        if (mOverlay == null) return;
        boolean available = screenAvailable();
        mOverlay.setVisibility(available ? View.VISIBLE : View.GONE);
        if (!available) {
            mSampling = false;
            mMain.removeCallbacks(mTick);
            mFps.close();
        } else if (!mSampling) {
            mSampling = true;
            mMain.post(mTick);
        }
    }

    @Override public void onDestroy() {
        mMain.removeCallbacks(mTick);
        if (mFps != null) mFps.close();
        if (mReceiverRegistered) unregisterReceiver(mScreenReceiver);
        if (mOverlay != null) {
            mWindows.removeView(mOverlay);
            mOverlay = null;
        }
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) { return null; }
}
