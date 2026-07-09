# LiveOrtung – Setup

## Funktionsprinzip
Beide installieren die App und geben denselben **Raum-Code** ein. Jedes Gerät schreibt seine Position alle 5 s in die Firebase Realtime Database (`/rooms/{code}/{deviceId}`) und zeigt die des anderen live als Marker auf Google Maps. Ein Foreground-Service hält das Senden auch im Hintergrund am Laufen.

## Einrichtung (~10 min) — OSM-Variante: kein Maps-API-Key nötig!

1. **Android Studio**: Neues Projekt "Empty Views Activity" (Kotlin), Paketname `com.example.liveortung`. Dateien aus diesem Ordner übernehmen.

2. **Firebase**:
   - console.firebase.google.com → Projekt anlegen → Android-App mit Paketname registrieren
   - `google-services.json` herunterladen → in `app/` legen
   - Realtime Database erstellen (Standort `europe-west1`), Regeln fürs Testen:
     ```json
     { "rules": { "rooms": { ".read": true, ".write": true } } }
     ```
     (Für echten Betrieb: Auth + restriktive Regeln!)
   - In `build.gradle.kts` (Projekt-Ebene): Plugin `com.google.gms.google-services` Version 4.4.2 hinzufügen.


4. Auf beide Handys installieren (`Build → Build APK`), gleichen Raum-Code eingeben → fertig.

## Verbesserungsideen
- Firebase Auth statt offener Regeln
- Hintergrund-Standort (`ACCESS_BACKGROUND_LOCATION`) falls App komplett geschlossen sein soll
- Polyline für zurückgelegte Strecke, Entfernungsanzeige
- Batterie: Intervall auf 15–30 s erhöhen

## Rechtlicher Hinweis
Nur mit Einwilligung der georteten Person nutzen — heimliches Tracking ist in Deutschland strafbar (§ 202a StGB / DSGVO). Da beide die App bewusst installieren und den Code teilen, ist das hier gegeben.
