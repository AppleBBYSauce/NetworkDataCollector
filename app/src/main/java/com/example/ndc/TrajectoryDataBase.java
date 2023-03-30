package com.example.ndc;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import androidx.annotation.Nullable;

import org.checkerframework.checker.units.qual.C;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.Callable;

public class TrajectoryDataBase extends SQLiteOpenHelper {


    public TrajectoryDataBase(@Nullable Context context, @Nullable String name, @Nullable SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String create_GpsPool = "create table GpsPool(GpsHash VARCHAR(10), Tid INTEGER,   Cid INTEGER primary key, status INTEGER)";
        String create_Trajectory = "create table Trajectory(trajectory TEXT,       Tid INTEGER primary key,  date TEXT)";
        db.execSQL(create_GpsPool);
        db.execSQL(create_Trajectory);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }

}

class ManipulateDataBase {

    private static final TrajectoryDataBase db = new TrajectoryDataBase(Utils.context, "user.db", null, 1);
    private static final SQLiteDatabase db_writer = db.getWritableDatabase();
    private static final SQLiteDatabase db_reader = db.getReadableDatabase();

    public static Random rand = new Random();


    ManipulateDataBase() {
    }

    public static void StorageTrajectory(Trajectory T) {
        if (exist("Tid", T.Tid)) {
            Log.e("TR", "trajectory exist");
            return;
        }
        ContentValues values = new ContentValues();
        values.put("trajectory", T.Trajectory2String());
        values.put("Tid", T.Tid);
        values.put("date", T.date);
        db_writer.insert("Trajectory", null, values);
        Log.e("TR", "store trajectory");

    }

    public static void StorageGPS(STCoordination c) {
        if (exist("Cid", c.Cid)) {
            return;
        }
        ContentValues values = new ContentValues();
        values.put("Cid", c.Cid);
        values.put("Tid", c.Tid);
        values.put("status", c.status);
        values.put("GpsHash", Coordinate.encode(c.lat, c.lon, 9));
        db_writer.insert("GpsPool", null, values);
    }

    private static int gen() {
        int start_num = 2147483647;
        int end_num = 2147483646;
        if (rand.nextBoolean()) {
            return rand.nextInt(end_num);
        } else {
            return -rand.nextInt(start_num);
        }
    }

    public static Integer generateUnique(String type) {

        int b = gen();
        if (Objects.equals(type, "Cid")) {
            while (STCoordination.existCid(b) || exist("Cid", b)) {
                b = gen();
            }
        } else {
            while (Trajectory.existTid(b) || exist("Tid", b)) {
                b = gen();
            }
        }
        return b;
    }

    public static boolean exist(String type, int id) {
        if (Objects.equals(type, "Cid")) {
            String sql = "SELECT * FROM GpsPool WHERE Cid=" + id;
            Cursor cursor = db_reader.rawQuery(sql, null);
            if (cursor.moveToFirst()) {
                cursor.close();
                return true;
            } else {
                cursor.close();
                return false;
            }
        } else {
            String sql = "SELECT * FROM Trajectory WHERE Tid=" + id;
            Cursor cursor = db_reader.rawQuery(sql, null);
            if (cursor.moveToFirst()) {
                cursor.close();
                return true;
            } else {
                cursor.close();
                return false;
            }
        }

    }

    public static boolean existCid(Integer Cid) {
        String sql = "SELECT * FROM GpsPool WHERE Cid=" + Cid;
        Cursor cursor = db_reader.rawQuery(sql, null);
        if (cursor.moveToFirst()) {
            cursor.close();
            return true;
        }
        cursor.close();
        return false;
    }

    public static boolean existTid(Integer Tid) {
        String sql = "SELECT * FROM GpsPool WHERE Tid=" + Tid;
        Cursor cursor = db_reader.rawQuery(sql, null);
        if (cursor.moveToFirst()) {
            cursor.close();
            return true;
        }
        cursor.close();
        return false;
    }

    public static Trajectory DeSerializationTrajectory(Integer Tid) {
        String sql = "SELECT * FROM Trajectory WHERE Tid=" + Tid;
        Cursor cursor = db_reader.rawQuery(sql, null);
        ArrayList<STCoordination> stc = new ArrayList<>();
        if (cursor.moveToFirst()) {
            String trajectory = cursor.getString(0);
            for (String s : trajectory.split(",")) {
                Integer Cid = Integer.parseInt(s);
                if (!STCoordination.existCid(Cid)) {
                    stc.add(DeSerializationSTCoordinationFromCid(Cid, cursor));
                } else {
                    stc.add(STCoordination.getCoordinateFromCid(Cid));
                }
            }
        }
        cursor.close();
        return new Trajectory(Tid, stc);
    }


    public static STCoordination DeSerializationSTCoordinationFromCid(Integer Cid, Cursor cursor) {
        String sql = "SELECT * FROM GpsPool WHERE Cid=" + Cid;
        boolean flag = false;
        if (Objects.equals(cursor, null)) {
            cursor = db_reader.rawQuery(sql, null);
            flag = true;
        }
//        if(!cursor.moveToFirst()) return new STCoordination(4E-32, 4e-32, "1900-01-03 00:00:00");
        if (!cursor.moveToFirst()) return null;
        byte status = (byte) cursor.getInt(3);
        String geoHash = cursor.getString(0);
        Integer Tid = cursor.getInt(1);
        if (flag) cursor.close();
        return new STCoordination(geoHash, status, Tid);

    }

    public static List<STCoordination> SearchSurroundFromGeoHash(String geoHash, Integer precision) {
        HashSet<String> su = STCoordination.getSurroundGeoHash(geoHash, precision);
        List<STCoordination> st = new ArrayList<>();
        Cursor cursor = null;
        for (String s : su) {
            String sql = "SELECT * FROM GpsPool WHERE GpsHash LIKE" + "'" + s.substring(0, precision) + "%'";
            cursor = db_reader.rawQuery(sql, null);
            if (cursor.moveToFirst()) {
                Integer Cid = cursor.getInt(2);
                st.add(STCoordination.getCoordinateFromCid(Cid));
            }
        }
        if (cursor != null) {
            cursor.close();
        }
        return st;
    }

    /*
    This is test Function!!!
     */
    public static List<STCoordination> getAllSTCoordinate() {
        String sql = "SELECT * FROM GpsPool";
        List<STCoordination> st = new ArrayList<>();
        Cursor cursor = db_reader.rawQuery(sql, null);
        if (cursor.moveToFirst()) {
            do {
                Integer Cid = cursor.getInt(2);
                st.add(STCoordination.getCoordinateFromCid(Cid));
            }
            while (cursor.moveToNext());
        }
        cursor.close();
        return st;

    }

    public static void deleteAllTable(){
        String sql_gps = "delete from GpsPool";
        String sql_trajectory = "delete from Trajectory";
        db_writer.execSQL(sql_gps);
        db_writer.execSQL(sql_trajectory);
    }


}
