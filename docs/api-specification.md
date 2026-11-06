# FoodConnect API Specification

**Base URL:** `http://localhost:8080/api/v1/`  
**Authentication:** JWT Bearer token in `Authorization` header  
**Content-Type:** `application/json`

---

## Authentication

### Register

`POST /auth/register`

**Request:**
```json
{
  "name": "John Doe",
  "email": "john@restaurant.com",
  "password": "securepass123",
  "role": "RESTAURANT",
  "phone": "+91-9876543210",
  "restaurantName": "Green Kitchen",
  "address": "123 Main St, Delhi",
  "latitude": 28.6139,
  "longitude": 77.2090
}
```

For NGO registration, use `"role": "NGO"` and include `ngoName` and optionally `registrationNumber` instead of `restaurantName`.

**Response (200):**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "user": {
    "id": 1,
    "name": "John Doe",
    "email": "john@restaurant.com",
    "role": "RESTAURANT",
    "phone": "+91-9876543210",
    "accountStatus": "ACTIVE",
    "createdAt": "2024-01-15T10:30:00"
  },
  "role": "RESTAURANT"
}
```

**Errors:** `409` duplicate email, `400` validation errors

---

### Login

`POST /auth/login`

**Request:**
```json
{
  "email": "john@restaurant.com",
  "password": "securepass123"
}
```

**Response (200):** Same as Register response

**Errors:** `401` invalid credentials

---

### Get Current User

`GET /auth/me` 🔒

**Response (200):**
```json
{
  "id": 1,
  "name": "John Doe",
  "email": "john@restaurant.com",
  "role": "RESTAURANT",
  "phone": "+91-9876543210",
  "accountStatus": "ACTIVE",
  "createdAt": "2024-01-15T10:30:00"
}
```

---

## Restaurant Profiles

### Get My Profile

`GET /restaurants/me` 🔒 (RESTAURANT only)

**Response (200):**
```json
{
  "id": 1,
  "userId": 1,
  "restaurantName": "Green Kitchen",
  "address": "123 Main St, Delhi",
  "latitude": 28.6139,
  "longitude": 77.2090,
  "profileImageUrl": null,
  "verificationStatus": "PENDING"
}
```

### Update My Profile

`PUT /restaurants/me` 🔒 (RESTAURANT only)

**Request:**
```json
{
  "restaurantName": "Green Kitchen Deluxe",
  "address": "456 New St, Delhi",
  "latitude": 28.6200,
  "longitude": 77.2150
}
```

### Get Restaurant by ID

`GET /restaurants/{id}` 🔒

---

## NGO Profiles

### Get My Profile

`GET /ngos/me` 🔒 (NGO only)

**Response (200):**
```json
{
  "id": 1,
  "userId": 2,
  "ngoName": "Feed India Foundation",
  "address": "789 NGO Lane, Mumbai",
  "latitude": 19.0760,
  "longitude": 72.8777,
  "registrationNumber": "NGO-2024-001",
  "profileImageUrl": null,
  "verificationStatus": "PENDING"
}
```

### Update My Profile

`PUT /ngos/me` 🔒 (NGO only)

### Get NGO by ID

`GET /ngos/{id}` 🔒

---

## Donations

### Create Donation

`POST /donations` 🔒 (RESTAURANT only)

**Request:**
```json
{
  "foodName": "Mixed Vegetable Curry",
  "description": "Freshly prepared, serves 20",
  "foodCategory": "COOKED_MEALS",
  "quantity": 5.0,
  "quantityUnit": "KG",
  "preparedAt": "2024-01-15T12:00:00",
  "availableUntil": "2024-01-15T18:00:00",
  "pickupDeadline": "2024-01-15T20:00:00",
  "storageInstructions": "Keep refrigerated",
  "latitude": 28.6139,
  "longitude": 77.2090,
  "address": "123 Main St, Delhi",
  "imageUrl": null
}
```

**Response (201):**
```json
{
  "id": 1,
  "restaurantId": 1,
  "restaurantName": "Green Kitchen",
  "foodName": "Mixed Vegetable Curry",
  "description": "Freshly prepared, serves 20",
  "foodCategory": "COOKED_MEALS",
  "quantity": 5.0,
  "quantityUnit": "KG",
  "imageUrl": null,
  "preparedAt": "2024-01-15T12:00:00",
  "availableUntil": "2024-01-15T18:00:00",
  "pickupDeadline": "2024-01-15T20:00:00",
  "storageInstructions": "Keep refrigerated",
  "latitude": 28.6139,
  "longitude": 77.2090,
  "address": "123 Main St, Delhi",
  "status": "AVAILABLE",
  "distance": null,
  "claimedByNgoId": null,
  "claimedByNgoName": null,
  "createdAt": "2024-01-15T12:30:00",
  "updatedAt": "2024-01-15T12:30:00"
}
```

**Side effect:** Sends push notification to NGOs within 10km radius.

### Get Donation

`GET /donations/{id}` 🔒

### Get My Donations (Restaurant)

`GET /donations/mine?status=AVAILABLE&page=0&size=20` 🔒 (RESTAURANT only)

**Response (200):**
```json
{
  "content": [ /* DonationResponse[] */ ],
  "page": 0,
  "size": 20,
  "totalElements": 5,
  "totalPages": 1,
  "last": true
}
```

### Get Nearby Donations (NGO)

`GET /donations/nearby?lat=19.076&lng=72.877&radiusKm=10` 🔒 (NGO only)

**Response (200):** Array of `DonationResponse` with `distance` field populated (km).

### Accept Donation

`POST /donations/{id}/accept` 🔒 (NGO only)

Atomically claims the donation. Returns `409` if already claimed by another NGO.

**Response (200):**
```json
{
  "id": 1,
  "donationId": 1,
  "ngoId": 1,
  "ngoName": "Feed India Foundation",
  "acceptedAt": "2024-01-15T13:00:00",
  "pickupStatus": "ASSIGNED",
  "collectedAt": null,
  "completedAt": null,
  "completionNotes": null
}
```

### Cancel Donation

`POST /donations/{id}/cancel` 🔒 (RESTAURANT only)

### Mark Collected

`POST /donations/{id}/collected` 🔒 (NGO only)

### Mark Completed

`POST /donations/{id}/complete` 🔒 (NGO only)

### Get NGO Pickups

`GET /donations/pickups?status=ASSIGNED` 🔒 (NGO only)

**Response (200):** Array of `DonationClaimResponse`

---

## Notifications

### Get Notifications

`GET /notifications` 🔒

**Response (200):**
```json
[
  {
    "id": 1,
    "donationId": 1,
    "notificationType": "NEW_DONATION_NEARBY",
    "title": "New food available nearby!",
    "message": "Green Kitchen has Mixed Vegetable Curry (5.0 KG) available",
    "isRead": false,
    "createdAt": "2024-01-15T12:30:00"
  }
]
```

### Mark Notification Read

`PATCH /notifications/{id}/read` 🔒

---

## User / Device

### Update Location

`PUT /users/me/location` 🔒

```json
{ "latitude": 19.076, "longitude": 72.877 }
```

### Register Device Token

`POST /users/me/device-tokens` 🔒

```json
{ "fcmToken": "fMR7x...", "platform": "ANDROID" }
```

### Remove Device Token

`DELETE /users/me/device-tokens/{id}` 🔒

---

## Error Responses

All errors return:
```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "timestamp": "2024-01-15T12:30:00",
  "path": "/api/v1/auth/register",
  "fieldErrors": {
    "email": "must be a valid email",
    "password": "size must be at least 8"
  }
}
```

| Code | Meaning |
|---|---|
| `400` | Validation errors or bad request |
| `401` | Missing or invalid token |
| `403` | Insufficient role permissions |
| `404` | Resource not found |
| `409` | Conflict (duplicate email, donation already claimed) |
| `500` | Internal server error |

---

🔒 = Requires `Authorization: Bearer <token>` header
