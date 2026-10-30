package com.finjacks.towerrushassistant;

import android.graphics.Bitmap;
import android.graphics.Color;

import java.util.ArrayDeque;

/**
 * Tracks the warm-coloured suspended building in the Tower Rush 2 play area.
 *
 * Call update() with fresh MediaProjection frames and their monotonic capture time.
 * Coordinates are normalised to the full captured frame so the HUD can scale to
 * different Galaxy A05 display sizes. This is a visual tracker, not a game-outcome
 * predictor. The ROI and colour thresholds must be validated on-device.
 */
public final class SwingMotionTracker {
    public enum Direction { LEFT, RIGHT, REVERSING, UNKNOWN }

    public static final class Sample {
        public final boolean detected;
        public final float centerX;       // 0..1 across the captured screen
        public final float velocityPxPerSecond;
        public final Direction direction;
        public final float confidence;    // heuristic 0..1, not probability
        public final long timestampMs;

        private Sample(boolean detected, float centerX, float velocity,
                       Direction direction, float confidence, long timestampMs) {
            this.detected = detected;
            this.centerX = centerX;
            this.velocityPxPerSecond = velocity;
            this.direction = direction;
            this.confidence = confidence;
            this.timestampMs = timestampMs;
        }

        static Sample missing(long time) {
            return new Sample(false, 0f, 0f, Direction.UNKNOWN, 0f, time);
        }
    }

    // The recording places the hanging building in the upper-middle game area.
    // These proportional bounds deliberately exclude the lower street/buildings.
    private static final float ROI_TOP = 0.27f;
    private static final float ROI_BOTTOM = 0.53f;
    private static final int SAMPLE_STEP = 2;
    private static final int MIN_COMPONENT_PIXELS = 90;
    private static final float MAX_JUMP_FRACTION = 0.28f;

    private float previousX = Float.NaN;
    private float smoothedVelocity;
    private long previousTime;
    private int lastSign;
    private int reversalFrames;
    private float filteredX = Float.NaN;

    /**
     * Analyses one captured frame. Do not call concurrently.
     * The caller retains ownership of the bitmap.
     */
    public Sample update(Bitmap frame, long timestampMs) {
        if (frame == null || frame.isRecycled() || frame.getWidth() < 2 || frame.getHeight() < 2) {
            return Sample.missing(timestampMs);
        }

        int width = frame.getWidth();
        int height = frame.getHeight();
        int top = Math.max(0, (int) (height * ROI_TOP));
        int bottom = Math.min(height, (int) (height * ROI_BOTTOM));
        int cols = (width + SAMPLE_STEP - 1) / SAMPLE_STEP;
        int rows = Math.max(1, (bottom - top + SAMPLE_STEP - 1) / SAMPLE_STEP);
        boolean[] mask = new boolean[cols * rows];

        int index = 0;
        for (int y = top; y < bottom; y += SAMPLE_STEP) {
            for (int x = 0; x < width; x += SAMPLE_STEP) {
                int c = frame.getPixel(x, y);
                int r = Color.red(c), g = Color.green(c), b = Color.blue(c);
                // Coral/red facade pixels; blue sky and yellow crane are rejected.
                mask[index++] = r > 105 && r - g > 22 && g - b > 5
                        && r > (int) (g * 1.16f);
            }
        }

        Component best = largestComponent(mask, cols, rows);
        if (best == null || best.area < MIN_COMPONENT_PIXELS) {
            return missingAndAge(timestampMs);
        }

        float rawX = ((best.minX + best.maxX) * 0.5f * SAMPLE_STEP) / width;
        float x = Float.isNaN(filteredX) ? rawX : filteredX * 0.35f + rawX * 0.65f;
        float confidence = Math.min(1f, best.area / 1100f);

        float velocity = 0f;
        Direction direction = Direction.UNKNOWN;
        if (previousTime > 0L && timestampMs > previousTime) {
            float dt = Math.min(0.5f, (timestampMs - previousTime) / 1000f);
            float jump = x - previousX;
            if (Math.abs(jump) <= MAX_JUMP_FRACTION) {
                float instant = (jump * width) / dt;
                smoothedVelocity = smoothedVelocity * 0.55f + instant * 0.45f;
                velocity = smoothedVelocity;
                int sign = Math.abs(velocity) < 18f ? 0 : (velocity > 0f ? 1 : -1);
                if (sign != 0 && lastSign != 0 && sign != lastSign) reversalFrames++;
                else if (sign != 0) reversalFrames = 0;
                if (sign != 0) lastSign = sign;

                if (reversalFrames >= 2) direction = Direction.REVERSING;
                else if (sign > 0) direction = Direction.RIGHT;
                else if (sign < 0) direction = Direction.LEFT;
            } else {
                // A large discontinuity is treated as a lost/reacquired target.
                smoothedVelocity = 0f;
                lastSign = 0;
                reversalFrames = 0;
            }
        }

        filteredX = x;
        previousX = x;
        previousTime = timestampMs;
        return new Sample(true, x, velocity, direction, confidence, timestampMs);
    }

    private Sample missingAndAge(long timestampMs) {
        // Do not extrapolate a stale target: caller should hide/grey the indicator.
        if (previousTime > 0L && timestampMs - previousTime > 450L) reset();
        return Sample.missing(timestampMs);
    }

    public void reset() {
        previousX = Float.NaN;
        filteredX = Float.NaN;
        smoothedVelocity = 0f;
        previousTime = 0L;
        lastSign = 0;
        reversalFrames = 0;
    }

    private static final class Component {
        int area, minX, maxX, minY, maxY;
        Component(int x, int y) {
            area = 1; minX = maxX = x; minY = maxY = y;
        }
        void add(int x, int y) {
            area++;
            minX = Math.min(minX, x); maxX = Math.max(maxX, x);
            minY = Math.min(minY, y); maxY = Math.max(maxY, y);
        }
    }

    private static Component largestComponent(boolean[] mask, int width, int height) {
        boolean[] seen = new boolean[mask.length];
        int[] queue = new int[mask.length];
        Component largest = null;
        for (int start = 0; start < mask.length; start++) {
            if (!mask[start] || seen[start]) continue;
            int head = 0, tail = 0;
            queue[tail++] = start;
            seen[start] = true;
            int minX = start % width, maxX = minX;
            int minY = start / width, maxY = minY;
            int area = 0;
            while (head < tail) {
                int p = queue[head++];
                int x = p % width, y = p / width;
                area++;
                minX = Math.min(minX, x); maxX = Math.max(maxX, x);
                minY = Math.min(minY, y); maxY = Math.max(maxY, y);
                if (x > 0) tail = enqueue(p - 1, mask, seen, queue, tail);
                if (x + 1 < width) tail = enqueue(p + 1, mask, seen, queue, tail);
                if (y > 0) tail = enqueue(p - width, mask, seen, queue, tail);
                if (y + 1 < height) tail = enqueue(p + width, mask, seen, queue, tail);
            }
            // Discard narrow UI fragments and crane/rope-like shapes.
            int boxW = maxX - minX + 1, boxH = maxY - minY + 1;
            if (area >= MIN_COMPONENT_PIXELS && boxW >= 12 && boxH >= 12
                    && boxW <= width * 0.72f && boxH <= height * 0.95f) {
                if (largest == null || area > largest.area) {
                    largest = new Component(minX, minY);
                    largest.area = area;
                    largest.maxX = maxX; largest.maxY = maxY;
                }
            }
        }
        return largest;
    }

    private static int enqueue(int p, boolean[] mask, boolean[] seen,
                               int[] queue, int tail) {
        if (mask[p] && !seen[p]) {
            seen[p] = true;
            queue[tail++] = p;
        }
        return tail;
    }
}
