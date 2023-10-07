package com.example.fatigaapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import org.opencv.android.CameraActivity;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint2f;
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

    CameraBridgeViewBase cameraBridgeViewBase;  // para la vista de la camara

    // varianles para clasificadores
    CascadeClassifier faceCascade;
    CascadeClassifier eyeCascade;
    Mat grayMat; //matriz para guardar el frame de la camara ya modificado en escala de grises

    //puntos para la matriz del rostro
    Point tl=null;
    Point br=null;

    //etiqueta para el contador de parpadeos
    private TextView blinkCountTextView;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //
        setContentView(R.layout.activity_main);
        blinkCountTextView = findViewById(R.id.blinkCountTextView);
        cameraBridgeViewBase = findViewById(R.id.cameraView);

        getPermission(); //permisos de la camara

        cameraBridgeViewBase.setCameraIndex(1); // cambiar a camara frontal
        cameraBridgeViewBase.setCvCameraViewListener(new CameraBridgeViewBase.CvCameraViewListener2() {
            @Override
            public void onCameraViewStarted(int width, int height) {

                grayMat = new Mat(); // se inicializa la matriz para la escala de grises
            }

            @Override
            public void onCameraViewStopped() {
                if (grayMat != null) {
                    grayMat.release(); // si se detiene la app, se libera recursos de la matriz
                }
            }

            @Override
            public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {

////////////////////// UNO ////////////////////////////

// se almacena en rgbaFrame los datos de la camara (inputFrame)
// se realiza rotacion de la matriz e entrada

                Mat rgbaFrame = inputFrame.rgba();


                // Definir el ángulo de rotación en grados (en sentido horario)
                double angle = 90; // Rotación de 90 grados para sentido antihorario

                // Obtener la altura y el ancho de la imagen original
                int width = rgbaFrame.cols();
                int height = rgbaFrame.rows();

                // Calcular el centro de la imagen
                Point center = new Point(width / 2, height / 2);

                // Calcular la matriz de rotación
                Mat rotationMatrix = Imgproc.getRotationMatrix2D(center, angle, 1);

                // Aplicar la matriz de rotación a la imagen
                Mat rotatedFrame = new Mat();


                Imgproc.warpAffine(rgbaFrame, rotatedFrame, rotationMatrix, rgbaFrame.size());
                Core.flip(rotatedFrame, rotatedFrame, 1);

 ////////////////////////////////////////////////////

/*
                Point[] entradaPuntos = new Point[4];
                entradaPuntos[0] = new Point(150, 20); // Esquina superior izquierda
                entradaPuntos[1] = new Point(width - 150, 20); /// Esquina superior derecha
                entradaPuntos[2] = new Point(100, height - 1); // Esquina inferior izquierda
                entradaPuntos[3] = new Point(width - 100, height - 1); /// Esquina inferior derecha



                Point[] salidaPuntos = new Point[4];
                salidaPuntos[0] = new Point(0, 0); // Nueva posición de la esquina superior izquierda
                salidaPuntos[1] = new Point(width - 1, 0); // Nueva posición de la esquina superior derecha
                salidaPuntos[2] = new Point(0, height - 1); // Nueva posición de la esquina inferior izquierda
                salidaPuntos[3] = new Point(width - 1, height - 1); // Nueva posición de la esquina inferior derecha

                Mat perspectiveMatrix = Imgproc.getPerspectiveTransform(new MatOfPoint2f(entradaPuntos), new MatOfPoint2f(salidaPuntos));

                Mat correctedFrame = new Mat();
                Imgproc.warpPerspective(rotatedFrame, correctedFrame, perspectiveMatrix, rotatedFrame.size());

*/
////////////////////////////////////////

//////////////////////  DOS ////////////////////////////

// se guarda la matriz del paso 1 (rotatedFrame) en grayMat realizando procesamiento a la imagen (filtro y escala de grises)
                Mat binaryImage = new Mat();

                Imgproc.cvtColor(rotatedFrame, grayMat, Imgproc.COLOR_RGBA2GRAY);
                Imgproc.adaptiveThreshold(grayMat, binaryImage, 255, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY, 11, 2);

                //Imgproc.equalizeHist(grayMat, grayMat);

//////////////////// TRES //////////////////////////////
// se usa grayMat para encontrar los rectangulos en donde se encuentra el rostro usando detectMultiscale con los
// el clasificador de rostro (faceCascade)

                MatOfRect faces = new MatOfRect();

                    if (faceCascade != null) {
                        faceCascade.detectMultiScale(grayMat, faces, 1.25, 6, 2,
                                new Size(200, 200), new Size());
                    }

                    Rect[] facesArray = faces.toArray();
                    for (Rect face : facesArray) {
                        Imgproc.rectangle(rotatedFrame, face.tl(), face.br(), new Scalar(255, 0, 0, 255), 3);

                        tl = face.tl();
                        br = face.br();

                        String tlText = "sup (" + tl.x + ", " + tl.y + ")";
                        String brText = "inf (" + br.x + ", " + br.y + ")";

                        Imgproc.putText(rotatedFrame, tlText, tl, Imgproc.FONT_HERSHEY_SIMPLEX, 0.7, new Scalar(0, 0, 255), 3);
                        Imgproc.putText(rotatedFrame, brText, br, Imgproc.FONT_HERSHEY_SIMPLEX, 0.7, new Scalar(0, 0, 255), 3);

////////////////////////////////////////////////////////////////////////////////////////////

//////////////////////// CUATRO ////////////////////////////////////////////////////////

// se extraen los puntos del rectangulo donde esta el rostro calculado en el paso anterior
// luego se crean nuevos puntos para la parte superior del rostro


                        int tlX = face.x; /// x de la esquina superior izquierda
                        int tlY = face.y; /// y de la esquina superior izquierda
                        int brX = face.x + face.width; // x de la esquina inferior derecha
                        int brY = face.y + face.height; // y de la esquina inferior derecha

                        // nuevos puntos para el rectangulo superior del rostro
                        int nuevobrY = brY - 150;
                        int nuevotlY = tlY+60;

                        //se calcula la region de interes para los ojos

                        Mat faceRegion = grayMat.submat(new Rect(tlX, nuevotlY, brX - tlX, nuevobrY - tlY));
                        Imgproc.rectangle(rotatedFrame, new Point(tlX, nuevotlY), new Point(brX, nuevobrY), new Scalar(0, 255, 255, 255), 3);

/////////////////////// CINCO ////////////////////////////////////////////
/////se aplica el clasificador de ojos sobre la region anterior


                        MatOfRect eyes = new MatOfRect();
                        if (eyeCascade != null) {
                            eyeCascade.detectMultiScale(faceRegion, eyes, 1.1, 6, 2,
                                    new Size(50, 50), new Size(70, 70));
                        }

                        // se encuentra los ojos y se dibujas circulos sobre ellos
                        Rect[] eyesArray = eyes.toArray();
                        for (Rect eye : eyesArray) {
                            Point eyeTl = new Point(eye.tl().x + tlX, eye.tl().y + nuevotlY);
                            Point eyeBr = new Point(eye.br().x + tlX, eye.br().y + nuevotlY);

                            //Imgproc.rectangle(rotatedFrame, eyeTl, eyeBr, new Scalar(0, 255, 0, 255), 3);

                            //dibujar circulo
                            Point eyeCenter = new Point((eyeTl.x + eyeBr.x) / 2, (eyeTl.y + eyeBr.y) / 2);
                            int radius = (int) Math.max((eyeBr.x - eyeTl.x) / 2, (eyeBr.y - eyeTl.y) / 2);
                            Imgproc.circle(rotatedFrame, eyeCenter, radius, new Scalar(0, 255, 0, 255), 3);
                        }
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
        //carga de los clasificadores
        loadCascadeFaceClassifier();
        loadCascadeEyeClassifier();
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
