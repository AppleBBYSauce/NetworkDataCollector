package com.example.ndc;

import static android.os.SystemClock.sleep;

import android.text.Editable;
import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Objects;

public class CommunicationUtils {

    private DatagramSocket SendSocket = null;
    public DatagramChannel ReceiveSocket = null;
    private Thread UDPServer = null;
    private Thread BroadServer = null;
    public View view;
    public StringBuilder Location = new StringBuilder();
    public Integer period = 5;


    CommunicationUtils() throws IOException {
        BindPort();
    }

    void BindPort() {
        view = new View(this);
        Log.e("CommUtils", "create view!");

        // bind self port
        try {
            SendSocket = new DatagramSocket(9001);
        } catch (IOException e) {
            Log.e("CommUtils", "bind port fail!");
            e.printStackTrace();
        }
        try {
            ReceiveSocket = DatagramChannel.open();
            ReceiveSocket.socket().bind(new InetSocketAddress(9000));
        } catch (Exception e) {
            Log.e("CommUtils", "bind port fail!");
            e.printStackTrace();
        }
    }

    Integer sendMessage(String IntentIP, Integer IntentPort, String Message) throws UnknownHostException {
        // 0-program error, -1-thread timed out, 1-send successfully

        if (SendSocket == null) {
            BindPort();
        }
        // bind target address
        IntentIP = Objects.equals(IntentIP, "") ? "192.168.2.20" : IntentIP;
        try {
            InetAddress TargetAddress = InetAddress.getByName(IntentIP);

            // convert to byte
            byte[] data = Message.getBytes();

            // construct Datagrampacket
            DatagramPacket packet = new DatagramPacket(data, data.length, TargetAddress, IntentPort);

            // open a thread to send message
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        SendSocket.send(packet);
                    } catch (IOException e) {
                        Log.e("CommUtils", "send message fail!");
                        e.printStackTrace();
                    }
                }
            });
            thread.start();

            // detecting whether the thread is overtime.
            try {
                thread.join(3000);
                if (thread.isAlive()) {
                    Log.e("CommUtils", "send overtime!");
                    thread.interrupt();
                    return -1;
                }
            } catch (Exception e) {
                return 0;
            }
        } catch (Exception e) {
            return 0;
        }
        Log.e("CommUtils", "send successfully!");
        return 1;
    }

    Integer OverTimer(Thread t) {
        t.start();
        try {
            t.join(1000);
            if (t.isAlive()) {
                t.interrupt();
                return -1;
            } else {
                Log.e("CommUtils", "send!");
                return 1;
            }
        } catch (Exception e) {
            return 0;
        }
    }

    void RunUDPServer() {
        ByteBuffer buffer = ByteBuffer.allocate(4096);
        if (ReceiveSocket == null) {
            BindPort();
        }
        Log.e("CommUtils", "run UDP Service!");
        UDPServer = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    buffer.clear();
                    try {
                        SocketAddress senderIP = ReceiveSocket.receive(buffer);
                        buffer.flip();
                        String data = String.valueOf(StandardCharsets.UTF_8.decode(buffer));
                        view.Switch(data, senderIP.toString());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        UDPServer.start();
    }

    // terminate server
    void TerminateServer() throws IOException {
        if (!Objects.equals(BroadServer, null)){
            BroadServer.interrupt();
        }
        if (!Objects.equals(view.GroupServer, null)){
            view.GroupServer.interrupt();
        }
        if (!Objects.equals(SendSocket, null)){
            SendSocket.close();
        }
        if (!Objects.equals(ReceiveSocket, null)){
            ReceiveSocket.close();
        }
        Log.e("CommUtils", "end Server!");
    }

    //  broadcast message to search available devices
    void BroadCast(String message) {
        BroadServer = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        Log.e("BroadCast", "start BroadCast");
                        DatagramSocket socket = new DatagramSocket();
                        byte[] meg = message.getBytes();
                        DatagramPacket packet = new DatagramPacket(meg,
                                meg.length,
                                InetAddress.getByName("255.255.255.255"), 9000);
                        socket.send(packet);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    sleep(1000 * 5);
                }
            }
        });
        BroadServer.start();
    }

    void BroadCast_() {
        try {
            DatagramSocket socket = new DatagramSocket();
            byte[] meg = "B\n".getBytes();
            DatagramPacket packet = new DatagramPacket(meg,
                    meg.length,
                    InetAddress.getByName("255.255.255.255"), 9000);
            socket.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void GroupBroadCast(ArrayList<String> Address, String Message) {
        byte[] data = Message.getBytes();
        for (String IP : Address) {
            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        DatagramSocket tempSocket = new DatagramSocket();
                        DatagramPacket packet = new DatagramPacket(data, data.length, InetAddress.getByName(IP), 9000);
                        tempSocket.send(packet);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
            t.start();
        }
    }


    // transiently send and receive in port:9003
    Integer send_receive(String data, String targetIP) throws SocketException, UnknownHostException {
        DatagramSocket tempSocket = new DatagramSocket();
        InetAddress targetAddress = InetAddress.getByName(targetIP);
        Thread t1 = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    byte[] ByteData = data.getBytes();
                    DatagramPacket packet = new DatagramPacket(ByteData, ByteData.length, targetAddress, 9000);
                    tempSocket.send(packet);
                    byte[] buf = new byte[64];
                    DatagramPacket RecPacket = new DatagramPacket(buf, buf.length);
                    tempSocket.receive(RecPacket);
                    tempSocket.close();
                } catch (Exception e) {
                    tempSocket.close();
                    e.printStackTrace();
                }
            }
        });
        return OverTimer(t1);
    }
}
