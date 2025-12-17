package com.kredily.location.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.work.*;

import com.kredily.location.R;
import com.kredily.location.service.LocationForegroundService;
import com.kredily.location.util.Constants;
import com.kredily.location.util.NetworkUtil;
import com.kredily.location.worker.LocationSyncWorker;

import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    MainViewModel vm;
    private static final int LOCATION_PERMISSION_REQUEST = 1001;
    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_main);

        vm = new ViewModelProvider(this).get(MainViewModel.class);

        Button start = findViewById(R.id.startBtn);
        Button stop = findViewById(R.id.stopBtn);
        TextView pending = findViewById(R.id.pendingCount);

        start.setOnClickListener(v -> {
            if (!hasLocationPermission()) {
                requestLocationPermission();
                return;
            }

            if (!hasNotificationPermission()) {
                requestNotificationPermission();
                return;
            }
            startTrackingService();
        });

        stop.setOnClickListener(v ->
                stopService(new Intent(this, LocationForegroundService.class)));

        // ✅ Periodic WorkManager (ONLY ONCE)
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        PeriodicWorkRequest syncWork =
                new PeriodicWorkRequest.Builder(
                        LocationSyncWorker.class,
                        15, TimeUnit.MINUTES)
                        .setConstraints(constraints)
                        .setBackoffCriteria(
                                BackoffPolicy.EXPONENTIAL,
                                10, TimeUnit.SECONDS
                        )
                        .build();


        WorkManager.getInstance(this)
                .enqueueUniquePeriodicWork(
                        Constants.WORK_NAME_SYNC,
                        ExistingPeriodicWorkPolicy.KEEP,
                        syncWork
                );

        vm.getPendingCount().observe(this, count -> {
            pending.setText("Pending: " + (count == null ? 0 : count));
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (NetworkUtil.isOnline(this)) {
            triggerImmediateSync();
        }
    }


    private boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestLocationPermission() {
        ActivityCompat.requestPermissions(
                this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                LOCATION_PERMISSION_REQUEST
        );
    }

    private void startTrackingService() {
        Intent intent = new Intent(this, LocationForegroundService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
    }

    private boolean hasNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return true;
        }
        return ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    2001
            );
        }
    }


    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        // 1️⃣ Location permission result
        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                // Location granted → now check notification permission
                if (!hasNotificationPermission()) {
                    requestNotificationPermission();
                } else {
                    startTrackingService();
                }

            } else {
                Toast.makeText(
                        this,
                        "Location permission is required to start tracking",
                        Toast.LENGTH_SHORT
                ).show();
            }
        }

        // 2️⃣ Notification permission result
        else if (requestCode == 2001) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                // Notification granted → user can now start tracking
                startTrackingService();

            } else {
                Toast.makeText(
                        this,
                        "Notification permission is required to show tracking status",
                        Toast.LENGTH_SHORT
                ).show();
            }
        }
    }

    private void triggerImmediateSync() {

        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        OneTimeWorkRequest syncNow =
                new OneTimeWorkRequest.Builder(LocationSyncWorker.class)
                        .setConstraints(constraints)
                        .setBackoffCriteria(
                                BackoffPolicy.EXPONENTIAL,
                                10, TimeUnit.SECONDS
                        )
                        .build();


        WorkManager.getInstance(this)
                .enqueueUniqueWork(
                        "immediate_location_sync",
                        ExistingWorkPolicy.KEEP,
                        syncNow
                );
    }



}
