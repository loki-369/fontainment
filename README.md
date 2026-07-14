# Fontainment

> **Transform Your Android Phone into a Premium Automotive Infotainment System**

Fontainment is a native Android application built with **Kotlin** and **Jetpack Compose** following **Clean Architecture** patterns. It replaces standard launcher elements with a premium, zero-clutter automotive shell designed specifically for horizontally mounted dashboard devices.

---

## 📸 Design Presets & Themes

Fontainment supports multiple premium theme presets mapped to the typography scales defined in `presentation/theme/`:

| Theme Preset | Accent Color | Primary Background | Vibe |
| :--- | :--- | :--- | :--- |
| **Tesla Dark** | `#E82127` (Crimson) | `#0F0F12` (Charcoal) | Minimalist, clean EV dash |
| **BMW Blue** | `#1C69D4` (BMW Blue) | `#070B11` (Deep Navy) | Traditional German cluster |
| **Cyber Neon** | `#00FFC2` (Neon Cyan) | `#08060D` (Dark Violet) | Sci-Fi, futuristic grid |
| **Nothing Style** | `#FF3B30` (Dot Red) | `#0A0A0A` (Dot Slate) | Industrial monochrome dotted |
| **AMOLED Black** | `#E5E5E5` (Silver) | `#000000` (Pure Black) | OLED efficient cluster |
| **Lucid White** | `#C5A059` (Copper Gold)| `#F9F9F9` (Warm Sand) | Light theme, luxurious interior |

---

## 🛠️ Architecture & Package Structure

The project strictly follows the **MVVM** pattern layered around **Clean Architecture** boundaries:

```
+-----------------------------------------------------------+
|                    Presentation Layer                     |
|  [Compose UI Screens] <---> [ViewModels]                  |
+-----------------------------+-----------------------------+
                              |
                              v
+-----------------------------------------------------------+
|                       Domain Layer                        |
|  [Repository Interfaces] <---> [Data Models]              |
+-----------------------------+-----------------------------+
                              |
                              v
+-----------------------------------------------------------+
|                        Data Layer                         |
|  [SettingsRepositoryImpl]  --> Room SQLite DB             |
|  [MediaRepositoryImpl]     --> simulated audio streams    |
|  [AutomationRepositoryImpl]--> BT & Power state broadcast |
+-----------------------------------------------------------+
```

### Complete Codebase Layout
* `settings.gradle.kts` & `build.gradle.kts`: Gradle build configuration and Version Catalog mapping.
* `app/src/main/AndroidManifest.xml`: Declarations for location, phone calls, Bluetooth connections, foreground services, and boot receiver.
* `app/src/main/java/com/fontainment/app/`
  * `FontainmentApp.kt`: Application base class with Hilt setup (`@HiltAndroidApp`).
  * `MainActivity.kt`: Fullscreen layout activity. Disables status/navigation bars, locks screen on, requests runtime permissions, and boots Hilt injection.
  * `di/`
    * AppModule.kt: Configures Hilt bindings for Room database, sensors, and repository managers.
  * `domain/`
    * `model/`
      * VehicleState.kt: Model for live speed, battery status, network strength, and weather metrics.
      * SpotifyTrack.kt: Model mapping active audio title, artist, album, duration, progress, and playback state.
      * Widget.kt: Custom widgets enum for Desk Mode.
    * `repository/`
      * SettingsRepository.kt: Domain boundary for persistent preferences.
      * MediaRepository.kt: Boundary for player controls.
      * AutomationRepository.kt: Boundary for device launch automation triggers.
  * `data/`
    * `database/`
      * SettingsEntity.kt & SettingsDao.kt & AppDatabase.kt: Room SQLite setup for persistent key-value configuration.
    * `repository/`
      * SettingsRepositoryImpl.kt: Handles saving metrics, scales, and active display themes.
      * MediaRepositoryImpl.kt: Manages play, pause, seek, volume, and playback ticks using simulated fallbacks when Spotify isn't running.
      * AutomationRepositoryImpl.kt: Tracks power supply and Bluetooth MAC pairings.
    * `service/`
      * MediaPlaybackService.kt: ExoPlayer and MediaSession backend integration.
      * AutomationService.kt: Foreground Service that processes broadcasts for paired Bluetooth audio connections and power plugs to trigger `MainActivity` launch.
    * `receiver/`
      * BootReceiver.kt: Re-engages the Automation Service on phone boot.
  * `presentation/`
    * `theme/`
      * Color.kt & Type.kt & Theme.kt: Composition bindings for themes and typography.
    * `navigation/`
      * Screen.kt & NavGraph.kt: Graph router handling transitions between Drive, Desk, and Settings screens.
    * `drive/`
      * DriveViewModel.kt: Controls instrument cluster telemetry, search widgets, assistant overlay states, and music controller methods.
      * DriveModeScreen.kt: Split 3-panel UI. Left: stylized dark navigation map canvas. Center: instrument cluster displaying speed, compass, weather, and battery. Right: glassmorphism Spotify controller with blurred track background. Includes voice assistant pulsing mic HUD and Bluetooth call HUD cards.
    * `desk/`
      * DeskViewModel.kt: Controls widgets and periodic OLED pixel-shifting coordinates.
      * DeskModeScreen.kt: Landscape StandBy screen with oversized clock, calendar widgets, mini-player, and system load monitors.
    * `settings/`
      * SettingsViewModel.kt: Bridges user selections to the Room database.
      * SettingsScreen.kt: Formatted sidebar preferences editor. Controls themes, brightness thresholds, unit types, and automation features.

---

## ⚡ Key Architectural Highlights

### 1. Immersion Mode & Safety
To lock the Pixel 5a as a dedicated automotive cluster, `MainActivity` calls:
```kotlin
windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
```
Additionally, `android:keepScreenOn="true"` is set in the manifest (plus window flags) to override screensavers while driving, ensuring speed and maps are always visible.

### 2. OLED Burn-In Prevention
In **Desk Mode**, `DeskViewModel` runs a coroutine loop shifting the entire UI canvas coordinates by a small random offset `[-5dp, 5dp]` every 60 seconds:
```kotlin
while (true) {
    delay(60000)
    val dx = Random.nextFloat() * 10f - 5f
    val dy = Random.nextFloat() * 10f - 5f
    _burnInOffset.value = Pair(dx, dy)
}
```
This forces pixel shifts, protecting OLED screens during prolonged Desk Mode operations.

### 3. Background Automation Monitor
`AutomationService` runs as a foreground service with a low importance channel. It registers receivers for `Intent.ACTION_POWER_CONNECTED` and `BluetoothDevice.ACTION_ACL_CONNECTED` so that mounting the device in the car automatically boots the cluster without requiring physical screen taps.

---

## 🚀 Building and Running

### Requirements
* Android Studio Koala (or newer)
* JDK 17+ (JDK 21 recommended)
* Gradle 8.14+

### Setup
1. Clone or open the workspace folder (`c:\Desktop\fontainment`) in Android Studio.
2. Android Studio will automatically identify settings, build files, and resolve the Compose dependency catalog in `gradle/libs.versions.toml`.
3. Select your target device (e.g. Google Pixel 5a) or AVD emulator, and click **Run**.
