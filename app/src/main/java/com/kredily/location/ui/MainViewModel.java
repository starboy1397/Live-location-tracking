package com.kredily.location.ui;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.kredily.location.data.repository.LocationRepository;

public class MainViewModel extends AndroidViewModel {
    private final LocationRepository repository;
    public MainViewModel(@NonNull Application app) {
        super(app);
        repository = new LocationRepository(app);
    }

    public LiveData<Integer> getPendingCount() {
        return repository.getPendingCount();
    }
}
