# SoundBubble — Floating Soundboard App

## ⚠️ Please Understand First (Very Important)
This app does NOT inject digital audio directly into another app's (e.g. Free Fire) microphone — Android does not allow this through any permission (including Accessibility) **without root**. This is a security design, not a bug.

The app works through two acoustic methods (toggle available in Settings):
- **📢 Speaker Mode** — audio plays through the phone's speaker, and your mic picks it up itself (will not work with headphones)
- **🎧 Bluetooth Mode** — if a Bluetooth handsfree/earbud is connected, audio is sent to its SCO line so it plays close to the mic
- **Auto Mode** — automatically decides whether Bluetooth is connected or not

The result depends on the device, volume, and echo cancellation — there is no 100% guarantee.

## New Features
1. **📄 Single File Import** — add one mp3/wav file
2. **📁 Whole Folder Import** — go to Settings and select an entire folder; all audio files inside are added automatically (including subfolders)
3. **🗜 ZIP Import** — select a ZIP file and the app automatically extracts all audio files (no matter how many folders they are inside)
4. **Pre-included Default Sounds** — see below how every new user automatically gets some sounds on first launch
5. **🎧 Bluetooth Permission Button** — permission for Bluetooth routing on Android 12+
6. **🔊 Output Mode Toggle** — switch between Auto / Speaker / Bluetooth

## How to Give Default (pre-included) Sounds to Every New User
You only need to place your audio files in this folder (before building):
```
app/src/main/assets/default_sounds/
    hello.mp3
    laugh.mp3
    taunt1.mp3
```
As soon as any new user opens the app for the first time, all these files are automatically copied into their audio list — no code change needed. Just put the files in this folder and push to GitHub.

(Important: I cannot generate audio content myself — you need to add the real recorded/meme sounds to this folder yourself.)

## How to Use the App
1. Grant the Overlay Permission
2. (Optional) Grant the Bluetooth Permission
3. Add your audios (single file / folder / zip / record) — or default sounds will already be there
4. Select the Output Mode (or leave it on Auto)
5. Press **Launch** → a round floating button will appear; drag it anywhere
6. Open the game, tap the button → the audio panel opens, play any audio

## How to Build the APK by Pushing to GitHub (without Android Studio)

1. Push this whole folder into a new GitHub repository:
   ```bash
   git init
   git add .
   git commit -m "Initial commit"
   git branch -M main
   git remote add origin <YOUR_REPO_URL>
   git push -u origin main
   ```
2. Open your repo on GitHub → go to the **Actions** tab
3. The "Build APK" workflow will run automatically
4. Once finished, open that workflow run → under **Artifacts** you will find `SoundBubble-debug-apk`
5. Download and extract it → inside you will find `app-debug.apk`
6. Transfer this APK to your phone and install it (you will first need to allow "Unknown sources")

## Important Permissions the App Will Request
- **Display over other apps (Overlay)** — required for the floating button
- **Microphone** — only if you want to record a new audio yourself
- **Bluetooth Connect** (Android 12+) — only if you want to use the Bluetooth output mode
- **Notifications** — to show the notification while the service is running (Android 13+)

Note: The Accessibility permission was intentionally not included because it is useless for audio routing — it is only for UI automation.

## Folder Structure
```
SoundBubble/
├── app/
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── assets/default_sounds/     (place your default audio files here)
│       ├── java/com/soundbubble/app/
│       │   ├── MainActivity.kt        (import/record/launch/settings)
│       │   ├── FloatingService.kt     (draggable bubble + panel)
│       │   ├── AudioAdapter.kt        (audio list + play/delete)
│       │   ├── AudioRouter.kt         (speaker/Bluetooth smart routing)
│       │   └── FileImportHelper.kt    (folder import + zip extract + defaults)
│       └── res/                       (layouts, drawables, strings)
├── .github/workflows/build-apk.yml    (automatic APK build)
├── build.gradle.kts
└── settings.gradle.kts
```

## Ideas for Future Improvements
- Polish the app icon like a Play Store app
- Divide audio slots into categories (greetings, taunts, etc.)
- Auto-start the service after boot (optional)
