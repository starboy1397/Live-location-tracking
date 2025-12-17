package com.kredily.location.data.db;


import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "locations")
public class LocationEntity {

    @PrimaryKey(autoGenerate = true)
    public long id;

    public double latitude;
    public double longitude;
    public float accuracy;
    public float speed;
    public long timestamp;

    public boolean synced; // false = pending, true = synced
}
