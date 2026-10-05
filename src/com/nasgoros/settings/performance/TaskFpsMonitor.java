/* SPDX-FileCopyrightText: 2026 The nasgorOS Project
 * SPDX-License-Identifier: Apache-2.0 */
package com.nasgoros.settings.performance;

import android.app.ActivityManager;
import android.content.Context;
import android.os.SystemClock;
import android.view.WindowManager;
import android.window.TaskFpsCallback;

import java.util.List;

/** Actual SurfaceFlinger task FPS; never substitutes display Hz or Choreographer ticks. */
public final class TaskFpsMonitor implements AutoCloseable {
    private final Context mContext;
    private final WindowManager mWindowManager;
    private final ActivityManager mActivityManager;
    private TaskFpsCallback mCallback;
    private int mTaskId = -1;
    private int mGeneration;
    private float mFps = Float.NaN;
    private long mLastReport;
    private String mPackage = "";

    public TaskFpsMonitor(Context context) {
        mContext = context;
        mWindowManager = context.getSystemService(WindowManager.class);
        mActivityManager = context.getSystemService(ActivityManager.class);
    }

    /** Call on the main thread. Returns NaN while waiting or when unavailable. */
    public float sample() {
        try {
            List<ActivityManager.RunningTaskInfo> tasks = mActivityManager.getRunningTasks(1);
            if (tasks.isEmpty() || tasks.get(0).topActivity == null) {
                close();
                return Float.NaN;
            }
            ActivityManager.RunningTaskInfo task = tasks.get(0);
            if (task.taskId != mTaskId) {
                close();
                final int generation = mGeneration;
                mTaskId = task.taskId;
                mPackage = task.topActivity.getPackageName();
                mCallback = new TaskFpsCallback() {
                    @Override public void onFpsReported(float fps) {
                        if (generation != mGeneration || !Float.isFinite(fps) || fps < 0) return;
                        mFps = fps;
                        mLastReport = SystemClock.uptimeMillis();
                    }
                };
                mWindowManager.registerTaskFpsCallback(mTaskId, mContext.getMainExecutor(), mCallback);
            }
            return SystemClock.uptimeMillis() - mLastReport <= 6000 ? mFps : Float.NaN;
        } catch (RuntimeException e) {
            close();
            return Float.NaN;
        }
    }

    public String getPackageName() { return mPackage; }

    @Override public void close() {
        mGeneration++;
        if (mCallback != null) {
            try { mWindowManager.unregisterTaskFpsCallback(mCallback); }
            catch (RuntimeException ignored) { }
        }
        mCallback = null;
        mTaskId = -1;
        mFps = Float.NaN;
        mPackage = "";
        mLastReport = 0;
    }
}
