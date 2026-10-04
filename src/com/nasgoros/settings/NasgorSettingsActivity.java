/*
 * SPDX-FileCopyrightText: The nasgorOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.nasgoros.settings;

import android.os.Bundle;

import com.android.settingslib.collapsingtoolbar.CollapsingToolbarBaseActivity;

/** Entry point for Settings > NasgorOS. */
public class NasgorSettingsActivity extends CollapsingToolbarBaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Make sure the LineageParts entries live here, not in the stock categories,
        // even before the first BOOT_COMPLETED after flashing.
        LineagePartsTiles.hideFromStockSettings(this);
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(com.android.settingslib.collapsingtoolbar.R.id.content_frame,
                            new NasgorSettingsFragment())
                    .commit();
        }
    }
}
