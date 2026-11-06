# 🍽️ FoodConnect

**Connecting surplus food from restaurants to NGOs — reducing waste, feeding communities.**

FoodConnect is a full-stack Android application that bridges restaurants with surplus food and NGOs who collect and distribute it to people in need. The platform provides real-time notifications, location-based discovery, and turn-by-turn navigation for efficient food rescue.

---

## ✨ Key Features

| Feature | Description |
|---|---|
| **Role-based Access** | Separate flows for Restaurants and NGOs |
| **JWT Authentication** | Secure Spring Security + BCrypt + JWT tokens |
| **Donation Management** | Full lifecycle: Available → Accepted → Collected → Completed |
| **Race-safe Acceptance** | Atomic DB updates prevent double-claiming |
| **Location Discovery** | Haversine-based nearby donation search (configurable radius, default 10 km) |
| **Push Notifications** | Firebase Cloud Messaging alerts NGOs of nearby donations |
| **Google Maps Navigation** | In-app map with markers + launch Google Maps turn-by-turn |
| **Scheduled Expiration** | Backend auto-expires donations past their pickup deadline |

---

## 🏗️ Tech Stack

### Android Frontend
- **Kotlin** · Jetpack Compose · Material 3
- **MVVM** architecture · Navigation Compose
- **Hilt** dependency injection · Kotlin Coroutines & Flow
- **Retrofit** + OkHttp · Gson serialization
- **Google Maps SDK** · Fused Location Provider
- **Firebase Cloud Messaging** · Jetpack DataStore
- **Coil** image loading

### Backend
- **Java 21** · Spring Boot 3.3.5 · Maven
- **Spring Security** · JWT (jjwt 0.12.6)
- **Spring Data JPA** · Hibernate · Flyway migrations
- **MySQL 8.0** · Bean Validation
- **Firebase Admin SDK** · SpringDoc OpenAPI
- **JUnit 5** · H2 (test)

---

## 📁 Project Structure

```
FoodConnect/
├── android-app/          # Native Android application
│   ├── app/src/main/
│   │   ├── java/com/foodconnect/
│   │   │   ├── app/              # Navigation, splash
│   │   │   ├── core/network/     # Retrofit, interceptors
│   │   │   ├── core/security/    # Session management
│   │   │   ├── core/notifications/ # FCM service
│   │   │   ├── core/ui/theme/    # Material 3 theme
│   │   │   ├── data/model/       # DTOs and enums
│   │   │   ├── data/remote/      # API interface
│   │   │   ├── data/repository/  # Repositories
│   │   │   ├── feature/auth/     # Login, register
│   │   │   ├── feature/restaurant/ # Restaurant dashboard
│   │   │   ├── feature/ngo/      # NGO dashboard
│   │   │   ├── feature/donations/ # Donation details
│   │   │   ├── feature/navigation/ # Pickup navigation
│   │   │   └── feature/notifications/ # Notification list
│   │   └── res/
│   └── build.gradle.kts
├── backend/              # Spring Boot REST API
│   ├── src/main/java/com/foodconnect/
│   │   ├── config/       # Security, CORS, OpenAPI config
│   │   ├── security/     # JWT, filters, user details
│   │   ├── controller/   # REST controllers
│   │   ├── service/      # Business logic
│   │   ├── repository/   # JPA repositories
│   │   ├── entity/       # JPA entities + enums
│   │   ├── dto/          # Request/response DTOs
│   │   ├── mapper/       # Entity ↔ DTO mappers
│   │   ├── exception/    # Custom exceptions + handler
│   │   └── notification/ # FCM integration
│   ├── src/main/resources/
│   │   ├── application.yml
│   │   └── db/migration/ # Flyway V1–V7
│   └── pom.xml
├── docs/                 # Documentation
└── README.md
```

---

## 🚀 Quick Start

### Prerequisites
- Java 21+ and Maven 3.9+
- MySQL 8.0+
- Android Studio (Hedgehog or later) with Android SDK 35
- A Firebase project (for Cloud Messaging)
- Google Maps API key (for maps)

### 1. Database Setup

```sql
CREATE DATABASE foodconnect CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 2. Backend

```bash
cd backend

# Set environment variables (see .env.example)
export DB_URL=jdbc:mysql://localhost:3306/foodconnect
export DB_USERNAME=root
export DB_PASSWORD=your_password
export JWT_SECRET=your-256-bit-secret-key-at-least-32-characters-long

# Run
mvn spring-boot:run
```

The API will be available at `http://localhost:8080/api/v1/`.  
Swagger UI: `http://localhost:8080/swagger-ui.html`

### 3. Android App

```bash
cd android-app

# Ensure local.properties has your SDK path:
# sdk.dir=C:\\Users\\username\\AppData\\Local\\Android\\Sdk

# Add your google-services.json to app/ (from Firebase Console)

# Optional: set Google Maps key in gradle.properties:
# GOOGLE_MAPS_API_KEY=AIza...

# Build
./gradlew assembleDebug
```

> **Emulator note:** The app uses `10.0.2.2:8080` to reach the host machine's localhost from the Android emulator.

### 4. Run Tests

```bash
cd backend
mvn test -Dspring.profiles.active=test    # 21 tests, uses H2 in-memory DB
```

---

## 🔄 Donation Lifecycle

```
AVAILABLE → ACCEPTED → COLLECTED → COMPLETED
    ↓           ↓
 EXPIRED    CANCELLED
```

| Status | Meaning |
|---|---|
| `AVAILABLE` | Restaurant posted, visible to NGOs |
| `ACCEPTED` | An NGO claimed it (atomic, race-safe) |
| `COLLECTED` | NGO picked up the food |
| `COMPLETED` | Delivery finished |
| `EXPIRED` | Pickup deadline passed (auto-set by scheduler) |
| `CANCELLED` | Restaurant cancelled the listing |

---

## 🔐 Environment Variables

| Variable | Required | Description |
|---|---|---|
| `DB_URL` | Yes | MySQL JDBC URL |
| `DB_USERNAME` | Yes | MySQL username |
| `DB_PASSWORD` | Yes | MySQL password |
| `JWT_SECRET` | Yes | JWT signing key (≥32 chars) |
| `FIREBASE_CREDENTIALS_PATH` | No | Path to Firebase service account JSON |
| `GOOGLE_MAPS_API_KEY` | No | Google Maps API key |

---

## 📄 Documentation

- [Architecture](docs/architecture.md)
- [Database Schema](docs/database-schema.md)
- [API Specification](docs/api-specification.md)
- [Setup Guide](docs/setup-guide.md)

---

## 📜 License

This project is licensed under the MIT License.
"# FlashFood" 
