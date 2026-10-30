# Tower Rush 2 Assistant

## Goal
A standalone Android companion for Galaxsys Tower Rush 2, targeting the Samsung Galaxy A05 (Android 14). It uses user-approved screen capture to explain visible game state and offer cautious, non-predictive decision support.

## First release boundaries
- No casino login, account access, deposits, or payment handling.
- Screen capture only after the Android system consent prompt.
- Show observations and uncertainty; never claim to know a hidden/random next outcome.
- The player makes all game decisions and taps game controls manually.
- No automatic betting, game-control tapping, wager sizing, auto-cashout, or loss-chasing.
- No guarantee of wins or profit.

## Architecture
1. Android UI: dashboard, capture status, session controls, recommendation panel.
2. MediaProjection foreground service: explicit system consent, persistent notification, immediate stop control.
3. Frame processing: inspect only user-visible information; do not save or upload frames without a separate explicit feature and consent.
4. Decision support: explanations from visible facts; no prediction of random outcomes.
5. Session notes: optional local-only record with stop/reset controls.

## Milestones
1. Android project and CI debug build (completed; latest workflow run #6 succeeded).
2. Permission-based screen capture with clear start/stop controls (implemented; device verification pending).
3. On-screen recognition validated against user-provided screenshots (next).
4. Decision-support panel that explains risk and uncertainty.
5. Local session history, accessibility checks, and device testing.

## Stage 2 implementation
- Start capture only after Android's MediaProjection consent dialog.
- Use a foreground service with the mediaProjection service type and persistent notification.
- Provide a Stop capture action in the notification and in the app.
- Drain captured frames and immediately close them; no image storage, network upload, OCR, or game interaction in this stage.
- Capture can be stopped by the user or by Android revoking the projection.

## Acceptance criteria
- Builds a debug APK in GitHub Actions.
- Installs and launches on Android 14 without parsing errors.
- Capture starts only after system consent and stops immediately on request.
- No app feature can place a wager or activate a game control.


## BUILD interface-status assistant
- The app reports READY TO DECIDE only when OCR sees both BUILD and BUY BONUS and does not see cashout-related text.
- If BUILD alone is seen, it reports UNCLEAR; if BUILD is absent or the latest scan is stale, it reports WAIT.
- The label describes the apparent interface state only. It is not a timing, safety, or win prediction.
- OCR can misread the screen. The player remains responsible for all game decisions; the app never taps BUILD or changes a wager.
