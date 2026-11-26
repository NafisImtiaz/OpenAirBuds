# OpenAirBuds

OpenAirBuds is a lightweight, privacy-focused, open-source Android companion application for realme, OnePlus, and OPPO (BBK ecosystem) True Wireless Stereo (TWS) earbuds. It serves as a fully offline replacement for proprietary vendor apps such as realme Link and HeyMelody, operating without user accounts, cloud dependencies, internet permissions, background analytics, or device telemetry.

The application communicates directly with supported earbuds over standard Bluetooth Classic RFCOMM using the reverse-engineered OPOv1 binary protocol, providing full hardware control, acoustic calibration, and real-time state synchronization.

---

## Key Features

### Privacy and Architecture
- **Completely Offline**: Operates without internet access permissions (`android.permission.INTERNET` is neither requested nor used).
- **Zero Cloud Reliance**: No accounts, login prompts, tracking libraries, or remote servers.
- **Low Resource Footprint**: Native Jetpack Compose implementation designed for fast startup and minimal battery consumption.

### Active Noise Control (ANC)
- **Three Core Modes**: Noise Cancellation, Normal (Off), and Transparency.
- **Multi-Level Noise Suppression**: Select between Deep (up to 50 dB), Moderate, Mild, and Smart/Dynamic noise cancellation levels.
- **Custom Hold Cycle**: Configure the physical touch-and-hold gesture on the earbuds to cycle only through your preferred subset of modes (e.g., ANC and Transparency only, or all three).
- **In-Ear Awareness**: Automatically adapts noise control transitions when one or both earbuds are removed.

### Battery and Charging Telemetry
- **Granular Levels**: Accurate percentage reporting for the Left earbud, Right earbud, and Charging Case.
- **Charging Status**: Real-time hardware charging indicators for each component.

### Equalizer and Audio Calibration
- **Factory Tuning Presets**: Instant switching between Nature Balance, Bass Boost, Clear Vocals, and Clear Bass.
- **Custom Multi-Band Equalizer**: 6-band interactive equalizer with support for multiple user-defined custom EQ profiles.
- **BassWave (Dynamic Bass)**: Real-time dynamic low-frequency gain enhancement adjustable from -5 dB to +5 dB.

### Smart Device Features
- **In-Ear Wear Detection**: Toggle automatic playback pause and resume upon earbud insertion and removal.
- **Game Mode**: Enable ultra-low latency audio transmission (down to 55 ms) for gaming and video synchronization.
- **Dual Connection (Multi-Point)**: View and manage paired host devices and switch active audio routing directly from the interface.
- **Find My Earbuds**: Trigger high-pitch acoustic locator chirps on individual earbuds.
- **Earbud Fit Test**: Run automated acoustic seal diagnostics to verify tip fit and sound isolation.
- **Additional Enhancements**: Hi-Res Audio mode, Volume Enhancer, and Wind Noise Reduction toggles (hardware permitting).

### Gesture Customization
- **Per-Earbud Mapping**: Independently assign actions for the Left and Right earbuds.
- **Supported Gestures**: Single Tap, Double Tap, Triple Tap, Touch and Hold, Long Hold, and Slide.
- **Available Actions**: Play/Pause, Next Track, Previous Track, Voice Assistant, Noise Control Cycling, Volume Up, Volume Down, Switch Devices, and Game Mode.

### System Integration
- **Quick Settings Tile**: Add the Noise Control tile to Android's pull-down quick settings shade to toggle ANC modes directly from anywhere without opening the app.

### Kenkashi Design System
- **Minimalist Aesthetic**: An editorial, typography-driven audio interface inspired by precision acoustic hardware.
- **Dual Themes**: Warm paper ivory theme and matte charcoal dark theme.
- **Responsive Animations**: Tuned spring physics and geometric layout indicators.

---

## Supported Devices

OpenAirBuds supports BBK-ecosystem Bluetooth earbuds communicating via the OPlus/BBK OPOv1 protocol over Bluetooth Classic SPP/RFCOMM.

### Primary Verified Models
- realme Buds Air 6
- realme Buds Air 6 Pro
- realme Buds Air 5 / 5 Pro
- realme Buds T300 / T310
- OnePlus Buds Pro 2 / Pro 3
- OnePlus Buds 3 / Nord Buds series
- OPPO Enco Air / Enco X series

*Note: Feature availability (such as 50 dB ANC depth, multi-point dual connection, or BassWave) automatically scales based on the hardware capability bitmap queried during the connection handshake.*

---

## Protocol Overview

Communication is established over Bluetooth Classic RFCOMM (Serial Port Profile).

- **SDP / RFCOMM UUID**: `0000079A-D102-11E1-9B23-00025B00A5A5` (Service Name: `oppointeraction`)
- **Fallback SPP UUID**: `00001101-0000-1000-8000-00805F9B34FB`

### Binary Frame Format
Every packet exchanged with the earbuds adheres to the following binary structure:

```
[0xAA] [TotalLen LEB128] [0x00 0x00] [CmdId LE: 2B] [Seq: 1B] [PayLen LE: 2B] [Payload: N Bytes]
```

- **Start of Frame (SOF)**: `0xAA`
- **Total Length**: LEB128-encoded length of the subsequent frame bytes (`7 + PayloadLength`).
- **Slice Header**: `0x00 0x00` (for unfragmented frames).
- **Command ID**: 16-bit little-endian identifier.
- **Sequence Number**: 8-bit incrementing transaction counter.
- **Payload Length**: 16-bit little-endian byte count of the command payload.
- **Payload**: Command-specific binary data or parameter payload.

### Primary Command IDs
| Command ID (Hex) | Direction | Description |
|---|---|---|
| `0x0100` / `0x8100` | Host / Bud | Handshake negotiation and supported feature bitmap |
| `0x0103` / `0x8103` | Host / Bud | Product ID query and response (e.g., `063412` for Buds Air 6) |
| `0x0105` / `0x8105` | Host / Bud | Firmware version string query |
| `0x0106` / `0x8106` | Host / Bud | Battery telemetry (levels and charging states) |
| `0x0108` / `0x0401` | Host / Bud | Touch gesture configuration query and write |
| `0x0109` / `0x8109` | Host / Bud | In-ear wear detection state query |
| `0x010C` / `0x0404` | Host / Bud | ANC mode selection, depth calibration, and hold cycle mask |
| `0x010D` / `0x0403` | Host / Bud | Feature toggles (Game Mode, Wear Detection, Dual Connection) |
| `0x010F` / `0x0406` | Host / Bud | Equalizer preset selection and custom gain bands |
| `0x0112` / `0x0429` | Host / Bud | Dual connection paired host list and active device management |
| `0x0124` / `0x041B` | Host / Bud | Dynamic Bass (BassWave) level (-5 dB to +5 dB) |
| `0x0200` / `0x0205` | Host / Bud | Broadcast subscription codes |
| `0x0204` | Bud to Host | Inbound real-time push events (Battery, Wear, ANC, Game Mode) |
| `0x0400` | Host to Bud | Acoustic locator ping (Find My Earbuds) |
| `0x0405` | Host to Bud | Earbud Fit Test acoustic seal evaluation |
| `0x0500` / `0x8500` | Bud to Host | Epoch timestamp clock synchronization |

---

## Technical Stack

- **Platform**: Android 8.0+ (API 26 through API 35)
- **Language**: Kotlin 2.0+
- **UI Toolkit**: Jetpack Compose with Material 3 foundation
- **Architecture**: MVI / Unidirectional Data Flow (StateFlow, SharedFlow, ViewModel)
- **Asynchronous Execution**: Kotlin Coroutines
- **Persistence**: Jetpack DataStore Preferences
- **Build System**: Gradle with Kotlin DSL (`build.gradle.kts`) and version catalog (`libs.versions.toml`)

---

## Getting Started

### Prerequisites
1. An Android smartphone running Android 8.0 (API 26) or newer.
2. Bluetooth enabled.
3. Your compatible earbuds paired to your phone in Android Bluetooth Settings before opening the app.

### Building from Source
1. Clone the repository:
   ```bash
   git clone https://github.com/NafisImtiaz/OpenAirBuds.git
   cd OpenAirBuds
   ```
2. Build the project using Gradle:
   ```bash
   ./gradlew assembleDebug
   ```
   For an optimized release build:
   ```bash
   ./gradlew assembleRelease
   ```
3. Install the generated APK onto an Android device via ADB:
   ```bash
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```

### Quick Settings Tile Setup
1. Swipe down from the top of your screen to open the Quick Settings shade.
2. Tap the pencil or edit icon to modify active tiles.
3. Locate **Noise Control** under the OpenAirBuds section and drag it into your active tile list.
4. Tap the tile at any time to cycle between Noise Cancelling, Transparency, and Normal modes without launching the application.

---

## Contributing

Contributions, bug reports, and hardware packet captures for unverified earbud models are welcome. Please ensure new pull requests include relevant unit tests and follow existing project conventions.

---

## License

This project is licensed under the Apache License, Version 2.0. See the [LICENSE](LICENSE) file for complete details.
