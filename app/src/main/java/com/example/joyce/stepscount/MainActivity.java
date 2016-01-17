package com.example.joyce.stepscount;

import android.annotation.SuppressLint;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.opengl.Matrix;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class MainActivity extends AppCompatActivity implements SensorEventListener {
    /**
     * Whether or not the system UI should be auto-hidden after
     * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
     */
    private static final boolean AUTO_HIDE = true;

    /**
     * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
     * user interaction before hiding the system UI.
     */
    private static final int AUTO_HIDE_DELAY_MILLIS = 3000;

    /**
     * Some older devices needs a small delay between UI widget updates
     * and a change of the status and navigation bar.
     */
    private static final int UI_ANIMATION_DELAY = 300;
    private final Handler mHideHandler = new Handler();
    private View mContentView;
    private final Runnable mHidePart2Runnable = new Runnable() {
        @SuppressLint("InlinedApi")
        @Override
        public void run() {
            // Delayed removal of status and navigation bar

            // Note that some of these constants are new as of API 16 (Jelly Bean)
            // and API 19 (KitKat). It is safe to use them, as they are inlined
            // at compile-time and do nothing on earlier devices.
            mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    };
    private View mControlsView;
    private final Runnable mShowPart2Runnable = new Runnable() {
        @Override
        public void run() {
            // Delayed display of UI elements
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.show();
            }
            mControlsView.setVisibility(View.VISIBLE);
        }
    };
    private boolean mVisible;
    private final Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            hide();
        }
    };
    /**
     * Touch listener to use for in-layout UI controls to delay hiding the
     * system UI. This is to prevent the jarring behavior of controls going away
     * while interacting with activity UI.
     */
    private final View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            if (AUTO_HIDE) {
                delayedHide(AUTO_HIDE_DELAY_MILLIS);
            }
            return false;
        }
    };

    private static int sensorType = Sensor.TYPE_ACCELEROMETER;
    private SensorManager senSensorManager;
    private Sensor senAccelerometer;
    private Sensor senGrav;
    private Sensor senMag;
    private static final float smoothValue = 3;
    private static int numOfJump = 0;

    // Find the time when accerlation is zero
    ArrayList al = new ArrayList();


    TextView textViewX;
    TextView textViewY;
    TextView textViewZ;
    TextView textViewMax;
    TextView textViewSamples;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        textViewX = (TextView) findViewById(R.id.textView);
        textViewY = (TextView) findViewById(R.id.textView2);
        textViewZ = (TextView) findViewById(R.id.textView3);
        textViewMax = (TextView) findViewById(R.id.textView4);
        //textViewSamples = (TextView) findViewById(R.id.textView5);

        mVisible = true;
        mControlsView = findViewById(R.id.fullscreen_content_controls);
        mContentView = findViewById(R.id.fullscreen_content);

        // Set up the user interaction to manually show or hide the system UI.
        //mContentView.setOnClickListener(new View.OnClickListener() {
          //  @Override
            //public void onClick(View view) {
              //  toggle();
            //}
        //});

        // Upon interacting with UI controls, delay any scheduled hide()
        // operations to prevent the jarring behavior of controls going away
        // while interacting with the UI.
        findViewById(R.id.initiate).setOnTouchListener(mDelayHideTouchListener);

        senSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        senAccelerometer = senSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        senGrav = senSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);

        senMag = senSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);


        final SensorEventListener context = this;
        final Button button = (Button) findViewById(R.id.initiate);

        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Perform action on click
                if ("start".equals(button.getText())) {
                    button.setText("end");
                    senSensorManager.registerListener(context, senAccelerometer, SensorManager.SENSOR_DELAY_FASTEST);
                    senSensorManager.registerListener(context, senGrav, SensorManager.SENSOR_DELAY_FASTEST);
                    senSensorManager.registerListener(context, senMag, SensorManager.SENSOR_DELAY_FASTEST);
                    numOfJump++;
                    yReadings.clear();
                }
                else if ("end".equals(button.getText())) {
                    button.setText("start");
                    //unregister listener
                    senSensorManager.unregisterListener(context);
                    //Analyse
                    List<Reading> smoothedReadings = lowpass(yReadings, 0.01);

                    Markers mark = markReadings(smoothedReadings);

                    //textViewSamples.setText(String.format("Samples: %s", mark.liftPhase.size()));

                    textViewMax.setText(String.format("Max Acc: %.3f", findMaxAcc(mark.liftPhase )));
                    numOfJump =0;

                }
            }
        });
    }
    private List<Reading> lowpass(List<Reading> in, double RC){
        List<Reading> result = new ArrayList<Reading>();
        result.add(in.get(0));
        Collections.sort(in);

        if(in.size() < 2)
            return in;

        for(int i = 1; i < in.size(); i++){
            double dt = (in.get(i).time - in.get(i - 1).time) / 1000.0;

            double a = dt / (dt + RC);

            result.add(new Reading(in.get(i).time, a * in.get(i).value + (1.0 - a) * result.get(i - 1).value));
        }

        return result;
    }

    static class Markers{
        List<Reading> liftPhase = new ArrayList<Reading>();
        Reading dipPoint;
    }

    private Markers markReadings(List<Reading> readings){
        Markers results = new Markers();
        int stateMachineNum = 0;

        for(Reading reading : readings) {
            if (stateMachineNum == 0) {
                if (reading.value < -smoothValue) {
                    stateMachineNum = 1;
                    results.dipPoint = reading;
                }
            } else if (stateMachineNum == 1) {
                if (reading.value > 0) {
                    stateMachineNum = 2;
                    results.liftPhase.add(reading);
                }
            } else if (stateMachineNum == 2) {
                results.liftPhase.add(reading);
                if (reading.value < 0)
                    break;
            }
        }
        return results;
    }

    private double findMaxAcc(List<Reading> readings){
        double currentMax = 0;
        for(Reading reading : readings){
            if(reading.value > currentMax){
                currentMax = reading.value;
            }
        }
        return currentMax;
    }


    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedHide(100);
    }

    private void toggle() {
        if (mVisible) {
            hide();
        } else {
            show();
        }
    }

    private void hide() {
        // Hide UI first
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        //mControlsView.setVisibility(View.GONE);
        mVisible = false;

        // Schedule a runnable to remove the status and navigation bar after a delay
        mHideHandler.removeCallbacks(mShowPart2Runnable);
        mHideHandler.postDelayed(mHidePart2Runnable, UI_ANIMATION_DELAY);
    }

    @SuppressLint("InlinedApi")
    private void show() {
        // Show the system bar
        mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        mVisible = true;

        // Schedule a runnable to display UI elements after a delay
        mHideHandler.removeCallbacks(mHidePart2Runnable);
        mHideHandler.postDelayed(mShowPart2Runnable, UI_ANIMATION_DELAY);
    }

    /**
     * Schedules a call to hide() in [delay] milliseconds, canceling any
     * previously scheduled calls.
     */
    private void delayedHide(int delayMillis) {
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, delayMillis);
    }

    float [] accelValues;
    float [] gravValues;
    float [] magValues;

    static class Reading implements Comparable<Reading>{
        public long time;
        public double value;

        public Reading(long time, double value){
            this.time = time;
            this.value = value;
        }


        //Built for small differences
        @Override
        public int compareTo(Reading another) {
            return (int)(this.time - another.time);
        }
    }

    List<Reading> yReadings = new ArrayList<Reading>();

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        Sensor mySensor = sensorEvent.sensor;


        if (mySensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float x = sensorEvent.values[0];
            float y = sensorEvent.values[1];
            float z = sensorEvent.values[2];

            accelValues = sensorEvent.values.clone();

            float [] R = new float[16];
            float [] I = new float[16];

            if(gravValues != null && magValues != null) {
                senSensorManager.getRotationMatrix(R, I, gravValues, magValues);

                float [] Va = new float[4];
                float [] Vf = new float[]{x, y, z, 0};

                float [] Ri = new float[16];

                Matrix.invertM(Ri, 0, R, 0);

                Matrix.multiplyMV(Va, 0, Ri, 0, Vf, 0);

                textViewX.setText(String.format("H : %.3f", Math.sqrt(Va[0] * Va[0]) + (Va[1] * Va[1])));
                textViewY.setText(String.format("Y : %.3f", Va[2]-9.8f));
                textViewZ.setText(String.format("Z : %.3f", Va[1]));

                yReadings.add(new Reading(sensorEvent.timestamp, Va[2] - 9.8));
            }
        } else if (mySensor.getType() == Sensor.TYPE_GRAVITY) {
            gravValues = sensorEvent.values.clone();
        } else if (mySensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            magValues = sensorEvent.values.clone();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}
