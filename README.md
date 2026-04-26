# Attendly

An Android app (API 24+) for meeting attendance. The host scans participants' QR codes; each scan appends a row to a Google Sheet instantly — **no Google Cloud Console, no OAuth, no sign-in required**.

Scan records are also **saved locally on the device**, so attendance data is preserved even without an internet connection.

| Column A | Column B |
|----------|----------|
| Timestamp (`yyyy-MM-dd HH:mm:ss`) | ID (extracted QR text) |

---

## How it works

The app calls a **Google Apps Script Web App** deployed inside your Google Sheet. The script runs as the Sheet owner and appends rows anonymously. No API keys or OAuth tokens are exchanged with the Android app.

```
Android app  ──POST JSON──►  Apps Script Web App  ──appendRow──►  Google Sheet
               └──────────────────────────────────────────────────► Local Storage (Room DB)
```

---

## One-time setup (5 minutes)

### Step 1 — Prepare your Google Sheet

1. Open (or create) your Google Sheet, e.g. `https://docs.google.com/spreadsheets/d/YOUR_ID/edit`.
2. Rename or add a sheet **tab** for each event, e.g. **Acara-1**.
3. In **Row 1** of that tab add headers: `Timestamp` (A1) and `ID` (B1).  
   *(The script appends below row 1 automatically.)*

### Step 2 — Deploy the Apps Script

1. Inside the sheet click **Extensions → Apps Script**.
2. Delete any existing code and paste the contents of [`apps-script/Code.gs`](apps-script/Code.gs).
3. Click **Deploy → New deployment**.
4. Set **Type**: `Web App`.
5. Set **Execute as**: `Me`.
6. Set **Who has access**: `Anyone` *(or "Anyone, even anonymous")*.
7. Click **Deploy** — copy the **Web App URL** (looks like `https://script.google.com/macros/s/AKfy.../exec`).

> You need to re-deploy (New deployment or a new version) after every code change.

### Step 3 — Configure the Android app

1. Open the app → tap **Settings**.
2. Paste the **Web App URL** into *Apps Script Web App URL*.
3. Type the **sheet tab name** exactly (e.g. `Acara-1`).
4. Tap **Save Settings**.

---

## Build & run

1. Open `qr-scanner/` in **Android Studio**.
2. Let Gradle sync.
3. Run on a physical Android device (camera required).

---

## Switching events

Just go to **Settings** and change the **Sheet tab name** to the new event tab (e.g. `Acara-2`). The Apps Script URL stays the same.

---

## Troubleshooting

| Symptom | Fix |
|---------|-----|
| "Sheet tab not found: Acara-1" | The tab name in the app must match the sheet tab **exactly** (case-sensitive) |
| HTTP 302 or redirect error | The Apps Script URL was not deployed with "Anyone" access — redeploy |
| `Apps Script error` in the app | Open Apps Script editor → View → Executions to see the error log |
| App says "configure sheet" | Paste the Web App URL in Settings and tap Save |

---

## Dependencies

| Library | Purpose |
|---------|---------|
| `zxing-android-embedded:4.3.0` | QR code camera scanner |
| `okhttp3:4.12.0` | HTTP POST to Apps Script |
| `kotlinx-coroutines-android:1.7.3` | Background threading |
| `room:2.x` | Local database for offline scan storage |
