package com.app.tourguide.database;


import androidx.room.Database;
import androidx.room.RoomDatabase;

import com.app.tourguide.database.dao.TourPackageDao;
import com.app.tourguide.database.entity.PackageSpots;
import com.app.tourguide.database.entity.TourPackage;


@Database(entities = {TourPackage.class, PackageSpots.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    public abstract TourPackageDao tourPackageDao();

}
