package com.finjacks.towerrushassistant;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.DisplayMetrics;
import android.view.WindowManager;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.util.concurrent.atomic.AtomicBoolean;

public class ScreenCaptureService extends Service {
    public static final String EXTRA_RESULT_CODE = "capture_result_code";
    public static final String EXTRA_RESULT_DATA = "capture_result_data";
    public static final String ACTION_STOP = "com.finjacks.towerrushassistant.STOP_CAPTURE";
    public static final String PREFS = "tower_rush_local_scan";
    public static final String KEY_REPORT = "latest_report";

    private static final String CHANNEL_ID = "tower_rush_capture";
    private static final int NOTIFICATION_ID = 72;
    private static final long SCAN_INTERVAL_MS = 1400L;

    private MediaProjection projection;
    private VirtualDisplay virtualDisplay;
    private ImageReader imageReader;
    private HandlerThread captureThread;
    private Handler captureHandler;
    private TextRecognizer recognizer;
    private final AtomicBoolean processing = new AtomicBoolean(false);
    private long lastScanAt;
    private volatile boolean stopping;

    private final MediaProjection.Callback projectionCallback =
            new MediaProjection.Callback() {
                @Override public void onStop() {
                    stopCaptureResources();
                    stopForeground(true);
                    stopSelf();
                }
            };

    @Override public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            stopCaptureResources();
            stopForeground(true);
            stopSelf();
            return START_NOT_STICKY;
        }
        if (projection != null) return START_NOT_STICKY;
        if (intent == null || !intent.hasExtra(EXTRA_RESULT_DATA)) {
            stopSelf();
            return START_NOT_STICKY;
        }

        Notification notification = buildNotification();
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(NOTIFICATION_ID, notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION);
        } else startForeground(NOTIFICATION_ID, notification);

        try {
            MediaProjectionManager manager =
                    (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
            Intent resultData = getResultData(intent);
            projection = manager.getMediaProjection(
                    intent.getIntExtra(EXTRA_RESULT_CODE, 0), resultData);
            if (projection == null) {
                stopForeground(true);
                stopSelf();
                return START_NOT_STICKY;
            }

            DisplayMetrics metrics = new DisplayMetrics();
            WindowManager windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
            windowManager.getDefaultDisplay().getRealMetrics(metrics);
            int width = metrics.widthPixels;
            int height = metrics.heightPixels;
            int density = metrics.densityDpi;

            captureThread = new HandlerThread("TowerRushScreenFrames");
            captureThread.start();
            captureHandler = new Handler(captureThread.getLooper());
            imageReader = ImageReader.newInstance(width, height,
                    PixelFormat.RGBA_8888, 2);
            imageReader.setOnImageAvailableListener(this::readAndRecognise, captureHandler);

            projection.registerCallback(projectionCallback, captureHandler);
            virtualDisplay = projection.createVirtualDisplay(
                    "TowerRush2AssistantCapture", width, height, density,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                    imageReader.getSurface(), null, captureHandler);
            if (virtualDisplay == null) {
                stopCaptureResources();
                stopForeground(true);
                stopSelf();
            } else {
                saveReport("Capture connected. Waiting for readable game text…");
            }
        } catch (SecurityException | IllegalStateException | NullPointerException error) {
            saveReport("Screen capture could not start. Stop and try granting Android capture permission again.");
            stopCaptureResources();
            stopForeground(true);
            stopSelf();
        }
        return START_NOT_STICKY;
    }

    private void readAndRecognise(ImageReader reader) {
        Image frame = null;
        try {
            frame = reader.acquireLatestImage();
            if (frame == null || stopping) return;
            long now = SystemClock.elapsedRealtime();
            if (now - lastScanAt < SCAN_INTERVAL_MS || !processing.compareAndSet(false, true)) return;
            lastScanAt = now;

            // ImageReader RGBA frames are copied into a temporary bitmap for ML Kit.
            final Image currentFrame = frame;
            android.graphics.Bitmap bitmap = rgbaImageToBitmap(currentFrame);
            if (bitmap == null) {
                processing.set(false);
                return;
            }
            InputImage input = InputImage.fromBitmap(bitmap, 0);
            recognizer.process(input)
                    .addOnSuccessListener(result -> {
                        if (!stopping) saveReport(GameScreenInterpreter.makeReport(result.getText()));
                    })
                    .addOnFailureListener(error -> {
                        if (!stopping) saveReport("Text recognition is temporarily unavailable. Keep the game screen visible and try again.");
                    })
                    .addOnCompleteListener(task -> {
                        bitmap.recycle();
                        currentFrame.close();
                        processing.set(false);
                    });
            frame = null; // ownership passes to the completion listener
        } catch (IllegalStateException | IllegalArgumentException error) {
            // The display may be shutting down while a frame is delivered.
        } finally {
            if (frame != null) frame.close();
        }
    }

    private android.graphics.Bitmap rgbaImageToBitmap(Image image) {
        Image.Plane[] planes = image.getPlanes();
        if (planes == null || planes.length == 0) return null;
        Image.Plane plane = planes[0];
        java.nio.ByteBuffer buffer = plane.getBuffer();
        int pixelStride = plane.getPixelStride();
        int rowStride = plane.getRowStride();
        int rowPadding = rowStride - pixelStride * image.getWidth();
        android.graphics.Bitmap padded = android.graphics.Bitmap.createBitmap(
                image.getWidth() + rowPadding / pixelStride,
                image.getHeight(), android.graphics.Bitmap.Config.ARGB_8888);
        padded.copyPixelsFromBuffer(buffer);
        android.graphics.Bitmap cropped = android.graphics.Bitmap.createBitmap(
                padded, 0, 0, image.getWidth(), image.getHeight());
        padded.recycle();
        return cropped;
    }

    private Intent getResultData(Intent intent) {
        if (Build.VERSION.SDK_INT >= 33) {
            return intent.getParcelableExtra(EXTRA_RESULT_DATA, Intent.class);
        }
        return intent.getParcelableExtra(EXTRA_RESULT_DATA);
    }

    private void saveReport(String value) {
        getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                .putString(KEY_REPORT, value).apply();
    }

    private Notification buildNotification() {
        Intent stopIntent = new Intent(this, ScreenCaptureService.class);
        stopIntent.setAction(ACTION_STOP);
        int pendingFlags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= 23) pendingFlags |= PendingIntent.FLAG_IMMUTABLE;
        PendingIntent stopPending = PendingIntent.getService(this, 73, stopIntent, pendingFlags);

        Notification.Builder builder = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(this, CHANNEL_ID) : new Notification.Builder(this);
        builder.setSmallIcon(android.R.drawable.ic_menu_view)
                .setContentTitle("Tower Rush 2 Assistant")
                .setContentText("On-device text reading active — tap Stop to end")
                .setOngoing(true)
                .addAction(android.R.drawable.ic_media_pause, "Stop analysis", stopPending);
        return builder.build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "Screen analysis", NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Shows when Tower Rush 2 Assistant is reading visible screen text");
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }
    }

    private void stopCaptureResources() {
        stopping = true;
        if (projection != null) {
            try { projection.unregisterCallback(projectionCallback); }
            catch (IllegalStateException ignored) { }
        }
        if (virtualDisplay != null) {
            virtualDisplay.release();
            virtualDisplay = null;
        }
        if (imageReader != null) {
            imageReader.setOnImageAvailableListener(null, null);
            imageReader.close();
            imageReader = null;
        }
        if (projection != null) {
            projection.stop();
            projection = null;
        }
        if (captureThread != null) {
            captureThread.quitSafely();
            captureThread = null;
            captureHandler = null;
        }
    }

    @Override public void onDestroy() {
        stopCaptureResources();
        if (recognizer != null) recognizer.close();
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) { return null; }
}
