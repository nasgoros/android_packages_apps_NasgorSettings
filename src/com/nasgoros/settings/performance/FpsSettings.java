/* SPDX-FileCopyrightText: 2026 The nasgorOS Project
 * SPDX-License-Identifier: Apache-2.0 */
package com.nasgoros.settings.performance;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

/**
 * Persisted overlay state: which counters are shown (app FPS, screen refresh rate)
 * and the position the user dragged the overlay to.
 */
final class FpsSettings {
    private static final String FILE = "fps_overlay";
    /** App frame rate counter. Kept as "enabled" for compatibility with older builds. */
    private static final String KEY_ENABLED = "enabled";
    /** Screen refresh rate (Hz) counter. */
    private static final String KEY_REFRESH_RATE = "refresh_rate";
    private static final String KEY_X = "x";
    private static final String KEY_Y = "y";
    /** No saved position: the overlay starts in the top-right corner. */
    static final int UNSET = Integer.MIN_VALUE;

    private FpsSettings() { }

    private static SharedPreferences prefs(Context context) {
        // Device-protected storage keeps the state readable during boot.
        return context.createDeviceProtectedStorageContext()
                .getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    static boolean isEnabled(Context context) {
        return prefs(context).getBoolean(KEY_ENABLED, false);
    }

    static void setEnabled(Context context, boolean enabled) {
        prefs(context).edit().putBoolean(KEY_ENABLED, enabled).apply();
    }

    static boolean isRefreshRateEnabled(Context context) {
        return prefs(context).getBoolean(KEY_REFRESH_RATE, false);
    }

    static void setRefreshRateEnabled(Context context, boolean enabled) {
        prefs(context).edit().putBoolean(KEY_REFRESH_RATE, enabled).apply();
    }

    /** True when at least one counter is on, so the overlay should run. */
    static boolean isOverlayNeeded(Context context) {
        return isEnabled(context) || isRefreshRateEnabled(context);
    }

    static void disableAll(Context context) {
        prefs(context).edit().putBoolean(KEY_ENABLED, false)
                .putBoolean(KEY_REFRESH_RATE, false).apply();
    }

    static int getX(Context context) { return prefs(context).getInt(KEY_X, UNSET); }

    static int getY(Context context) { return prefs(context).getInt(KEY_Y, UNSET); }

    static void setPosition(Context context, int x, int y) {
        prefs(context).edit().putInt(KEY_X, x).putInt(KEY_Y, y).apply();
    }

    /** Starts or stops the overlay to match the saved state. */
    static void apply(Context context) {
        Intent service = new Intent(context, FpsOverlayService.class);
        if (isOverlayNeeded(context)) context.startService(service);
        else context.stopService(service);
    }
}
