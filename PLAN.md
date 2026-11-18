# PLAN: Full-App Kenkashi Mechanical Motion Pass

## Objective
Implement a cohesive, industrial mechanical animation pass across OpenAirBuds matching the Kenkashi Acoustic aesthetic (snappy 180–260ms timing, zero-overshoot deceleration, tactile haptics, sliding indicators, live acoustic EQ curve visualizer, and directional screen transitions).

## Seams & Interfaces
1. `KenkashiMotion.kt`: Motion tokens, mechanical easing curves, spring specs, and `Modifier.kenkashiPressable`.
2. `KenkashiComponents.kt`:
   - `KenkashiSegmentedBar` sliding active indicator block + haptics.
   - `KenkashiEqFrequencyVisualizer` dynamic 6-band Bezier response curve.
   - `KenkashiBatteryCell` animated number roll + hairline level bar + charging pulse.
3. `MainActivity.kt`:
   - Horizontal slide push/pop between Main Scaffold and `EqualizerScreen`.
   - Directional tab paging `AnimatedContent` for `SoundTab`, `ControlsTab`, and `SettingsTab`.
   - `KenkashiBottomNavigationBar` sliding underline indicator.
4. `ControlsTab.kt`: Directional slide transition for Left <-> Right earbud line-art showcase.
5. `EqualizerScreen.kt` & `CustomEqSheet.kt`: Integrated live morphing acoustic response curve.

## Verification
- Unit test suite: `.\gradlew testDebugUnitTest`
- Build APK: `.\gradlew assembleDebug` (with versionName/code increment per AGENTS.md)
