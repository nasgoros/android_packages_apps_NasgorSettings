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
port keeps the launcher and ordinary apps unchanged, adds a Performance (FPS info) tab,
and applies NasgorOS branding. See `vendor/nasgoros/ports/crdroid/README.md`.
The original LineageParts hub is available through Miscellaneous > Additional
system settings, or directly on builds without the port. The monitor does not
depend on crDroid APIs; the hub still needs LineageParts and LineagePreferenceLib.
Porting notes and API boundaries: `vendor/nasgoros/PORTING.md`.

**FPS info** (Performance) is owned by this app and has a single switch:

- Shows the actual foreground-task FPS from
  `WindowManager.registerTaskFpsCallback()` in a small counter over apps.
  Static apps can report low FPS; display refresh rate (Hz) is not substituted.
- Default position is the top-right corner; drag the counter to move it. The
  position and on/off state persist (device-protected storage) and the overlay
  is restored after reboot.
- No notification: `FpsOverlayService` is a plain started service. The app is on
  the power-save allowlist (`nasgor-settings-sysconfig.xml`,
  `allow-in-power-save`), and its visible overlay window keeps the process
  perceptible.
- Sampling stops while the screen is off or locked.

Platform signing is required for the FPS/task/window APIs. The manifest declares
`ACCESS_FPS_COUNTER`, `REAL_GET_TASKS` and `SYSTEM_ALERT_WINDOW`
(`TYPE_APPLICATION_OVERLAY`, which unlike system overlays can be dragged);
privileged permissions are listed in the allowlist.
