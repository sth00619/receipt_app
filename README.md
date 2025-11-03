# Receipt App

Android application for receipt management.

## Setup

### Prerequisites

- Android Studio
- Java JDK (usually bundled with Android Studio)

### Keytool Setup

If you encounter errors when running `keytool` commands (e.g., for checking keystores or generating signing keys), please refer to the [Keytool Setup Guide](KEYTOOL_SETUP.md).

**Quick fix for Windows:**
```powershell
.\get-keytool.ps1
```

**Quick fix for macOS/Linux:**
```bash
./get-keytool.sh
```

## Building the Project

```bash
# Clean and build
./gradlew clean build

# Run on connected device/emulator
./gradlew installDebug
```

## Development

### Debug Keystore

To get your debug keystore information for Firebase, Google Maps, or other services:

```bash
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
```

If the `keytool` command is not found, see [KEYTOOL_SETUP.md](KEYTOOL_SETUP.md) for setup instructions.

## Project Structure

```
receipt_app/
├── app/                    # Main application module
├── gradle/                 # Gradle wrapper files
├── build.gradle.kts       # Project-level build configuration
├── settings.gradle.kts    # Project settings
├── KEYTOOL_SETUP.md       # Keytool configuration guide
├── get-keytool.ps1        # Windows keytool finder script
└── get-keytool.sh         # macOS/Linux keytool finder script
```

## Troubleshooting

### Common Issues

1. **"keytool: command not found"**
   - See [KEYTOOL_SETUP.md](KEYTOOL_SETUP.md)
   - Run the appropriate helper script for your platform

2. **Gradle sync issues**
   - File → Invalidate Caches / Restart in Android Studio
   - Delete `.gradle` folder and sync again

3. **SDK not found**
   - Set `ANDROID_HOME` environment variable to your Android SDK location
   - Windows: `C:\Users\[username]\AppData\Local\Android\Sdk`
   - macOS: `~/Library/Android/sdk`
   - Linux: `~/Android/Sdk`

## License

[Add your license here]
