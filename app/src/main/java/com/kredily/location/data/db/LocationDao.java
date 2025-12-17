package com.kredily.location.data.db;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface LocationDao {

    @Insert
    void insert(LocationEntity entity);

    @Query("SELECT * FROM locations WHERE synced = 0 ORDER BY timestamp ASC")
    List<LocationEntity> getPendingLocations();

    @Query("UPDATE locations SET synced = 1 WHERE id = :id")
    void markSynced(long id);

    @Query("SELECT COUNT(*) FROM locations WHERE synced = 0")
    LiveData<Integer> pendingCount();

    @Insert
    long insertAndReturnId(LocationEntity entity);
}
