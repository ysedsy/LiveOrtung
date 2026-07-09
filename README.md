# LiveOrtung

A minimal Android app for sharing live location between two devices. Both users install the app and enter the same **room code**. Each device writes its position to Firebase Realtime Database every 5 seconds (`/rooms/{code}/{deviceId}`) and shows the other device's position live as a marker on an OpenStreetMap view. A foreground service keeps sending location updates while the app is in the background.

No Google Maps API key required — map rendering uses [osmdroid](https://github.com/osmdroid/osmdroid) (OpenStreetMap).

## Requirements

- Android Studio (recent stable version)
- JDK 17
- A Firebase project

## Setup

1. **Clone the project**
   ```
   git clone https://github.com/ysedsy/LiveOrtung.git
   ```
   Open it in Android Studio.

2. **Firebase**
   - Go to [console.firebase.google.com](https://console.firebase.google.com) and create a project.
   - Add an Android app with package name `com.example.liveortung`.
   - Download the generated `google-services.json` and place it in `app/`. This file is gitignored and must be added manually — the app won't build without it.
   - Create a Realtime Database (e.g. region `europe-west1`). For local testing, open rules can be used:
     ```json
     { "rules": { "rooms": { ".read": true, ".write": true } } }
     ```
     For production use, add authentication and restrict the rules accordingly.

3. **Build & run**
   - Sync Gradle, then build/run on two devices (`Build → Build APK` or `Run`).
   - Enter the same room code on both devices to start sharing location.

## Permissions

The app requests location (fine/coarse), foreground service, notifications, and internet access — required for continuous background location sharing and Firebase connectivity.

## Ideas for improvement

- Firebase Auth instead of open database rules
- Background location (`ACCESS_BACKGROUND_LOCATION`) if the app should keep tracking while fully closed
- Polyline for traveled route, distance display
- Increase update interval (15–30 s) to save battery

## Legal notice

Only use this with the consent of the person being located — covert tracking is a criminal offense in Germany (§ 202a StGB / GDPR). Since both parties knowingly install the app and share the room code, consent is given by design.
