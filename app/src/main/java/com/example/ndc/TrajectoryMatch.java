package com.example.ndc;

import java.util.ArrayList;
import java.util.List;

public class TrajectoryMatch {



    public static double[][] MatrixMultiple(double[][] A, double[][] B) {
        int[] A_shape = new int[]{A.length, A[0].length};
        int[] B_shape = new int[]{B.length, B[0].length};
        double[][] C = new double[A_shape[0]][B_shape[1]];

        for (int i = 0; i < A_shape[0]; i++) {
            for (int j = 0; j < B_shape[1]; j++) {
                double c = 0;
                for (int k = 0; k < A_shape[1]; k++) {
                    c += A[i][k] * B[k][j];
                }
                C[i][j] = c;
            }
        }
        return C;
    }

    public double[] normalize(double[] weight) {
        double min = weight[0];
        double max = weight[1];
        for (double value : weight) {
            if (value < min) min = value;
            if (value > max) max = value;
        }
        for (int i = 0; i < weight.length; i++) {
            weight[i] = (weight[i] - min) / max;
        }
        return weight;
    }

    public double[] getInitialProbability(STCoordination cur_pos, ArrayList<STCoordination> st) {
//        List<STCoordination> st = ManipulateDataBase.SearchSurroundFromGeoHash(Coordinate.encode(cur_pos.lat, cur_pos.lon, precise), precise);
        double[] probability = new double[st.size()];



        return probability;

    }


}
