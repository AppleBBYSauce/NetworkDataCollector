package com.example.ndc;

import static android.os.SystemClock.sleep;

import android.content.Context;
import android.content.res.AssetManager;
import android.os.Build;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;

public class View {

    private CommunicationUtils commUtils = null;
    private Map<String, FuncPoint> funMap = null;
    public Map<String, Integer> IPMap = null;
    public DeviceInfo Informer = null;
    private String SYN = null;
    private HashMap<String, String> SynInfo = null;
    public Thread GroupServer = null;
    public static String LocalIP = null;
    private final Integer MaxEndurance = 6;
    public static String Device_Manifests = null;
    public static String Activity_Device = null;
    public static HashMap<String, Queue<ArrayList<Float>>> OnlineBuffer = null;
    public static int MAX_LEN = 12;
    public int SR = 0;
    public static String UnKnown = "-1\t-1\t-1\t-1\t1\t-1\t-1\t-1";
    public static String[] miss = new String[]{"-1", "-1", "-1"};

    public static int patience = 1;
    public static int counter = 0;

    public Locationer locationer = null;

    public interface FuncPoint {
        void run(String[] data, String IP) throws UnknownHostException;
    }

    View(CommunicationUtils commUtils_) {
        commUtils = commUtils_;
        Informer = new DeviceInfo();
        IPMap = new HashMap<>();
        SynInfo = new HashMap<>();
        LocalIP = Informer.getIP(Utils.context);
        Device_Manifests = "";
        Activity_Device = "";
        OnlineBuffer = new HashMap<>();
        SR = Informer.getNetStatus(Utils.context);


        funMap = new HashMap<>();
        funMap.put("T", this::SendMySelf); // send self-network information
        funMap.put("T1", this::SaveBuffer); // save device's IP
        funMap.put("O", this::SendMySelf);
        funMap.put("O1", this::SaveBufferOnline);
        funMap.put("B", this::ReportSelf); // explore device through broadcast
        funMap.put("B1", this::SaveIP); // save device's IP
        locationer = new Locationer();

        try {
            locationer.initLocation(Utils.context);
        } catch (Exception e) {
            e.printStackTrace();
        }
        locationer.mLocationClient.start();

    }

    private void SaveBufferOnline(String[] data, String IP) {
        if (!OnlineBuffer.containsKey(IP)) {
            Queue<ArrayList<Float>> queue = new LinkedList<>();
            OnlineBuffer.put(IP, queue);
        }

        Queue<ArrayList<Float>> queue = OnlineBuffer.get(IP);
        if (queue.size() == MAX_LEN) {
            queue.poll();
        }

        ArrayList<Float> OnlineData = new ArrayList<>();

        // Judge Packet Loss
        if (Objects.equals("-1", data[2]) | !Objects.equals(SYN, data[1])) data[2] = UnKnown;

        String[] receiver = Informer.DynamicInfoCompile(Utils.context).split("\t");
        String[] sender = data[2].split("\t");

        if (Objects.equals(sender[0], "null")) sender[0] = "-1\t-1\t-1\t-1";
        // Signal Quality
        OnlineData.add(Float.parseFloat(receiver[0]));
        OnlineData.add(Float.parseFloat(sender[0]));

        // Transmission Speed
        for (int i = 1; i < 3; i++) {
            OnlineData.add((float) (Float.parseFloat(receiver[i]) / 2401.0));
            OnlineData.add((float) (Float.parseFloat(sender[i]) / 2401.0));
        }

        // Frequency Band
        OnlineData.add((float) ((Objects.equals(receiver[3], "5")) ? 1.0 : 0.0));
        OnlineData.add((float) ((Objects.equals(sender[3], "5")) ? 1.0 : 0.0));

        // Calculate RTT
        long StartTime = Long.parseLong(data[1]);
        float RTT = (StartTime == -1) ? -1 : System.currentTimeMillis() - StartTime;
        RTT = (float) (RTT >= 0 && RTT <= 3000 ? RTT / 3000.0 : (RTT > 0 ? 1 : -1));
        Log.e("Pytorch", "RTT" + RTT);
        OnlineData.add(RTT);

        // Add Number of Device
        OnlineData.add((float) OnlineBuffer.size() / 10);

        // Location Data
        OnlineData.add(Objects.equals("-1", receiver[4]) ? (float) -1 : (float) (Float.parseFloat(receiver[4]) / 180.0));
        OnlineData.add(Objects.equals("-1", sender[4]) ? (float) -1 : (float) (Float.parseFloat(sender[4]) / 180.0));
        OnlineData.add(Objects.equals("-1", receiver[5]) ? (float) -1 : (float) (Float.parseFloat(receiver[5]) / 90.0));
        OnlineData.add(Objects.equals("-1", sender[5]) ? (float) -1 : (float) (Float.parseFloat(sender[5]) / 90.0));


        // Convert DateTime
        Calendar now = Calendar.getInstance();
        OnlineData.add((float) (now.get(Calendar.DAY_OF_MONTH) / 31.0));
        OnlineData.add((float) (now.get(Calendar.HOUR_OF_DAY) / 24.0));
        OnlineData.add((float) (now.get(Calendar.DAY_OF_WEEK) / 7.0));

        queue.add(OnlineData);
        SynInfo.put(IP, "1");
    }

    private void SaveBuffer(String[] data, String IP) {
        if (Objects.equals(data[1], SYN)) {
            if (!IPMap.containsKey(IP)) return;
            long RTT = System.currentTimeMillis() - Long.parseLong(SYN);
            SynInfo.put(IP, data[2] + "\t" + RTT);
            // update alive time.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                IPMap.replace(IP, 0);
            } else {
                IPMap.put(IP, 0);
            }
        }
    }

    private void ReportSelf(String[] data, String IP) throws UnknownHostException {
        commUtils.sendMessage(IP, 9000, "B1\n");
        Log.e("View", "Report My Self\t" + IP);
    }

    private void SaveIP(String[] data, String IP) {
        IPMap.put(IP, 0);
        Log.e("View", "Explore New Device" + "\t" + IP);
    }

    private void SendMySelf(String[] data, String IP) throws UnknownHostException {
        if (Objects.equals(data[0], "T")) {
            String syn = data[1];
            String message = "T1" + "\n" + syn + "\n" + Informer.DynamicInfoCompile(Utils.context);
            commUtils.sendMessage(IP, 9000, message);
            Log.e("View", "send my self to\t" + IP);
        } else if (Objects.equals(data[0], "O")) {
            String syn = data[1];
            String message = "O1" + "\n" + syn + "\n" + Informer.DynamicInfoCompile(Utils.context);
            commUtils.sendMessage(IP, 9000, message);
            Log.e("View", "send my self to\t" + IP);
        }

    }

    public void Switch(String data, String address) {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                String[] Info = data.split("\n");
                try {
                    String IP = address.substring(1).split(":")[0];
                    Log.e("View", "from\t" + IP + "\t" + LocalIP + "\t" + Info[0]);
                    if (!Objects.equals(IP, LocalIP))
                        Objects.requireNonNull(funMap.get(Info[0])).run(Info, IP);
                } catch (UnknownHostException e) {
                    Log.e("View", "function null");
                    e.printStackTrace();
                }
            }
        });
        thread.start();
    }

    private void Recycle() throws IOException {
        HashMap<String, Integer> cloneIPMap = new HashMap<>(IPMap);
        StringBuilder SB = new StringBuilder();

        // self network information
        SB.append(System.currentTimeMillis()).append("+");
        SB.append(LocalIP).append(":");
        SB.append(Informer.DynamicInfoCompile(Utils.context));

        // other device network information
        for (String IP : cloneIPMap.keySet()) {
            if (SynInfo.containsKey(IP)) {
                SB.append("+").append(IP).append(":").append(SynInfo.get(IP));
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    IPMap.merge(IP, 1, Integer::sum);
                } else {
                    IPMap.put(IP, IPMap.get(IP) + 1);
                }
                if (IPMap.get(IP) < MaxEndurance) SB.append("+").append(IP).append(":").append("*");
                else if (Objects.equals(IPMap.get(IP), MaxEndurance))
                    SB.append("+").append(IP).append(":").append("-");
            }
        }

        SB.append("\n");
        Calendar now = Calendar.getInstance();
        String date = now.get(Calendar.YEAR) + "-" + now.get(Calendar.MONTH) + "-" + now.get(Calendar.DAY_OF_MONTH);
        toFile(SB.toString(), date);
        Activity_Device = SB.toString();
        SynInfo.clear();
    }

    public void toFile(String data, String file_name) throws IOException {
        FileOutputStream out = null;
        BufferedWriter writer = null;
        out = Utils.context.openFileOutput(file_name, Context.MODE_APPEND);
        writer = new BufferedWriter(new OutputStreamWriter(out));
        writer.write(data);
        writer.close();
    }

    public void OfflineCollector_() {
        LocalIP = Informer.getIP(Utils.context);
        Device_Manifests = "";
        for (String IP : IPMap.keySet()) {
            Device_Manifests += IP + "\n";
        }
        Log.e("OfflineCollector", "start to Broadcast");
        SYN = String.valueOf(System.currentTimeMillis());
        HashMap<String, Integer> cloneIPMap = new HashMap<>(IPMap);
        Iterator<String> Keys = cloneIPMap.keySet().iterator();
        ArrayList<String> IPs = new ArrayList<>();
        while (Keys.hasNext()) {
            String IP = Keys.next();
            if (cloneIPMap.get(IP) <= 10 * MaxEndurance) IPs.add(IP);
            else if (cloneIPMap.get(IP) > 10 * MaxEndurance) IPMap.remove(IP);
        }
        Utils.getCommUtils().GroupBroadCast(IPs, "T" + '\n' + SYN);
        sleep(3000);
        Log.e("OfflineCollector", "start to recycle");
        try {
            Recycle();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void RecycleOnline() {
        HashMap<String, Integer> cloneIPMap = new HashMap<>(IPMap);

        for (String IP : cloneIPMap.keySet()) {
            if (!SynInfo.containsKey(IP)) {
                SaveBuffer(miss, IP);
            }
        }
        SynInfo.clear();
    }

    public void OnlineCollector_() {
        SYN = String.valueOf(System.currentTimeMillis());
        HashMap<String, Integer> cloneIPMap = new HashMap<>(IPMap);
        Iterator<String> Keys = cloneIPMap.keySet().iterator();
        ArrayList<String> IPs = new ArrayList<>();
        while (Keys.hasNext()) {
            String IP = Keys.next();
            if (cloneIPMap.get(IP) <= 10 * MaxEndurance) IPs.add(IP);
            else if (cloneIPMap.get(IP) > 10 * MaxEndurance) IPMap.remove(IP);
        }
        Utils.getCommUtils().GroupBroadCast(IPs, "O" + '\n' + SYN);
        sleep(3000);
        Log.e("OfflineCollector", "start to recycle");
        RecycleOnline();
    }

    public int[] ActivateJudge() {
        int network_type = Informer.getNetStatus(Utils.context);
        int strength_strengthen = 0;
        Log.e("SR", "Cell Signal Strength:\t" + Informer.listenGsmSignalStrength() + "\t" +
                "WIFI signal Strength\t" + Informer.getWifiRssi() + "\t" +
                "Connection type:\t" + (network_type == 1 ? "WIFI" : "CELL"));
        if (network_type == 2) {
            Log.e("SR", "Signal Level:\t" + Informer.listenGsmSignalStrength());
            if (Informer.listenGsmSignalStrength() > -95) strength_strengthen = 1;

        }

        if (network_type == 1) {
            if (Informer.getWifiRssi() > -95) strength_strengthen = 1;
        }

        return new int[] {network_type ,strength_strengthen};
    }

    public void StatusRecorder() {
        int new_SR = Informer.getNetStatus(Utils.context);
        int[] aj = ActivateJudge();

        locationer.mLocationClient.start();
        StringBuilder context_info = new StringBuilder();
        String[] loc = Utils.getCommUtils().Location.toString().split("\t");
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        context_info.append(sdf.format(new Date())).append("\t");
        try {
            context_info.append(loc[0]).append("\t");
            context_info.append(loc[1]).append("\t");
        } catch (Exception e) {
            context_info.append(-1).append("\t");
            context_info.append(-1).append("\t");
        }
        context_info.append(aj[0]).append(aj[1]).append("\n");

        try {
            Log.e("SR", "write successfully!");
            toFile(context_info.toString(), "status_record");
        } catch (IOException e) {
            Log.e("SR", "fail to write!");
            e.printStackTrace();
        }
        SR = new_SR;
    }

    /*
    This is test Function!!!
     */
    public static void SimulateStatusRecorder() throws IOException, ParseException {
        ManipulateDataBase.deleteAllTable();
        AssetManager assetManager = Utils.context.getApplicationContext().getAssets();
        InputStream inputStream =   assetManager.open("fake_trajectory.txt");
        InputStreamReader isr = new InputStreamReader(inputStream);
        BufferedReader bd = new BufferedReader(isr);
        String line;
        SpatioTemporalTrajectory.SQUISH squish = new SpatioTemporalTrajectory.SQUISH();
        while ((line =bd.readLine()) != null){
                String[] s = line.split(",");
                double[] GPS = new double[] {Double.parseDouble(s[0]), Double.parseDouble(s[1])};
                int[] network = new int[] {Integer.parseInt(s[2]), Integer.parseInt(s[3])};
                squish.run(GPS, network, s[4]); // call the interface
        }

        List<STCoordination> st = ManipulateDataBase.getAllSTCoordinate();
        for(STCoordination s:st){
            HashSet<String> st_hash = STCoordination.getSurroundGeoHash(Coordinate.encode(s.lat,s.lon, 8), 8);
            for(String s_: st_hash){
                List<STCoordination> s_co =  ManipulateDataBase.SearchSurroundFromGeoHash(s_, 8);
                int a = 0;
            }
        }
    }

    public HashMap<String, Queue<ArrayList<Float>>> getOnlineData() {
        return OnlineBuffer;
    }

}
