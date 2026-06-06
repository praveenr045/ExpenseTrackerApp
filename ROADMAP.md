# ExpenseTracker — Pending Features & Fixes

> Save this file in your project root. Paste it at the start of any new chat
> with Claude so it knows exactly where we left off.

---

## 🔧 Pending — To Be Implemented

### 1. Home screen Widget — Not appearing in widget picker
- **Problem**: Widget not showing up when long-pressing phone home screen.
- **Checklist**:
  - `res/xml/expense_widget_info.xml` exists
  - `res/layout/widget_expense.xml` exists
  - `res/drawable/widget_background.xml` exists
  - `AndroidManifest.xml` has `<receiver>` block for `ExpenseWidget`
  - `res/values/strings.xml` has `widget_description` string entry
  - After all files confirmed: Uninstall app → Reinstall → Long-press home screen
- **Files**: `widget/ExpenseWidget.kt`, widget XML files, `AndroidManifest.xml`

### 2. Notification tap — Open app and highlight the auto-added expense
- **Problem**: When a bank SMS is received and an expense is auto-added,
  the notification appears but tapping it just opens the app without any
  indication of which expense was just added.
- **Fix needed**:
  - Pass the newly inserted expense `id` (returned by `dao.insertExpense()`)
    as an Intent extra in the notification's `PendingIntent`
  - `MainActivity` reads the expense `id` from the incoming Intent
  - Navigate directly to the expense in `ExpensesScreen` or `HomeScreen`
  - Highlight/animate the matching expense card (colored border + scroll to it)
- **Files**: `sms/SmsReceiver.kt`, `MainActivity.kt`, `ui/screens/HomeScreen.kt`,
  `ui/screens/ExpensesScreen.kt`, `ui/viewmodel/ExpenseViewModel.kt`

### 3. Cloud Backup
- **Chosen solution**: Firebase (Google) — free tier is sufficient for personal use,
  integrates cleanly with Android, and handles auth + Firestore DB + auto-sync.
- **What needs to be built**:
  - **Google Sign-In**: Let user log in with their Google account (Firebase Auth)
  - **Firestore sync**: Mirror all Room DB expenses to Firestore collection
    under the user's UID so data is per-account and private
  - **Sync strategy**:
    - Upload: whenever an expense is added/edited/deleted locally → push to Firestore
    - Download: on first login or fresh install → pull all expenses from Firestore into Room
    - Conflict resolution: use `timestamp` field — latest write wins
  - **Settings screen** (new screen needed):
    - "Sign in with Google" button
    - Sync status indicator (last synced time)
    - "Sync now" manual trigger button
    - "Sign out" option
  - **Offline support**: Room DB remains the source of truth; Firestore is a mirror.
    App works fully offline; syncs when connectivity is restored.
- **New dependencies needed**:
  - `com.google.firebase:firebase-auth-ktx`
  - `com.google.firebase:firebase-firestore-ktx`
  - `com.google.android.gms:play-services-auth`
  - Firebase BOM for version management
- **New files needed**:
  - `auth/AuthManager.kt` — Google Sign-In flow
  - `sync/FirestoreSync.kt` — upload/download logic
  - `ui/screens/SettingsScreen.kt` — auth + sync UI
- **Setup required** (one-time, before coding):
  - Create a Firebase project at console.firebase.google.com
  - Add an Android app (use package name `com.personal.expensetracker`)
  - Download `google-services.json` → place in `app/` folder
  - Add `google-services` plugin to both `build.gradle.kts` files
- **Files to modify**: `di/AppModule.kt`, `MainScreen.kt` (add Settings tab),
  `data/repository/ExpenseRepository.kt` (trigger sync on write)

### 4. Production-Ready APK & Play Store Release
- **Step A — Prepare the app**:
  - Add Privacy Policy URL (required by Play Store for apps with SMS permission)
  - Add an About screen with version info and privacy policy link
  - Review and finalise `AndroidManifest.xml` permissions — remove any unused ones
  - Set `isMinifyEnabled = true` and configure ProGuard rules for release build
  - Add missing ProGuard rules for Room, Hilt, MPAndroidChart, Coroutines

- **Step B — Create a signing keystore**:
  ```
  Android Studio → Build → Generate Signed Bundle / APK
  → Create new keystore (.jks file)
  → Fill in alias, password, validity (25 years), your name/org
  → Save keystore file somewhere SAFE (losing it = can never update the app)
  ```
  - Store keystore password in `local.properties` (never commit to git):
    ```
    KEYSTORE_PATH=../keystore/expense_tracker.jks
    KEYSTORE_PASSWORD=yourpassword
    KEY_ALIAS=expensetracker
    KEY_PASSWORD=yourpassword
    ```
  - Reference in `app/build.gradle.kts`:
    ```kotlin
    signingConfigs {
        create("release") {
            storeFile = file(properties["KEYSTORE_PATH"]!!)
            storePassword = properties["KEYSTORE_PASSWORD"]!!.toString()
            keyAlias = properties["KEY_ALIAS"]!!.toString()
            keyPassword = properties["KEY_PASSWORD"]!!.toString()
        }
    }
    ```

- **Step C — Build the release AAB** (Play Store prefers AAB over APK):
  ```
  Build → Generate Signed Bundle / APK → Android App Bundle → release
  ```
  Output: `app/release/app-release.aab`

- **Step D — Play Store setup** (one-time, costs $25 registration fee):
  - Go to play.google.com/console → Create account → Pay $25
  - Create new app → Fill in title, description (max 4000 chars), short description
  - Upload screenshots (phone: min 2, max 8) — take from your device
  - Upload a feature graphic (1024×500 px banner image)
  - Set content rating (complete questionnaire — this app is "Everyone")
  - Set up Privacy Policy (required because of SMS permission):
    - Host a simple privacy policy page (free: GitHub Pages or Google Sites)
    - Paste the URL in Play Console
  - Upload the `.aab` file to the Internal Testing track first
  - Test on your own device via Internal Testing before going to Production

- **Step E — SMS permission declaration** (critical for Play Store approval):
  - Apps using `READ_SMS` / `RECEIVE_SMS` require **special approval** from Google
  - You must submit a "Permissions Declaration Form" explaining why SMS is needed
  - Justification to write: *"The app automatically reads bank transaction SMS
    messages to record expenses without manual input. No SMS content is stored
    on servers or shared with third parties."*
  - Google may take 1–3 weeks to review this declaration

- **Files to create/modify**:
  - `app/build.gradle.kts` — signingConfig, minify, proguard
  - `proguard-rules.pro` — keep rules for all libraries
  - `ui/screens/AboutScreen.kt` — version, privacy policy link
  - `MainScreen.kt` — add Settings/About to navigation

---

## 📁 Key Files Reference

| What | File path in project |
|---|---|
| SMS parsing logic | `sms/SmsParser.kt` |
| SMS receiver | `sms/SmsReceiver.kt` |
| Voice parser | `voice/VoiceParser.kt` |
| Database schema | `data/model/Expense.kt` |
| DAO queries | `data/db/ExpenseDao.kt` |
| Repository | `data/repository/ExpenseRepository.kt` |
| DI module | `di/AppModule.kt` |
| ViewModel | `ui/viewmodel/ExpenseViewModel.kt` |
| Navigation | `ui/screens/MainScreen.kt` |
| Home screen | `ui/screens/HomeScreen.kt` |
| Expenses list | `ui/screens/ExpensesScreen.kt` |
| Analytics | `ui/screens/AnalyticsScreen.kt` |
| Add/Edit sheet | `ui/components/AddExpenseSheet.kt` |
| Expense card | `ui/components/ExpenseItem.kt` |
| Export sheet | `ui/components/ExportSheet.kt` |
| Widget | `widget/ExpenseWidget.kt` |
| App theme | `ui/theme/Theme.kt` |
| Manifest | `app/src/main/AndroidManifest.xml` |
| Gradle deps | `app/build.gradle.kts` |
| Gradle versions | `gradle/libs.versions.toml` |

---

## 🏦 Bank SMS Formats Tested

| Bank | Sender IDs | Pattern |
|---|---|---|
| ICICI | ICICI, ICICIB, ICICIBK | `; NAME credited` for UPI peers; `at MERCHANT on` for shops |
| HDFC | HDFCBK, HDFCBANK, HDFC | `debited from A/c at MERCHANT on` |
| Canara | CNRB, CBSSMS, CANBK, CANBNK, CNBNK, CANBNKUPI | `transferred to NAME on` |

---

## 🛠 Tech Stack

- **Language**: Kotlin
- **UI**: Jetpack Compose + Material 3
- **DB**: Room (local, no cloud)
- **DI**: Hilt
- **Charts**: MPAndroidChart
- **Navigation**: Jetpack Navigation Compose
- **Cloud** (pending): Firebase Auth + Firestore
- **Min SDK**: 26 (Android 8.0) · **Target SDK**: 35 (Android 15)

---

## ⚠️ Important Notes for Play Store

- **SMS permission review**: Google manually reviews apps that use `READ_SMS`.
  Budget 1–3 weeks for approval. Have your justification text ready.
- **Keystore backup**: Store your `.jks` keystore file in a safe place
  (cloud drive, external drive). If lost, you can NEVER push updates to the
  same Play Store listing — you'd have to publish as a brand new app.
- **Privacy Policy**: Mandatory. Must be hosted at a public URL.
  Can be a simple one-page Google Site or GitHub Pages site.
- **$25 registration**: One-time fee for a Google Play Developer account.
  Covers unlimited app publications forever.
