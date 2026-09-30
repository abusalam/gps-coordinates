# Project Plan

Create an android app to display gps coordinates of a location

## Project Brief

# Project Brief: GPS Coordinates App

## Features
1. **Real-Time Coordinates Display:** Automatically fetch and display the user's current latitude and longitude with high accuracy.
2. **Location Permission Management:** Seamlessly handle Android runtime permissions for location access, including rationale and fallback states if permission is denied.
3. **Copy to Clipboard:** A simple action button allowing users to instantly copy their current GPS coordinates to the clipboard for easy sharing.
4. **Adaptive Responsive UI:** A layout that adjusts fluidly across different device form factors, orienting effectively on phones, foldables, and tablets.

## High-Level Tech Stack
* **Programming Language:** Kotlin
* **UI Toolkit:** Jetpack Compose
* **Navigation:** Jetpack Navigation 3 (State-driven navigation approach)
* **Adaptive Strategy:** Compose Material Adaptive library (MediaQuery, WindowSizeClass) for robust multi-screen support
* **Location Services:** Google Play Services Location (`FusedLocationProviderClient`) for acquiring coordinates
* **Concurrency & State Management:** Kotlin Coroutines and StateFlow (for observing and emitting location updates asynchronously)
* **Architecture:** MVVM (Model-View-ViewModel) to separate location tracking logic from the Compose UI layer

## Implementation Steps

### Task_1_LocationServices: Set up FusedLocationProviderClient, Kotlin Coroutines, and ViewModel to handle fetching GPS coordinates and exposing them via StateFlow. Include Manifest permissions.
- **Status:** IN_PROGRESS
- **Updates:** Reopened to downgrade gradle wrapper to a stable accessible version (e.g., 8.9) to fix SocketTimeoutException.
- **Acceptance Criteria:**
  - ViewModel emits latitude and longitude
  - Location permissions are declared in the AndroidManifest

### Task_2_AdaptiveUI: Implement Compose Adaptive UI with Jetpack Navigation 3. Create screens to display coordinates, handle runtime location permissions with fallbacks, and implement 'Copy to Clipboard' action.
- **Status:** COMPLETED
- **Updates:** Implemented Jetpack Navigation 3 flow alongside the Compose Material Adaptive library (ListDetailSceneStrategy) for adaptive multi-pane layouts. Created ListScreen and LocationScreen. Integrated Accompanist Permissions for runtime location permissions handling with rationale and fallback. Implemented ClipboardManager functionality for copying coordinates. Integrated with LocationViewModel.
- **Acceptance Criteria:**
  - UI displays coordinates accurately
  - App smoothly handles denied location permissions
  - Copy to clipboard action copies correct string format
  - UI adapts to different form factors

### Task_3_RunAndVerify: Run and Verify: instruct critic_agent to verify application stability (no crashes), confirm alignment with user requirements, and report critical UI issues.
- **Status:** IN_PROGRESS
- **Updates:** Pending refinement to fix a Gradle build download issue.
- **Acceptance Criteria:**
  - make sure all existing tests pass
  - build pass
  - app does not crash
  - GPS features function as expected without crashing
- **StartTime:** 2026-09-29 00:35:02 IST

