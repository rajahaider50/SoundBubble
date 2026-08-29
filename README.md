# SoundBubble — Floating Soundboard App

## ⚠️ پہلے یہ سمجھیں (بہت ضروری)
یہ ایپ کسی دوسری ایپ (جیسے Free Fire) کے مائیکروفون میں براہِ راست ڈیجیٹل audio inject نہیں کرتی — Android بغیر **root** کے کسی بھی permission (Accessibility سمیت) کے ذریعے یہ اجازت نہیں دیتا۔ یہ security design ہے، bug نہیں۔

ایپ دو acoustic طریقوں سے کام کرتی ہے (Settings میں toggle موجود ہے):
- **📢 Speaker موڈ** — audio فون کے speaker سے بجتی ہے، آپ کا mic خود اسے pick اپ کر لیتا ہے (headphones کے ساتھ کام نہیں کرے گا)
- **🎧 Bluetooth موڈ** — اگر Bluetooth handsfree/earbuds لگی ہوں تو audio اسی کی SCO لائن پر بھیجی جاتی ہے، تاکہ mic کے قریب بجے
- **Auto موڈ** — خودکار طور پر طے کرتی ہے کہ Bluetooth لگی ہے یا نہیں

نتیجہ ڈیوائس، والیوم، اور echo cancellation پر منحصر ہے — کوئی 100% ضمانت نہیں۔

## نئے فیچرز
1. **📄 سنگل فائل امپورٹ** — ایک mp3/wav فائل شامل کریں
2. **📁 پورا فولڈر امپورٹ** — Settings میں جا کر ایک پورا فولڈر منتخب کریں، اندر کی تمام audio files خودکار شامل ہو جائیں گی (subfolders سمیت)
3. **🗜 ZIP امپورٹ** — ZIP فائل منتخب کریں، ایپ خودکار extract کر کے تمام audio files نکال لے گی (چاہے کتنے ہی folders کے اندر ہوں)
4. **پہلے سے شامل Default Sounds** — نیچے دیکھیں کہ ہر نئے یوزر کو خودکار کچھ آواز پہلے سے کیسے ملیں گی
5. **🎧 Bluetooth Permission بٹن** — Android 12+ پر Bluetooth routing کے لیے اجازت
6. **🔊 Output Mode ٹوگل** — Auto / Speaker / Bluetooth کے درمیان سوئچ

## ہر نئے یوزر کو Default (پہلے سے موجود) اڈیوز کیسے دیں
آپ کو صرف اپنی audio files اس فولڈر میں ڈالنی ہیں (build کرنے سے پہلے):
```
app/src/main/assets/default_sounds/
    hello.mp3
    laugh.mp3
    taunt1.mp3
```
جیسے ہی کوئی نیا یوزر ایپ پہلی بار کھولے گا، یہ سب فائلیں خودکار اس کی audio list میں کاپی ہو جائیں گی — کوڈ میں کچھ بدلنے کی ضرورت نہیں، بس فائلیں اس فولڈر میں رکھیں اور GitHub پر push کر دیں۔

(اہم: میں خود آڈیو کانٹینٹ generate نہیں کر سکتا — یہ حقیقی recorded/meme sounds آپ کو خود اس فولڈر میں شامل کرنے ہوں گے۔)

## ایپ کیسے استعمال کریں
1. Overlay Permission دیں
2. (اختیاری) Bluetooth Permission دیں
3. اپنی audios شامل کریں (single file / folder / zip / record) — یا default sounds پہلے سے موجود ہوں گی
4. Output Mode منتخب کریں (یا Auto رہنے دیں)
5. **لانچ** دبائیں → گول فلوٹنگ بٹن نظر آئے گا، اسے drag کر کے کہیں بھی رکھیں
6. گیم کھولیں، بٹن پر ٹیپ کریں → audio panel کھلے گا، کوئی بھی audio play کریں

## GitHub پر پش کر کے APK کیسے بنائیں (بغیر Android Studio)

1. یہ پورا فولڈر ایک نئی GitHub repository میں پش کریں:
   ```bash
   git init
   git add .
   git commit -m "Initial commit"
   git branch -M main
   git remote add origin <YOUR_REPO_URL>
   git push -u origin main
   ```
2. GitHub پر اپنی repo کھولیں → **Actions** tab پر جائیں
3. "Build APK" workflow خودکار چل جائے گا
4. مکمل ہونے کے بعد اس workflow run کو کھولیں → نیچے **Artifacts** میں `SoundBubble-debug-apk` ملے گی
5. ڈاؤن لوڈ کر کے extract کریں → اندر `app-debug.apk` ملے گی
6. یہ APK فون میں transfer کر کے انسٹال کریں (پہلے "Unknown sources" کی اجازت دینی پڑے گی)

## اہم اجازتیں جو ایپ مانگے گی
- **Display over other apps (Overlay)** — فلوٹنگ بٹن کے لیے لازمی
- **Microphone** — صرف اگر آپ خود نئی audio ریکارڈ کریں
- **Bluetooth Connect** (Android 12+) — صرف اگر Bluetooth output موڈ استعمال کرنا ہو
- **Notifications** — سروس چلنے کی اطلاع کے لیے (Android 13+)

نوٹ: Accessibility permission جان بوجھ کر شامل نہیں کی گئی کیونکہ یہ audio routing میں کسی کام کی نہیں — صرف UI automation کے لیے ہوتی ہے۔

## فولڈر structure
```
SoundBubble/
├── app/
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── assets/default_sounds/     (یہاں اپنی default audio files ڈالیں)
│       ├── java/com/soundbubble/app/
│       │   ├── MainActivity.kt        (import/record/launch/settings)
│       │   ├── FloatingService.kt     (draggable bubble + panel)
│       │   ├── AudioAdapter.kt        (audio list + play/delete)
│       │   ├── AudioRouter.kt         (speaker/Bluetooth smart routing)
│       │   └── FileImportHelper.kt    (folder import + zip extract + defaults)
│       └── res/                       (layouts, drawables, strings)
├── .github/workflows/build-apk.yml    (خودکار APK build)
├── build.gradle.kts
└── settings.gradle.kts
```

## بعد میں بہتری کے لیے آئیڈیاز
- App icon کو Play Store جیسا polish کرنا
- Audio slots کو categories میں تقسیم کرنا (greetings, taunts, وغیرہ)
- سروس کو boot کے بعد خودکار شروع کرنا (اختیاری)
