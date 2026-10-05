/* SPDX-FileCopyrightText: 2026 The nasgorOS Project
 * SPDX-License-Identifier: Apache-2.0 */
package com.nasgoros.settings.performance;

/** Numeric parsing kept independent from Android for host-side tests. */
public final class MetricParser {
    private MetricParser() {}

    public static int cpuCount(String value) {
        try {
            int count = 0;
            int previous = -1;
            for (String entry : value.trim().split(",")) {
                String[] range = entry.split("-", -1);
                if (range.length < 1 || range.length > 2) return -1;
                int first = Integer.parseInt(range[0]);
                int last = range.length == 2 ? Integer.parseInt(range[1]) : first;
                if (first < 0 || last < first || first <= previous || last > 4095) return -1;
                count += last - first + 1;
                previous = last;
            }
            return count;
        } catch (RuntimeException e) { return -1; }
    }

    public static long[] cpuTicks(String line) {
        if (line == null) return null;
        String[] values = line.trim().split("\\s+");
        if (values.length < 5 || !values[0].equals("cpu")) return null;
        try {
            long total = 0;
            // guest and guest_nice are already counted in user and nice.
            for (int i = 1; i < Math.min(9, values.length); i++) {
                long value = Long.parseLong(values[i]);
                if (value < 0) return null;
                total = Math.addExact(total, value);
            }
            long idle = Long.parseLong(values[4]);
            if (values.length > 5) idle = Math.addExact(idle, Long.parseLong(values[5]));
            return new long[]{total, idle};
        } catch (RuntimeException e) { return null; }
    }

    public static double cpuPercent(long[] before, long[] after) {
        if (before == null || after == null) return Double.NaN;
        long total = after[0] - before[0];
        long idle = after[1] - before[1];
        if (total <= 0 || idle < 0 || idle > total) return Double.NaN;
        return (total - idle) * 100.0 / total;
    }
}
