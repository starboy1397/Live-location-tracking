package com.kredily.location.worker;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.kredily.location.data.repository.LocationRepository;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class LocationSyncWorker extends Worker {

    public LocationSyncWorker(
            @NonNull Context context,
            @NonNull WorkerParameters params
    ) {
        super(context, params);
    }


    @NonNull
    @Override
    public Result doWork() {

        Log.d("LocationSyncWorker", "doWork() started at " + System.currentTimeMillis());

        LocationRepository repo =
                new LocationRepository(getApplicationContext());

        CountDownLatch latch = new CountDownLatch(1);

        repo.syncPendingLocationsBlocking(latch);

        try {
            // 🔴 Block Worker until Firebase sync completes
            boolean finished = latch.await(2, TimeUnit.MINUTES);

            return finished ? Result.success() : Result.retry();

        } catch (InterruptedException e) {
            return Result.retry();
        }
    }
}
