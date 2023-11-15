package com.example.fatigaapp;

import androidx.annotation.NonNull;

import android.Manifest;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.fatigaapp.detection.CascadeClassifierLoader;
import com.example.fatigaapp.detection.Detection;

import org.opencv.android.CameraActivity;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.OpenCVLoader;
import org.opencv.objdetect.CascadeClassifier;

import java.util.Collections;
import java.util.List;
import android.os.Vibrator;

public class MainActivity extends CameraActivity {

    // Sensor de luz:
    private SensorManager sensorManager;
    private Sensor lightSensor;
    private SensorEventListener lightEventListener;
    private double valueSensorLigth;
    private Handler handler;
    int countSensorLigth = 0;
    private TextView sensorTextView;


    // Camara:
    CameraBridgeViewBase cameraBridgeViewBase;

    // Clasificadores:
    CascadeClassifier faceCascade;
    CascadeClassifier eyeCascade;
    CascadeClassifier yawnCascade;


    // Etiquetas:
    private TextView blinkCountTextView; // eqtiqueta para contador de parpadeos
    private TextView blinkTimeTextView; //etiqueta para tiempo del parpadeo
    private TextView yawnCountTextView; // etiqueta para contador de bostezos
    private TextView timeTextView; // etiqueta para tiempo de aplicacion
    private  TextView stateTextView; // etiqueta para estado de fatiga

    //Botones
    private Button startButton;
    private Button stopButton;
    private boolean start = false;
    private boolean stop = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Etiquetas:
        blinkCountTextView = findViewById(R.id.blinkCountTextView);
        yawnCountTextView = findViewById(R.id.yawnCountTextView);
        cameraBridgeViewBase = findViewById(R.id.cameraView);
        blinkTimeTextView = findViewById(R.id.blinkTimeTextView);
        timeTextView = findViewById(R.id.timeTextView);
        sensorTextView = findViewById(R.id.sensorTextView);
        stateTextView = findViewById(R.id.stateTextView);
        //botones
        startButton = findViewById(R.id.startButton);
        stopButton = findViewById(R.id.stopButton);

        getPermissionFunction(); //permisos de la camara
        lightSensorFunction();  //activacion sensor luz

        cameraBridgeViewBase.setCameraIndex(1); // Camara frontal


        Detection d = new Detection(yawnCountTextView,  blinkCountTextView,  blinkTimeTextView,
                                    timeTextView, sensorTextView, stateTextView) {
            @Override
            public void showTitle(TextView textView, String text, long count) {
                runOnUiThread(() -> textView.setText(text+ count));
            }

            @Override
            public void showState(TextView textView, String text) {
                runOnUiThread(() -> textView.setText(text));
            }

            @Override
            public void alertFatiga() {
                Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
                if (vibrator != null) {
                    vibrator.vibrate(500);
                }
                Toast.makeText(MainActivity.this, "¡ALERTA!", Toast.LENGTH_SHORT).show();
                MediaPlayer mediaPlayer = MediaPlayer.create(MainActivity.this, R.raw.alertasound); // R.raw.my_sound es el identificador de tu archivo de sonido
                mediaPlayer.start();


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
        faceCascade = CascadeClassifierLoader.loadCascade(this, R.raw.haarcascade_frontalface_alt, "haarcascade_frontalface_alt.xml");
        eyeCascade = CascadeClassifierLoader.loadCascade(this, R.raw.haarcascade_eye, "haarcascade_eye.xml");
        yawnCascade = CascadeClassifierLoader.loadCascade(this, R.raw.haarcascade_yawn, "haarcascade_yawn.xml");
        d.faceCascade = faceCascade;
        d.eyeCascade = eyeCascade;
        d.yawnCascade = yawnCascade;

        pressButtons(d);

        }

    // Funcion Permisos de la camara
    void getPermissionFunction() {
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, 101);
        }
    }
    void pressButtons(Detection d){
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                start = true; // Establecer la variable en true
                Toast.makeText(MainActivity.this, "Deteccion Fatiga Iniciada ", Toast.LENGTH_SHORT).show();
                d.start = start;

            }
        });

        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stop = true; // Establecer la variable en true
                Toast.makeText(MainActivity.this, "Deteccion Fatiga Finalizada", Toast.LENGTH_SHORT).show();
                d.stop = stop;

            }
        });
    }

    //funcion Permisos del sensor
    void lightSensorFunction(){

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);

        if (lightSensor == null) {
            Toast.makeText(this, "El dispositivo no tiene sensor de luz!", Toast.LENGTH_SHORT).show();
            valueSensorLigth = 40;
            runOnUiThread(() -> sensorTextView.setText(""+valueSensorLigth));
        }else{
            handler = new Handler(Looper.getMainLooper());
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    //stopSensorManager();
                }
            }, 5000);

            lightEventListener = new SensorEventListener() {
                @Override
                public void onSensorChanged(SensorEvent sensorEvent) {
                    runOnUiThread(() -> sensorTextView.setText(""+Math.round(sensorEvent.values[0])));
                }
                @Override
                public void onAccuracyChanged(Sensor sensor, int i) {
                }
            };
            sensorManager.registerListener(lightEventListener, lightSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }

    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
            getPermissionFunction();
        }
    }


    @Override
    protected List<? extends CameraBridgeViewBase> getCameraViewList() {
        return Collections.singletonList(cameraBridgeViewBase);
    }

    private void stopSensorManager() {
        sensorManager.unregisterListener(lightEventListener);
    }

}