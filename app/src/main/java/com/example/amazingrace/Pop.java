package com.example.amazingrace;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayDeque;
import java.util.Deque;


public class Pop extends Activity implements SensorEventListener {

    private SensorManager mSensorManager;
    private Sensor mAccelerometer;

    private SensorManager stepSensorManager;
    private Sensor stepCounter;

    private MediaPlayer mediaPlayer;




    float magnitude;

    Deque<Double> real = new ArrayDeque<Double>();

    int windowSize = 64;

    FFT fft = new FFT(windowSize);

    int runCount = 0;



    ProgressBar progressBar;

    Button button;

    TextView textView;



    boolean moving = false;



    @Override
    protected void onCreate( Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.popupwindow);

        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);

        int width = dm.widthPixels;
        int height = dm.heightPixels;

        mediaPlayer = MediaPlayer.create(this,R.raw.success);


        progressBar = findViewById(R.id.progressBar);

        getWindow().setLayout((int)(width*.8),(int)(height*.6));

        progressBar.setMax(500);

        button = findViewById(R.id.button);

        textView = findViewById(R.id.textView);


        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        if (mAccelerometer != null) {
            mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_GAME);
        }

        stepSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if(stepSensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
                != null)
        {
            stepCounter =
                    mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
            moving = true;
        }
        else
        {
            moving = false;

        }


        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (runCount < 500){
                    Toast.makeText(getApplicationContext(),"Finish the Challenge Before Continuing",Toast.LENGTH_LONG).show();
                }else{
                    onBackPressed();

                }
            }
        });




    }

    @Override
    protected void onPause() {
        super.onPause();
        if(moving)
        {
            stepSensorManager.unregisterListener(this);
        }

    }

    @Override
    protected void onResume() {
        super.onResume();

        if(moving)
        {
            stepSensorManager.registerListener(this, stepCounter,
                    SensorManager.SENSOR_DELAY_GAME);
        }
    }


    @Override
    public void onSensorChanged(SensorEvent event) {
        float x = event.values[0];
        float y = event.values[1];
        float z = event.values[2];

        double m = Math.sqrt((Math.pow(x,2))+(Math.pow(y,2))+(Math.pow(z,2)));
        float mM = (float)m;

        magnitude = mM;


        real.add(m);

        double max = 0;
        int ii = 0;



        while (real.size()> fft.getWindowSize()){
            real.remove();
        }


        if(real.size() == fft.getWindowSize()){
            Object[] realD = real.toArray();
            double[] chat = new double[fft.getWindowSize()];
            for (int i = 0 ; i < fft.getWindowSize() ; i++){
                chat[i] = (Double)realD[i];

            }
            double[] imagine = new double[fft.getWindowSize()];
            fft.fft(chat,imagine);

            float powerChat[] = new float[fft.getWindowSize()/2];

            for(int i = 1; i < fft.getWindowSize()/2; i++){
                powerChat[i] = (float)Math.sqrt(chat[i]*chat[i]+imagine[i]*imagine[i]);
            }

            for(int i=0; i<powerChat.length;i++){
                if(powerChat[i]>max){
                    max = powerChat[i];
                    ii = i;

                }
            }
            System.out.println(moving);


            if(max >190 && moving == false){
                runCount++;
            }
        }
        progressBar.setProgress(runCount);
        if(runCount<500){
            button.setBackgroundColor(getResources().getColor(R.color.Red));
        }else{
            button.setBackgroundColor(getResources().getColor(R.color.Green));
        }
        if (runCount == 500){
            mediaPlayer.start();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

}
