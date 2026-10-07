/* SPDX-FileCopyrightText: 2026 The nasgorOS Project
 * SPDX-License-Identifier: Apache-2.0 */
package com.nasgoros.settings.performance;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Restores the FPS overlay after boot or an app update when the user left it on. */
public class FpsBootReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        if (FpsSettings.isOverlayNeeded(context)) FpsSettings.apply(context);
    }
}
