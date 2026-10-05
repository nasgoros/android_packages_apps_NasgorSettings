/* SPDX-FileCopyrightText: 2026 The nasgorOS Project
 * SPDX-License-Identifier: Apache-2.0 */
package com.nasgoros.settings.performance;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Context;
import android.opengl.EGL14;
import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLSurface;
import android.opengl.GLES20;
import android.os.Build;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Read-only device sampling. Unreadable kernel nodes are reported as unavailable. */
public final class HardwareReader {
    private final ActivityManager mActivityManager;
    private final List<File> mCpuPolicies = new ArrayList<>();
    private String mGpu;
    private long[] mPreviousCpu;

    public HardwareReader(Context context) {
        mActivityManager = context.getSystemService(ActivityManager.class);
        File[] policies = new File("/sys/devices/system/cpu/cpufreq").listFiles(
                file -> file.getName().matches("policy[0-9]+"));
        if (policies != null) {
            Arrays.sort(policies, (a, b) -> Integer.compare(
                    Integer.parseInt(a.getName().substring(6)),
                    Integer.parseInt(b.getName().substring(6))));
            mCpuPolicies.addAll(Arrays.asList(policies));
        }
    }

    public Snapshot read() {
        Snapshot result = new Snapshot();
        result.cpuModel = (!Build.SOC_MANUFACTURER.equals(Build.UNKNOWN)
                ? Build.SOC_MANUFACTURER + " " : "")
                + (!Build.SOC_MODEL.equals(Build.UNKNOWN) ? Build.SOC_MODEL : Build.HARDWARE);
        result.cores = Runtime.getRuntime().availableProcessors();
        String present = readLine(new File("/sys/devices/system/cpu/present"));
        if (present != null) {
            int count = MetricParser.cpuCount(present);
            if (count > 0) result.cores = count;
        }
        result.abis = String.join(", ", Build.SUPPORTED_ABIS);
        StringBuilder frequencies = new StringBuilder();
        for (File policy : mCpuPolicies) {
            long current = readNumber(new File(policy, "scaling_cur_freq"));
            if (current < 0) current = readNumber(new File(policy, "cpuinfo_cur_freq"));
            long max = readNumber(new File(policy, "cpuinfo_max_freq"));
            if (current <= 0 && max <= 0) continue;
            if (frequencies.length() > 0) frequencies.append('\n');
            String cpus = readLine(new File(policy, "related_cpus"));
            frequencies.append("CPU ").append(cpus != null ? cpus : policy.getName().substring(6))
                    .append(": ").append(current > 0 ? current / 1000 + " MHz" : "—");
            if (max > 0) frequencies.append(" / ").append(max / 1000).append(" MHz");
        }
        result.cpuFrequencies = frequencies.toString();
        long[] ticks = MetricParser.cpuTicks(readLine(new File("/proc/stat")));
        result.cpuPercent = MetricParser.cpuPercent(mPreviousCpu, ticks);
        mPreviousCpu = ticks;
        if (mGpu == null) mGpu = readGpu();
        result.gpu = mGpu;
        ActivityManager.MemoryInfo memory = new ActivityManager.MemoryInfo();
        mActivityManager.getMemoryInfo(memory);
        result.totalMemory = memory.totalMem;
        result.availableMemory = memory.availMem;
        return result;
    }

    private static long readNumber(File path) {
        String value = readLine(path);
        try { return value == null ? -1 : Long.parseLong(value.trim()); }
        catch (NumberFormatException e) { return -1; }
    }

    private static String readLine(File path) {
        try (BufferedReader reader = new BufferedReader(new FileReader(path))) {
            return reader.readLine();
        } catch (IOException | SecurityException e) { return null; }
    }

    /** Probe a private pbuffer on the sampling thread, never on the UI thread. */
    private static String readGpu() {
        EGLDisplay display = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);
        if (display == EGL14.EGL_NO_DISPLAY) return "";
        EGLContext context = EGL14.EGL_NO_CONTEXT;
        EGLSurface surface = EGL14.EGL_NO_SURFACE;
        try {
            int[] version = new int[2];
            if (!EGL14.eglInitialize(display, version, 0, version, 1)) return "";
            EGLConfig[] configs = new EGLConfig[1];
            int[] count = new int[1];
            int[] attributes = {EGL14.EGL_SURFACE_TYPE, EGL14.EGL_PBUFFER_BIT,
                    EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT, EGL14.EGL_NONE};
            if (!EGL14.eglChooseConfig(display, attributes, 0, configs, 0, 1, count, 0)
                    || count[0] == 0) return "";
            context = EGL14.eglCreateContext(display, configs[0], EGL14.EGL_NO_CONTEXT,
                    new int[]{EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE}, 0);
            if (context == EGL14.EGL_NO_CONTEXT) return "";
            surface = EGL14.eglCreatePbufferSurface(display, configs[0],
                    new int[]{EGL14.EGL_WIDTH, 1, EGL14.EGL_HEIGHT, 1, EGL14.EGL_NONE}, 0);
            if (surface == EGL14.EGL_NO_SURFACE
                    || !EGL14.eglMakeCurrent(display, surface, surface, context)) return "";
            String renderer = GLES20.glGetString(GLES20.GL_RENDERER);
            String vendor = GLES20.glGetString(GLES20.GL_VENDOR);
            String api = GLES20.glGetString(GLES20.GL_VERSION);
            return (renderer == null ? "" : renderer) + "\n"
                    + (vendor == null ? "" : vendor) + "\n" + (api == null ? "" : api);
        } catch (RuntimeException e) {
            return "";
        } finally {
            EGL14.eglMakeCurrent(display, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE,
                    EGL14.EGL_NO_CONTEXT);
            if (surface != EGL14.EGL_NO_SURFACE) EGL14.eglDestroySurface(display, surface);
            if (context != EGL14.EGL_NO_CONTEXT) EGL14.eglDestroyContext(display, context);
            EGL14.eglTerminate(display);
            EGL14.eglReleaseThread();
        }
    }

    public static final class Snapshot {
        public String cpuModel, cpuFrequencies, abis, gpu;
        public int cores;
        public double cpuPercent = Double.NaN;
        public long totalMemory, availableMemory;
    }
}
