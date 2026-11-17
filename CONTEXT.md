# Project GAMMA — Domain Context & Architecture

## Overview
OpenAirBuds is a native Android companion application for BBK-ecosystem Bluetooth TWS earbuds (realme, OnePlus, OPPO) providing high-fidelity ANC control, gesture customization, equalizer adjustment, and device diagnostics over RFCOMM Bluetooth SPP sockets.

## Domain Vocabulary
- **RFCOMM / SPP**: Bluetooth Serial Port Profile (UUID `00001101-0000-1000-8000-00805F9B34FB`) used for bidirectional binary command and telemetry exchange.
- **OPO Protocol**: Packet framing protocol for HeyMelody / realme Link devices:
  - Magic header `0xFF 0xAA` or `0xFF 0x55`
  - Command ID (e.g., `0x01` battery telemetry, `0x02` ANC control, `0x03` EQ, `0x04` gestures, `0x06` game mode)
  - Payload length and checksum
- **Noise Control Modes**:
  - `NOISE_CANCELLATION`: Active cancellation (Smart, Max, Mild)
  - `TRANSPARENCY`: Ambient audio pass-through
  - `OFF` / `NORMAL`: Passive isolation only
- **Hold Cycle Mask**: Bitmask determining which noise control modes are toggled via the physical earbud touch-and-hold gesture (`ANC`, `Transparency`, `Off`).
- **Gesture Bindings**: Device (`1` = Left, `2` = Right), Button (`1` = Touch Sensor), Action (`1` = Single Tap, `2` = Double Tap, `3` = Triple Tap, `4` = Touch & Hold), Function (`0` = None, `1` = Play/Pause, `2` = Next, `3` = Previous, `4` = Voice Assistant, `5` = ANC Mode, `6` = Volume Up, `7` = Volume Down).
- **BudsState**: Central immutable UI state data class capturing connection status, battery, active noise mode, EQ preset, custom EQ gains, gesture bindings, and hardware capabilities.
- **DeviceProfile**: Static definition of supported earbud models, product IDs, name patterns, capabilities, and associated visual artwork.

## UI Design Paradigms
- **Kenkashi Acoustic**: Minimalist, brutalist, editorial audio design language characterized by:
  - Warm paper/ivory canvas (`#F9F8F5`) or matte charcoal dark (`#121212`)
  - 1px hairline geometric grid dividers (`#DCD9D1` / `#2C2B29`)
  - High-contrast classic Serif headings and large battery figures
  - Monospace uppercase captions and labels with generous letter-spacing
  - Geometric segmented noise control blocks and rectangular minimalist toggle switches
  - 3-tab bottom navigation (`DEVICES`, `EQ`, `PROFILE`) with underlined active state
