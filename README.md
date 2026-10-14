# Glyph 3D Printer Link (Nothing OS)

A clean, minimalist Android application designed for Nothing OS devices equipped with a rear circular **Glyph Matrix** (including Nothing Phone 3 and Nothing Phone 4a Pro). 

The app connects directly to your 3D printer over local MQTT/TLS, continuously synchronizing live telemetry onto the rear circular LED matrix—both in the foreground, in the background, and when the phone screen is off.

---

## Key Features

- **Circular Glyph Matrix Engine**:
  - **Circular Progress Ring**: Fills clockwise from 12 o'clock around the perimeter of the circular LED matrix in direct proportion to print completion (`0%` to `100%`).
  - **Centered Print Percentage**: Displays high-contrast, centered digits (`0`–`100`) against a pure black background.
  - **Paused State (`||`)**: Displays a centered pause symbol with a subtle brightness pulse while preserving the print progress ring.
  - **100% Finished State (`✓`)**: Renders a centered checkmark with a smooth sinusoidal breathing brightness effect upon print completion.
  - **Preheating State (`PH`)**: Displays `PH` centered in the matrix with radial heating wave pulses.

- **Dual Hardware Support**:
  - **Nothing Phone (3)**: 25x25 circular matrix profile.
  - **Nothing Phone (4a) Pro**: 13x13 circular matrix profile.
  - Auto-detection via Nothing Glyph SDK with in-app preview switching.

- **Background & Screen-Off Persistence**:
  - Android 14 `connectedDevice` Foreground Service (`PrinterForegroundService`) with partial wake-lock management to prevent network suspension when the device is locked.
  - Native Nothing OS Glyph Toy integration (`com.nothing.glyph.TOY`) with Always-On Display (`aod_support`) and lock screen Carousel support.

- **Clean Aesthetic & Privacy**:
  - Pure monochrome Nothing OS dot-matrix design.
  - Zero emojis.
  - Direct local LAN communication over encrypted TLS (no third-party cloud required).

---

## Architecture & Code Structure

```
app/src/main/java/com/example/bambuglyph/
├── MainActivity.kt               # Entry point and theme wrapper
├── Navigation.kt                 # Navigation graph (Onboarding <-> Dashboard)
├── BambuGlyphApp.kt              # Application subclass and notification channels
├── data/
│   ├── PreferencesManager.kt     # Encrypted local credential & device storage
│   └── PrinterRepository.kt      # StateFlow telemetry streams and matrix frame buffers
├── glyph/
│   ├── GlyphController.kt        # Nothing OS Ketchum SDK binder and device dispatcher
│   └── GlyphMatrixRenderer.kt    # Circular matrix algorithms, fonts, and LED frame rendering
├── model/
│   ├── GlyphDeviceType.kt        # Hardware matrix resolution definitions
│   └── PrinterConfig.kt          # Telemetry state models and connection settings
├── mqtt/
│   └── BambuMqttClient.kt        # Local TLS MQTT client subscribing to printer telemetry
├── service/
│   ├── PrinterForegroundService.kt # Persistent background service with wake-lock
│   └── PrinterGlyphToyService.kt  # Native Nothing OS Glyph Toy Carousel & AOD provider
└── ui/
    ├── components/
    │   └── GlyphMatrixView.kt    # Circular LED matrix visualizer canvas
    ├── dashboard/
    │   └── DashboardScreen.kt    # Main monitoring dashboard, gauges, and settings
    ├── onboarding/
    │   └── OnboardingScreen.kt   # 3-step setup and permission granting workflow
    └── theme/                    # Nothing OS monochrome theme tokens
```

---

## Getting Started

### Prerequisites
- Android Studio Ladybug / Meerkat or Android CLI with SDK 34+.
- Java 17+.
- A Nothing Phone with Glyph Matrix (or the built-in circular matrix simulator on an emulator).

### Build & Run
```bash
# Clone the repository
git clone https://github.com/vmg265/nosglyph-3dprinter-link.git
cd nosglyph-3dprinter-link

# Build the debug APK
./gradlew assembleDebug

# Install onto connected device / emulator
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## License
MIT License
