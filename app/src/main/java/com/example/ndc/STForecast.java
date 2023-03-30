package com.example.ndc;

import android.app.IntentService;
import android.content.Intent;
import android.content.Context;
import android.util.Log;

import java.io.IOException;
import java.text.ParseException;


public class STForecast extends IntentService {



    public STForecast() {
        super("STForecast");

    }
    @Override
    protected void onHandleIntent(Intent intent) {
        try {
            View.SimulateStatusRecorder();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (ParseException e) {
            Log.e("TR", "time error");
            throw new RuntimeException(e);
        }
    }




}