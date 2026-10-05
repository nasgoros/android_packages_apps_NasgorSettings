/* SPDX-FileCopyrightText: 2026 The nasgorOS Project
 * SPDX-License-Identifier: Apache-2.0 */
package com.nasgoros.settings.performance;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.text.format.Formatter;
import android.view.Display;
import android.widget.Toast;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;
import com.nasgoros.settings.R;
import java.util.Locale;

public class PerformanceFragment extends PreferenceFragmentCompat {
    private final Handler mMain = new Handler(Looper.getMainLooper());
    private HandlerThread mThread;
    private Handler mWorker;
    private TaskFpsMonitor mFps;
    private SwitchPreferenceCompat mOverlay;
    private volatile int mGeneration;

    @Override public void onCreatePreferences(Bundle state, String rootKey) {
        setPreferencesFromResource(R.xml.nasgor_performance, rootKey);
        mOverlay = findPreference("performance_overlay");
        mOverlay.setOnPreferenceChangeListener((preference, value) -> {
            Intent service = new Intent(requireContext(), PerformanceOverlayService.class);
            try {
                if ((Boolean) value) requireContext().startForegroundService(service);
                else requireContext().stopService(service);
                return true;
            } catch (RuntimeException e) {
                Toast.makeText(requireContext(), R.string.performance_overlay_error,
                        Toast.LENGTH_LONG).show();
                return false;
            }
        });
    }

    @Override public void onResume() {
        super.onResume();
        mOverlay.setChecked(PerformanceOverlayService.isRunning());
        mFps = new TaskFpsMonitor(requireContext());
        mThread = new HandlerThread("NasgorHardwareInfo");
        mThread.start();
        mWorker = new Handler(mThread.getLooper());
        final Handler worker = mWorker;
        final int generation = ++mGeneration;
        final HardwareReader reader = new HardwareReader(requireContext());
        worker.post(new Runnable() {
            @Override public void run() {
                if (generation != mGeneration) return;
                HardwareReader.Snapshot snapshot = reader.read();
                mMain.post(() -> {
                    if (generation != mGeneration || !isResumed()) return;
                    update(snapshot);
                });
                if (generation == mGeneration) worker.postDelayed(this, 1000);
            }
        });
    }

    private void update(HardwareReader.Snapshot s) {
        summary("cpu_model", s.cpuModel);
        summary("cpu_cores", getString(R.string.cpu_core_count, s.cores));
        summary("cpu_abis", s.abis);
        summary("cpu_frequency", available(s.cpuFrequencies));
        summary("cpu_usage", Double.isNaN(s.cpuPercent) ? getString(R.string.metric_unavailable)
                : String.format(Locale.getDefault(), "%.0f%%", s.cpuPercent));
        summary("gpu_model", available(s.gpu));
        summary("memory_info", getString(R.string.memory_available,
                Formatter.formatShortFileSize(requireContext(), s.availableMemory),
                Formatter.formatShortFileSize(requireContext(), s.totalMemory)));
        Display display = requireActivity().getDisplay();
        summary("display_refresh", display == null ? getString(R.string.metric_unavailable)
                : String.format(Locale.getDefault(), "%.1f Hz", display.getRefreshRate()));
        float fps = mFps.sample();
        summary("app_fps", Float.isNaN(fps) ? getString(R.string.fps_waiting)
                : String.format(Locale.getDefault(), "%.1f FPS", fps));
        mOverlay.setChecked(PerformanceOverlayService.isRunning());
    }

    private String available(String value) {
        return value == null || value.trim().isEmpty() ? getString(R.string.metric_unavailable) : value;
    }
    private void summary(String key, String value) {
        Preference preference = findPreference(key);
        if (preference != null) preference.setSummary(value);
    }

    @Override public void onPause() {
        ++mGeneration;
        mMain.removeCallbacksAndMessages(null);
        if (mFps != null) mFps.close();
        if (mWorker != null) mWorker.removeCallbacksAndMessages(null);
        if (mThread != null) mThread.quitSafely();
        super.onPause();
    }
}
