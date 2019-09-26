package com.example.amazingrace;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.io.Console;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;


//import android.support.v7.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity implements SensorEventListener {
    PieChart chart;

    private static final String TAG = "MainActivity";
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;


    float magnitude;

    TextView currentActivity;
    TextView speedText;
    TextView distance;
    TextView gps_value;

    Deque<Double> real = new ArrayDeque<Double>();

    int windowSize = 64;

    FFT fft = new FFT(windowSize);

    Button results;
    Button button_gps;

    int runCount = 0;
    int walkCount = 0;
    int standCount = 0;

    boolean gps = true;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        currentActivity = findViewById(R.id.running);
        speedText = findViewById(R.id.speed);
        distance = findViewById(R.id.distance);

        gps_value = findViewById(R.id.gps_value);
        button_gps = findViewById(R.id.button_gps);

        chart = findViewById(R.id.chart);

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        List<Sensor> sensors = mSensorManager.getSensorList(Sensor.TYPE_ALL);

        button_gps.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                GPStracker g = new GPStracker(getApplicationContext());
                Location l = g.getLocation();

                if (l != null) {
                    double lat = l.getLatitude();
                    double lon = l.getLongitude();
                    String string = new String("Lat: "+ lat+ " Long: "+lon);
                    gps_value.setText(string);
                    System.out.println(lat);
                }
            }
        });

        if (mAccelerometer != null) {
            mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_GAME);
        }

        chart.setUsePercentValues(true);
        chart.getDescription().setEnabled(false);

        chart.setDrawHoleEnabled(true);
        chart.setHoleColor(Color.WHITE);

        chart.setMaxAngle(180f); // HALF CHART
        chart.setRotationAngle(180f);
        chart.setCenterTextOffset(0, -20);


        chart.setTransparentCircleColor(Color.WHITE);
        chart.setTransparentCircleAlpha(110);

        chart.setHoleRadius(58f);
        chart.setTransparentCircleRadius(61f);

        chart.setDrawCenterText(true);

        chart.setRotationEnabled(false);
        chart.setHighlightPerTapEnabled(true);



        chart.animateY(1400, Easing.EasingOption.EaseInCirc);

        Legend l = chart.getLegend();
        l.setVerticalAlignment(Legend.LegendVerticalAlignment.TOP);
        l.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
        l.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        l.setDrawInside(false);
        l.setXEntrySpace(7f);
        l.setYEntrySpace(0f);
        l.setYOffset(0f);


        // entry label styling
        chart.setEntryLabelColor(Color.WHITE);
        chart.setEntryLabelTextSize(12f);



    }

    private void setData(int count, float range) {

        ArrayList<PieEntry> values = new ArrayList<>();


        values.add(new PieEntry((float)runCount,"Running"));
        values.add(new PieEntry((float) walkCount,"Walking"));
        values.add(new PieEntry((float) standCount,"Standing"));


        PieDataSet dataSet = new PieDataSet(values,"");
        dataSet.setSliceSpace(3f);
        dataSet.setSelectionShift(5f);

        dataSet.setColors(ColorTemplate.MATERIAL_COLORS);
        //dataSet.setSelectionShift(0f);

        PieData data = new PieData(dataSet);
        data.setValueFormatter(new PercentFormatter());
        data.setValueTextSize(11f);
        data.setValueTextColor(Color.WHITE);
        chart.setData(data);
        chart.notifyDataSetChanged();
        chart.invalidate();

    }




    @Override
    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);

    }

    @Override
    public final void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Do something here if sensor accuracy changes.
    }

    @Override
    public final void onSensorChanged(SensorEvent event) {
        float x = event.values[0];
        float y = event.values[1];
        float z = event.values[2];

        double m = Math.sqrt((Math.pow(x,2))+(Math.pow(y,2))+(Math.pow(z,2)));
        float mM = (float)m;

        magnitude = mM;


        real.add(m);

        double max = 0;
        int ii = 0;

        setData(3, 100);


        String running = ("Running") ;
        String walking = ("Walking");
        String standing = ("Stationary");

        speedText.setText("Top Speed:");
        distance.setText("Distance");






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

                //System.out.println(max);
                //System.out.println(Integer.toString(ii));
            }



            if (max > 20 && max < 150) {
                currentActivity.setText(walking);
                walkCount++;
            }else if(max < 20){
                currentActivity.setText(standing);
                standCount++;
            }
            if(max >150){
                currentActivity.setText(running);
                runCount++;
            }


        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_GAME);
    }

    @Override
    protected void onDestroy() {
        mSensorManager.unregisterListener(MainActivity.this);

        super.onDestroy();
    }
}