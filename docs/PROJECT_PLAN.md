# Tower Rush 2 Assistant

## Goal
A standalone Android companion for the Galaxsys Tower Rush 2 game, targeting the Samsung Galaxy A05 (Android 14). It uses user-approved screen capture to explain visible game state and offer cautious, non-predictive decision support.

## First release boundaries
- Android-native application; no casino login, account access, deposits, or payment handling.
- Screen capture only after the Android system consent prompt.
- Show observations and uncertainty; never claim to know a hidden/random next outcome.
- The player makes all game decisions and taps game controls manually.
- No automatic betting, game-control tapping, wager sizing, auto-cashout, or loss-chasing.
- No guarantee of wins or profit.

## Architecture
1. Android UI: dashboard, capture status, session controls, recommendation panel.
2. MediaProjection capture: explicit user consent and visible capture state.
3. Frame processing: crop/scale and detect only visible UI labels, multiplier, floor count, and round state.
4. Decision support: rule-based explanations from visible facts; no prediction of random outcomes.
5. Session notes: optional local-only record with stop/reset controls.

## Milestones
1. Android project and CI debug build (current foundation).
2. Permission-based screen capture with clear start/stop controls.
3. On-screen recognition validated against user-provided screenshots.
4. Decision-support panel that explains risk and uncertainty.
5. Local session history, accessibility checks, and device testing.

## Acceptance criteria
- Builds a debug APK in GitHub Actions.
- Installs and launches on Android 14 without parsing errors.
- Capture starts only after system consent and stops immediately on request.
- No app feature can place a wager or activate a game control.
