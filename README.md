# Pasiflonet Mobile (Kotlin) — TDLib Client + Media3

## מה יש בפרויקט הזה
- חיבור לטלגרם דרך **TDLib Client בלבד** (בלי Bot API).
- TDLib כ-**AAR מקומי** ב-`app/libs/td-1.8.56.aar`.
- עריכת מדיה **בלי FFmpegKit**:
  - תמונות: טשטוש מלבנים + לוגו/Watermark (Bitmap).
  - וידאו: Export דרך **Media3 Transformer** עם Overlay (Watermark) בלבד.
    - אין כרגע Blur rectangles לוידאו כי Media3 לא מספק אפקט מובנה לטשטוש אזורים שרירותיים כמו FFmpeg.
    - יש OverlayEffect/BitmapOverlay לפי ה-API.

## תנאי חובה של Telegram
TDLib חייב `API_ID` ו-`API_HASH` (לא Bot API).  
אפשר לספק אותם בשתי דרכים:

1) מומלץ ל-CI: ב-`~/.gradle/gradle.properties`:
```
TELEGRAM_API_ID=123456
TELEGRAM_API_HASH=0123456789abcdef0123456789abcdef
```

2) דרך מסך **Settings** באפליקציה (נשמר ב-DataStore).

## Termux — Bootstrap + Build (פקודה אחת)
בתוך תיקיית הפרויקט (root):
```bash
bash scripts/termux_bootstrap_build.sh
```

הסקריפט:
- יוצר `app/libs/`
- מוריד את TDLib AAR ישירות מהקישור שלך
- מריץ `./gradlew :app:assembleDebug --no-daemon`

## הערה חשובה לגבי Android SDK ב-Termux
בנייה של APK דורשת Android SDK (platforms/build-tools) ו-JDK 17.
ב-Termux אפשר:
- או להתקין SDK מקומית (commandline-tools) ולכוון `sdk.dir` ב-`local.properties`
- או לבצע build ב-GitHub Actions / מחשב עם Android SDK.

## Git push
```bash
git init
git add .
git commit -m "Pasiflonet Mobile initial"
git branch -M main
git remote add origin https://github.com/<USER>/<REPO>.git
git push -u origin main
```

אם GitHub מבקש Token:
- צור Personal Access Token (classic) עם `repo`
- ואז השתמש בכתובת:
  `https://<TOKEN>@github.com/<USER>/<REPO>.git`
