package com.app.tourguide.database.entity;


import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.io.Serializable;

@Entity
public class TourPackage implements Serializable {

    @PrimaryKey(autoGenerate = true)
    private int id;

    @ColumnInfo(name = "package_id")
    private String packageId;

    //@TypeConverters(DataConverter.class)
    @ColumnInfo(name = "spots")
    private String spots; //custom type objects


    public TourPackage(String packageId, String spots) {
        this.packageId = packageId;
        this.spots = spots;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getPackageId() {
        return packageId;
    }

    public void setPackageId(String packageId) {
        this.packageId = packageId;
    }

    public String getSpots() {
        return spots;
    }

    public void setSpots(String spots) {
        this.spots = spots;
    }
}
