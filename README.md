# NasgorOS Settings

Adds **Settings › NasgorOS**, a single home for NasgorOS customisation.

- Injected into the Settings home page (`com.android.settings.category.ia.homepage`);
  routes into the customization host when the crDroid port is enabled.
- The additional-system-settings hub uses `LineagePartsPreference` items to open LineageParts screens
  (status bar, buttons, power menu, gestures, LiveDisplay, charging, lights, profiles,
  Trust) and hide themselves when a feature is not supported by the device.
- LineageParts' own entries in the stock Settings categories are disabled at runtime
  (`LineagePartsTiles`), so each feature appears only once — under NasgorOS.

Branch `17.0` targets LineageOS 24.0 (Android 17).

## Full customization and performance monitor

With `ro.nasgoros.crdroid_settings=true`, the NasgorOS entry opens the full
customization host supplied by the pinned crDroid Settings port. The vendor
port keeps the launcher and ordinary apps unchanged, adds a Performance tab,
and applies NasgorOS branding. See `vendor/nasgoros/ports/crdroid/README.md`.
The original LineageParts hub is available through Miscellaneous > Additional
system settings, or directly on builds without the port. The monitor does not
depend on crDroid APIs; the hub still needs LineageParts and LineagePreferenceLib.
Porting notes and API boundaries: `vendor/nasgoros/PORTING.md`.

**FPS, CPU & GPU info** is a separate activity owned by this app:

- Actual foreground-task FPS from `WindowManager.registerTaskFpsCallback()`;
  display refresh rate is shown separately in Hz. Static apps can report low FPS.
- Chipset, core count, supported ABIs, current/maximum CPU cluster frequencies,
  and CPU usage when `/proc/stat` is readable.
- GPU renderer, vendor and OpenGL ES version from a small offscreen EGL context.
- Available and total RAM.
- Optional noninteractive FPS/CPU/GPU/RAM overlay with a foreground notification
  and Stop action. It pauses while locked or screen off and is off after reboot.

Kernel reads and EGL discovery run on a worker thread. Sampling and FPS callbacks
are removed on pause/stop; stale callbacks from a previous task are ignored.
Missing/denied kernel information is shown as unavailable, not a fabricated
percentage. The GPU row describes the GPU; it does not claim to measure GPU load.
No root commands or extra network access are used.

Platform signing is required for the FPS/task/window APIs. The manifest declares
`ACCESS_FPS_COUNTER`, `REAL_GET_TASKS`, `INTERNAL_SYSTEM_WINDOW` and foreground
service permissions; privileged permissions are listed in the allowlist.

Run the host parser tests:

```bash
mkdir -p /tmp/nasgor-performance-tests
javac -d /tmp/nasgor-performance-tests \
  src/com/nasgoros/settings/performance/MetricParser.java tests/MetricParserTest.java
java -cp /tmp/nasgor-performance-tests MetricParserTest
```
