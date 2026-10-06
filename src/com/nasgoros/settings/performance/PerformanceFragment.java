/* SPDX-FileCopyrightText: 2026 The nasgorOS Project
 * SPDX-License-Identifier: Apache-2.0 */
package com.nasgoros.settings.performance;

import android.content.Context;
import android.os.Bundle;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;
import com.nasgoros.settings.R;

public class PerformanceFragment extends PreferenceFragmentCompat {
    private SwitchPreferenceCompat mFps;

    @Override public void onCreatePreferences(Bundle state, String rootKey) {
        setPreferencesFromResource(R.xml.nasgor_performance, rootKey);
        mFps = findPreference("fps_overlay");
        mFps.setOnPreferenceChangeListener((preference, value) -> {
            Context context = requireContext();
            FpsSettings.setEnabled(context, (Boolean) value);
            FpsSettings.apply(context);
            return true;
        });
    }

    @Override public void onResume() {
        super.onResume();
        mFps.setChecked(FpsSettings.isEnabled(requireContext()));
    }
}
