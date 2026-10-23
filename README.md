# LetsTrack

A Kotlin-based Android implementation of a group location tracking application using Android Geofencing and Firebase.

## Overview

LetsTrack allows users to create or join a tracking group and associate the group with a geofenced location.

When a group member leaves the registered geofence:

1. Android detects the geofence exit.
2. A `BroadcastReceiver` receives the exit event.
3. The app identifies the user and tracking group.
4. The exit event is stored in Firebase Firestore.

## Features

- Anonymous Firebase Authentication
- Create and join tracking groups
- Firebase Firestore group storage
- Android Geofencing API
- Background geofence event handling through `BroadcastReceiver`
- Geofence exit event storage in Firestore
- Local tracking preferences for the active group
- Jetpack Compose UI

## Tech Stack

- **Kotlin**
- **Jetpack Compose**
- **Firebase Authentication**
- **Firebase Cloud Firestore**
- **Google Play Services Location / Geofencing**
- **Coroutines**
- **Android BroadcastReceiver**

## Architecture

The main Android flow is:

```text
User
 │
 ├── Create / Join Group
 │        │
 │        ▼
 │    Firebase Auth
 │        │
 │        ▼
 │    Cloud Firestore
 │
 └── Start Tracking
          │
          ▼
   GeofencingClient
          │
          ▼
    Geofence Exit
          │
          ▼
 GeofenceBroadcastReceiver
          │
          ▼
    tracking_events
          │
          ▼
     Firestore
```

## Setup

### 1. Clone the repository

```bash
git clone https://github.com/Ayushigupta2005/LetsTrack.git
```

### 2. Configure Firebase

Create a Firebase project and register the Android application using:

```text
com.ayushi.letstrack
```

Enable:

- Anonymous Authentication
- Cloud Firestore

Download `google-services.json` and place it at:

```text
AndroidApp/google-services.json
```

The Firebase configuration file is intentionally excluded from Git using `.gitignore`.

### 3. Open the project

Open the project in Android Studio and allow Gradle to sync.

### 4. Run

Run the `AndroidApp` module on an Android device.

The application requires location permissions for geofence monitoring.
