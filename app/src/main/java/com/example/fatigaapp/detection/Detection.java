package com.example.fatigaapp.detection;

import android.util.Log;
import android.widget.TextView;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.features2d.ORB;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import com.example.fatigaapp.util.Time;


public abstract class Detection implements CameraBridgeViewBase.CvCameraViewListener2 {

    private static final int DELAY_TIME_TO_START = 5; // 5 SEGUNDOS PARA DETECCION
    private Mat grayMat; // MATRIZ ESCALA DE GRISES FRAME CAMARA

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
    Time blinkDuration = new Time();
    private TextView blinkTimeTextView;


    //contar bostezos:
    private long numYawFra = 0;
    private int yawCount =0;
    private boolean yawnDetected = false;
    private TextView yawnCountTextView;


    // tiempo del bostezo
    Time yawDuration = new Time();


    // tiempos
    Time timeApp = new Time();
    Time timeDet = new Time();
    long timApp = 0;
    long timDetAct = 0;
    TextView timeTextView;

    // variables del sensor:
    private TextView sensorTextView;

    // fatiga
    private  TextView stateTextView;

    //PERCLOS
    double numfraTot = 0;
    double numFramEyeClo = 0;
    double medPer;


    public Detection( TextView yawnCountTextView, TextView blinkCountTextView, TextView blinkTimeTextView,
                      TextView timeTextView, TextView sensorTextView, TextView stateTextView) {

        this.yawnCountTextView = yawnCountTextView;
        this.blinkCountTextView = blinkCountTextView;
        this.blinkTimeTextView = blinkTimeTextView;
        this.timeTextView = timeTextView;
        this.sensorTextView = sensorTextView;
        this.stateTextView = stateTextView;
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        grayMat = new Mat(); // se inicializa la matriz para la escala de grises
        timeApp.setTimIni(System.currentTimeMillis()/1000);
    }

    @Override
    public void onCameraViewStopped() {
        if (grayMat != null) {
            grayMat.release();
        }

    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {

        numfraTot++;
        double value = Double.parseDouble(sensorTextView.getText().toString());

        TimeFunction();
        Mat rotatedFrame = RotatedFrameFunction(inputFrame.rgba());

        int variable;
        variable = value<20?1:value>=30&&value<=100?2:80<value && value<=250?3:0;

        grayMat = processsingImagenFunction(rotatedFrame,variable);
        //////////////////// TRES //////////////////////////////

        //Deteccion de rostro:
        MatOfRect faces = new MatOfRect();
        if (faceCascade != null) {
            faceCascade.detectMultiScale(grayMat, faces, 1.28, 6, 2, new Size(270, 270));
        }

        Rect[] facesArray = faces.toArray();
        for (Rect face : facesArray) {
            Imgproc.rectangle(rotatedFrame, face.tl(), face.br(), new Scalar(255, 0, 0, 255), 3);
            Imgproc.rectangle(grayMat, face.tl(), face.br(), new Scalar(255, 0, 0, 255), 3);

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
            MatOfRect eyeMatRec = new MatOfRect();
            if (eyeCascade != null) {
                eyeCascade.detectMultiScale(eyeRegion, eyeMatRec, 1.1, 12, 2, new Size(30, 30), new Size(100, 100));
            }

            Rect[] eyesArray = eyeMatRec.toArray();
            for (Rect eye : eyesArray) {
                Point eyeTl = new Point(eye.tl().x + faceX1, eye.tl().y + eyesY1);
                Point eyeBr = new Point(eye.br().x + faceX1, eye.br().y + eyesY1);
                Imgproc.rectangle(rotatedFrame, eyeTl, eyeBr, new Scalar(0, 255, 0, 255), 3);
                Imgproc.rectangle(grayMat, eyeTl, eyeBr, new Scalar(0, 255, 0, 255), 3);
            }

            // deteccion bostezos:
            MatOfRect yawMatRec = new MatOfRect();
            if (yawnCascade != null) {
                yawnCascade.detectMultiScale(mouthRegion, yawMatRec, 1.2, 15, 2, new Size(100, 100));
            }
            Rect[] yawnArray = yawMatRec.toArray();
            for (Rect yawRect : yawnArray) {
                Point yawnTl = new Point(yawRect.tl().x + faceX1, yawRect.tl().y + mouthY1);
                Point yawnBr = new Point(yawRect.br().x + faceX1, yawRect.br().y + mouthY1);

                Imgproc.rectangle(rotatedFrame, yawnTl, yawnBr, new Scalar(0, 255, 0, 255), 3);
                Imgproc.rectangle(grayMat, yawnTl, yawnBr, new Scalar(0, 255, 0, 255), 3);
            }

            if(timApp >DELAY_TIME_TO_START && value > 15 && value <200) {
                blinkDetectFunction(eyeMatRec); // contador de parpadeos
                yawnDetectFunction(yawMatRec); // contador bostezos
            }
        }
        return grayMat;
    }

    public abstract void showTitle (TextView textView, String text, long count);

    public void TimeFunction(){
        timeApp.setTimFin(System.currentTimeMillis()/1000);
        timApp = timeApp.getTimAct();
        if(timApp == DELAY_TIME_TO_START){
            timeDet.setTimIni(System.currentTimeMillis()/1000);
        }else if(timApp > 5){
            if(timDetAct <30) {
                timeDet.setTimFin(System.currentTimeMillis() / 1000);
                timDetAct = timeDet.getTimAct();
                showTitle(timeTextView, "Tiempo: ", timDetAct);
            }else if(timDetAct == 30){
                int varPer = PERCLOS();
                fatigaDetectionFunction(varPer);
                timDetAct =0;
                timeDet.setTimIni(System.currentTimeMillis()/1000);
            }
        }

    }

    private void blinkDetectFunction(MatOfRect eyeMatRec) {
        if (!eyeMatRec.empty()) {
            eyesDetected = true;
            blinkDuration.setTimFin(System.currentTimeMillis());

        } else {
            if (blinkDuration.getTimIni() == 0) {
                blinkDuration.setTimIni(System.currentTimeMillis());
            }
            eyesDetected = false;
            numFramEyeClo++;
        }
        if (eyesDetected && !previousEyesDetected && !yawnDetected) {
            blinkCount++;
            showTitle(blinkCountTextView,"Parpadeos: ",blinkCount);
            if(blinkCount>0){
                showTitle(blinkTimeTextView,"Tiempo Parpadeo: ",blinkDuration.getTimAct());
            }
            blinkDuration.setTimIni(0);
            blinkDuration.setTimFin(0);
        }
        previousEyesDetected = eyesDetected;
    }


    private void yawnDetectFunction(MatOfRect yawMatRec){
        if (!yawMatRec.empty()) {
            yawnDetected = true;
            if(yawDuration.getTimIni()==0) {
                yawDuration.setTimIni(System.currentTimeMillis() / 1000);
            }
            numYawFra++;
            showTitle(yawnCountTextView, "Bostezos: ", yawCount);
        }else{
            yawDuration.setTimFin(System.currentTimeMillis()/1000);
            if(yawnDetected == true && yawDuration.getTimAct() > 5){
                if(numYawFra>20){
                    yawCount++;
                    showTitle(yawnCountTextView, "Bostezos: ", yawCount);
                }else{
                    showTitle(yawnCountTextView, "Bostezos: ", yawCount);
                }
                yawDuration.setTimAct(0);
                yawDuration.setTimIni(0);
                yawDuration.setTimFin(0);
                numYawFra=0;
                yawnDetected = false;
            }
        }
    }

    private Mat processsingImagenFunction(Mat intImage, int variable) {

        Mat outImg = new Mat();
        Imgproc.cvtColor(intImage, outImg, Imgproc.COLOR_RGBA2GRAY);

        double brightnessFactor=0;
        double contrastFactor=0;
        
        switch (variable) {

            case 1:
                brightnessFactor = 1.2;
                Imgproc.equalizeHist(outImg,outImg);
                Core.multiply(outImg, new Scalar(brightnessFactor, brightnessFactor, brightnessFactor), outImg);
                Log.d("CASO 1", ""+variable);
                break;
            case 2:
                Imgproc.equalizeHist(outImg,outImg);
                //outImg = outImg;
                Log.d("CASO 2", ""+variable);
                break;
            case 3:
                brightnessFactor = 0.8;
                //contrastFactor =1.4;
                Core.multiply(outImg, new Scalar(brightnessFactor, brightnessFactor, brightnessFactor), outImg);
                //outImg.convertTo(outImg, -1, contrastFactor*1.8, -(10*contrastFactor)); //
                Imgproc.equalizeHist(outImg,outImg);
                Log.d("CASO 3", ""+variable);
                break;
        }

        return outImg;
    }

    private Mat RotatedFrameFunction(Mat inImg){

        Mat outImg = new Mat();

        double angle = 90; // Rotación de 90 grados
        int width = inImg.cols();
        int height = inImg.rows();
        Point center = new Point(width / 2, height / 2);
        Mat rotationMatrix = Imgproc.getRotationMatrix2D(center, angle, 1);
        Imgproc.warpAffine(inImg, outImg, rotationMatrix, inImg.size());
        Core.flip(outImg, outImg, 1);

        return outImg;
    }

  public void fatigaDetectionFunction(int medPer){
        double PMPM=15;
        double PPM = blinkCount;
        double BPM = yawCount;

        if(0<PPM && PPM<=PMPM && BPM == 0){
            showTitle(stateTextView,"Estado: Sin Fatiga ", 0);
            Log.d("Estado", "NO FATIGA");
            Log.d("Estado", ""+PMPM + PPM);
        }else if(PPM>PMPM && BPM ==0){
            Log.d("Estado", "POSIBLE FATIGA");
            showTitle(stateTextView,"Estado: Posible Fatiga ", 1);
            Log.d("Estado", ""+PMPM + PPM);
        }else if(PPM>PMPM && BPM>0 && medPer== 0){
            showTitle(stateTextView,"Estado: Fatiga ", 2);
            Log.d("Estado", ""+PMPM + PPM+BPM);
        } else if (PPM>PMPM && BPM>0 && medPer == 1) {
            showTitle(stateTextView,"Estado: Fatiga Critico ", 2);
            Log.d("Estado", ""+PMPM + PPM+BPM);
        }
  }

    public int PERCLOS(){
        medPer = (numFramEyeClo / numfraTot)*100;
        Log.d("ESTADO: ", ""+medPer);
        double Per = (30/numfraTot)*100 + ((30/numfraTot)*100)*0.2;

        numfraTot=0;
        numFramEyeClo=0;

        if(medPer>Per){
            return 1;
        }else{
            return 0;
        }
    }

}


