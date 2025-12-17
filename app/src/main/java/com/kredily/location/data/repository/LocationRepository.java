package com.kredily.location.data.repository;

import android.content.Context;
import android.location.Location;

import androidx.lifecycle.LiveData;

import com.google.firebase.firestore.FirebaseFirestore;
import com.kredily.location.data.db.AppDatabase;
import com.kredily.location.data.db.LocationEntity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

public class LocationRepository {

    private final AppDatabase db;
    private final FirebaseFirestore firestore;

    public LocationRepository(Context context) {
        db = AppDatabase.getInstance(context);
        firestore = FirebaseFirestore.getInstance();
    }

    public void saveLocation(Location location) {
        Executors.newSingleThreadExecutor().execute(() -> {
            LocationEntity e = new LocationEntity();
            e.latitude = location.getLatitude();
            e.longitude = location.getLongitude();
            e.accuracy = location.getAccuracy();
            e.speed = location.getSpeed();
            e.timestamp = location.getTime();
            e.synced = false;

            db.locationDao().insert(e);
        });
    }

    public void syncPendingLocations() {
        Executors.newSingleThreadExecutor().execute(() -> {
            List<LocationEntity> pending = db.locationDao().getPendingLocations();

            for (LocationEntity e : pending) {
                Map<String, Object> payload = new HashMap<>();
                payload.put("employeeId", "EMP001");
                payload.put("latitude", e.latitude);
                payload.put("longitude", e.longitude);
                payload.put("accuracy", e.accuracy);
                payload.put("speed", e.speed);
                payload.put("timestamp", e.timestamp);

                firestore.collection("locations")
                        .add(payload)
                        .addOnSuccessListener(r ->
                                Executors.newSingleThreadExecutor()
                                        .execute(() -> db.locationDao().markSynced(e.id))
                        );
            }
        });
    }

    public LiveData<Integer> getPendingCount() {
        return db.locationDao().pendingCount();
    }
}
