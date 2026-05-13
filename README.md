# nabla notes

A minimalist Android note-taking app backed by OneDrive. Write plain text and Markdown notes, synced to your Microsoft account via OneDrive.

---

## Features

- **OneDrive sync** — notes stored in a configurable OneDrive folder
- **Markdown rendering** — live preview for `.md` files using Markwon
- **Foldable / tablet support** — split-pane layout on `MEDIUM`/`EXPANDED` window sizes
- **MSAL authentication** — secure sign-in with Microsoft personal or work accounts
- **Material You** — dynamic colour scheme on Android 12+
- **Offline-first editing** — edit locally, save when ready

---

## Requirements

- **Android Studio Hedgehog** (2023.1.1) or newer
- **JDK 17** (bundled with Android Studio)
- **Android SDK 35** (compile) / **minSdk 26**
- **Azure AD app registration** (see below)

---

## Build Instructions

### 1. Clone the repository

```bash
git clone <repo-url>
cd android-notepad
```

### 2. Create `local.properties`

In the project root (next to `settings.gradle.kts`), create `local.properties` with:

```properties
sdk.dir=/path/to/Android/sdk
msal.clientId=<YOUR_AZURE_CLIENT_ID>
```

> `local.properties` is git-ignored and never committed.

### 3. Configure `msal_config.json`

The file `app/src/main/res/raw/msal_config.json` must contain the **literal** client ID string (it cannot reference BuildConfig at runtime):

```json
{
  "client_id": "<YOUR_AZURE_CLIENT_ID>",
  "authorization_user_agent": "DEFAULT",
  "redirect_uri": "msauth://com.nabla.notes/<YOUR_SIGNATURE_HASH>",
  "account_mode": "SINGLE",
  "broker_redirect_uri_registered": false,
  "authorities": [
    {
      "type": "AAD",
      "audience": { "type": "PersonalMicrosoftAccount" }
    }
  ]
}
```

Replace `<YOUR_SIGNATURE_HASH>` with your actual signing certificate hash (see below).

### 4. Build the APK

```bash
./gradlew assembleDebug
# Output: app/build/outputs/apk/debug/app-debug.apk
```

---

## Azure AD App Registration

The app registration ID is `<YOUR_AZURE_CLIENT_ID>`.

You must register the Android redirect URI in the Azure portal:

1. Go to [Azure Portal → App Registrations](https://portal.azure.com/#blade/Microsoft_AAD_IAM/ActiveDirectoryMenuBlade/RegisteredApps)
2. Open the app registration for `<YOUR_AZURE_CLIENT_ID>`
3. Navigate to **Authentication → Add a platform → Android**
4. Set **Package name**: `com.nabla.notes`
5. Set **Signature hash**: (see below)
6. Copy the generated redirect URI — it looks like:  
   `msauth://com.nabla.notes/<BASE64_HASH>`
7. Paste that URI into `msal_config.json` as `redirect_uri`

### Getting the Signature Hash

**Debug keystore (development):**

```bash
keytool -exportcert \
  -alias androiddebugkey \
  -keystore ~/.android/debug.keystore \
  | openssl sha1 -binary \
  | openssl base64
```

Default debug keystore password: `android`

**Release keystore:**

```bash
keytool -exportcert \
  -alias <your-alias> \
  -keystore /path/to/release.keystore \
  | openssl sha1 -binary \
  | openssl base64
```

---

## First-Time Setup (In-App)

1. Launch the app and tap **Sign in with Microsoft**
2. Complete the Microsoft sign-in flow
3. Open **Settings** (gear icon in the top bar)
4. Tap **Change Folder** and select the OneDrive folder where your notes live
5. Navigate back — your notes will load automatically

---

## Project Structure

```
app/src/main/kotlin/com/nabla/notes/
├── MainActivity.kt               # Entry point, single/split pane routing
├── NotepadApp.kt                 # Application class (Hilt)
├── auth/
│   └── MsalManager.kt            # MSAL auth wrapper
├── di/
│   └── AppModule.kt              # Hilt dependency graph
├── model/
│   ├── AppSettings.kt            # DataStore settings model
│   └── NoteFile.kt               # OneDrive file model
├── repository/
│   ├── OneDriveRepository.kt     # Graph API file operations
│   └── SettingsRepository.kt     # DataStore persistence
├── ui/
│   ├── browser/
│   │   └── FileBrowserScreen.kt  # File list + create dialog
│   ├── editor/
│   │   └── EditorScreen.kt       # Text / Markdown editor
│   ├── settings/
│   │   └── SettingsScreen.kt     # Folder picker + sign-out
│   └── theme/
│       └── NotepadTheme.kt       # Material You theme
└── viewmodel/
    ├── BrowserViewModel.kt       # File list state
    ├── EditorViewModel.kt        # Editor state + CRUD
    └── SettingsViewModel.kt      # Settings state
```

---

## Notes

- **`msal_config.json` cannot use BuildConfig** — it is a static JSON resource. The client ID must be a literal string. Update both `local.properties` (for `BuildConfig.MSAL_CLIENT_ID`) and `msal_config.json` when changing environments.
- **Debug vs Release** — the signature hash is different for debug and release builds. Register both in Azure if needed.
- **OneDrive folder** — only `.txt` and `.md` files are shown. Configure the folder in Settings.

---

## License

Private / internal use.

