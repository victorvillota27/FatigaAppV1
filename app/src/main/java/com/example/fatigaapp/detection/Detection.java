package com.example.fatigaapp.detection;

import android.util.Log;
import android.widget.TextView;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Function;

public abstract class Detection implements CameraBridgeViewBase.CvCameraViewListener2 {


    private Mat grayMat;

    //clasificadores:
    public CascadeClassifier faceCascade = null;
    public CascadeClassifier eyeCascade = null;
    public CascadeClassifier yawnCascade = null;


    //contar parpadeos:
    private boolean eyesDetected = false;
    private boolean previousEyesDetected = false;
    private int blinkCount = -1;
    private TextView blinkCountTextView;

    //tiempo de parpadeo:
    private long blinkTime1 = 0;
    private long blinkTime2 = 0;
    private long elapsedTimeSeconds = 0;
    private TextView blinkTimeTextView;


    //contar bostezos:
    private int yawnCount = 0;
    private boolean previousYawnDetected = false;
    private boolean yawnDetected = false;
    private TextView yawnCountTextView;
    private long yawnTime1=0;
    private long yawnTime2=0;
    private  long elapsedTimeSeconds2 =0;

    private int nYawnDetected =0;




    long time1;
    long time2 = 0;
    long time3 = 0;
    TextView timeTextView;

    public Detection( TextView yawnCountTextView, TextView blinkCountTextView, TextView blinkTimeTextView, long time1,
                      TextView timeTextView) {
        this.yawnCountTextView = yawnCountTextView;
        this.blinkCountTextView = blinkCountTextView;
        this.blinkTimeTextView = blinkTimeTextView;
        this.time1 = time1;
        this.timeTextView = timeTextView;
    }


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

        time2 = System.currentTimeMillis()/1000;
        time3 = time2 - time1;
        showTitle(timeTextView,"Tiempo: ",time3);
////////////////////// UNO ////////////////////////////
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
        ////////////// PROCESAMIENTO DE IMAGEN /////////////////


        int value = 400;
        int variable = 0;
        if(value < 4){
            variable = 0;
        }else if (4<=value && value<=10){
            variable = 1;
        }else if(10<value && value<=500){
            variable = 2;
        }else{
            variable = 3;
        }

        grayMat = procesamiento(rotatedFrame,variable);

/*
        double brightnessFactor=0;
        double contrastFactor=0;

        if(variable<50){

            brightnessFactor = 1.6;
            Core.multiply(grayMat, new Scalar(brightnessFactor, brightnessFactor, brightnessFactor), grayMat);
            //contrastFactor =0.5;
            //grayMat.convertTo(grayMat, -1, contrastFactor, -(100*contrastFactor));

        }else if(50<variable && variable<490){
            Imgproc.equalizeHist(grayMat, grayMat);
            brightnessFactor = 1;
            contrastFactor =1;
            Core.multiply(grayMat, new Scalar(brightnessFactor, brightnessFactor, brightnessFactor), grayMat);
            grayMat.convertTo(grayMat, -1, contrastFactor*1.8, -(10*contrastFactor));
        }else if(variable>500){
            brightnessFactor = 0.5;
            contrastFactor =1.2;
            Core.multiply(grayMat, new Scalar(brightnessFactor, brightnessFactor, brightnessFactor), grayMat);
            grayMat.convertTo(grayMat, -1, contrastFactor*1.5, -(30*contrastFactor));
        }
*/

        //////////////////// TRES //////////////////////////////
        //Deteccion de rostro:
        MatOfRect faces = new MatOfRect();
        if (faceCascade != null) {
            faceCascade.detectMultiScale(grayMat, faces, 1.3, 8, 2, new Size(300, 300));
        }

        Rect[] facesArray = faces.toArray();
        for (Rect face : facesArray) {
            Imgproc.rectangle(rotatedFrame, face.tl(), face.br(), new Scalar(255, 0, 0, 255), 3);

            int faceX1 = face.x; /// x de la esquina superior izquierda
            int faceY1 = face.y; /// y de la esquina superior izquierda
            int faceX2 = face.x + face.width; // x de la esquina inferior derecha
            int faceY2 = face.y + face.height; // y de la esquina inferior derecha

            // puntos para region de los ojos:
            int eyesY2 = faceY1 + (faceY2 - faceY1)/2;
            int eyesY1 = faceY1 + (eyesY2-faceY1)/3;
            Mat eyeRegion = grayMat.submat(new Rect(faceX1, eyesY1, faceX2 - faceX1, eyesY2 - faceY1));
            Imgproc.rectangle(rotatedFrame, new Point(faceX1, eyesY1), new Point(faceX2, eyesY2), new Scalar(0, 255, 255, 255), 3);

            // npuntos para region de la boca:
            int mouthY1 = faceY1 + (faceY2 - faceY1)/2;
            int mouthY2 = faceY2 + 30;
            Mat mouthRegion = grayMat.submat(new Rect(faceX1, mouthY1, faceX2 - faceX1, mouthY2 - mouthY1));
            Imgproc.rectangle(rotatedFrame, new Point(faceX1, mouthY1), new Point(faceX2, mouthY2), new Scalar(255, 100, 255, 0), 3);

            // Deteccion de ojos:
            MatOfRect eyes = new MatOfRect();
            if (eyeCascade != null) {
                eyeCascade.detectMultiScale(eyeRegion, eyes, 1.1, 12, 2, new Size(30, 30), new Size(100, 100));
            }
            Rect[] eyesArray = eyes.toArray();
            for (Rect eye : eyesArray) {


                Point eyeTl = new Point(eye.tl().x + faceX1, eye.tl().y + eyesY1);
                Point eyeBr = new Point(eye.br().x + faceX1, eye.br().y + eyesY1);
                Imgproc.rectangle(rotatedFrame, eyeTl, eyeBr, new Scalar(0, 255, 0, 255), 3);

                /*
                //////////////////////// Dibujar elipse en lugar de Circulo /////////////////////////////////
                Point eyeTl = new Point(eye.tl().x + faceX1, eye.tl().y + eyesY1);
                Point eyeBr = new Point(eye.br().x + faceX1, eye.br().y + eyesY1);

                double aspectRatio = 0.7; // Puedes ajustar este valor según tus necesidades
                double semiMajorAxis = eye.width / 2;
                double semiMinorAxis = semiMajorAxis * aspectRatio;
                Point eyeCenter = new Point((eyeTl.x + eyeBr.x) / 2, (eyeTl.y + eyeBr.y) / 2);
                Size axes = new Size((eyeBr.x - eyeTl.x) / 2, (eyeBr.y - eyeTl.y) / 2); // Semiejes horizontal y vertical de la elipse

                Imgproc.ellipse(rotatedFrame, eyeCenter, new Size(semiMajorAxis, semiMinorAxis), 0, 0, 360, new Scalar(0, 255, 0, 255), 3);
                */
            }

            // deteccion bostezos:
            MatOfRect mouth = new MatOfRect();
            if (yawnCascade != null) {
                yawnCascade.detectMultiScale(mouthRegion, mouth, 1.2, 15, 2, new Size(100, 100));
            }
            Rect[] mouthArray = mouth.toArray();
            for (Rect yawn : mouthArray) {
                Point yawnTl = new Point(yawn.tl().x + faceX1, yawn.tl().y + mouthY1);
                Point yawnBr = new Point(yawn.br().x + faceX1, yawn.br().y + mouthY1);

                Imgproc.rectangle(rotatedFrame, yawnTl, yawnBr, new Scalar(0, 255, 0, 255), 3);
                nYawnDetected++;
                Log.d("nyawnConter","numero: "+nYawnDetected);

            }

            if(time3>5) {
                // contador de parpadeos
                blink(eyes);
                // contador bostezos
                yawn(mouth);
            }
        }

        return rotatedFrame;
    }

    public abstract void showTitle (TextView textView, String text, long count);

    //public abstract MatOfRect detectMultiScale();

    public void setFaceCascade(){
        //this.faceCascade = faceCascade;
    }

    private void blink(MatOfRect eyes) {
        if (!eyes.empty()) {
            eyesDetected = true;
            blinkTime2 = System.currentTimeMillis();

        } else {
            if (blinkTime1 == 0) {
                blinkTime1 = System.currentTimeMillis();
            }
            eyesDetected = false;
        }
        if (eyesDetected && !previousEyesDetected && !yawnDetected) {
            blinkCount++;
            if(blinkCount<1){
                elapsedTimeSeconds =0;
            }else{
                elapsedTimeSeconds = (blinkTime2 - blinkTime1);
            }

            showTitle(blinkCountTextView,"Parpadeos: ",blinkCount);
            showTitle(blinkTimeTextView,"Tiempo ultimo parpadeo: ",elapsedTimeSeconds);
            blinkTime1 = 0;
            blinkTime2 = 0;
        }
        previousEyesDetected = eyesDetected;
    }
    private void yawn(MatOfRect mouth){
        if (!mouth.empty()) {
            yawnDetected = true;

        } else {
            yawnDetected = false;
        }
        if (yawnDetected && nYawnDetected>70) {
            yawnCount++;
            showTitle(yawnCountTextView, "Bostezos: ", yawnCount);
            nYawnDetected=0;
        }
    }

    private Mat procesamiento(Mat inputImage, int variable) {

        Mat outputImage = new Mat();
        Imgproc.cvtColor(inputImage, outputImage, Imgproc.COLOR_RGBA2GRAY);

        double brightnessFactor=0;
        double contrastFactor=0;

        // Aplicar el filtro espacial según el tipo especificado
        switch (variable) {
            case 0:
                outputImage = outputImage;
                break;
            case 1:
                brightnessFactor = 1.6;
                Core.multiply(outputImage, new Scalar(brightnessFactor, brightnessFactor, brightnessFactor), outputImage);
                break;
            case 2:
                Imgproc.equalizeHist(outputImage,outputImage);
                brightnessFactor = 1;
                contrastFactor =1;
                Core.multiply(outputImage, new Scalar(brightnessFactor, brightnessFactor, brightnessFactor), outputImage);
                outputImage.convertTo(outputImage, -1, contrastFactor*1.8, -(10*contrastFactor));
                break;
            case 3:
                brightnessFactor = 0.5;
                contrastFactor =1.2;
                Core.multiply(outputImage, new Scalar(brightnessFactor, brightnessFactor, brightnessFactor), outputImage);
                outputImage.convertTo(outputImage, -1, contrastFactor*1.5, -(30*contrastFactor)); // Puedes ajustar los parámetros según tus necesidades
                break;
        }

        return outputImage;
    }

}
