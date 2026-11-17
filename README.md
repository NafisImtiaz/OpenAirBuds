# OpenAirBuds 🎧

**OpenAirBuds** is a modern, lightweight, privacy-first open-source Android companion app for the **realme Buds Air 6** (and compatible OPlus / realme / OPPO / OnePlus TWS earbuds).

It communicates directly with your earbuds over Bluetooth using the reverse-engineered **OPOv1** protocol, freeing you from needing the proprietary **realme Link** app, cloud accounts, or telemetry.

---

## ✨ Features

- **🛡️ 100% Offline & Private**: Zero cloud login, zero telemetry, zero trackers, no account required.
- **🔇 Noise Control**:
  - **Active Noise Cancellation (ANC)** (supports Smart, 50 dB Deep ANC, Moderate, Mild).
  - **Normal Mode (Off)**.
  - **Transparency Mode**.
- **🔋 Detailed Battery Telemetry**:
  - Individual Left earbud percentage + charging state.
  - Individual Right earbud percentage + charging state.
  - Charging case percentage + charging state.
- **⚡ Quick Settings Tile**:
  - Add the **Noise Control** tile to your Android pull-down quick settings to cycle between ANC, Transparency, and Normal modes with one tap.
- **🎮 Game Mode (Low Latency)**:
  - Toggle 55ms ultra-low latency audio for competitive gaming and video sync.
- **🎵 Sound Effects & Equalizer**:
  - Presets for **Bass Boost+**, **Balanced/Pure**, and **Clear Vocals**.
- **👂 In-Ear Wear Detection**:
  - Toggle auto-pause/auto-resume when taking out an earbud.

---

## 🛠️ How It Works (Protocol Reverse Engineering)

Realme earbuds use the OPlus / BBK **OPOv1** communication protocol over standard Bluetooth Classic RFCOMM.

### Bluetooth Service
- **Service Name**: `oppointeraction`
- **SDP / RFCOMM UUID**: `0000079A-D102-11E1-9B23-00025B00A5A5`

### Packet Frame Format
Every command packet sent to or received from the earbuds follows the real reverse-engineered **OPOv1** binary frame:
```
[0xAA] [TotalLen LEB128] [0x00 0x00] [CmdId LE: 2B] [Seq: 1B] [PayLen LE: 2B] [Payload: N Bytes]
```
- **SOF Byte**: `0xAA`
- **Total Length**: LEB128 encoded count of all following bytes (`7 + PayLen`).
- **Slice Header**: `0x00 0x00` (for unfragmented messages).
- **16-bit Little-Endian Command IDs (`CmdId`)**:
  - `0x0100` / `0x8100`: Handshake & Command Bitmap
  - `0x0103` / `0x8103`: Product ID (`063412` for realme Buds Air 6)
  - `0x0105` / `0x8105`: Firmware Version query
  - `0x0106` / `0x8106`: Battery Telemetry (`level = raw and 0x7F`, `isCharging = raw and 0x80`)
  - `0x0108` / `0x0401`: Touch Gestures query and write
  - `0x0109` / `0x8109`: In-Ear Wear Detection status
  - `0x010C` / `0x0404`: ANC Modes (50dB Deep, Moderate, Mild, Normal, Transparency) & Hold cycle mask
  - `0x010D` / `0x0403`: Feature switches (Game Mode `0x06`, Wear Detection `0x04`, Dual Connection `0x11`, BassWave `0x1D`)
  - `0x010F` / `0x0406`: Equalizer presets (Balanced `0`, Deep Bass `1`, Clear Vocals `2`, Bass Boost+ `3`)
  - `0x0112` / `0x0429`: Dual Connection paired devices list & management
  - `0x0124` / `0x041B`: Dynamic Bass (BassWave) level (-5 to +5 dB)
  - `0x0200` / `0x0205`: Broadcast Codes and Event Subscriptions
  - `0x0204`: Inbound push events (Battery `01`, Wear `02`, ANC `03`, Fit Test `04`, Game Mode `05`, Dual Devices `06`)
  - `0x0400`: Find My Earbuds acoustic ping
  - `0x0405`: Earbud Fit Test seal diagnosis
  - `0x0500` / `0x8500`: Host-bud Clock Synchronization reply (`00 <EpochSeconds u32 LE>`)
  - `0x0503`: Real-time feature toggle pushes from earbuds
  - `0x0504`: Real-time EQ preset changes from earbuds

---

## 🚀 Getting Started

### Prerequisites
- Android device running **Android 8.0 (API 26) or higher** (Android 14/15 fully supported).
- Pair your **realme Buds Air 6** in your Android device's Bluetooth settings first.

### Building from Source
1. Clone this repository:
   ```bash
   git clone https://github.com/your-username/OpenAirBuds.git
   cd OpenAirBuds
   ```
2. Open the project in **Android Studio (Ladybug / Iguana or later)** with JDK 17+.
3. Build and install the debug APK to your connected phone or emulator:
   ```bash
   ./gradlew assembleDebug
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```

---

## 📱 Quick Settings Tile Setup
1. Swipe down twice from the top of your Android screen to open the Quick Settings panel.
2. Tap the **Edit (Pencil)** icon to customize tiles.
3. Scroll down and drag **Noise Control** (`OpenAirBuds`) into your active tiles.
4. Tap the tile anytime to switch between Noise Cancelling, Transparency, and Normal modes!

---

## 🤝 Contributing
Contributions, issue reports, and packet captures for other Realme/Oppo earbud models are welcome!
Licensed under the [Apache License 2.0](LICENSE).
