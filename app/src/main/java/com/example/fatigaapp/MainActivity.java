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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends CameraActivity {

    CameraBridgeViewBase cameraBridgeViewBase;  // para la vista de la camara

    // varianles para clasificadores
    CascadeClassifier faceCascade;
    CascadeClassifier eyeCascade;
    CascadeClassifier yawnCascade;
    Mat grayMat; //matriz para guardar el frame de la camara ya modificado en escala de grises

    //puntos para la matriz del rostro
    Point tl=null;
    Point br=null;

    //etiqueta para el contador de parpadeos
    private TextView blinkCountTextView;
    private boolean eyesDetected = false;
    private boolean previousEyesDetected = false;
    private int blinkCount = 0;

    //etiqueta bostezos
    private TextView yawnCountTextView;
    private boolean yawnDetected = false;
    private boolean previousYawnDetected = false;
    private int yawnCount = 0;



//////////////////////////////////////////////////////////////////////////////////////////////////
    ///////////// Variables para EAR /////////////////////////
  /*  private int blinkCount = 0;  // Contador de parpadeos
    private static final double BLINK_EAR_THRESHOLD = 0.610327780786685;  // Umbral para considerar un parpadeo
    private static final long blinkDurationMillis = 1000;  // Duración (en milisegundos) para contar como un parpadeo
    private boolean isBlinkInProgress = false;
*/
///////////////////////////////////////////////////////////////////////


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //
        setContentView(R.layout.activity_main);
        blinkCountTextView = findViewById(R.id.blinkCountTextView);
        yawnCountTextView = findViewById(R.id.yawnCountTextView);
        cameraBridgeViewBase = findViewById(R.id.cameraView);
        //FaceMesh faceMesh = new FaceMesh(context, FaceMesh.FaceMeshStaticValue.MODEL_NAME, FaceMesh.FaceMeshStaticValue.FLIP_FRAMES);


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

////////////////////////////////////////////////////////////////////////////////////////////
//////////////////////// CUATRO ////////////////////////////////////////////////////////

// se extraen los puntos del rectangulo donde esta el rostro calculado en el paso anterior
// luego se crean nuevos puntos para la parte superior del rostro


                        int faceX1 = face.x; /// x de la esquina superior izquierda
                        int faceY1 = face.y; /// y de la esquina superior izquierda
                        int faceX2 = face.x + face.width; // x de la esquina inferior derecha
                        int faceY2 = face.y + face.height; // y de la esquina inferior derecha

                        // nuevos puntos para el rectangulo superior del rostro
                        int eyesY2 = faceY2 - 170;
                        int eyesY1 = faceY1+70;
                        //se calcula la region de interes para los ojos
                        Mat eyeRegion = grayMat.submat(new Rect(faceX1, eyesY1, faceX2 - faceX1, eyesY2 - faceY1));
                        Imgproc.rectangle(rotatedFrame, new Point(faceX1, eyesY1), new Point(faceX2, eyesY2), new Scalar(0, 255, 255, 255), 3);

                        // nuevos puntos para el rectangulo inferior
                        int mouthY2 = faceY2 +40;
                        int mouthY1 = faceY1+200;

                        //se calcula la region de interes para la boca
                        Mat mouthRegion = grayMat.submat(new Rect(faceX1, mouthY1, faceX2 - faceX1, mouthY2 - mouthY1));
                        Imgproc.rectangle(rotatedFrame, new Point(faceX1, mouthY1), new Point(faceX2, mouthY2), new Scalar(255, 100, 255, 0), 3);

/////////////////////// CINCO ////////////////////////////////////////////
/////se aplica el clasificador de ojos sobre la region anterior


                        MatOfRect eyes = new MatOfRect();
                        if (eyeCascade != null) {
                            eyeCascade.detectMultiScale(eyeRegion, eyes, 1.08, 15, 2,
                                    new Size(50, 50), new Size(70, 70));
                        }

                        // se encuentra los ojos y se dibujas circulos sobre ellos
                        Rect[] eyesArray = eyes.toArray();
                        for (Rect eye : eyesArray) {


                            Point eyeTl = new Point(eye.tl().x + faceX1, eye.tl().y + eyesY1);
                            Point eyeBr = new Point(eye.br().x + faceX1, eye.br().y + eyesY1);

//////////////////////// Dibujar elipse en lugar de Circulo /////////////////////////////////

                            double aspectRatio = 0.7; // Puedes ajustar este valor según tus necesidades

                            // Calcular los tamaños de los ejes en función del ancho y la relación de aspecto
                            double semiMajorAxis = eye.width / 2;
                            double semiMinorAxis = semiMajorAxis * aspectRatio;


                            Point eyeCenter = new Point((eyeTl.x + eyeBr.x) / 2, (eyeTl.y + eyeBr.y) / 2);
                            Size axes = new Size((eyeBr.x - eyeTl.x) / 2, (eyeBr.y - eyeTl.y) / 2); // Semiejes horizontal y vertical de la elipse

                            // Dibujar la elipse
                            Imgproc.ellipse(rotatedFrame, eyeCenter, new Size(semiMajorAxis, semiMinorAxis), 0, 0, 360,new Scalar(0, 255, 0, 255), 3);


///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////// CALCULAR REALACION DE ASPECTO DEL OJO (EAR)
                           /*
                            int numAdditionalPoints = 4;
                            List<Point> additionalPoints = new ArrayList<>();

                            for (int i = 0; i < numAdditionalPoints; i++) {
                                angle = 2 * Math.PI * i / numAdditionalPoints;
                                double x = eyeCenter.x + semiMajorAxis * Math.cos(angle);
                                double y = eyeCenter.y + semiMinorAxis * Math.sin(angle);
                                additionalPoints.add(new Point(x, y));
                            }

                            Point p1 = additionalPoints.get(0); // Primer punto
                            Point p2 = additionalPoints.get(1); // Segundo punto
                            Point p3 = additionalPoints.get(2); // Tercer punto
                            Point p4 = additionalPoints.get(3); // Cuarto punto

                            Scalar pointColor = new Scalar(255, 0, 0); // Color azul
                            int pointRadius = 2; // Radio del punto
                            Imgproc.circle(rotatedFrame, p1, pointRadius, pointColor, 2); // Dibujar p1
                            Imgproc.circle(rotatedFrame, p2, pointRadius, pointColor, 2); // Dibujar p2
                            Imgproc.circle(rotatedFrame, p3, pointRadius, pointColor, 2); // Dibujar p3
                            Imgproc.circle(rotatedFrame, p4, pointRadius, pointColor, 2); // Dibujar p4

                            // Calcular EAR
                            double distance1 = Math.sqrt(Math.pow(p1.x - p2.x, 2) + Math.pow(p1.y - p2.y, 2));
                            double distance2 = Math.sqrt(Math.pow(p3.x - p4.x, 2) + Math.pow(p3.y - p4.y, 2));
                            double distance3 = Math.sqrt(Math.pow(p1.x - p3.x, 2) + Math.pow(p1.y - p3.y, 2));

                            double ear = (distance1 + distance2) / (2.0 * distance3);


                            Log.d("EAR", "EAR: " + ear);

                            if (ear < BLINK_EAR_THRESHOLD && !isBlinkInProgress) {
                                isBlinkInProgress = true;
                                blinkCount++;
                                runOnUiThread(() -> blinkCountTextView.setText("Parpadeos: " + blinkCount));

                            }else if(ear >= BLINK_EAR_THRESHOLD){
                                isBlinkInProgress = false;
                            }
                            */
                        }


///////////////////////////////////////////////////////////////
                        // contador de parpadeos

                        if (!eyes.empty()) {
                            eyesDetected = true;
                            runOnUiThread(() ->blinkCountTextView.setText("Parpadeos" + blinkCount));

                        }else{
                            eyesDetected = false;
                            runOnUiThread(() ->blinkCountTextView.setText("Parpadeos" + blinkCount));
                        }
                        if (eyesDetected && !previousEyesDetected) {
                            // Hubo una transición de false a true, por lo que incrementamos el contador
                            blinkCount++;
                            runOnUiThread(() ->blinkCountTextView.setText("Parpadeos" + blinkCount));
                        }
                        previousEyesDetected = eyesDetected;
///////////////////////////////////////////////////////////////////////////////////////////////////////

                        MatOfRect mouth = new MatOfRect();
                        if (yawnCascade != null) {
                            yawnCascade.detectMultiScale(mouthRegion, mouth, 1.2, 9, 2,
                                    new Size(100, 100));
                        }

                        // se encuentra los ojos y se dibujas circulos sobre ellos
                        Rect[] mouthArray = mouth.toArray();
                        for (Rect yawn : mouthArray) {
                            Point yawnTl = new Point(yawn.tl().x + faceX1, yawn.tl().y + mouthY1);
                            Point yawnBr = new Point(yawn.br().x + faceX1, yawn.br().y + mouthY1);

                            Imgproc.rectangle(rotatedFrame, yawnTl, yawnBr, new Scalar(0, 255, 0, 255), 3);
                        }

///////////////////////////////////////////////////////////////////////////////////////
                        // contador de bostezos

                        if (!mouth.empty()) {
                            yawnDetected = true;
                            runOnUiThread(() ->yawnCountTextView.setText("Bostezos:" + yawnCount));

                        }else{
                            yawnDetected = false;
                            runOnUiThread(() ->yawnCountTextView.setText("Bostezos:" + yawnCount));
                        }
                        if (yawnDetected && !previousYawnDetected) {
                            // Hubo una transición de false a true, por lo que incrementamos el contador
                            yawnCount++;
                            runOnUiThread(() ->yawnCountTextView.setText("Bostezos:" + yawnCount));
                        }
                        previousYawnDetected = yawnDetected;
///////////////////////////////////////////////////////////////////////////////////////////////////////
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
        loadCascadeYawnClassifier();
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
