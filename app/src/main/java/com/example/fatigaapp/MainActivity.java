package com.example.fatigaapp;

import androidx.annotation.NonNull;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import com.example.fatigaapp.detection.Detection;

import org.opencv.android.CameraActivity;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends CameraActivity {

    CameraBridgeViewBase cameraBridgeViewBase;
    CascadeClassifier faceCascade;
    CascadeClassifier eyeCascade;
    CascadeClassifier yawnCascade;


    //variables para los parpadeos
    private TextView blinkCountTextView;
    private boolean eyesDetected = false;
    private boolean previousEyesDetected = false;
    private int blinkCount;
    long elapsedTimeSeconds;
    private TextView blinkTimeTextView;

    //variables para bostezo
    private TextView yawnCountTextView;
    private boolean yawnDetected = false;
    private boolean previousYawnDetected = false;
    private int yawnCount = 0;


    private boolean yawnTimerActive = false;
    private long startTime;
    private Timer timer;

    private long blinkTime1 = 0;
    private long blinkTime2 = 0;


    //////
    long time1 = 0;
    private TextView timeTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        time1 = System.currentTimeMillis()/1000;

        setContentView(R.layout.activity_main);
        blinkCountTextView = findViewById(R.id.blinkCountTextView);
        yawnCountTextView = findViewById(R.id.yawnCountTextView);
        cameraBridgeViewBase = findViewById(R.id.cameraView);
        blinkTimeTextView = findViewById(R.id.blinkTimeTextView);
        timeTextView = findViewById(R.id.timeTextView);


        getPermission(); //permisos de la camara
        cameraBridgeViewBase.setCameraIndex(1); // cambiar a camara frontal

///////////////////////////////////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////      CODIGO NUEVO        ///////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////////////////////////////////////

        Detection d = new Detection(yawnCountTextView,  blinkCountTextView,  blinkTimeTextView, time1, timeTextView) {
            @Override
            public void showTitle(TextView textView, String text, long count) {
                runOnUiThread(() -> textView.setText(text+ count));
            }
        };
        cameraBridgeViewBase.setCvCameraViewListener(d);

        if (OpenCVLoader.initDebug()) {
            Log.d("LOADED", "success");

            cameraBridgeViewBase.enableView();
        } else {
            Log.d("LOADED", "error");
        }
        //carga de los clasificadores
        loadCascadeFaceClassifier();
        d.faceCascade = faceCascade;
        loadCascadeEyeClassifier();
        d.eyeCascade = eyeCascade;
        loadCascadeYawnClassifier();
        d.yawnCascade = yawnCascade;
        }


    void getPermission() {
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, 101);
        }
    }

    //clasificador de rostro
    private void loadCascadeFaceClassifier() {
        try {
            InputStream is = getResources().openRawResource(R.raw.haarcascade_frontalface_alt);
            File cascadeDir = getDir("cascade", MODE_PRIVATE);
            File cascadeFile = new File(cascadeDir, "haarcascade_frontalface_alt.xml");
            FileOutputStream os = new FileOutputStream(cascadeFile);

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }

            is.close();
            os.close();

            faceCascade = new CascadeClassifier(cascadeFile.getAbsolutePath());



            if (faceCascade.empty()) {
                Log.e("CASCADE", "Failed to load cascade classifier");
            } else {
                Log.d("CASCADE", "Loaded cascade classifier");
            }
        } catch (IOException e) {
            e.printStackTrace();
            Log.e("CASCADE", "Error copying cascade classifier");
        }
    }

    //clasificador de ojos
    private void loadCascadeEyeClassifier() {
        try {
            InputStream is = getResources().openRawResource(R.raw.haarcascade_eye);
            File cascadeDir = getDir("cascade", MODE_PRIVATE);
            File cascadeFile = new File(cascadeDir, "haarcascade_eye.xml");
            FileOutputStream os = new FileOutputStream(cascadeFile);

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }

            is.close();
            os.close();

            eyeCascade = new CascadeClassifier(cascadeFile.getAbsolutePath());

            if (eyeCascade.empty()) {
                Log.e("CASCADE", "Failed to load cascade classifier");
            } else {
                Log.d("CASCADE", "Loaded cascade classifier");
            }
        } catch (IOException e) {
            e.printStackTrace();
            Log.e("CASCADE", "Error copying cascade classifier");
        }
    }

    private void loadCascadeYawnClassifier() {
        try {
            InputStream is = getResources().openRawResource(R.raw.haarcascade_yawn);
            File cascadeDir = getDir("cascade", MODE_PRIVATE);
            File cascadeFile = new File(cascadeDir, "haarcascade_yawn.xml");
            FileOutputStream os = new FileOutputStream(cascadeFile);

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }

            is.close();
            os.close();

            yawnCascade = new CascadeClassifier(cascadeFile.getAbsolutePath());

            if (yawnCascade.empty()) {
                Log.e("CASCADE", "Failed to load cascade classifier");
            } else {
                Log.d("CASCADE", "Loaded cascade classifier");
            }
        } catch (IOException e) {
            e.printStackTrace();
            Log.e("CASCADE", "Error copying cascade classifier");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
            getPermission();
        }
    }

    @Override
    protected List<? extends CameraBridgeViewBase> getCameraViewList() {
        return Collections.singletonList(cameraBridgeViewBase);
    }

}