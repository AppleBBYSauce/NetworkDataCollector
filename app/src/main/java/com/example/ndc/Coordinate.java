package com.example.ndc;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class Coordinate {

    public double lon;

    public double lat;

    public double lon_rad;

    public double lat_rad;

    public Integer Cid;

    public static final String BASE32 = "0123456789bcdefghjkmnpqrstuvwxyz";

    private static final int[] BITS = {16, 8, 4, 2, 1};

    public static double rad(double c) {
        return c * Math.PI / 180.0;
    }

    /* invalid local value in Baidu */
    public boolean isValid() {
        return 4.9E-324 != lon && 4.9E-324 != lat && lon != -1 && lat != -1;
    }

    public static final HashMap Length2CellSize = new HashMap() {
        {
            put(7, new double[]{152.9, 152.4});
            put(8, new double[]{38.2, 19});
            put(9, new double[]{4.8, 4.8});
        }
    };


    Coordinate(double lon, double lat) {
        this.lon = lon;
        this.lat = lat;
        this.lon_rad = rad(lon);
        this.lat_rad = rad(lat);
    }

    /*
    precision:
    +---------------+--------+---------+
    | geoHash length | width  | height |
    +---------------+--------+---------+
    | 7              | 152.9m | 152.4m |
    | 8              | 38.2m  | 19m    |
    | 9              | 4.8m   | 4.8m   |
    +---------------+--------+---------+
     */

    public static String encode(double latitude, double longitude, int precision) {
        double[] latRange = new double[]{-90.0, 90.0};
        double[] lonRange = new double[]{-180.0, 180.0};
        StringBuilder geoHash = new StringBuilder();
        boolean switcher = true;
        int bits = 0;
        int counter = 0;

        while (geoHash.length() < precision) {
            double mid;
            if (switcher) {
                mid = (lonRange[0] + lonRange[1]) / 2.0;
                if (mid < longitude) {
                    bits |= Coordinate.BITS[counter];
                    lonRange[0] = mid;
                } else {
                    lonRange[1] = mid;
                }
            } else {
                mid = (latRange[0] + latRange[1]) / 2.0;
                if (mid < latitude) {
                    bits |= Coordinate.BITS[counter];
                    latRange[0] = mid;
                } else {
                    latRange[1] = mid;
                }
            }

            switcher = !switcher;
            if (counter < 4) {
                ++counter;
            } else {
                geoHash.append(Coordinate.BASE32.charAt(bits));
                bits = 0;
                counter = 0;
            }
        }

        if (bits != 0) {
            geoHash.append(Coordinate.BASE32.charAt(bits));
        }
        return geoHash.toString();
    }

    public static double[] decode(String geoHash, Integer precision) {
        double[] latRange = new double[]{-90.0, 90.0};
        double[] lonRange = new double[]{-180.0, 180.0};
        boolean isEven = true;

        for (int i = 0; i < precision; i++) {
            int ch = Coordinate.BASE32.indexOf(geoHash.charAt(i));
            for (int bit : BITS) {
                if (isEven) {
                    if ((ch & bit) != 0) {
                        lonRange[0] = (lonRange[0] + lonRange[1]) / 2.0;
                    } else {
                        lonRange[1] = (lonRange[0] + lonRange[1]) / 2.0;
                    }
                } else if ((ch & bit) != 0) {
                    latRange[0] = (latRange[0] + latRange[1]) / 2.0;
                } else {
                    latRange[1] = (latRange[0] + latRange[1]) / 2.0;
                }

                isEven = !isEven;
            }
        }
        return new double[]{(latRange[0] + latRange[1]) / 2.0, (lonRange[0] + lonRange[1]) / 2.0};
    }

    public static HashSet<String> getSurroundGeoHash(String geoHash, Integer precision) {
        int length = geoHash.length();
        double[] coordinate = decode(geoHash, precision);
        double[] cell_size = (double[]) Coordinate.Length2CellSize.get(geoHash.length());
        HashSet<String> res = new HashSet<>();
        double lat = coordinate[0];
        double lon = coordinate[1];
        assert cell_size != null;
        double lat_offset = cell_size[0] * 0.00001141;
        double lon_offset = cell_size[1] * 0.00000899;

        double left_lat = lat - lat_offset;
        double right_lat = lat + lat_offset;
        double up_lon = lon + lon_offset;
        double down_lon = lon - lon_offset;

        double[][] vicinity = new double[][]{
                {left_lat,   up_lon}, {lat,   up_lon}, {right_lat,   up_lon},
                {left_lat,      lon},                  {right_lat,      lon},
                {left_lat, down_lon}, {lat, down_lon}, {right_lat, down_lon},
        };
        for (double[] c: vicinity){
            res.add(Coordinate.encode(c[0], c[1], length));
        }
        return res;
    }

}


class STCoordination extends Coordinate {

    public Date date;

    public byte status;
    /*
    record the locality status
    +------------------------+-----------------------------+-----------------------------+
    | high 2bit              | middle 2bit                 | low 4bit                    |
    +------------------------+-----------------------------+-----------------------------+
    | 01 -> Stagnation point | 01 -> High signal strength  | 0000 -> Network unavailable |
    | 00 -> Move point       | 00 -> Low signal strength   | 0001 -> Mobile network      |
    |                        |                             | 0010 -> WIFI                |
    +------------------------+-----------------------------+-----------------------------+
     */

    public static int INVALID_CID = 2147483647;

    public int Cid; // unique id for coordinate

    public int Tid; // unique id for coordinate

    public static HashMap<Integer, STCoordination> CoordinationMap = new HashMap<>();

    public int[] SplitPoint = new int[]{};

    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyy-MM-dd HH:mm:SS");

    public static boolean existCid(Integer Cid){
        return STCoordination.CoordinationMap.containsKey(Cid);
    }

    /*
    Load from online collector
     */
    STCoordination(double lon, double lat, String date) throws ParseException {
        super(lon, lat);
        this.date = sdf.parse(date);
    }

    /*
    Load from database
     */
    STCoordination(String geoHash, byte status, Integer Tid) {
        super(0, 0);
        double[] lat_lon = Coordinate.decode(geoHash, geoHash.length());
        this.lat = lat_lon[0];
        this.lon = lat_lon[1];
        this.status = status;
        this.Tid = Tid;
    }

    /**
     * the trajectory split time point
     */
    public static boolean judgeBoundary(Date d) {
        Calendar cd = Calendar.getInstance();
        cd.setTime(d);
        int t = cd.get(Calendar.MINUTE);
        return t == 0 || t == 15 || t == 30 || t == 45;
    }

    /*
    get time span between two time points(minutes)
     */
//    public static Integer getTimeSpan(Date a, Date b){
//        Calendar a1 = Calendar.getInstance();
//        a1.setTime(a);
//        Calendar b1 = Calendar.getInstance();
//        a1.setTime(b);
//        return b1.get(Calendar.MINUTE) - a1.get(Calendar.MINUTE);
//    }
//
    public static int putInPool(STCoordination c) {
        if (Objects.equals(c, null)) {
            return INVALID_CID;
        }
        Integer b = ManipulateDataBase.generateUnique("Cid");
        c.Cid = b;
        CoordinationMap.put(b, c);
        return b;
    }

    public static STCoordination getCoordinateFromCid(Integer C_) {

        /* load from cache */
        if (STCoordination.CoordinationMap.containsKey(C_)) {
            return STCoordination.CoordinationMap.get(C_);
        }
        /* load from database */
        else {
            STCoordination new_ = ManipulateDataBase.DeSerializationSTCoordinationFromCid(C_, null);
            STCoordination.putInPool(new_);
            return new_;
        }
    }

    public static Long getTimeSpan(Date a, Date b){
        return (a.getTime() - b.getTime()) / 60000L;
    }

}


class Trajectory {

    public List<Integer> trajectory = new ArrayList<>();

    public int Tid;

    public static final int EARTH_RADIUS = 6371000;

    public static int VALID_TID = 2147483647;


    private static final HashMap<Integer, Trajectory> TrajectoryMap = new HashMap<>();

    public String date;


    Trajectory(int Tid) {
        this.Tid = Tid;
    }

    Trajectory(int Tid, List<STCoordination> discrete_point) {
        this.Tid = Tid;
        for (STCoordination c : discrete_point) {
            STCoordination.putInPool(c);
            this.trajectory.add(c.Cid);
        }
    }

    public static Trajectory getTrajectoryFromTid(Integer Tid) {
        if (TrajectoryMap.containsKey(Tid)) return TrajectoryMap.get(Tid);
        Trajectory t = ManipulateDataBase.DeSerializationTrajectory(Tid);
        TrajectoryMap.put(Tid, t);
        return t;
    }

    public String Trajectory2String(){
        StringBuilder sp = new StringBuilder();
        sp.append(this.trajectory.get(0));
        for (int i = 1; i < this.trajectory.size(); i++) {
            sp.append(",");
            sp.append(this.trajectory.get(i));
        }
        return sp.toString();
    }

    public static Integer putInPool(Trajectory t){
        if(Objects.equals(t, null)) return VALID_TID;
        if (TrajectoryMap.containsKey(t.Tid)) return t.Tid;
        Integer Tid = ManipulateDataBase.generateUnique("Tid");
        t.Tid = Tid;
        TrajectoryMap.put(Tid, t);
        return Tid;
    }

    public List<STCoordination> ExpandTrajectory() throws ParseException {
        List<STCoordination> res = new ArrayList<>();
        for (Integer C_ : this.trajectory) {
            res.add(STCoordination.getCoordinateFromCid(C_));
        }
        return res;
    }

    public void addPoint(int Cid) {
        this.trajectory.add(Cid);
    }

    public void remove(int index) {
        this.trajectory.remove(index);
    }

    public int getSize() {
        return this.trajectory.size();
    }

    public static Coordinate getCenterPoint(Coordinate c1, Coordinate c2){
        Double X = (Math.cos(c1.lat_rad) * Math.cos(c1.lon_rad) + Math.cos(c2.lat_rad) * Math.cos(c2.lon_rad)) / 2.0;
        Double Y = (Math.cos(c1.lat_rad) * Math.sin(c1.lon_rad) + Math.cos(c2.lat_rad) * Math.sin(c2.lon_rad)) / 2.0;
        double Z = (Math.sin(c1.lat_rad) + Math.sin(c2.lat_rad)) / 2.0;
        double Lon = Math.atan2(Y, X);
        double Hyp = Math.sqrt(X * X + Y * Y);
        double Lat = Math.atan2(Z, Hyp);
        return new Coordinate(Lon * 180 / Math.PI, Lat * 180 / Math.PI);
    }



    public static Double getDistance(Integer Cid1, Integer Cid2) throws ParseException {
        Coordinate c1 = STCoordination.getCoordinateFromCid(Cid1);
        Coordinate c2 = STCoordination.getCoordinateFromCid(Cid2);
        return Distance(c1, c2);
    }

    public static Double getDistance(Coordinate c1, Coordinate c2) {
        return Distance(c1, c2);
    }

    private static Double Distance(Coordinate c1, Coordinate c2) {
        double radLat1 = c1.lat_rad;
        double radLat2 = c2.lat_rad;
        double a = radLat1 - radLat2;
        double b = c1.lon_rad - c2.lon_rad;
        double s = 2 * Math.asin(Math.sqrt(Math.pow(Math.sin(a / 2), 2)) + Math.cos(radLat1) * Math.cos(radLat2) * Math.pow(Math.sin(b / 2), 2));
        s = s * EARTH_RADIUS;
        s = Math.round(s * 10000) / 10000.0;
        return s;
    }

    public Trajectory getSubTrajectory(Integer i, Integer j) {
        Trajectory t = new Trajectory(this.Tid);
        t.trajectory = this.trajectory.subList(i, j);
        return t;
    }

    public List<Integer> getSubTrajectoryIndex(Integer i, Integer j) {
        return this.trajectory.subList(i, j);
    }

    public int Index2Cid(Integer index) {
        return trajectory.get(index);
    }

    public STCoordination getCoordinateFromIndex(Integer index) {
        Integer Cid = this.trajectory.get(index);
        return STCoordination.getCoordinateFromCid(Cid);
    }

    public static boolean existTid(Integer Tid){
        return TrajectoryMap.containsKey(Tid);
    }

    public static Trajectory getAffiliateTrajectory(Integer Cid){
        STCoordination c = STCoordination.getCoordinateFromCid(Cid);
        return Trajectory.getTrajectoryFromTid(c.Tid);
    }

    public static List<Trajectory> getReference(){
        return  new ArrayList<>(Trajectory.TrajectoryMap.values());
    }

}