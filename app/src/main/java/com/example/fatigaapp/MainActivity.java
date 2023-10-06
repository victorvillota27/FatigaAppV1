package com.example.fatigaapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;

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

public class MainActivity extends CameraActivity {

    CameraBridgeViewBase cameraBridgeViewBase;
    CascadeClassifier faceCascade;
    Mat grayMat, transpose_grayMat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getPermission();

        cameraBridgeViewBase = findViewById(R.id.cameraView);

        cameraBridgeViewBase.setCameraIndex(1);
        cameraBridgeViewBase.setCvCameraViewListener(new CameraBridgeViewBase.CvCameraViewListener2() {
            @Override
            public void onCameraViewStarted(int width, int height) {
                grayMat = new Mat();
            }

            @Override
            public void onCameraViewStopped() {
                if (grayMat != null) {
                    grayMat.release();
                }
            }

            @Override
            public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {

                Mat rgbaFrame = inputFrame.rgba();

// Definir el ángulo de rotación en grados (en sentido horario)
                double angle = 450; // Rotación de -90 grados para sentido antihorario

// Obtener la altura y el ancho de la imagen original
                int width = rgbaFrame.cols();
                int height = rgbaFrame.rows();

// Calcular el centro de la imagen
                Point center = new Point(width / 2, height/2);

// Calcular la matriz de rotación
                Mat rotationMatrix = Imgproc.getRotationMatrix2D(center, angle, 1);

// Aplicar la matriz de rotación a la imagen
                Mat rotatedFrame = new Mat();

                Imgproc.warpAffine(rgbaFrame, rotatedFrame, rotationMatrix, rgbaFrame.size());
                Core.flip(rotatedFrame, rotatedFrame, 1);


                Imgproc.cvtColor(rotatedFrame, grayMat, Imgproc.COLOR_RGBA2GRAY);
                Imgproc.equalizeHist(grayMat, grayMat);

                MatOfRect faces = new MatOfRect();
                if (faceCascade != null) {
                    faceCascade.detectMultiScale(grayMat, faces, 1.3, 4, 2,
                            new Size(30, 30), new Size());
                }

                Rect[] facesArray = faces.toArray();
                for (Rect face : facesArray) {
                    Imgproc.rectangle(rotatedFrame, face.tl(), face.br(), new Scalar(255, 0, 0, 255), 3);

                    Point tl = face.tl();
                    Point br = face.br();

                    String tlText = "sup (" + tl.x + ", " + tl.y + ")";
                    String brText = "inf (" + br.x + ", " + br.y + ")";

                    Imgproc.putText(rotatedFrame, tlText, tl, Imgproc.FONT_HERSHEY_SIMPLEX, 0.8, new Scalar(0, 0, 255), 2);
                    Imgproc.putText(rotatedFrame, brText, br, Imgproc.FONT_HERSHEY_SIMPLEX, 0.8, new Scalar(0, 0, 255), 2);
                }

                return rotatedFrame;
            }
        });

        if (OpenCVLoader.initDebug()) {
            Log.d("LOADED", "success");
            cameraBridgeViewBase.enableView();
        } else {
            Log.d("LOADED", "error");
        }

        loadCascadeClassifier();
    }

    void getPermission() {
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, 101);
        }
    }

    private void loadCascadeClassifier() {
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
