# Live Location Tracking – Android (Offline First)

## Overview
This Android application implements **continuous live location tracking** with a strong **offline-first design**.  
All location updates are reliably stored locally when the device is offline and automatically synced to **Firebase Firestore** once connectivity is restored.

The solution is designed using modern Android best practices:
- Foreground Service for uninterrupted location tracking
- Room database for offline persistence
- WorkManager for reliable background synchronization
- MVVM architecture
- Network-aware sync with retry and exponential backoff

---

## Architecture Explanation

The application follows a **clean MVVM (Model–View–ViewModel) architecture** with clear separation of concerns.

### UI Layer
- **MainActivity**
  - Provides Start / Stop tracking controls
  - Displays pending offline location count
  - Displays real-time Online / Offline network status
  - Requests runtime permissions
  - Triggers immediate sync when network becomes available

### ViewModel Layer
- **MainViewModel**
  - Exposes `LiveData<Integer>` for pending location count
  - Ensures the UI reacts automatically to database changes

### Data Layer
- **Room Database**
  - `LocationEntity` stores each captured location
  - Includes a `synced` flag to track upload state
- **LocationDao**
  - Inserts location records
  - Queries pending (unsynced) records
  - Updates sync status after successful upload
- **LocationRepository**
  - Single source of truth
  - Coordinates between Room and Firestore
  - Handles upload logic and sync retries

### Background Execution
- **LocationForegroundService**
  - Collects location updates at fixed intervals
  - Runs as a foreground service with persistent notification
  - Ensures tracking continues when app is backgrounded
- **WorkManager**
  - Periodic background sync every 15 minutes
  - Immediate sync when app resumes and network is available
  - Guaranteed execution even after app kill or device reboot
  - Retry support with exponential backoff

---

## How Offline Storage Works

The application uses an **offline-first strategy**.

### Flow
1. Every location update is **always written to the Room database first**
2. Each record is saved with `synced = false`
3. This behavior is independent of network availability

### Behavior
- Works in airplane mode or without mobile data
- No data loss during connectivity changes
- Pending count reflects unsynced records in real time

### Local Database Fields
- `latitude`
- `longitude`
- `accuracy`
- `speed`
- `timestamp`
- `synced` (false = pending, true = synced)

---

## How Sync Mechanism Works

### 1. Immediate Sync (Internet Available)
- Location is saved to Room
- Upload to Firestore is attempted immediately
- On successful upload:
  - Local record is marked `synced = true`
  - Pending count decreases automatically

### 2. Deferred Sync (Offline Mode)
- Location records remain in Room
- `synced = false`
- Pending count increases
- No upload attempts are made

### 3. Background Sync with WorkManager
- **PeriodicWorkRequest**
  - Runs every 15 minutes
  - Requires network connectivity
- **OneTimeWorkRequest**
  - Triggered when app resumes and network is available

### 4. Retry Strategy
- If Firestore upload fails:
  - Worker returns `Result.retry()`
  - Exponential backoff is applied
- Retries continue until upload succeeds

### Outcome
- All pending locations are eventually synced
- Sync status is updated in Room
- UI reflects changes automatically via LiveData

---

## API Contract Used for Sending Locations

Each location is uploaded to Firebase Firestore in the following JSON format:

```json
{
  "employeeId": "EMP001",
  "latitude": 28.6139,
  "longitude": 77.2090,
  "accuracy": 10.5,
  "timestamp": 1712230429000,
  "speed": 1.8
}
```
## API Notes

- **Firestore collection:** `locations`
- **Document ID:** Local Room database ID (used to prevent duplicate uploads)
- **employeeId:** Currently hardcoded (`EMP001`) for demonstration purposes

---

## Assumptions

- Single employee use case (`EMP001`)
- Firestore authentication is not required
- User explicitly starts and stops location tracking
- Location permission is granted by the user
- Network availability determines sync behavior

---

## Limitations

- Location tracking does **not auto-restart after device reboot**
- Battery optimization whitelist is **not requested explicitly**
- No per-record sync status UI (only aggregate pending count)
- No user authentication or login flow

---

## Summary

This project demonstrates a **robust, production-style Android solution** for live location tracking with:

- Reliable offline data storage
- Automatic background synchronization
- Retry handling with exponential backoff
- Battery-safe execution using foreground services
- Clean MVVM architecture
- Modern Android APIs and best practices
