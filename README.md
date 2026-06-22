# 📋 Attendly

**Version 1.1.1** — An Android app (API 24+) for meeting attendance. The host scans participants' QR codes; each scan appends a row to a Google Sheet instantly — **no Google Cloud Console, no OAuth, no sign-in required**.

Scan records are also **saved locally on the device**, so attendance data is preserved even without an internet connection.

| Column A | Column B |
|----------|----------|
| Timestamp (`yyyy-MM-dd HH:mm:ss`) | ID (extracted QR text) |

---

## ✨ Features

- 📷 Scan QR codes via camera (square viewfinder) or pick from device gallery
- ✍️ Enter IDs manually
- 📊 Direct Google Sheets integration via Apps Script — no backend, no API keys
- 📦 Offline / local storage mode with bulk upload
- 🔢 Automatic scan numbering (Scan #1, #2…) with reset option in Settings
- 🌐 Bilingual UI — **Indonesian** (default) and **English**, switchable in Settings
- 🌙 Dark mode support
- 🔀 Configurable sheet tab name — switch events without redeploying

---

## ⚙️ How it works

The app calls a **Google Apps Script Web App** deployed inside your Google Sheet. The script runs as the Sheet owner and appends rows anonymously. No API keys or OAuth tokens are exchanged with the Android app.

```
Android app  ──POST JSON──►  Apps Script Web App  ──appendRow──►  Google Sheet
               └──────────────────────────────────────────────────► Local Storage (SQLite)
```

---

## 🚀 One-time setup (5 minutes)

### Step 1 — Prepare your Google Sheet

1. Open (or create) your Google Sheet, e.g. `https://docs.google.com/spreadsheets/d/YOUR_ID/edit`.
2. Rename or add a sheet **tab** for each event, e.g. **Acara-1**.
3. In **Row 1** of that tab add headers: `Timestamp` (A1) and `ID` (B1).  
   *(The script appends below row 1 automatically.)*

### Step 2 — Deploy the Apps Script

1. Inside the sheet click **Extensions → Apps Script**.
2. Delete any existing code and paste the contents of [`apps-script/Code.gs`](files/apps-script/Code.gs).
3. Click **Deploy → New deployment**.
4. Set **Type**: `Web App`.
5. Set **Execute as**: `Me`.
6. Set **Who has access**: `Anyone` *(or "Anyone, even anonymous")*.
7. Click **Deploy** — copy the **Web App URL** (looks like `https://script.google.com/macros/s/AKfy.../exec`).

> ⚠️ You need to re-deploy (New deployment or a new version) after every code change.

### Step 3 — Configure the Android app

1. Open the app → tap **Pengaturan** (Settings).
2. Paste the **Web App URL** into *URL Web App Apps Script*.
3. Type the **sheet tab name** exactly (e.g. `Acara-1`).
4. Tap back to save.

---

## 🛠️ Build & run

1. Open the project root in **Android Studio**.
2. Let Gradle sync.
3. Run on a physical Android device (camera required).

**Requirements:**

| | |
|---|---|
| Android Studio | Hedgehog or newer |
| Gradle | 9.4.1 |
| Android Gradle Plugin | 9.2.1 |
| compileSdk / targetSdk | 35 |
| minSdk | 24 |

---

## 🔀 Switching events

Go to **Settings** and change the **Sheet tab name** to the new event tab (e.g. `Acara-2`). The Apps Script URL stays the same.

---

## 🌐 Language

The app defaults to **Indonesian**. To switch to English:

1. Open **Pengaturan** (Settings).
2. Scroll to the **Bahasa / Language** section.
3. Select **English**.

The app restarts automatically to apply the change.

---

## 🔧 Troubleshooting

| Symptom | Fix |
|---------|-----|
| "Sheet tab not found: Acara-1" | The tab name must match the sheet tab **exactly** (case-sensitive) |
| HTTP 302 or redirect error | The Apps Script URL was not deployed with "Anyone" access — redeploy |
| `Apps Script error` in the app | Open Apps Script editor → View → Executions to see the error log |
| App says "configure sheet" | Paste the Web App URL in Settings and tap back to save |
| Language not changing | The app restarts automatically — wait a moment after selecting a language |

---

## 📦 Dependencies

| Library | Version | Purpose |
|---------|---------|---------|
| `zxing-android-embedded` | 4.3.0 | QR code camera scanner |
| `mlkit:barcode-scanning` | 17.3.0 | QR decode from gallery image |
| `okhttp3` | 4.12.0 | HTTP POST to Apps Script |
| `kotlinx-coroutines-android` | 1.7.3 | Background threading |
| `androidx.recyclerview` | 1.3.2 | Local records table view |
| `lifecycle-runtime-ktx` | 2.7.0 | Coroutine lifecycle scope |

> 💾 Local storage uses Android's built-in **SQLite** via `SQLiteOpenHelper` — no Room dependency.

---

## 📝 Changelog

### 🆕 1.1.1
- 📷 **Square viewfinder** — camera scanner now uses a square framing box
- 🖼️ **Gallery QR picker** — long-press the Scan button to decode a QR code from a local image file
- 🔢 **Scan numbering** — every scan is labelled Scan #1, Scan #2… on the main screen
- ⚙️ **Reset scan number** — new button in Settings → Nomor Scan resets the counter to 0
- 🛠️ Gradle deprecation warnings resolved (Gradle 10 compatibility)

### 1.1.0
- 🌐 Bilingual support: Indonesian (default) + English, switchable in Settings
- 🔗 "See help" inline link in Settings → Sheet section
- 🔗 Clickable URL in Help → Step 3
- ✅ All UI strings fully localized (no hardcoded English text)
- 📱 targetSdk bumped to 35 (Play Store requirement)
- ⚡ Release build: minify + resource shrinking enabled
- 🔒 `allowBackup` disabled for security
- ℹ️ Version display localized in Info screen

### 1.0.1
- 🎉 Initial release

---

## 📄 License

MIT License — free to use, modify, and distribute.
