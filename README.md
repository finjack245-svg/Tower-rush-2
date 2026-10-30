# Tower Rush 2 Assistant

A standalone Android companion for **Galaxsys Tower Rush 2**, targeting the Samsung Galaxy A05 (Android 14).

## Current status

- **Milestone 1:** Android project foundation and GitHub Actions debug-APK build verified successfully (workflow run #6).
- **Milestone 2:** Consent-based Android screen capture has been implemented. The current build starts a MediaProjection foreground service after Android's system permission prompt, shows a persistent Stop notification, and discards captured frames immediately. The new build is awaiting CI verification and real-device testing.

## Planned capabilities

- Recognise visible game information and explain what can and cannot be inferred.
- Cautious decision support with uncertainty clearly shown.
- Local session notes and explicit start/stop controls.

## Player control and safety

This is an informational companion, not a winning system. Tower Rush 2 outcomes are not predictable from previous rounds or visual patterns. The app does not log in to 1win, place bets, tap Build/Cashout, change wagers, or chase losses. The player makes all game actions manually.

## Screen-capture privacy

Capture starts only after Android's own MediaProjection consent prompt. A persistent notification includes a Stop capture action. In this milestone, frames are drained and immediately discarded: no screenshots are saved, no frames are uploaded, and no OCR or game analysis is performed yet. Capture can include any visible app on the display, so start it only when you intend to share the screen and stop it when finished.

## Build

GitHub Actions runs the Android debug APK workflow on pushes to main and can be started manually from the Actions tab. The artifact is named `tower-rush-2-assistant-debug`.

## Device target

- Samsung Galaxy A05
- Android 14
- Minimum Android API: 23
- Java 17 / Android Gradle Plugin 8.7.3

See `docs/PROJECT_PLAN.md` for architecture, milestones, and acceptance criteria.
