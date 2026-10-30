# Suspended-building swing tracking

## Evidence from the supplied gameplay recording

The 28.4-second recording shows the suspended building changing horizontal position while hanging from the crane. Gameplay is visible for approximately the first 21 seconds; the Android notification shade covers the game near the end. The recording is useful for defining the tracking target, but it is not enough to certify exact tap timing or a reliable swing period.

## Initial implementation

`SwingMotionTracker` is an isolated on-device image-processing component. It:
- scans an upper-middle proportional region of a captured frame for the warm coral/red building facade;
- groups adjacent matching pixels into connected components and selects the largest plausible component;
- estimates the horizontal centre, smooths position, estimates horizontal velocity, and labels left/right/reversal/unknown;
- returns no detection when the target is missing rather than extrapolating a stale position.

The confidence value is a heuristic based on component area, not a statistical probability.

## Important current limitations

- The tracker is not yet connected to `ScreenCaptureService`.
- The capture service currently samples OCR at a slow interval; motion tracking needs a separate higher-rate, throttled frame path.
- The transparent floating HUD and Android overlay permission flow are not implemented by this change.
- The ROI and colour thresholds are provisional and must be checked on multiple real gameplay recordings, including different building skins/backgrounds and screen brightness.
- No build or device test is claimed by this change.

## Next integration steps

1. Add a bounded-rate frame-analysis path and invoke the tracker without blocking frame delivery.
2. Publish fresh samples to a local shared state channel; expire samples quickly when tracking is lost.
3. Add an explicit user-enabled overlay using Android's overlay permission. Draw a translucent left/right track and a marker at the detected building position.
4. Add a separate alignment reference based on the visible top of the stack; do not label a moment as guaranteed or profitable.
5. Test CPU use, dropped frames, false detections, rotation/resolution changes, overlay placement, and stop/revocation lifecycle on the Galaxy A05.

## Player control

The assistant remains advisory. It does not press BUILD, change stake settings, log in to the casino, or predict random outcomes. The player decides and taps manually.
