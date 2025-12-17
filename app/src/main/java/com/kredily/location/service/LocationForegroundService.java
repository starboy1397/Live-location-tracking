package com.kredily.location.service;

import android.Manifest;
import android.app.Service;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.*;
import com.kredily.location.data.repository.LocationRepository;
import com.kredily.location.util.Constants;
import com.kredily.location.R;

public class LocationForegroundService extends Service {

    private FusedLocationProviderClient client;
    private LocationCallback callback;
    private LocationRepository repository;

    @Override
    public void onCreate() {
        super.onCreate();
        repository = new LocationRepository(this);
        client = LocationServices.getFusedLocationProviderClient(this);
        createNotificationChannel();
        startForeground(1, buildNotification());
        startLocationUpdates();
    }

    private void startLocationUpdates() {

        //  Runtime permission check (MANDATORY) above android 6.0+
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED) {

            // Permission not granted → stop service safely
            stopSelf();
            return;
        }

        LocationRequest request = LocationRequest.create();
        request.setInterval(Constants.LOCATION_INTERVAL);
        request.setFastestInterval(Constants.LOCATION_INTERVAL);
        request.setPriority(Priority.PRIORITY_HIGH_ACCURACY);

        callback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult result) {
                for (Location location : result.getLocations()) {
                    repository.saveLocation(location);
                }
            }
        };

        client.requestLocationUpdates(request, callback, getMainLooper());
    }


    private Notification buildNotification() {
        return new NotificationCompat.Builder(this, Constants.CHANNEL_ID)
                .setContentTitle("Location Tracking")
                .setContentText("Tracking active")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel =
                    new NotificationChannel(Constants.CHANNEL_ID,
                            "Location Tracking",
                            NotificationManager.IMPORTANCE_LOW);
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }
    }

    @Override
    public void onDestroy() {
        client.removeLocationUpdates(callback);
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
