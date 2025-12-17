package com.kredily.location.worker;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.kredily.location.data.repository.LocationRepository;

public class LocationSyncWorker extends Worker {

    public LocationSyncWorker(@NonNull Context context,
                              @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        new LocationRepository(getApplicationContext())
                .syncPendingLocations();
        return Result.success();
    }
}
