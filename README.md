# ServiceApp — Android Service Marketplace

A full-featured Android service marketplace app connecting clients with service providers (plumbers, electricians, mechanics, cleaners, and more).

---

## Features

- **Splash Screen** — animated logo with auto-routing based on login state
- **Welcome / Login / Sign-Up** — Firebase Authentication with email & password
- **Role Selection** — Client or Service Provider account types
- **Home Screen (Client)** — 8 service categories to browse and request
- **Provider Search** — GPS-based search for nearby available providers
- **In-App Chat** — real-time messaging with image/photo sharing
- **Accept & Share Location** — client confirms and shares location with provider
- **Live Tracking** — real-time provider location on map, arrival notification
- **Provider Dashboard** — online/offline toggle, accept/decline requests, navigate to client
- **Mutual Ratings** — both client and provider rate each other after the job

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Java |
| Firebase Auth | Email/Password Authentication |
| Firebase Firestore | Real-time NoSQL database |
| Firebase Storage | Profile photos & chat images |
| Firebase FCM | Push notifications |
| Google Maps SDK | Maps, routing, location |
| Material Design 3 | UI components |
| Glide | Image loading |
| CircleImageView | Circular avatars |

---

## Project Structure

```
ServiceApp/
├── app/
│   ├── src/main/
│   │   ├── java/com/serviceapp/
│   │   │   ├── activities/
│   │   │   │   ├── SplashActivity.java
│   │   │   │   ├── WelcomeActivity.java
│   │   │   │   ├── LoginActivity.java
│   │   │   │   ├── SignUpActivity.java
│   │   │   │   ├── HomeActivity.java          ← Client home
│   │   │   │   ├── SearchingActivity.java     ← Find providers
│   │   │   │   ├── ChatActivity.java          ← Chat + accept
│   │   │   │   ├── TrackingActivity.java      ← Live tracking
│   │   │   │   ├── RatingActivity.java        ← Rate provider
│   │   │   │   ├── ProviderDashboardActivity.java ← Provider home
│   │   │   │   └── ProfileActivity.java
│   │   │   ├── adapters/
│   │   │   │   ├── ServiceAdapter.java
│   │   │   │   ├── ProviderAdapter.java
│   │   │   │   ├── MessageAdapter.java
│   │   │   │   └── PendingRequestAdapter.java
│   │   │   ├── models/
│   │   │   │   ├── User.java
│   │   │   │   ├── Service.java
│   │   │   │   ├── ServiceRequest.java
│   │   │   │   ├── Message.java
│   │   │   │   └── Rating.java
│   │   │   └── utils/
│   │   │       └── MyFirebaseMessagingService.java
│   │   ├── res/
│   │   │   ├── layout/        ← All XML layouts
│   │   │   ├── drawable/      ← Icons and backgrounds
│   │   │   ├── values/        ← Colors, strings, themes, dimens
│   │   │   └── mipmap-*/      ← App launcher icons
│   │   └── AndroidManifest.xml
│   ├── build.gradle
│   └── google-services.json  ← ⚠️ REPLACE THIS
├── build.gradle
├── settings.gradle
└── gradle.properties
```

---

## ⚠️ Setup Steps (Required Before Building)

### Step 1 — Create a Firebase Project

1. Go to [https://console.firebase.google.com](https://console.firebase.google.com)
2. Click **Add Project** → name it (e.g. "ServiceApp")
3. Enable Google Analytics if desired, then click **Create Project**

### Step 2 — Add Android App to Firebase

1. In the Firebase Console, click the **Android** icon
2. Enter the package name: `com.serviceapp`
3. Download the `google-services.json` file
4. **Replace** the placeholder file at `app/google-services.json` with your real one

### Step 3 — Enable Firebase Services

In Firebase Console, enable these services:

| Service | Where to enable |
|---------|----------------|
| Authentication | Build → Authentication → Sign-in method → Email/Password |
| Firestore Database | Build → Firestore Database → Create database (start in test mode) |
| Storage | Build → Storage → Get started |
| Cloud Messaging | Already enabled by default |

### Step 4 — Set Up Google Maps

1. Go to [https://console.cloud.google.com](https://console.cloud.google.com)
2. Select your project → APIs & Services → Enable **Maps SDK for Android**
3. Create an API key under **Credentials**
4. Open `app/src/main/AndroidManifest.xml`
5. Replace `YOUR_MAPS_API_KEY` with your actual API key

### Step 5 — Firestore Security Rules

In Firebase Console → Firestore Database → Rules, paste:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /users/{userId} {
      allow read: if request.auth != null;
      allow write: if request.auth != null && request.auth.uid == userId;
    }
    match /requests/{requestId} {
      allow read, write: if request.auth != null;
    }
    match /requests/{requestId}/messages/{messageId} {
      allow read, write: if request.auth != null;
    }
    match /ratings/{ratingId} {
      allow read: if request.auth != null;
      allow write: if request.auth != null;
    }
  }
}
```

### Step 6 — Open in Android Studio

1. Open **Android Studio**
2. File → Open → navigate to and select the `ServiceApp` folder
3. Wait for Gradle sync to complete
4. Connect a device or start an emulator (API 26+)
5. Click **Run**

---

## Firestore Data Structure

```
users/
  {uid}/
    firstName, lastName, username, email, phone
    accountType: "client" | "provider"
    profileImageUrl, rating, totalRatings
    lat, lng, online (providers only)
    fcmToken

requests/
  {requestId}/
    clientId, providerId, serviceType, status
    clientLat, clientLng, providerLat, providerLng
    agreedPrice, createdAt
    
    messages/
      {messageId}/
        senderId, receiverId, content, imageUrl
        type: "text" | "image"
        timestamp, isRead

ratings/
  {ratingId}/
    raterId, ratedUserId, requestId
    stars (1–5), comment
    raterName, raterImageUrl, timestamp
```

---

## App Flow

```
Splash
  ├─► Welcome ─► Login ────────────┐
  │            └─► Sign Up ────────┤
  │                                ▼
  └───────────────── Client: Home Screen
                          │
                          ▼ (select service)
                     SearchingActivity
                          │
                          ▼ (provider found)
                     ChatActivity ──► (accept)
                          │
                          ▼
                     TrackingActivity ──► (arrived)
                          │
                          ▼
                     RatingActivity ──► Home

                 Provider: ProviderDashboardActivity
                          │ (receive request)
                          ▼
                     ChatActivity (discuss price)
                          │ (client accepts)
                          ▼
                     Navigate to client → Mark Arrived
                          │ (job done)
                          ▼
                     Close Ticket + Rate Client
```

---

## Notes

- Providers must toggle **Online** in their dashboard to appear in client searches
- Location sharing only activates when the client taps **Accept** in ChatActivity
- FCM push notifications require a server key in production; test locally with Firestore listeners
- Replace all placeholder API keys before publishing to Google Play

  WTC-Y73BNFA5
