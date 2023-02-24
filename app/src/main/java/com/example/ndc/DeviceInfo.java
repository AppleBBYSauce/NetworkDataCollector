package com.example.ndc;

import static android.content.Context.TELEPHONY_SERVICE;

import android.Manifest;
import android.app.ActivityManager;
import android.app.UiAutomation;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.telephony.CellInfo;
import android.telephony.CellInfoCdma;
import android.telephony.CellInfoGsm;
import android.telephony.CellInfoLte;
import android.telephony.CellInfoNr;
import android.telephony.CellInfoWcdma;
import android.telephony.CellSignalStrengthCdma;
import android.telephony.CellSignalStrengthGsm;
import android.telephony.CellSignalStrengthLte;
import android.telephony.CellSignalStrengthNr;
import android.telephony.CellSignalStrengthWcdma;
import android.telephony.TelephonyManager;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Objects;

public class DeviceInfo {

    private static ActivityManager mActivityManager;
    public static int GsmSignalStrength = -1;
    public static String UnKnown = "-1\t-1\t-1\t-1";

    public String PatternInfoCompile(Context context) {
        return "\n;ID:" + Build.FINGERPRINT
                + "\n;型号:" + Build.MODEL
                + "\n;Android 版本:" + Build.VERSION.RELEASE
                + "\n;驱动:" + Build.DEVICE
                + "\n;主板:" + Build.BOARD
                + "\n;品牌:" + Build.BRAND
                + "\n;版本号:" + Build.ID
                + "\n;制造商:" + Build.MANUFACTURER
                + "\n;硬件:" + Build.HARDWARE
                + "\n;OS版本号:" + System.getProperty("os.version")
                + "\n;OS架构:" + System.getProperty("os.arch")
                + "\n;OS名称:" + System.getProperty("os.name")
                + "\n;网络运营商:" + getNetOperator(context)
                + "\n;CPU 当前频率:" + getCurCpuFreq()
                + "\n;CPU 最大频率:" + Float.parseFloat(getMaxCpuFreq()) / 1000000
                + "\n;内存大小:" + (getTotalMemory() / (float) Math.pow(2, 20))
                + "\n;内存占用率:" + getUsedPercentValue(context)
                + "\n;CPU 核心数量:" + Runtime.getRuntime().availableProcessors()
                + DynamicInfoCompile(context);
    }

    public String DynamicInfoCompile(Context context) {
        String Location = Utils.getCommUtils().Location.toString();
        if (Objects.equals(Location, "")) {
            Location = UnKnown;
        }
        return getNetworkInfo(context) + '\t' + Location;
    }

    public int listenGsmSignalStrength() {
        TelephonyManager telephonyManager = (TelephonyManager) Utils.context.getSystemService(TELEPHONY_SERVICE);
        List<CellInfo> cellInfoList;
        if (ActivityCompat.checkSelfPermission(Utils.context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            return -108;
        }
        cellInfoList = telephonyManager.getAllCellInfo();
        for (CellInfo cellInfo : cellInfoList) {
            if (cellInfo instanceof CellInfoGsm) {
                CellSignalStrengthGsm cellSignalStrengthGsm =

                        ((CellInfoGsm) cellInfo).getCellSignalStrength();

                return cellSignalStrengthGsm.getDbm();

            } else if (cellInfo instanceof CellInfoCdma) {

                CellSignalStrengthCdma cellSignalStrengthCdma =

                        ((CellInfoCdma) cellInfo).getCellSignalStrength();

                return cellSignalStrengthCdma.getLevel();

            } else if (cellInfo instanceof CellInfoWcdma) {

                CellSignalStrengthWcdma cellSignalStrengthWcdma =

                        ((CellInfoWcdma) cellInfo).getCellSignalStrength();

                return cellSignalStrengthWcdma.getDbm();
            }

            else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && cellInfo instanceof CellInfoNr) { // 5G
                    if (cellInfo.isRegistered()){
                        CellSignalStrengthNr cellSignalStrengthNr = null;
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                            cellSignalStrengthNr = (CellSignalStrengthNr) cellInfo.getCellSignalStrength();
                            return cellSignalStrengthNr.getDbm();
                        }
                }
                }

            else if (cellInfo instanceof CellInfoLte) {

                CellSignalStrengthLte cellSignalStrengthLte = ((CellInfoLte) cellInfo).getCellSignalStrength();

                return cellSignalStrengthLte.getDbm();
            }
            }

        return -108;
    }

    public static String intIP2StringIP(int ip) {
        return (ip & 0xFF) + "." +
                ((ip >> 8) & 0xFF) + "." +
                ((ip >> 16) & 0xFF) + "." +
                (ip >> 24 & 0xFF);
    }

    public String getIP(Context context) {
        WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifiManager != null) {
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            return intIP2StringIP(wifiInfo.getIpAddress());
        }
        return "";
    }

    public static Float getCPULeverageRate() {
        float rate = (float) -1.0;

        try {
            FileReader fr = new FileReader(
                    "/sys/devices/system/cpu/cpu0/cpufreq/stats/time_in_state"
            );
            BufferedReader buf = new BufferedReader(fr);
            String text = buf.readLine();
            String text2 = buf.readLine();
            Log.e("AAA", text);
            Log.e("AAA", text2);

        } catch (FileNotFoundException e) {
            Log.e("AAA", "i can not!!!");
            e.printStackTrace();
        } catch (IOException e) {
            Log.e("AAA", "i can not");
            e.printStackTrace();
        }
        return rate;
    }

    // 实时获取CPU当前频率
    public static Float getCurCpuFreq() {
        Float result = (float) -1.0;
        try {
            FileReader fr = new FileReader(
                    "/sys/devices/system/cpu/cpu0/cpufreq/scaling_cur_freq");
            BufferedReader br = new BufferedReader(fr);
            String text = br.readLine();
            return Float.parseFloat(text.trim()) / 1000000;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    // CPU 最大频率
    public static String getMaxCpuFreq() {
        StringBuilder result = new StringBuilder();
        ProcessBuilder cmd;
        try {
            String[] args = {"/system/bin/cat",
                    "/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_max_freq"};
            cmd = new ProcessBuilder(args);
            Process process = cmd.start();
            InputStream in = process.getInputStream();
            byte[] re = new byte[24];
            while (in.read(re) != -1) {
                result.append(new String(re));
            }
            in.close();
        } catch (IOException ex) {
            ex.printStackTrace();
            result = new StringBuilder("N/A");
        }
        return result.toString().trim();
    }

    public synchronized static ActivityManager getActivityManager(Context context) {
        if (mActivityManager == null) {
            mActivityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        }
        return mActivityManager;
    }

    public static float getUsedPercentValue(Context context) {
        long totalMemorySize = getTotalMemory();
        long availableSize = getAvailableMemory(context) / 1024;
        float percent = ((totalMemorySize - availableSize) / (float) totalMemorySize * 100);
        DecimalFormat df = new DecimalFormat("#.000");
        return percent / 100;
    }

    public static long getAvailableMemory(Context context) {
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        getActivityManager(context).getMemoryInfo(mi);
        return mi.availMem;
    }

    public static long getTotalMemory() {
        long totalMemorySize = 0;
        String dir = "/proc/meminfo";
        try {
            FileReader fr = new FileReader(dir);
            BufferedReader br = new BufferedReader(fr, 2048);
            String memoryLine = br.readLine();
            String subMemoryLine = memoryLine.substring(memoryLine.indexOf("MemTotal:"));
            br.close();
            totalMemorySize = Integer.parseInt(subMemoryLine.replaceAll("\\D+", ""));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return totalMemorySize;
    }

    public String getNetOperator(Context context) {
        TelephonyManager manager = (TelephonyManager) context.getSystemService(TELEPHONY_SERVICE);
        String iNumeric = manager.getSimOperator();
        String netOperator = "";
        if (iNumeric.length() > 0) {
            switch (iNumeric) {
                case "46000":
                case "46002":
                    // 中国移动
                    netOperator = "0";
                    break;
                case "46003":
                    // 中国电信
                    netOperator = "1";
                    break;
                case "46001":
                    // 中国联通
                    netOperator = "2";
                    break;
                default:
                    //未知
                    netOperator = "-1";
                    break;
            }
        }
        return netOperator;
    }

    public int getWifiRssi() {
        WifiManager wifiManager = (WifiManager) Utils.context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        return wifiInfo.getRssi();
    }

    public String getWifiInfo(Context context) {
        WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            int Rssi = wifiInfo.getRssi();
            String FrequencyBand = wifiInfo.getFrequency() / 1000 == 5 ? "5" : "2.4";
            return (2 * (java.lang.Math.min(Rssi, -50) + 100)) / 100.0 +
                    "\t" + wifiInfo.getRxLinkSpeedMbps() +
                    "\t" + wifiInfo.getTxLinkSpeedMbps() +
                    "\t" + FrequencyBand;
        }
        return null;
    }

    public String getNetworkInfo(Context context) {
        ConnectivityManager cm = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) {
            return UnKnown;//无连接
        }
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        if (networkInfo == null || !networkInfo.isAvailable()) {
            return UnKnown;//无连接
        }
        int type = networkInfo.getType();
        if (type == ConnectivityManager.TYPE_WIFI) {
            return getWifiInfo(context);
        } else if (type == ConnectivityManager.TYPE_MOBILE) {
            return listenGsmSignalStrength() + ""; // 移动网络
        }
        return UnKnown;
    }


    public static String runShellCommand(String command) {
        Runtime runtime;
        Process proc = null;
        StringBuilder stringBuffer = null;
        try {
            runtime = Runtime.getRuntime();
            proc = runtime.exec(command);
            stringBuffer = new StringBuilder();
            if (proc.waitFor() != 0) {
                System.err.println("exit value = " + proc.exitValue());
            }
            BufferedReader in = new BufferedReader(new InputStreamReader(
                    proc.getInputStream()));

            String line = null;
            while ((line = in.readLine()) != null) {
                stringBuffer.append(line).append(" ");
            }

        } catch (Exception e) {
            System.err.println(e);
        } finally {
            try {
                assert proc != null;
                proc.destroy();
            } catch (Exception ignored) {
            }
        }
        assert stringBuffer != null;
        return stringBuffer.toString();
    }

    public int getNetStatus(Context context) {
        ConnectivityManager cm = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) {
            return 0;//无连接
        }
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        if (networkInfo == null || !networkInfo.isAvailable()) {
            return 0;//无连接
        }
        int type = networkInfo.getType();
        if (type == ConnectivityManager.TYPE_WIFI) {
            return 1;
        } else if (type == ConnectivityManager.TYPE_MOBILE) {
            return 2; // 移动网络
        }
        return 0;

    }


}
