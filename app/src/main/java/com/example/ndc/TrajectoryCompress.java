package com.example.ndc;

import java.util.ArrayList;

public class TrajectoryCompress {


    private static final double EARTH_RADIUS1 = 6371000;
    public static double _compress_rate;
    public static double _error;

    public static int buffer_size = 5;

    public static ArrayList<Double[]> Buffer = new ArrayList<>();
    public static ArrayList<Double> Priority = new ArrayList<>();

    public TrajectoryCompress(double compress_rate, double error) {
        _compress_rate = compress_rate;
        _error = error;
    }


    public int getMinPriority() {
        Double minPriority = Priority.get(1);
        int index = 1;
        for (int i = 2; i < Priority.size(); i++) {
            Double p = Priority.get(i);
            if (minPriority > p) {
                index = i;
                minPriority = p;
            }
        }
        return index;
    }

    public double getPriority(Double[] start, Double[] end, Double[] mid){
        Double[] center = getCenterPoint(start[0], start[1], end[0], end[1]);
        return getDistance(center[0], center[1], mid[0], mid[1]);
    }

    public void addPoint(Double[] new_point, Double[] next_point) {
        if (Buffer.size() == buffer_size) {
            int min_index = getMinPriority();
            double p = Priority.get(min_index);
            Priority.set(min_index-1, Priority.get(min_index-1) + p);
            Priority.set(min_index+1, Priority.get(min_index+1) + p);
            Buffer.remove(min_index);
            Priority.remove(min_index);
        } else if (Buffer.size() == 0) {
            Priority.add(EARTH_RADIUS1*2);
            Buffer.add(new_point);
            return;
        }
        Priority.add(getPriority(Buffer.get(Buffer.size()-1), next_point, new_point));
        Buffer.add(new_point);

    }


    public static double rad(double c) {
        return c * Math.PI / 180.0;
    }

    public double getDistance(double lon1, double lat1, double lon2, double lat2) {
        double radLat1 = rad(lat1);
        double radLat2 = rad(lat2);
        double a = radLat1 - radLat2;
        double b = rad(lon1) - rad(lon2);
        double s = 2 * Math.asin(Math.sqrt(Math.pow(Math.sin(a / 2), 2)) + Math.cos(radLat1) * Math.cos(radLat2) * Math.pow(Math.sin(b / 2), 2));
        s = s * EARTH_RADIUS1;
        s = Math.round(s * 10000) / 10000.0;
        return s;

    }

    public static Double[] getCenterPoint(double lon1, double lat1, double lon2, double lat2) {
        lon1 = rad(lon1);
        lat1 = rad(lat1);
        lon2 = rad(lon2);
        lat2 = rad(lat2);
        double X = (Math.cos(lat1) * Math.cos(lon1) + Math.cos(lat2) * Math.cos(lon2)) / 2.0;
        double Y = (Math.cos(lat1) * Math.sin(lon1) + Math.cos(lat2) * Math.sin(lon2)) / 2.0;
        double Z = (Math.sin(lat1) + Math.sin(lat2)) / 2.0;
        double Lon = Math.atan2(Y, X);
        double Hyp = Math.sqrt(X * X + Y * Y);
        double Lat = Math.atan2(Z, Hyp);
        Double[] centreAxis = {Lon * 180 / Math.PI, Lat * 180 / Math.PI};
        return centreAxis;
    }


}
