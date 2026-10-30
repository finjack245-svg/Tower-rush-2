# Tower Rush 2 Assistant

A standalone Android companion for **Galaxsys Tower Rush 2**, built for the Samsung Galaxy A05 (Android 14).

## Current status

**Milestone 1 — Android foundation committed.** The repository now contains a minimal native Android app, project documentation, and a GitHub Actions debug-APK workflow. The build has not yet been verified by a completed Actions run.

## Planned capabilities

- User-approved Android screen capture.
- Read visible game information and explain what can and cannot be inferred.
- Cautious decision support with uncertainty clearly shown.
- Local session notes and explicit start/stop controls.

## Player control and safety

This is an informational companion, not a winning system. Tower Rush 2 outcomes are not predictable from previous rounds or visual patterns. The app will not log in to 1win, place bets, tap Build/Cashout, change wagers, or chase losses. The player must make all game actions manually.

## Build

GitHub Actions runs the Android debug APK workflow on pushes to main and can be started manually from the Actions tab. The artifact is named tower-rush-2-assistant-debug.

## Device target

- Samsung Galaxy A05
- Android 14
- Minimum Android API: 23
- Java 17 / Android Gradle Plugin 8.7.3

See docs/PROJECT_PLAN.md for the milestone plan and architecture.
