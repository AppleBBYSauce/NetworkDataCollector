package com.example.ndc;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

public class TrajectoryDataBase extends SQLiteOpenHelper {


    public TrajectoryDataBase(@Nullable Context context, @Nullable String name, @Nullable SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String create_GpsPool    = "create table GpsPool(GpsHash TEXT primary key, lon REAL,        lat REAL)";
        String create_GpsMap     = "create table GpsMap(Cid INTEGER primary key,   Tid INTEGER,     status INTEGER,   GpasHash TEXT)";
        String create_Trajectory = "create table Trajectory(trajectory TEXT,       Tid primary key, date TEXT)";
        db.execSQL(create_GpsMap);
        db.execSQL(create_GpsPool);
        db.execSQL(create_Trajectory);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    public void StorageTrajectory(String trajectory, Integer Tid, String date){
        ContentValues values = new ContentValues();
        values.put("trajectory", trajectory);
        values.put("Tid", Tid);
        values.put("date", date);
    }

    public void StorageGPS(Integer Cid, Integer Tid, String GpsHash, short status){
        ContentValues values = new ContentValues();
        values.put("Cid", Cid);
        values.put("Tid", Tid);
        values.put("status", status);


        values.put("GpsHash", GpsHash);

    }







}
