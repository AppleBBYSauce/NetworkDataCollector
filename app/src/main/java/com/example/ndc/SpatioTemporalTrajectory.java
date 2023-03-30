package com.example.ndc;

import android.os.Build;
import android.util.Log;

import java.text.ParseException;
import java.util.*;

public class SpatioTemporalTrajectory {


    public static class REST {


        /* parameters for redundancy*/
        public static final Integer REDUNDANCY_THRESHOLD = 5;
        public static final Integer REDUNDANCY_DISTANCE = 10;

        /* parameters for SpatioTemporalTrajectory.REST*/
        public static final Integer MATCH_THRESHOLD = 5;
        public static final Integer MATCH_DISTANCE = 10;

        public static final Integer CLOSE_THRESHOLD = 8;

        public List<Trajectory> ReferenceSet = null;


        /* judge whether T is the redundancy of reference trajectory R */
        public boolean judgeRedundancy(Trajectory T, Trajectory R) throws ParseException {
            float count = 0.0F;

            for (Integer Cid_T : T.trajectory) {
                for (Integer Cid_R : T.trajectory) {
                    if (Trajectory.getDistance(Cid_R, Cid_T) <= REST.REDUNDANCY_DISTANCE) {
                        ++count;
                    }
                }
            }
            return count / (float) T.getSize() > REST.REDUNDANCY_THRESHOLD;
        }

        /* calculate the maximum match discrepancy of two trajectories*/
        public Double MaxDTW(Trajectory T, Trajectory R) {
            double[][] dp = new double[T.getSize()][R.getSize()];
            dp[0][0] = 0.0;

            int i;
            for (i = 1; i < T.getSize(); ++i) {
                dp[i][0] = 1.2742E7;
            }

            for (i = 1; i < R.getSize(); ++i) {
                dp[0][i] = 1.2742E7;
            }

            for (i = 1; i < T.getSize(); ++i) {
                for (int j = 1; j < R.getSize(); ++j) {
                    Double dist = Trajectory.getDistance(R.getCoordinateFromIndex(j), T.getCoordinateFromIndex(i));
                    dp[i][j] = Math.max(dist, Math.min(Math.min(dp[i - 1][j], dp[i][j - 1]), dp[i - 1][j - 1]));
                }
            }
            return dp[T.getSize() - 1][R.getSize() - 1];
        }

        /**
         * search alternative reference trajectory for T
         *
         * @param T main trajectory
         * @return the String of trajectory and corresponding trajectory id (start, end, id)
         */
        public HashSet<String> MRT(Trajectory T) {
            HashSet<String> sub_reference = new HashSet<>();

            for (Trajectory R : this.ReferenceSet) {
                for (int i = 0; i < R.getSize(); i++) {
                    for (int j = i + 2; j <= R.getSize(); j++) {
                        if (this.MaxDTW(T, R.getSubTrajectory(i, j)) <= MATCH_DISTANCE) {
                            sub_reference.add(i + "," + j + "," + R.Tid);
                        }
                    }
                }
            }
            return sub_reference;
        }

        public Trajectory getTrajectoryFromTuple(int[] tup) {
            return (Objects.requireNonNull(Trajectory.getTrajectoryFromTid(tup[2]))).getSubTrajectory(tup[0], tup[1]);
        }

        public String int2String(int[] x) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                return Arrays.stream(x).mapToObj(String::valueOf).reduce((m, n) -> m + "-" + n).toString();
            }
            StringBuilder sp = new StringBuilder();
            sp.append(x[0]);
            for (int i = 1; i < x.length; i++) {
                sp.append("-");
                sp.append(x[i]);
            }
            return sp.toString();
        }

        public int[] String2int(String s) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                return Arrays.stream(s.split(",")).mapToInt(Integer::parseInt).toArray();
            }
            String[] sp = s.split(",");
            int[] res = new int[sp.length];
            for (int i = 0; i < sp.length; i++) {
                res[i] = Integer.parseInt(sp[i]);
            }
            return res;
        }

        public String segmentString(int i, int j) {
            StringBuilder s = new StringBuilder();
            s.append(i).append("-").append(j);
            return s.toString();
        }

        public HashMap<String, HashSet<String>> MatchableReferenceTrajectories(Trajectory T) {
            if (Objects.equals(this.ReferenceSet, null)) {
                this.ReferenceSet = Trajectory.getReference();
            }

            HashMap<String, HashSet<String>> mrt = new HashMap<>();

            int n;
            for (n = 0; n < T.getSize() - 1; ++n) {
                mrt.put(this.segmentString(n, n + 2), this.MRT(T.getSubTrajectory(n, n + 2)));
            }

            for (n = 3; n < T.getSize(); n++) {
                boolean stop = true;

                for (int i = 0; i < T.getSize() - n; i++) {
                    String sub_index_a = this.segmentString(i, i + n - 1);
                    String sub_index_b = this.segmentString(i + n - 2, i + n);
                    String sub_index = this.segmentString(i, i + n);
                    HashSet<String> subReference_a = mrt.get(sub_index_a);
                    HashSet<String> subReference_b = mrt.get(sub_index_b);
                    if (Objects.equals(subReference_a, null)) {
                        subReference_a = new HashSet<>();
                    } else {
                        stop = false;
                    }
                    if (Objects.equals(subReference_b, null)) {
                        subReference_b = new HashSet<>();
                    } else {
                        stop = false;
                    }

                    if (!stop) mrt.put(sub_index, new HashSet<>());

                    HashSet<String> first = subReference_a.size() != 0 ? subReference_a : subReference_b;
                    HashSet<String> second = subReference_a.size() == 0 ? subReference_a : subReference_b;
                    Trajectory sub_path = T.getSubTrajectory(i, i + n);

                    for (String sub_a : first) {
                        int[] sub_a_index = String2int(sub_a);
                        Trajectory Ta = getTrajectoryFromTuple(sub_a_index);
                        if (MaxDTW(Ta, sub_path) <= MATCH_DISTANCE) {
                            Objects.requireNonNull(mrt.get(sub_index)).add(sub_a);
                        }
                        for (String sub_b : second) {
                            int[] sub_b_index = String2int(sub_b);
                            Trajectory Tb = getTrajectoryFromTuple(sub_b_index);
                            if (MaxDTW(Tb, sub_path) <= MATCH_DISTANCE) {
                                Objects.requireNonNull(mrt.get(sub_index)).add(sub_b);
                            }

                            if (sub_a_index[2] == sub_b_index[2] && sub_a_index[1] == sub_b_index[0]) {
                                Objects.requireNonNull(mrt.get(sub_index)).add(sub_a_index[0] + "," + sub_b_index[1] + "," + sub_a_index[2]);
                            }
                        }
                    }

                }
                if (stop) {
                    return mrt;
                }
            }
            return mrt;
        }

        /**
         * @param T ordinary trajectory
         * @return Compress trajectory of T
         */
        public List<Integer[]> OptimalSpatialCompression(Trajectory T) {
            HashMap<String, HashSet<String>> mrt = MatchableReferenceTrajectories(T);

            int[][] dp = new int[T.getSize()][2];
            dp[0][0] = 0;
            dp[0][1] = 0;

            int i;
            for (i = 1; i < T.getSize(); i++) {
                int min_cost = i;
                dp[i][1] = i - 1;
                for (int j = 0; j < i; j++) {
                    String sub_index = this.segmentString(j, i + 1);
                    if (mrt.containsKey(sub_index) && !Objects.requireNonNull(mrt.get(sub_index)).isEmpty() && dp[j][0] + 1 < min_cost) {
                        min_cost = dp[j][0] + 1;
                        dp[i][1] = j;
                    }
                }
                dp[i][0] = min_cost;
            }

            i = T.getSize() - 1;
            ArrayList<Integer[]> CompressTrajectory = new ArrayList<>();

            while (0 <= i) {
                if (i == 0) {
                    break;
                }
                if (dp[i][1] != i - 1 && !(Objects.requireNonNull(mrt.get(this.segmentString(dp[i][1], i + 1)))).isEmpty()) {
                    int[] reference_trajectory = String2int(Objects.requireNonNull(mrt.get(this.segmentString(dp[i][1], i + 1))).iterator().next());
                    System.out.println(dp[i][1] + "," + i + "->" + reference_trajectory[0] + "," + reference_trajectory[1]);
                    CompressTrajectory.add(new Integer[]{reference_trajectory[0], reference_trajectory[1], reference_trajectory[2]});
                    i = dp[i][1];
                } else {
                    System.out.println(dp[i][1] + "," + i + "<->" + (i) + "," + i);
                    CompressTrajectory.add(new Integer[]{i, i + 1, T.Tid});
                    i--;
                }
            }
            return CompressTrajectory;
        }
    }

    static class SQUISH {

        public static int buffer_size = 10;

        public Trajectory Buffer = new Trajectory(0);

        public List<Double> Priority = new ArrayList<>();

        public int omission_counter = 0;

        private STCoordination first_cache;

        /* the minimum time span, the collection span below this span will be dropped */
        public static final int TIME_SPAN_LOW = 1;

        /* the middle time span, the collection greater than this span will be regarded as stagnant point */

        /* the maximum time span, the collection span greater than this span will be regarded as dwell point */
        public static final int TIME_SPAN_HIGH = 10;

        /* the minimum distance span, the collection span below than this span will be dropped.
         * In the next version this value will be dynamically adjust by location accuracy */
        public static final int DISTANCE_THRESHOLD = 5;

        /* next version will realize this function by heap */
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

        /* The ultimate interface provide for GPS collector. collector need convey (longitude, latitude),(network type,signal strength), date */
        public void run(double[] GPS, int[] Network, String date) throws ParseException {
            if (Objects.equals(GPS, null)) return;

            STCoordination cur = new STCoordination(GPS[0], GPS[1], date);
            cur.status |= (Network[0] | (Network[1] << 4));

            if (Objects.equals(first_cache, null)) {
                first_cache = cur;
                this.StreamProcess(cur, null);
            } else {
                this.StreamProcess(first_cache, cur);
                first_cache = cur;
            }
        }

        public STCoordination getStartPoint(){
            return STCoordination.getCoordinateFromCid(this.Buffer.trajectory.get(0));
        }

        /* storage current trajectory into database
        * the first date of coordinate will denote the whole trajectory, which will storage into attribute "date" of trajectory  */
        public void storage() {
            StringBuilder sb = new StringBuilder();
            STCoordination c = getStartPoint();

            String d = String.valueOf(c.date); // get the date of first coordinate
            Integer Tid = ManipulateDataBase.generateUnique("Tid");
            this.Buffer.Tid = Tid;
            Trajectory.putInPool(this.Buffer);
            this.Buffer.date = d;
            for (Integer Cid : this.Buffer.trajectory) {
                c = STCoordination.getCoordinateFromCid(Cid);
                c.Tid = Tid;
                ManipulateDataBase.StorageGPS(c);
                sb.append("(").append(c.lon).append(",").append(c.lat).append(")").append("->");
            }
            System.out.println(sb);

            /* storage trajectory segment into database */
            ManipulateDataBase.StorageTrajectory(this.Buffer);

            /* clear buffer but reserve last coordinate */
            Priority.subList(0, Buffer.getSize() - 1).clear();
            Buffer.getSubTrajectoryIndex(0, Buffer.getSize() - 1).clear();
        }

        public Double getPriority(Integer Cid_1, Integer Cid_2, Integer Cid_m) {
            STCoordination c1 = STCoordination.getCoordinateFromCid(Cid_1);
            STCoordination c2 = STCoordination.getCoordinateFromCid(Cid_2);
            STCoordination mid = STCoordination.getCoordinateFromCid(Cid_m);

            if (!Objects.equals(c2, null) && !Objects.equals(c1, null) && !Objects.equals(mid, null)  && c1.isValid() && c2.isValid() && mid.isValid()) {
                Coordinate center = Trajectory.getCenterPoint(c1, c2);
                return Trajectory.getDistance(center, mid);
            }
            return -1.0;
        }

        public void StreamProcess(STCoordination cur_pos, STCoordination next_pos) throws ParseException {
            if (Objects.equals(cur_pos, null)) return;

            if (!Objects.equals(next_pos, null)) {

                /* if arrive time split point, storage buffer into database and deal with the aftermath */
                if (STCoordination.judgeBoundary(next_pos.date) || STCoordination.getTimeSpan(getStartPoint().date, next_pos.date) >= 15) {
                    long time_span = STCoordination.getTimeSpan(cur_pos.date, next_pos.date);
                    if (time_span >= TIME_SPAN_HIGH) next_pos.status |= 0b01000000; // judge stagnant point
                    this.addPoint(cur_pos, next_pos);
                    this.addPoint(next_pos, null);
                    storage();
                }

                /* if the span between two location lower than threshold drop new location  */
                long time_span = STCoordination.getTimeSpan(cur_pos.date, next_pos.date);
                double distance = Trajectory.getDistance(cur_pos, next_pos);
                if (distance >= DISTANCE_THRESHOLD) {
//                    if (time_span < TIME_SPAN_LOW) return;
                    if (time_span >= TIME_SPAN_HIGH) cur_pos.status |= 0b01000000; // judge stagnant point
                    addPoint(cur_pos, next_pos);
                }


            } else addPoint(cur_pos, null);
        }

        public void addPoint(STCoordination new_point, STCoordination next_point) throws ParseException {
            int Cid_new = STCoordination.putInPool(new_point);
            int Cid_next = STCoordination.putInPool(next_point);
            if (Buffer.getSize() == buffer_size) {
                int min_index = this.getMinPriority();
                Double p = Priority.get(min_index);
                Priority.set(min_index - 1, Priority.get(min_index - 1) + p);
                if (min_index < Buffer.getSize() - 1) {
                    Priority.set(min_index + 1, Priority.get(min_index + 1) + p);
                }
                Buffer.remove(min_index);
                Priority.remove(min_index);
            }
            if (Buffer.getSize() == 0 || Objects.equals(next_point, null)) {
                Priority.add((double) Trajectory.EARTH_RADIUS);
                Buffer.addPoint(Cid_new);
                return;
            }
            Priority.add(getPriority(this.Buffer.Index2Cid(Buffer.getSize() - 1), Cid_next, Cid_new));
            Buffer.addPoint(Cid_new);
        }
    }
}
