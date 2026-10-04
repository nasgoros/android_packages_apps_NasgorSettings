/*
 * SPDX-FileCopyrightText: The nasgorOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.nasgoros.settings;

import android.os.Bundle;

import androidx.preference.PreferenceFragmentCompat;

/**
 * NasgorOS settings home. Entries are LineagePartsPreference items, which open the
 * existing LineageParts screens and hide themselves when a feature is unsupported.
 */
public class NasgorSettingsFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.nasgor_settings, rootKey);
    }
}
