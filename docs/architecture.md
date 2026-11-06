# FoodConnect Architecture

## System Overview

FoodConnect follows a client-server architecture with a native Android frontend communicating with a Spring Boot REST API backend backed by MySQL.

```mermaid
flowchart TD
    subgraph Android["Android App (Kotlin/Compose)"]
        UI["Compose UI Screens"]
        VM["ViewModels"]
        Repo["Repositories"]
        Retrofit["Retrofit + OkHttp"]
    end

    subgraph Backend["Spring Boot Backend (Java 21)"]
        Controllers["REST Controllers"]
        Services["Service Layer"]
        JPA["JPA Repositories"]
        Security["Spring Security + JWT"]
    end

    subgraph External["External Services"]
        FCM["Firebase Cloud Messaging"]
        Maps["Google Maps SDK"]
        MySQL["MySQL 8.0"]
    end

    UI --> VM --> Repo --> Retrofit
    Retrofit -->|"HTTP/REST"| Controllers
    Controllers --> Security
    Controllers --> Services
    Services --> JPA --> MySQL
    Services -->|"Push"| FCM
    FCM -->|"Notification"| Android
    Maps -->|"Map tiles, navigation"| UI
```

## Data Flow

```
Android UI → ViewModel → Repository → Retrofit → REST Controller → Service → JPA Repository → MySQL
```

Each layer has a clear responsibility:

| Layer | Responsibility |
|---|---|
| **Compose UI** | Render state, collect user input |
| **ViewModel** | Hold UI state, orchestrate use cases |
| **Repository** | Abstract network calls, map API responses to `ApiResult` |
| **Retrofit** | HTTP transport, JSON serialization |
| **Controller** | Request routing, input deserialization |
| **Service** | Business logic, authorization, validation |
| **JPA Repository** | Database queries, atomic updates |
| **MySQL** | Persistent storage |

## Android Architecture

### MVVM + Unidirectional Data Flow

```mermaid
flowchart LR
    User["User Action"] --> VM["ViewModel"]
    VM -->|"Update"| State["StateFlow&lt;UiState&gt;"]
    State -->|"Collect"| UI["Compose UI"]
    VM -->|"Call"| Repo["Repository"]
    Repo -->|"ApiResult"| VM
```

### Package Structure

| Package | Contents |
|---|---|
| `app/` | `FoodConnectNavHost`, `SplashScreen`, routes |
| `core/network/` | `NetworkModule` (Hilt), `AuthInterceptor` |
| `core/security/` | `SessionManager` (DataStore) |
| `core/notifications/` | `FCMService` |
| `core/ui/theme/` | Material 3 theme, colors, typography |
| `data/model/` | DTOs matching backend |
| `data/remote/` | `FoodConnectApi` (Retrofit interface) |
| `data/repository/` | `AuthRepository`, `DonationRepository`, etc. |
| `feature/auth/` | Login, Register, Role Selection |
| `feature/restaurant/` | Dashboard, Create Donation |
| `feature/ngo/` | Dashboard (Nearby + Pickups tabs) |
| `feature/donations/` | Donation Detail |
| `feature/navigation/` | Pickup Navigation (Google Maps) |
| `feature/notifications/` | Notification List |

## Backend Architecture

### Layered Architecture

```mermaid
flowchart TD
    Client["Android Client"] -->|"HTTP"| Controller
    Controller -->|"DTO"| Service
    Service -->|"Entity"| Repository
    Repository -->|"SQL"| DB["MySQL"]
    Service -->|"Push"| FCM["Firebase Admin SDK"]
    
    subgraph Cross-cutting
        Security["JWT Auth Filter"]
        Validation["Bean Validation"]
        ExHandler["Global Exception Handler"]
    end
    
    Controller -.-> Security
    Controller -.-> Validation
    Controller -.-> ExHandler
```

### Package Structure

| Package | Contents |
|---|---|
| `config/` | `SecurityConfig`, `AppConfig`, `OpenApiConfig` |
| `security/` | `JwtTokenProvider`, `JwtAuthenticationFilter`, `CustomUserDetails` |
| `controller/` | `AuthController`, `DonationController`, profile controllers |
| `service/` | `AuthService`, `DonationService`, `ProfileService`, `NotificationService` |
| `repository/` | JPA repositories with Haversine queries |
| `entity/` | JPA entities + `enums/` sub-package |
| `dto/` | Request/response DTOs with Bean Validation |
| `mapper/` | Entity ↔ DTO mapping components |
| `exception/` | Custom exceptions + `GlobalExceptionHandler` |

## Security Architecture

```mermaid
sequenceDiagram
    participant App as Android App
    participant API as REST API
    participant JWT as JWT Filter
    participant SC as SecurityContext

    App->>API: POST /auth/login {email, password}
    API->>API: Validate credentials (BCrypt)
    API->>App: {token, user, role}
    App->>App: Store token in DataStore

    App->>API: GET /donations/nearby (Authorization: Bearer <token>)
    JWT->>JWT: Extract & validate token
    JWT->>SC: Set Authentication
    API->>App: [donations...]
```

- **Password hashing:** BCrypt (Spring Security)
- **Token format:** JWT with user ID, email, role in claims
- **Token expiry:** 24 hours (configurable)
- **Authorization:** Role-based (`RESTAURANT`, `NGO`) enforced at service layer
- **Token transport:** `Authorization: Bearer <token>` header
- **Interceptor:** `AuthInterceptor` auto-attaches token from DataStore

## Donation State Machine

```mermaid
stateDiagram-v2
    [*] --> AVAILABLE: Restaurant creates listing
    AVAILABLE --> ACCEPTED: NGO accepts (atomic)
    AVAILABLE --> EXPIRED: Pickup deadline passes
    AVAILABLE --> CANCELLED: Restaurant cancels
    ACCEPTED --> COLLECTED: NGO picks up food
    ACCEPTED --> CANCELLED: Restaurant cancels
    COLLECTED --> COMPLETED: NGO confirms delivery
    EXPIRED --> [*]
    CANCELLED --> [*]
    COMPLETED --> [*]
```

## Location & Discovery

- NGO location obtained via **Fused Location Provider**
- Nearby donations queried using **Haversine formula** (native SQL)
- Default search radius: **10 km** (configurable via `app.notification-radius-km`)
- Push notifications sent to NGOs within radius when a new donation is created

## Notification Flow

```mermaid
sequenceDiagram
    participant R as Restaurant
    participant API as Backend
    participant FCM as Firebase CM
    participant NGO as NGO Device

    R->>API: POST /donations (create)
    API->>API: Save donation
    API->>API: Find NGOs within 10km
    API->>FCM: Send to matched device tokens
    FCM->>NGO: Push notification
    NGO->>NGO: Show local notification
    NGO->>API: GET /donations/{id}
```
