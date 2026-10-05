/* SPDX-FileCopyrightText: 2026 The nasgorOS Project
 * SPDX-License-Identifier: Apache-2.0 */
package com.nasgoros.settings.performance;

import android.app.KeyguardManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.text.format.Formatter;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;
import com.nasgoros.settings.R;
import java.util.Locale;

/** User-enabled, noninteractive overlay; sampling stops when locked or screen off. */
public class PerformanceOverlayService extends Service {
    private static final String CHANNEL = "nasgor_performance";
    private static final String STOP = "com.nasgoros.settings.STOP_PERFORMANCE";
    private static final int NOTIFICATION = 1001;
    private static volatile boolean sRunning;
    private final Handler mMain = new Handler(Looper.getMainLooper());
    private HandlerThread mThread;
    private Handler mWorker;
    private HardwareReader mReader;
    private TaskFpsMonitor mFps;
    private WindowManager mWindows;
    private TextView mOverlay;
    private volatile int mGeneration;
    private boolean mSampling;
    private boolean mReceiverRegistered;

    public static boolean isRunning() { return sRunning; }

    private final BroadcastReceiver mScreenReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) { updateScreenState(); }
    };

    @Override public void onCreate() {
        super.onCreate();
        mWindows = getSystemService(WindowManager.class);
        mFps = new TaskFpsMonitor(this);
        mThread = new HandlerThread("NasgorPerformanceOverlay");
        mThread.start();
        mWorker = new Handler(mThread.getLooper());
        mReader = new HardwareReader(this);
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_USER_PRESENT);
        registerReceiver(mScreenReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        mReceiverRegistered = true;
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && STOP.equals(intent.getAction())) {
            stopSelf();
            return START_NOT_STICKY;
        }
        NotificationManager notifications = getSystemService(NotificationManager.class);
        notifications.createNotificationChannel(new NotificationChannel(CHANNEL,
                getString(R.string.performance_title), NotificationManager.IMPORTANCE_LOW));
        PendingIntent open = PendingIntent.getActivity(this, 0,
                new Intent(this, PerformanceActivity.class), PendingIntent.FLAG_IMMUTABLE);
        PendingIntent stop = PendingIntent.getService(this, 1,
                new Intent(this, PerformanceOverlayService.class).setAction(STOP),
                PendingIntent.FLAG_IMMUTABLE);
        startForeground(NOTIFICATION, new Notification.Builder(this, CHANNEL)
                .setSmallIcon(R.drawable.ic_nasgor_settings)
                .setContentTitle(getString(R.string.performance_overlay_title))
                .setContentText(getString(R.string.performance_notification))
                .setContentIntent(open).setOngoing(true).setOnlyAlertOnce(true)
                .addAction(new Notification.Action.Builder(null,
                        getString(R.string.performance_stop), stop).build()).build());
        try {
            if (mOverlay == null) createOverlay();
            sRunning = true;
            updateScreenState();
        } catch (RuntimeException e) {
            Toast.makeText(this, R.string.performance_overlay_error, Toast.LENGTH_LONG).show();
            stopSelf();
        }
        // The service only runs after an explicit user action, never automatically at boot.
        return START_NOT_STICKY;
    }

    private void createOverlay() {
        TextView overlay = new TextView(this);
        overlay.setText(getString(R.string.fps_waiting));
        overlay.setTextColor(Color.rgb(255, 245, 225));
        overlay.setTextSize(12);
        overlay.setTypeface(Typeface.MONOSPACE);
        overlay.setPadding(dp(10), dp(8), dp(10), dp(8));
        overlay.setMaxWidth(dp(270));
        GradientDrawable background = new GradientDrawable();
        background.setColor(0xe6151b23);
        background.setCornerRadius(dp(12));
        overlay.setBackground(background);
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.END;
        params.x = dp(8);
        params.y = dp(48);
        params.setTitle("NasgorOS performance");
        mWindows.addView(overlay, params);
        mOverlay = overlay;
    }

    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }

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
            ++mGeneration;
            mWorker.removeCallbacksAndMessages(null);
            mMain.removeCallbacksAndMessages(null);
            mFps.close();
        } else if (!mSampling) {
            mSampling = true;
            final int generation = ++mGeneration;
            mWorker.post(new Runnable() {
                @Override public void run() {
                    if (generation != mGeneration) return;
                    HardwareReader.Snapshot snapshot = mReader.read();
                    mMain.post(() -> {
                        if (generation != mGeneration || mOverlay == null) return;
                        if (!screenAvailable()) { updateScreenState(); return; }
                        render(snapshot);
                    });
                    if (generation == mGeneration) mWorker.postDelayed(this, 1000);
                }
            });
        }
    }

    private void render(HardwareReader.Snapshot snapshot) {
        float fps = mFps.sample();
        String rate = Float.isNaN(fps) ? "—" : String.format(Locale.getDefault(), "%.1f", fps);
        String cpu = Double.isNaN(snapshot.cpuPercent) ? "—"
                : String.format(Locale.getDefault(), "%.0f%%", snapshot.cpuPercent);
        String gpu = snapshot.gpu.isEmpty() ? getString(R.string.metric_unavailable)
                : snapshot.gpu.split("\n", 2)[0];
        StringBuilder text = new StringBuilder("FPS  ").append(rate)
                .append("\nCPU  ").append(cpu).append(" · ").append(snapshot.cpuModel)
                .append("\nGPU  ").append(gpu)
                .append("\nRAM  ").append(Formatter.formatShortFileSize(this,
                        snapshot.totalMemory - snapshot.availableMemory))
                .append(" / ").append(Formatter.formatShortFileSize(this, snapshot.totalMemory));
        mOverlay.setText(text);
        mOverlay.setContentDescription(getString(R.string.performance_overlay_title)
                + ", " + text);
    }

    @Override public void onDestroy() {
        sRunning = false;
        ++mGeneration;
        if (mWorker != null) mWorker.removeCallbacksAndMessages(null);
        mMain.removeCallbacksAndMessages(null);
        if (mFps != null) mFps.close();
        if (mThread != null) mThread.quitSafely();
        if (mReceiverRegistered) unregisterReceiver(mScreenReceiver);
        if (mOverlay != null) {
            mWindows.removeView(mOverlay);
            mOverlay = null;
        }
        stopForeground(STOP_FOREGROUND_REMOVE);
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) { return null; }
}
