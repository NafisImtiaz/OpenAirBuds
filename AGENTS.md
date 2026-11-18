# Workspace Conventions — OpenAirBuds

## Build & Versioning
- **Rebuild Version Increment**: Whenever rebuilding the APK (`assembleDebug` or `assembleRelease`), always increment `versionName` by `0.0.1` (e.g. `1.0.0` → `1.0.1` → `1.0.2`) and increment `versionCode` by `1` in `app/build.gradle.kts` before building.
