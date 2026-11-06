# FoodConnect Setup Guide

Step-by-step guide to set up the FoodConnect development environment.

---

## Prerequisites

| Tool | Version | Purpose |
|---|---|---|
| **Java JDK** | 21+ | Backend compilation |
| **Maven** | 3.9+ | Backend dependency management |
| **MySQL** | 8.0+ | Database |
| **Android Studio** | Hedgehog+ | Android development |
| **Android SDK** | API 35 | Build target |
| **Git** | Latest | Version control |

---

## 1. Clone the Repository

```bash
git clone <repo-url>
cd FoodConnect
```

---

## 2. Database Setup

### Start MySQL

Ensure MySQL 8.0 is running. On Windows:
```bash
net start MySQL80
```

### Create Database

```sql
mysql -u root -p
CREATE DATABASE foodconnect CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
EXIT;
```

---

## 3. Backend Setup

### Environment Variables

Copy the example and fill in your values:

```bash
cd backend
cp .env.example .env
```

**Required variables:**

| Variable | Example Value |
|---|---|
| `DB_URL` | `jdbc:mysql://localhost:3306/foodconnect` |
| `DB_USERNAME` | `root` |
| `DB_PASSWORD` | `your_mysql_password` |
| `JWT_SECRET` | `my-super-secret-jwt-key-at-least-32-chars` |

**Optional variables:**

| Variable | Description |
|---|---|
| `FIREBASE_CREDENTIALS_PATH` | Path to Firebase service account JSON |
| `GOOGLE_MAPS_API_KEY` | Google Maps API key |

### Set Variables (Windows PowerShell)

```powershell
$env:DB_URL = "jdbc:mysql://localhost:3306/foodconnect"
$env:DB_USERNAME = "root"
$env:DB_PASSWORD = "your_password"
$env:JWT_SECRET = "my-super-secret-jwt-key-at-least-32-characters-long"
```

### Run Backend

```bash
cd backend
mvn spring-boot:run
```

On first run, Flyway will automatically create all 7 tables.

**Verify:** Open `http://localhost:8080/actuator/health` — should return `{"status":"UP"}`

**Swagger UI:** `http://localhost:8080/swagger-ui.html`

### Run Backend Tests

```bash
mvn test -Dspring.profiles.active=test
```

Tests use H2 in-memory database (no MySQL needed). All 21 tests should pass.

---

## 4. Android Setup

### SDK Configuration

Create `android-app/local.properties`:
```properties
sdk.dir=C:\\Users\\<username>\\AppData\\Local\\Android\\Sdk
```

### Google Services (Firebase)

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Create a new project (or use existing)
3. Add an Android app with package name `com.foodconnect`
4. Download `google-services.json`
5. Place it in `android-app/app/google-services.json`

> **Note:** A placeholder `google-services.json` is included for compilation. Replace it with your real Firebase config before testing notifications.

### Google Maps API Key (Optional)

1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Enable **Maps SDK for Android** and **Directions API**
3. Create an API key
4. Add to `android-app/gradle.properties`:
   ```properties
   GOOGLE_MAPS_API_KEY=AIzaSy...your-key...
   ```

### Build

```bash
cd android-app
.\gradlew.bat assembleDebug
```

The APK will be at `app/build/outputs/apk/debug/app-debug.apk`.

### Install on Emulator

```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

---

## 5. Running End-to-End

### Start Everything

1. **Start MySQL** (if not running)
2. **Start Backend:** `cd backend && mvn spring-boot:run`
3. **Run Android app** on emulator from Android Studio

### Network Configuration

| Device | Backend URL |
|---|---|
| Android Emulator | `http://10.0.2.2:8080/api/v1/` (configured by default) |
| Physical Device | `http://<your-pc-ip>:8080/api/v1/` (update `BuildConfig.BASE_URL`) |

> The default `BASE_URL` in `app/build.gradle.kts` uses `10.0.2.2` which maps to host localhost from the Android emulator.

### Test Flow

1. **Register a Restaurant** — fill in name, email, password, restaurant name, address
2. **Create a Donation** — add food name, quantity, address
3. **Register an NGO** (different email) — fill in name, email, password, NGO name
4. **View Nearby Donations** — enable location permission, see the donation
5. **Accept the Donation** — NGO claims it atomically
6. **Navigate to Pickup** — see map with restaurant marker, launch Google Maps
7. **Mark Collected** — NGO confirms pickup
8. **Mark Completed** — NGO confirms delivery

---

## 6. Firebase Cloud Messaging Setup

To enable push notifications:

### Backend (Firebase Admin SDK)

1. In Firebase Console → Project Settings → Service Accounts
2. Click "Generate new private key" → downloads a JSON file
3. Set `FIREBASE_CREDENTIALS_PATH` to the file path

### Android (FCM)

The `google-services.json` file (step 4) handles FCM registration automatically. The `FCMService` class in the app:
- Registers device tokens with the backend on new token
- Displays local notifications when push messages arrive

---

## Troubleshooting

| Issue | Solution |
|---|---|
| `ANDROID_HOME not set` | Create `local.properties` with `sdk.dir` path |
| `google-services.json not found` | Place Firebase config in `android-app/app/` |
| `Connection refused` on emulator | Ensure backend is running on port 8080 |
| `Manifest merger failed` | Ensure `GOOGLE_MAPS_API_KEY` placeholder exists in `gradle.properties` |
| `Database access denied` | Check MySQL credentials in env vars |
| `Flyway migration failed` | Ensure MySQL database `foodconnect` exists and is empty |
| Backend tests fail | Run with `-Dspring.profiles.active=test` to use H2 |
