package com.kredily.location.data.repository;

import android.content.Context;
import android.location.Location;

import androidx.lifecycle.LiveData;

import com.google.firebase.firestore.FirebaseFirestore;
import com.kredily.location.data.db.AppDatabase;
import com.kredily.location.data.db.LocationEntity;
import com.kredily.location.util.NetworkUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class LocationRepository {

    private final AppDatabase db;
    private final FirebaseFirestore firestore;
    private final Context context;


    public LocationRepository(Context context) {
        this.context = context.getApplicationContext();
        db = AppDatabase.getInstance(context);
        firestore = FirebaseFirestore.getInstance();
    }

    public void saveLocation(Location location) {
        Executors.newSingleThreadExecutor().execute(() -> {

            //  Always save to Room first
            LocationEntity e = new LocationEntity();
            e.latitude = location.getLatitude();
            e.longitude = location.getLongitude();
            e.accuracy = location.getAccuracy();
            e.speed = location.getSpeed();
            e.timestamp = location.getTime();
            e.synced = false;

            long rowId = db.locationDao().insertAndReturnId(e);
            e.id = rowId;

            // If internet is ON → upload immediately
            if (NetworkUtil.isOnline(context)) {
                uploadToFirebase(e);
            }
        });
    }

    private void uploadToFirebase(LocationEntity e) {

        Map<String, Object> payload = new HashMap<>();
        payload.put("employeeId", "EMP001");
        payload.put("latitude", e.latitude);
        payload.put("longitude", e.longitude);
        payload.put("accuracy", e.accuracy);
        payload.put("timestamp", e.timestamp);
        payload.put("speed", e.speed);

        firestore.collection("locations")
                .document(String.valueOf(e.id))
                .set(payload)
                .addOnSuccessListener(r -> {
                    Executors.newSingleThreadExecutor().execute(() ->
                            db.locationDao().markSynced(e.id)
                    );
                })
                .addOnFailureListener(err -> {

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
                        .document(String.valueOf(e.id))
                        .set(payload)
                        .addOnSuccessListener(r ->
                                Executors.newSingleThreadExecutor()
                                        .execute(() -> db.locationDao().markSynced(e.id))
                        );
            }
        });
    }

    public void syncPendingLocationsBlocking(CountDownLatch latch) {

        Executors.newSingleThreadExecutor().execute(() -> {

            List<LocationEntity> pending = db.locationDao().getPendingLocations();

            if (pending.isEmpty()) {
                latch.countDown();
                return;
            }

            AtomicInteger remaining = new AtomicInteger(pending.size());

            for (LocationEntity e : pending) {

                Map<String, Object> payload = new HashMap<>();
                payload.put("employeeId", "EMP001");
                payload.put("latitude", e.latitude);
                payload.put("longitude", e.longitude);
                payload.put("accuracy", e.accuracy);
                payload.put("speed", e.speed);
                payload.put("timestamp", e.timestamp);

                firestore.collection("locations")
                        .document(String.valueOf(e.id))
                        .set(payload)
                        .addOnSuccessListener(r -> {
                            Executors.newSingleThreadExecutor().execute(() -> {
                                db.locationDao().markSynced(e.id);

                                if (remaining.decrementAndGet() == 0) {
                                    latch.countDown();
                                }
                            });
                        })
                        .addOnFailureListener(err -> {
                            // Let WorkManager retry later
                            latch.countDown();
                        });
            }
        });
    }


    public LiveData<Integer> getPendingCount() {
        return db.locationDao().pendingCount();
    }
}
