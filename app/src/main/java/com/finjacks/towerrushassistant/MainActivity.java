package com.finjacks.towerrushassistant;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {
    private static final int REQUEST_CAPTURE = 41;
    private static final int REQUEST_NOTIFICATIONS = 42;

    private TextView status;
    private Button startButton;
    private Button stopButton;
    private boolean captureRequested;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        buildDashboard();
        requestNotificationPermissionIfNeeded();
    }

    private void buildDashboard() {
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setGravity(Gravity.CENTER_HORIZONTAL);
        page.setPadding(24, 30, 24, 24);
        page.setBackgroundColor(0xFF0F121B);

        TextView title = new TextView(this);
        title.setText("TOWER RUSH 2");
        title.setTextColor(0xFFFFFFFF);
        title.setTextSize(27);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 14, 0, 6);
        page.addView(title, matchWrap());

        TextView subtitle = new TextView(this);
        subtitle.setText("ASSISTED PLAYER  •  GALAXSYS");
        subtitle.setTextColor(0xFF70BEFF);
        subtitle.setTextSize(13);
        subtitle.setGravity(Gravity.CENTER);
        subtitle.setPadding(0, 0, 0, 24);
        page.addView(subtitle, matchWrap());

        status = new TextView(this);
        status.setText("SCREEN CAPTURE IS OFF\n\nStart capture when you are ready. Android will show its own permission screen before sharing your display.");
        status.setTextColor(0xFFE1E6F0);
        status.setTextSize(15);
        status.setGravity(Gravity.CENTER);
        status.setPadding(18, 22, 18, 22);
        status.setBackgroundColor(0xFF1D2330);
        page.addView(status, matchWrap());

        startButton = new Button(this);
        startButton.setText("Start screen capture");
        startButton.setAllCaps(false);
        LinearLayout.LayoutParams startParams = matchWrap();
        startParams.topMargin = 22;
        page.addView(startButton, startParams);
        startButton.setOnClickListener(v -> requestScreenCapture());

        stopButton = new Button(this);
        stopButton.setText("Stop screen capture");
        stopButton.setAllCaps(false);
        stopButton.setEnabled(false);
        page.addView(stopButton, matchWrap());
        stopButton.setOnClickListener(v -> stopScreenCapture());

        TextView next = new TextView(this);
        next.setText("NEXT: recognise visible Tower Rush 2 information and explain uncertainty. This version only establishes the secure Android capture session; it does not yet read or interpret game details.");
        next.setTextColor(0xFFB0B9C8);
        next.setTextSize(13);
        next.setGravity(Gravity.CENTER);
        next.setPadding(8, 24, 8, 12);
        page.addView(next, matchWrap());

        TextView safety = new TextView(this);
        safety.setText("Privacy and control: capture begins only after Android permission. Frames are immediately discarded in this stage; nothing is saved or uploaded. No bets or game buttons are tapped. You operate the game yourself.");
        safety.setTextColor(0xFFB0B9C8);
        safety.setTextSize(12);
        safety.setGravity(Gravity.CENTER);
        safety.setPadding(8, 8, 8, 8);
        page.addView(safety, matchWrap());

        setContentView(page);
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    REQUEST_NOTIFICATIONS);
        }
    }

    private void requestScreenCapture() {
        MediaProjectionManager manager =
                (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        if (manager == null) {
            status.setText("Screen capture is unavailable on this device.");
            return;
        }
        startButton.setEnabled(false);
        status.setText("Waiting for Android screen-capture permission…");
        startActivityForResult(manager.createScreenCaptureIntent(), REQUEST_CAPTURE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_CAPTURE) return;

        if (resultCode != RESULT_OK || data == null) {
            startButton.setEnabled(true);
            status.setText("Permission was not granted. Capture remains off.");
            return;
        }

        Intent service = new Intent(this, ScreenCaptureService.class);
        service.putExtra(ScreenCaptureService.EXTRA_RESULT_CODE, resultCode);
        service.putExtra(ScreenCaptureService.EXTRA_RESULT_DATA, data);
        if (Build.VERSION.SDK_INT >= 26) {
            startForegroundService(service);
        } else {
            startService(service);
        }
        captureRequested = true;
        status.setText("Starting secure capture… Check the persistent notification. Then switch to Tower Rush 2. This version does not analyse frames yet.");
        stopButton.setEnabled(true);
    }

    private void stopScreenCapture() {
        Intent service = new Intent(this, ScreenCaptureService.class);
        service.setAction(ScreenCaptureService.ACTION_STOP);
        startService(service);
        captureRequested = false;
        status.setText("Stop requested. Screen capture will end and the notification will disappear.");
        startButton.setEnabled(true);
        stopButton.setEnabled(false);
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
