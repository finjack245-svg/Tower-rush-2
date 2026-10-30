package com.finjacks.towerrushassistant;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class MainActivity extends Activity {
    private static final int REQUEST_CAPTURE = 41;
    private static final int REQUEST_NOTIFICATIONS = 42;

    private TextView status;
    private TextView report;
    private Button startButton;
    private Button stopButton;
    private final Handler uiHandler = new Handler();
    private final Runnable refreshReport = new Runnable() {
        @Override public void run() {
            if (report != null) {
                String value = getSharedPreferences(ScreenCaptureService.PREFS, MODE_PRIVATE)
                        .getString(ScreenCaptureService.KEY_REPORT, "No scan yet. Start screen capture to read visible game text.");
                report.setText(value);
            }
            uiHandler.postDelayed(this, 1200);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        buildDashboard();
        requestNotificationPermissionIfNeeded();
    }

    private void buildDashboard() {
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(22, 28, 22, 22);
        page.setBackgroundColor(0xFF0F121B);

        TextView title = new TextView(this);
        title.setText("TOWER RUSH 2");
        title.setTextColor(0xFFFFFFFF);
        title.setTextSize(26);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 12, 0, 5);
        page.addView(title, matchWrap());

        TextView subtitle = new TextView(this);
        subtitle.setText("SCREEN READER  •  ASSISTED MODE");
        subtitle.setTextColor(0xFF70BEFF);
        subtitle.setTextSize(12);
        subtitle.setGravity(Gravity.CENTER);
        subtitle.setPadding(0, 0, 0, 18);
        page.addView(subtitle, matchWrap());

        status = new TextView(this);
        status.setText("SCREEN CAPTURE IS OFF");
        status.setTextColor(0xFFE1E6F0);
        status.setTextSize(14);
        status.setGravity(Gravity.CENTER);
        status.setPadding(14, 16, 14, 16);
        status.setBackgroundColor(0xFF1D2330);
        page.addView(status, matchWrap());

        startButton = new Button(this);
        startButton.setText("Start screen analysis");
        startButton.setAllCaps(false);
        LinearLayout.LayoutParams startParams = matchWrap();
        startParams.topMargin = 12;
        page.addView(startButton, startParams);
        startButton.setOnClickListener(v -> requestScreenCapture());

        stopButton = new Button(this);
        stopButton.setText("Stop screen analysis");
        stopButton.setAllCaps(false);
        stopButton.setEnabled(false);
        page.addView(stopButton, matchWrap());
        stopButton.setOnClickListener(v -> stopScreenCapture());

        TextView reportTitle = new TextView(this);
        reportTitle.setText("LIVE SCREEN OBSERVATIONS");
        reportTitle.setTextColor(0xFF70BEFF);
        reportTitle.setTextSize(13);
        reportTitle.setPadding(2, 22, 2, 8);
        page.addView(reportTitle, matchWrap());

        report = new TextView(this);
        report.setText("No scan yet. Start screen capture to read visible game text.");
        report.setTextColor(0xFFE1E6F0);
        report.setTextSize(14);
        report.setPadding(16, 18, 16, 18);
        report.setBackgroundColor(0xFF1D2330);

        ScrollView scroll = new ScrollView(this);
        scroll.addView(report);
        LinearLayout.LayoutParams reportParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        page.addView(scroll, reportParams);

        TextView safety = new TextView(this);
        safety.setText("Privacy: OCR runs on this phone. Screen images are not saved or uploaded. The reader only reports visible text; it does not recommend bets, press BUILD, activate AUTO, buy bonuses, or predict random results. You control every game action.");
        safety.setTextColor(0xFFB0B9C8);
        safety.setTextSize(11);
        safety.setPadding(4, 14, 4, 4);
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
        if (Build.VERSION.SDK_INT >= 26) startForegroundService(service);
        else startService(service);

        status.setText("Screen analysis active. Switch to Tower Rush 2; recognised text will appear here when you return.");
        stopButton.setEnabled(true);
    }

    private void stopScreenCapture() {
        Intent service = new Intent(this, ScreenCaptureService.class);
        service.setAction(ScreenCaptureService.ACTION_STOP);
        startService(service);
        status.setText("Stop requested. Screen analysis is ending.");
        startButton.setEnabled(true);
        stopButton.setEnabled(false);
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    @Override protected void onResume() {
        super.onResume();
        uiHandler.post(refreshReport);
    }

    @Override protected void onPause() {
        uiHandler.removeCallbacks(refreshReport);
        super.onPause();
    }
}
