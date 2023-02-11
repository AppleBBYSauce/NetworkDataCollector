package com.example.ndc;

import androidx.appcompat.app.AppCompatActivity;

import org.pytorch.IValue;
import org.pytorch.LiteModuleLoader;
import org.pytorch.Module;
import org.pytorch.Tensor;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Objects;
import java.util.Queue;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class NetworkForecast extends AppCompatActivity {

    Module module = null;
    final Integer MAX_LEN = 12;
    final Integer HIDDEN_SIZE = 17;
    Button Start;
    Button Forecast;
    Button End;
    TextView text;

    public static HashMap<String, Queue<ArrayList<Float>>> OnlineData = Utils.CommUtils.view.getOnlineData();

    public ArrayList<Float> sigmoid(float[] array){
        ArrayList<Float> res = new ArrayList<>();
        for (Float d : array){
            res.add((float) (1 / (1+Math.exp(-d))));
        }
        return res;

    }


    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_network_forecast);
        Forecast = findViewById(R.id.start_forecast);
        Start = findViewById(R.id.start_collect);
        End = findViewById(R.id.end_online_server);
        text = findViewById(R.id.display_forecast);

        End.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    Utils.getCommUtils().TerminateServer();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        Start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(NetworkForecast.this, OnlineCollector.class);
                startService(intent);
            }
        });

        Forecast.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.e("Pytorch", OnlineData.toString());
                run();
            }
        });


        try {
            Log.e("Pytorch", assetFilePath(this, "m.ptl"));
            String path = assetFilePath(this, "m.ptl");
            module = LiteModuleLoader.load(assetFilePath(this, "m.ptl"));
        } catch (IOException e) {
            Log.e("PytorchHelloWorld", "Error reading assets", e);
            e.printStackTrace();
        }
        Log.e("PytorchHelloWorld", "Load Successfully!");

    }

    public void run() {
        StringBuilder Result = new StringBuilder();
        for (String IP : OnlineData.keySet()) {
            Queue<ArrayList<Float>> item = Objects.requireNonNull(OnlineData.get(IP));
            Result.append(IP).append("\t");
            if (item.size() < MAX_LEN) {
                Result.append("Do not collect enough data!" + "data length: ").append(item.size());
            } else {
                Tensor InputTensor = getData(item);
                IValue input = IValue.from(InputTensor);
                IValue output = module.forward(input);
                Result.append(sigmoid(output.toTensor().getDataAsFloatArray()).toString());
//                Result.append(output.toString());
            }
            Result.append('\n');
        }
        text.setText(Result.toString());

    }


    public Tensor getData(Queue<ArrayList<Float>> data) {
        final long[] InputShape = new long[]{1, MAX_LEN, HIDDEN_SIZE};
        final FloatBuffer Buffer = Tensor.allocateFloatBuffer(MAX_LEN * HIDDEN_SIZE);
        for (ArrayList<Float> item : data) {
            float[] tmp = new float[item.size()];
            for (int i = 0; i < item.size(); i++) {
                tmp[i] = item.get(i);
            }
            Buffer.put(tmp, 0, item.size());
        }
        return Tensor.fromBlob(Buffer, InputShape);
    }

    public static String assetFilePath(Context context, String assetName) throws IOException {
        File file = new File(context.getFilesDir(), assetName);
        if (file.exists() && file.length() > 0) {
            return file.getAbsolutePath();
        }

        try (InputStream is = context.getAssets().open(assetName)) {
            try (OutputStream os = new FileOutputStream(file)) {
                byte[] buffer = new byte[4 * 1024];
                int read;
                while ((read = is.read(buffer)) != -1) {
                    os.write(buffer, 0, read);
                }
                os.flush();
            }
            return file.getAbsolutePath();
        }
    }

}