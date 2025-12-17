package com.kredily.location.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.work.*;

import com.kredily.location.R;
import com.kredily.location.service.LocationForegroundService;
import com.kredily.location.util.Constants;
import com.kredily.location.worker.LocationSyncWorker;

import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    MainViewModel vm;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_main);

        vm = new ViewModelProvider(this).get(MainViewModel.class);

        Button start = findViewById(R.id.startBtn);
        Button stop = findViewById(R.id.stopBtn);
        TextView pending = findViewById(R.id.pendingCount);

        start.setOnClickListener(v ->
                startService(new Intent(this, LocationForegroundService.class)));

        stop.setOnClickListener(v ->
                stopService(new Intent(this, LocationForegroundService.class)));

        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        WorkRequest syncWork =
                new PeriodicWorkRequest.Builder(
                        LocationSyncWorker.class,
                        15, TimeUnit.MINUTES)
                        .setConstraints(constraints)
                        .build();

        WorkManager.getInstance(this)
                .enqueueUniquePeriodicWork(
                        Constants.WORK_NAME_SYNC,
                        ExistingPeriodicWorkPolicy.KEEP,
                        (PeriodicWorkRequest) syncWork);

        vm.getPendingCount().observe(this, count -> {
            pending.setText("Pending: " + (count == null ? 0 : count));
        });
    }
}
