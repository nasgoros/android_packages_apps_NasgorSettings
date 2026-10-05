/*
 * SPDX-FileCopyrightText: The nasgorOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.nasgoros.settings;

import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemProperties;
import android.util.Log;

import com.android.settingslib.collapsingtoolbar.CollapsingToolbarBaseActivity;

/** Entry point for Settings > NasgorOS. */
public class NasgorSettingsActivity extends CollapsingToolbarBaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Make sure the LineageParts entries live here, not in the stock categories,
        // even before the first BOOT_COMPLETED after flashing.
        LineagePartsTiles.hideFromStockSettings(this);
        if (SystemProperties.getBoolean("ro.nasgoros.crdroid_settings", false)
                && !getIntent().getBooleanExtra("com.nasgoros.settings.SYSTEM_SETTINGS", false)) {
            try {
                startActivity(new Intent().setComponent(new ComponentName("com.android.settings",
                        "com.android.settings.Settings$crDroidSettingsLayoutActivity")));
                finish();
                return;
            } catch (ActivityNotFoundException | SecurityException e) {
                // Keep the hardware monitor and original hub usable on an incomplete install.
                Log.w("NasgorSettings", "Customization host unavailable; opening system hub", e);
            }
        }
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(com.android.settingslib.collapsingtoolbar.R.id.content_frame,
                            new NasgorSettingsFragment())
                    .commit();
        }
    }
}
