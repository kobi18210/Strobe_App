# ⚡ StrobeRPM — Fan Speed Meter

Measure the RPM of your ceiling fan using your phone's flashlight as a stroboscope.

## How it works
The app blinks your flashlight at a set frequency. When the frequency matches the fan's rotation speed, the blades appear **frozen** due to the stroboscopic effect. Read the RPM directly from the display.

## Features
- Adjustable strobe frequency: 0.5 Hz to 25 Hz
- Fine-tune with +/- buttons
- Set number of fan blades for accurate RPM calculation
- Aliasing warning (a 3-blade fan also freezes at 1/3 the true frequency)

## Build Instructions

### Requirements
- Android Studio (Hedgehog or newer)
- JDK 17+
- Android SDK 34

### Steps
1. Open Android Studio → **File → Open** → select this folder (`StrobeRPM/`)
2. Wait for Gradle sync to finish
3. Connect your Android phone (USB debugging on) or use an emulator
4. Press **▶ Run** (Shift+F10)
5. On first launch, grant the **Camera** permission

### Build a release APK
- **Build → Build Bundle(s)/APK(s) → Build APK(s)**
- APK will be at `app/build/outputs/apk/debug/app-debug.apk`

## Requirements
- Android 6.0+ (API 23)
- Rear camera with flash

## Usage Tips
- Use in a **dark room** for best results
- Start at a low frequency and slowly increase
- When blades appear frozen → you've found the frequency
- Multiply Hz × 60 = RPM (already shown on screen)
- With N blades: actual RPM could be the displayed value × any integer up to N
