/* SPDX-FileCopyrightText: 2026 The nasgorOS Project
 * SPDX-License-Identifier: Apache-2.0 */
package com.nasgoros.settings.performance;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

/** Persisted FPS overlay state: on/off and the position the user dragged it to. */
final class FpsSettings {
    private static final String FILE = "fps_overlay";
    private static final String KEY_ENABLED = "enabled";
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

    static int getX(Context context) { return prefs(context).getInt(KEY_X, UNSET); }

    static int getY(Context context) { return prefs(context).getInt(KEY_Y, UNSET); }

    static void setPosition(Context context, int x, int y) {
        prefs(context).edit().putInt(KEY_X, x).putInt(KEY_Y, y).apply();
    }

    /** Starts or stops the overlay to match the saved state. */
    static void apply(Context context) {
        Intent service = new Intent(context, FpsOverlayService.class);
        if (isEnabled(context)) context.startService(service);
        else context.stopService(service);
    }
}
