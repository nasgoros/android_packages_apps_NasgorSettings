# NasgorOS Settings

Adds **Settings › NasgorOS**, a single home for NasgorOS customisation.

- Injected into the Settings home page (`com.android.settings.category.ia.homepage`);
  Settings itself is not forked.
- Entries are `LineagePartsPreference` items that open the existing LineageParts screens
  (status bar, buttons, power menu, gestures, LiveDisplay, charging, lights, profiles,
  Trust) and hide themselves when a feature is not supported by the device.
- LineageParts' own entries in the stock Settings categories are disabled at runtime
  (`LineagePartsTiles`), so each feature appears only once — under NasgorOS.

Branch `17.0` targets LineageOS 24.0 (Android 17).
