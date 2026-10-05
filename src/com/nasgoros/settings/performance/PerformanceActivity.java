/* SPDX-FileCopyrightText: 2026 The nasgorOS Project
 * SPDX-License-Identifier: Apache-2.0 */
package com.nasgoros.settings.performance;

import android.os.Bundle;
import com.android.settingslib.collapsingtoolbar.CollapsingToolbarBaseActivity;

public class PerformanceActivity extends CollapsingToolbarBaseActivity {
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        if (state == null) getSupportFragmentManager().beginTransaction()
                .replace(com.android.settingslib.collapsingtoolbar.R.id.content_frame,
                        new PerformanceFragment()).commit();
    }
}
