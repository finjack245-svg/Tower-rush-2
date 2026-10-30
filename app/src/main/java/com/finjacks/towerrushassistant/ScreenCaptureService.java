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
import android.util.DisplayMetrics;
import android.view.WindowManager;

public class ScreenCaptureService extends Service {
    public static final String EXTRA_RESULT_CODE = "capture_result_code";
    public static final String EXTRA_RESULT_DATA = "capture_result_data";
    public static final String ACTION_STOP = "com.finjacks.towerrushassistant.STOP_CAPTURE";

    private static final String CHANNEL_ID = "tower_rush_capture";
    private static final int NOTIFICATION_ID = 72;

    private MediaProjection projection;
    private VirtualDisplay virtualDisplay;
    private ImageReader imageReader;
    private HandlerThread captureThread;
    private Handler captureHandler;
    private final MediaProjection.Callback projectionCallback =
            new MediaProjection.Callback() {
                @Override
                public void onStop() {
                    stopCaptureResources();
                    stopSelf();
                }
            };

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            stopCaptureResources();
            stopForeground(true);
            stopSelf();
            return START_NOT_STICKY;
        }

        if (intent == null || !intent.hasExtra(EXTRA_RESULT_DATA)) {
            stopSelf();
            return START_NOT_STICKY;
        }

        Notification notification = buildNotification();
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(NOTIFICATION_ID, notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION);
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }

        try {
            MediaProjectionManager manager =
                    (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
            Intent resultData = getResultData(intent);
            projection = manager.getMediaProjection(
                    intent.getIntExtra(EXTRA_RESULT_CODE, 0), resultData);
            if (projection == null) {
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
            imageReader.setOnImageAvailableListener(reader -> {
                Image frame = null;
                try {
                    frame = reader.acquireLatestImage();
                    // Stage 2 only verifies that frames arrive. Do not store or transmit them.
                } catch (IllegalStateException ignored) {
                    // Reader may be closing while the user stops capture.
                } finally {
                    if (frame != null) frame.close();
                }
            }, captureHandler);

            projection.registerCallback(projectionCallback, captureHandler);
            virtualDisplay = projection.createVirtualDisplay(
                    "TowerRush2AssistantCapture",
                    width, height, density,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                    imageReader.getSurface(), null, captureHandler);
            if (virtualDisplay == null) {
                stopCaptureResources();
                stopSelf();
            }
        } catch (SecurityException | IllegalStateException | NullPointerException error) {
            stopCaptureResources();
            stopSelf();
        }
        return START_NOT_STICKY;
    }

    private Intent getResultData(Intent intent) {
        if (Build.VERSION.SDK_INT >= 33) {
            return intent.getParcelableExtra(EXTRA_RESULT_DATA, Intent.class);
        }
        return intent.getParcelableExtra(EXTRA_RESULT_DATA);
    }

    private Notification buildNotification() {
        Intent stopIntent = new Intent(this, ScreenCaptureService.class);
        stopIntent.setAction(ACTION_STOP);
        int pendingFlags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= 23) pendingFlags |= PendingIntent.FLAG_IMMUTABLE;
        PendingIntent stopPending = PendingIntent.getService(
                this, 73, stopIntent, pendingFlags);

        Notification.Builder builder = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(this, CHANNEL_ID)
                : new Notification.Builder(this);
        builder.setSmallIcon(android.R.drawable.ic_menu_view)
                .setContentTitle("Tower Rush 2 Assistant")
                .setContentText("Screen capture active — tap Stop to end sharing")
                .setOngoing(true)
                .addAction(android.R.drawable.ic_media_pause, "Stop capture", stopPending);
        return builder.build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "Screen capture", NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Shows when Tower Rush 2 Assistant is sharing the screen");
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    private void stopCaptureResources() {
        if (projection != null) {
            try {
                projection.unregisterCallback(projectionCallback);
            } catch (IllegalStateException ignored) { }
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

    @Override
    public void onDestroy() {
        stopCaptureResources();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
