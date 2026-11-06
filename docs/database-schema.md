# FoodConnect Database Schema

## Overview

FoodConnect uses **MySQL 8.0** with schema managed by **Flyway** migrations (V1–V7). All tables use `BIGINT AUTO_INCREMENT` primary keys, `TIMESTAMP` columns for audit trails, and `ENUM` types for constrained values.

## Entity Relationship Diagram

```mermaid
erDiagram
    users ||--o| restaurant_profiles : "has (if RESTAURANT)"
    users ||--o| ngo_profiles : "has (if NGO)"
    users ||--o{ device_tokens : "registers"
    users ||--o{ notifications : "receives"
    restaurant_profiles ||--o{ donations : "creates"
    ngo_profiles ||--o{ donation_claims : "makes"
    donations ||--o| donation_claims : "claimed by"
    donations ||--o{ notifications : "triggers"

    users {
        BIGINT id PK
        VARCHAR name
        VARCHAR email UK
        VARCHAR password_hash
        ENUM role
        VARCHAR phone
        ENUM account_status
        TIMESTAMP created_at
        TIMESTAMP updated_at
    }

    restaurant_profiles {
        BIGINT id PK
        BIGINT user_id FK,UK
        VARCHAR restaurant_name
        TEXT address
        DECIMAL latitude
        DECIMAL longitude
        VARCHAR profile_image_url
        ENUM verification_status
        TIMESTAMP created_at
        TIMESTAMP updated_at
    }

    ngo_profiles {
        BIGINT id PK
        BIGINT user_id FK,UK
        VARCHAR ngo_name
        TEXT address
        DECIMAL latitude
        DECIMAL longitude
        VARCHAR registration_number
        VARCHAR profile_image_url
        ENUM verification_status
        TIMESTAMP created_at
        TIMESTAMP updated_at
    }

    donations {
        BIGINT id PK
        BIGINT restaurant_id FK
        VARCHAR food_name
        TEXT description
        ENUM food_category
        DECIMAL quantity
        ENUM quantity_unit
        VARCHAR image_url
        TIMESTAMP prepared_at
        TIMESTAMP available_until
        TIMESTAMP pickup_deadline
        TEXT storage_instructions
        DECIMAL latitude
        DECIMAL longitude
        TEXT address
        ENUM status
        TIMESTAMP created_at
        TIMESTAMP updated_at
    }

    donation_claims {
        BIGINT id PK
        BIGINT donation_id FK,UK
        BIGINT ngo_id FK
        TIMESTAMP accepted_at
        ENUM pickup_status
        TIMESTAMP collected_at
        TIMESTAMP completed_at
        TEXT completion_notes
        TIMESTAMP created_at
        TIMESTAMP updated_at
    }

    device_tokens {
        BIGINT id PK
        BIGINT user_id FK
        VARCHAR fcm_token
        VARCHAR platform
        BOOLEAN active
        TIMESTAMP created_at
        TIMESTAMP updated_at
    }

    notifications {
        BIGINT id PK
        BIGINT user_id FK
        BIGINT donation_id FK
        ENUM notification_type
        VARCHAR title
        TEXT message
        BOOLEAN is_read
        TIMESTAMP created_at
    }
```

## Tables

### `users` (V1)

| Column | Type | Constraints | Description |
|---|---|---|---|
| `id` | `BIGINT` | PK, AUTO_INCREMENT | |
| `name` | `VARCHAR(255)` | NOT NULL | Full name |
| `email` | `VARCHAR(255)` | NOT NULL, UNIQUE | Login identifier |
| `password_hash` | `VARCHAR(255)` | NOT NULL | BCrypt hash |
| `role` | `ENUM('RESTAURANT','NGO')` | NOT NULL | User type |
| `phone` | `VARCHAR(20)` | | Contact number |
| `account_status` | `ENUM('ACTIVE','INACTIVE','SUSPENDED')` | DEFAULT `'ACTIVE'` | |
| `created_at` | `TIMESTAMP` | DEFAULT `CURRENT_TIMESTAMP` | |
| `updated_at` | `TIMESTAMP` | ON UPDATE `CURRENT_TIMESTAMP` | |

**Indexes:** `idx_users_email` (email), `idx_users_role` (role)

### `restaurant_profiles` (V2)

| Column | Type | Constraints | Description |
|---|---|---|---|
| `id` | `BIGINT` | PK, AUTO_INCREMENT | |
| `user_id` | `BIGINT` | NOT NULL, UNIQUE, FK → `users(id)` | One profile per user |
| `restaurant_name` | `VARCHAR(255)` | NOT NULL | |
| `address` | `TEXT` | | Full address |
| `latitude` | `DECIMAL(10,8)` | | GPS coordinate |
| `longitude` | `DECIMAL(11,8)` | | GPS coordinate |
| `profile_image_url` | `VARCHAR(512)` | | |
| `verification_status` | `ENUM('PENDING','VERIFIED','REJECTED')` | DEFAULT `'PENDING'` | |
| `created_at` | `TIMESTAMP` | DEFAULT `CURRENT_TIMESTAMP` | |
| `updated_at` | `TIMESTAMP` | ON UPDATE `CURRENT_TIMESTAMP` | |

**Indexes:** `idx_restaurant_user_id` (user_id)

### `ngo_profiles` (V3)

| Column | Type | Constraints | Description |
|---|---|---|---|
| `id` | `BIGINT` | PK, AUTO_INCREMENT | |
| `user_id` | `BIGINT` | NOT NULL, UNIQUE, FK → `users(id)` | |
| `ngo_name` | `VARCHAR(255)` | NOT NULL | |
| `address` | `TEXT` | | |
| `latitude` | `DECIMAL(10,8)` | | |
| `longitude` | `DECIMAL(11,8)` | | |
| `registration_number` | `VARCHAR(255)` | | Gov registration ID |
| `profile_image_url` | `VARCHAR(512)` | | |
| `verification_status` | `ENUM('PENDING','VERIFIED','REJECTED')` | DEFAULT `'PENDING'` | |
| `created_at` | `TIMESTAMP` | DEFAULT `CURRENT_TIMESTAMP` | |
| `updated_at` | `TIMESTAMP` | ON UPDATE `CURRENT_TIMESTAMP` | |

**Indexes:** `idx_ngo_user_id` (user_id)

### `donations` (V4)

| Column | Type | Constraints | Description |
|---|---|---|---|
| `id` | `BIGINT` | PK, AUTO_INCREMENT | |
| `restaurant_id` | `BIGINT` | NOT NULL, FK → `restaurant_profiles(id)` | |
| `food_name` | `VARCHAR(255)` | NOT NULL | |
| `description` | `TEXT` | | |
| `food_category` | `ENUM(...)` | DEFAULT `'OTHER'` | See Food Categories |
| `quantity` | `DECIMAL(10,2)` | NOT NULL | |
| `quantity_unit` | `ENUM('KG','PORTIONS','PACKETS','LITERS','PIECES')` | NOT NULL | |
| `image_url` | `VARCHAR(512)` | | |
| `prepared_at` | `TIMESTAMP` | NULL | When food was prepared |
| `available_until` | `TIMESTAMP` | NOT NULL | |
| `pickup_deadline` | `TIMESTAMP` | NOT NULL | Auto-expiry trigger |
| `storage_instructions` | `TEXT` | | Handling notes |
| `latitude` | `DECIMAL(10,8)` | NOT NULL | Pickup location |
| `longitude` | `DECIMAL(11,8)` | NOT NULL | |
| `address` | `TEXT` | NOT NULL | |
| `status` | `ENUM(...)` | DEFAULT `'AVAILABLE'` | See Donation Statuses |
| `created_at` | `TIMESTAMP` | DEFAULT `CURRENT_TIMESTAMP` | |
| `updated_at` | `TIMESTAMP` | ON UPDATE `CURRENT_TIMESTAMP` | |

**Indexes:** `idx_donations_restaurant` (restaurant_id), `idx_donations_status` (status), `idx_donations_deadline` (pickup_deadline), `idx_donations_location` (latitude, longitude)

### `donation_claims` (V5)

| Column | Type | Constraints | Description |
|---|---|---|---|
| `id` | `BIGINT` | PK, AUTO_INCREMENT | |
| `donation_id` | `BIGINT` | NOT NULL, UNIQUE, FK → `donations(id)` | One claim per donation |
| `ngo_id` | `BIGINT` | NOT NULL, FK → `ngo_profiles(id)` | |
| `accepted_at` | `TIMESTAMP` | DEFAULT `CURRENT_TIMESTAMP` | |
| `pickup_status` | `ENUM('ASSIGNED','EN_ROUTE','ARRIVED','COLLECTED','COMPLETED')` | DEFAULT `'ASSIGNED'` | |
| `collected_at` | `TIMESTAMP` | NULL | |
| `completed_at` | `TIMESTAMP` | NULL | |
| `completion_notes` | `TEXT` | | |
| `created_at` | `TIMESTAMP` | DEFAULT `CURRENT_TIMESTAMP` | |
| `updated_at` | `TIMESTAMP` | ON UPDATE `CURRENT_TIMESTAMP` | |

**Indexes:** `idx_claims_donation` (donation_id), `idx_claims_ngo` (ngo_id)

### `device_tokens` (V6)

| Column | Type | Constraints | Description |
|---|---|---|---|
| `id` | `BIGINT` | PK, AUTO_INCREMENT | |
| `user_id` | `BIGINT` | NOT NULL, FK → `users(id)` | |
| `fcm_token` | `VARCHAR(512)` | NOT NULL | Firebase token |
| `platform` | `VARCHAR(20)` | DEFAULT `'ANDROID'` | |
| `active` | `BOOLEAN` | DEFAULT `TRUE` | |
| `created_at` | `TIMESTAMP` | DEFAULT `CURRENT_TIMESTAMP` | |
| `updated_at` | `TIMESTAMP` | ON UPDATE `CURRENT_TIMESTAMP` | |

**Unique constraint:** `uq_user_token` (user_id, fcm_token)  
**Indexes:** `idx_device_tokens_user` (user_id)

### `notifications` (V7)

| Column | Type | Constraints | Description |
|---|---|---|---|
| `id` | `BIGINT` | PK, AUTO_INCREMENT | |
| `user_id` | `BIGINT` | NOT NULL, FK → `users(id)` | |
| `donation_id` | `BIGINT` | NULL, FK → `donations(id)` | |
| `notification_type` | `ENUM(...)` | NOT NULL | See Notification Types |
| `title` | `VARCHAR(255)` | NOT NULL | |
| `message` | `TEXT` | | |
| `is_read` | `BOOLEAN` | DEFAULT `FALSE` | |
| `created_at` | `TIMESTAMP` | DEFAULT `CURRENT_TIMESTAMP` | |

**Indexes:** `idx_notifications_user` (user_id), `idx_notifications_read` (is_read), `idx_notifications_created` (created_at)

## Enums

### Roles
`RESTAURANT`, `NGO`

### Account Status
`ACTIVE`, `INACTIVE`, `SUSPENDED`

### Verification Status
`PENDING`, `VERIFIED`, `REJECTED`

### Donation Status
`AVAILABLE`, `ACCEPTED`, `COLLECTED`, `COMPLETED`, `EXPIRED`, `CANCELLED`

### Food Category
`COOKED_MEALS`, `RAW_INGREDIENTS`, `BAKERY`, `DAIRY`, `FRUITS_VEGETABLES`, `BEVERAGES`, `PACKAGED`, `OTHER`

### Quantity Unit
`KG`, `PORTIONS`, `PACKETS`, `LITERS`, `PIECES`

### Pickup Status
`ASSIGNED`, `EN_ROUTE`, `ARRIVED`, `COLLECTED`, `COMPLETED`

### Notification Type
`NEW_DONATION_NEARBY`, `DONATION_ACCEPTED`, `DONATION_COLLECTED`, `DONATION_COMPLETED`, `DONATION_CANCELLED`, `DONATION_EXPIRED`
