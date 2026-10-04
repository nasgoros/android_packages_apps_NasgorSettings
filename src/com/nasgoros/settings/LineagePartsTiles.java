/*
 * SPDX-FileCopyrightText: The nasgorOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.nasgoros.settings;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.util.Log;

/**
 * LineageParts injects its screens into stock Settings categories (System, Display,
 * Gestures, Privacy) through activity-aliases. NasgorOS shows them under
 * Settings > NasgorOS instead, so those aliases are disabled here. The screens keep
 * working because NasgorOS opens PartsActivity directly (see LineagePartsPreference).
 *
 * No LineageOS code is modified; if LineageParts renames an alias it is skipped.
 */
public final class LineagePartsTiles {

    private static final String TAG = "NasgorSettings";
    private static final String LINEAGEPARTS = "org.lineageos.lineageparts";

    /** Aliases that inject LineageParts screens into stock Settings categories. */
    private static final String[] ALIASES = {
        ".statusbar.StatusBarSettings",
        ".input.ButtonSettings",
        ".profiles.ProfilesSettings",
        ".livedisplay.LiveDisplaySettings",
        ".gestures.TouchscreenGestureSettings",
        ".trust.TrustPreferences",
    };

    private LineagePartsTiles() {}

    static void hideFromStockSettings(Context context) {
        PackageManager pm = context.getPackageManager();
        for (String alias : ALIASES) {
            ComponentName cn = new ComponentName(LINEAGEPARTS, LINEAGEPARTS + alias);
            try {
                if (pm.getComponentEnabledSetting(cn)
                        != PackageManager.COMPONENT_ENABLED_STATE_DISABLED) {
                    pm.setComponentEnabledSetting(cn,
                            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                            PackageManager.DONT_KILL_APP);
                }
            } catch (IllegalArgumentException | SecurityException e) {
                Log.w(TAG, "Unable to disable " + cn.flattenToShortString(), e);
            }
        }
    }

    public static class BootReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            hideFromStockSettings(context);
        }
    }
}
