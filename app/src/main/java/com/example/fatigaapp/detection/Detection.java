package com.example.fatigaapp.detection;

import android.media.MediaPlayer;
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
import org.opencv.ml.ParamGrid;
import org.opencv.objdetect.CascadeClassifier;

import com.example.fatigaapp.util.Time;



public abstract class Detection implements CameraBridgeViewBase.CvCameraViewListener2 {

    private static final int TIME_OF_DETECTION =60; // 60 SEGUNDOS DE DETECCION
    private Mat grayMat; // MATRIZ ESCALA DE GRISES FRAME CAMARA
    private Mat rotatedFrame; //MATRIZ MUESTRA EN PANTALLA

    //clasificadores:
    public CascadeClassifier faceCascade = null;
    public CascadeClassifier eyeCascade = null;
    public CascadeClassifier yawnCascade = null;

    //Inicio de Deteccion
    public boolean start = false;
    public boolean stop = false;
    public boolean stateDetection = false;


    //contar parpadeos:
    private boolean eyesDetected = false;
    private boolean previousEyesDetected = false;
    private int blinkCount = -1;
    private TextView blinkCountTextView;


    //tiempo de parpadeo:
    Time blinkDuration = new Time();
    private int longBlinkCount = 0;
    private TextView blinkTimeTextView;



    //contar bostezos:
    private long numYawFra = 0;
    private int yawCount =0;
    private boolean yawnDetected = false;
    private TextView yawnCountTextView;


    // tiempo del bostezo
    Time yawDuration = new Time();


    // tiempos
    //Time timeApp = new Time();
    Time timeDet = new Time();
    //long timApp = 0;
    long timDetAct = 0;
    TextView timeTextView;

    // variables del sensor:
    private TextView sensorTextView;

    // fatiga

    private boolean perclos = false;
    private boolean durationBlink = false;
    private boolean presenceYawn = false;
    private boolean frecuencyBlink = false;
    int fatigaStatesCount = 0;
    private  TextView stateTextView;

    //PERCLOS
    double numfraTot = 0;
    double numFramEyeClo = 0;
    double medPer;

    // medida de lux para procesamiento
    int medLux;

    public Detection(TextView yawnCountTextView, TextView blinkCountTextView, TextView blinkTimeTextView,
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
        rotatedFrame = new Mat();
        //timeApp.setTimIni(System.currentTimeMillis()/1000);
    }

    @Override
    public void onCameraViewStopped() {
        if (grayMat != null) {
            grayMat.release();
        }

    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {

        double value = Double.parseDouble(sensorTextView.getText().toString());

        TimeFunction();
        rotatedFrame = RotatedFrameFunction(inputFrame.rgba());

        medLux = value>=15&&value<=55?1:55<value && value<=120?2:0;
        grayMat = processsingImagenFunction(rotatedFrame, medLux,value);
        //////////////////// TRES //////////////////////////////

        //Deteccion de rostro:
        MatOfRect faces = new MatOfRect();
        if (faceCascade != null) {
            faceCascade.detectMultiScale(grayMat, faces, 1.3, 8, 2, new Size(270, 270));
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
                eyeCascade.detectMultiScale(eyeRegion, eyeMatRec, 1.08, 15, 2, new Size(10, 10), new Size(80, 80));
            }

            Rect[] eyesArray = eyeMatRec.toArray();
            for (Rect eye : eyesArray) {
                /*
                Point eyeTl = new Point(eye.tl().x + faceX1, eye.tl().y + eyesY1);
                Point eyeBr = new Point(eye.br().x + faceX1, eye.br().y + eyesY1);
                Imgproc.rectangle(rotatedFrame, eyeTl, eyeBr, new Scalar(0, 255, 0, 255), 3);
                Imgproc.rectangle(grayMat, eyeTl, eyeBr, new Scalar(0, 255, 0, 255), 3);
                */
                Point eyeTl = new Point(eye.tl().x + faceX1, eye.tl().y + eyesY1);
                Point eyeBr = new Point(eye.br().x + faceX1, eye.br().y + eyesY1);

                double aspectRatio = 0.8; // Puedes ajustar este valor según tus necesidades
                double semiMajorAxis = eye.width / 2;
                double semiMinorAxis = semiMajorAxis * aspectRatio;
                Point eyeCenter = new Point((eyeTl.x + eyeBr.x) / 2, (eyeTl.y + eyeBr.y) / 2);

                Imgproc.ellipse(rotatedFrame, eyeCenter, new Size(semiMajorAxis, semiMinorAxis), 0, 0, 360, new Scalar(0, 255, 0, 255), 3);
                //EARFunction(rotatedFrame,eyeCenter,semiMinorAxis,semiMajorAxis);

            }

            // deteccion bostezos:
            MatOfRect yawMatRec = new MatOfRect();
            if (yawnCascade != null) {
                yawnCascade.detectMultiScale(mouthRegion, yawMatRec, 1.3, 12, 2, new Size(70, 70));
            }
            Rect[] yawnArray = yawMatRec.toArray();
            for (Rect yawRect : yawnArray) {
                Point yawnTl = new Point(yawRect.tl().x + faceX1, yawRect.tl().y + mouthY1);
                Point yawnBr = new Point(yawRect.br().x + faceX1, yawRect.br().y + mouthY1);

                Imgproc.rectangle(rotatedFrame, yawnTl, yawnBr, new Scalar(0, 255, 0, 255), 3);
                Imgproc.rectangle(grayMat, yawnTl, yawnBr, new Scalar(0, 255, 0, 255), 3);
            }

            if(stateDetection == true && value > 15 && value <120) {
                numfraTot++;
                blinkDetectFunction(eyeMatRec); // contador de parpadeos
                yawnDetectFunction(yawMatRec); // contador bostezos
            }
        }
        return rotatedFrame;
    }

    public abstract void showTitle (TextView textView, String text, long count);
    public abstract void showState(TextView textView,String text);
    public abstract void alertFatiga();

    public void TimeFunction(){
        //timeApp.setTimFin(System.currentTimeMillis()/1000);
        //timApp = timeApp.getTimAct();
        //showTitle(timeTextView, "Tiempo: ", timApp);


        if(start == true){
            timeDet.setTimIni(System.currentTimeMillis()/1000);
            start = false;
            blinkCount = 0;
            yawCount = 0;
            showTitle(yawnCountTextView, "Bostezos: ",0);
            showTitle(blinkCountTextView,"Parpadeos: ",0);
            showTitle(blinkTimeTextView,"Tiempo Parpadeo: ",0);
            stateDetection = true;
        }
        if(stop == true){
            stop = false;
            showTitle(yawnCountTextView, "Bostezos: ",0);
            showTitle(blinkCountTextView,"Parpadeos: ",0);
            showTitle(blinkTimeTextView,"Tiempo Parpadeo: ",0);

            showTitle(timeTextView, "Tiempo: ",0);
            showState(stateTextView,"ESTADO: ");


            stateDetection = false;
        }

        if( stateDetection == true){
            timeDet.setTimFin(System.currentTimeMillis()/1000);
            timDetAct = timeDet.getTimAct();
            showTitle(timeTextView, "Tiempo: ", timDetAct);
            if(timDetAct <TIME_OF_DETECTION) {
                timeDet.setTimFin(System.currentTimeMillis() / 1000);
                timDetAct = timeDet.getTimAct();
                showTitle(timeTextView, "Tiempo: ", timDetAct);
            }else if(timDetAct == TIME_OF_DETECTION){
                fatigaDetectionFunction();
                timDetAct =0;
                timeDet.setTimIni(System.currentTimeMillis()/1000);

                showTitle(yawnCountTextView, "Bostezos: ", 0);
                showTitle(blinkCountTextView,"Parpadeos: ",0);
            }
        }else {
            stop = false;
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
            if(blinkCount>0 && blinkDuration.getTimAct()<400){
                showTitle(blinkTimeTextView,"Tiempo Parpadeo: ",blinkDuration.getTimAct());
                if(blinkDuration.getTimAct()>200 && blinkDuration.getTimAct()<400){
                    longBlinkCount++;
                }
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
                if(numYawFra>30){
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

    private void EARFunction(Mat rotatedFrame, Point eyeCenter, double semiMinorAxis, double semiMajorAxis){
        Point topPoint = new Point(eyeCenter.x, eyeCenter.y - semiMinorAxis);
        Point bottomPoint = new Point(eyeCenter.x, eyeCenter.y + semiMinorAxis);
        Point leftPoint = new Point(eyeCenter.x - semiMajorAxis, eyeCenter.y);
        Point rightPoint = new Point(eyeCenter.x + semiMajorAxis, eyeCenter.y);

        Imgproc.circle(rotatedFrame, topPoint, 2, new Scalar(255, 100, 0), 2);
        Imgproc.circle(rotatedFrame, bottomPoint, 2, new Scalar(255, 110, 0), 2);
        Imgproc.circle(rotatedFrame, leftPoint, 2, new Scalar(255, 0, 110), 2);
        Imgproc.circle(rotatedFrame, rightPoint, 2, new Scalar(255, 0, 110), 2);

        double distanceTopBottom = Math.sqrt(Math.pow(bottomPoint.x - topPoint.x, 2) + Math.pow(bottomPoint.y - topPoint.y, 2));
        double distanceLeftRight = Math.sqrt(Math.pow(leftPoint.x - rightPoint.x, 2) + Math.pow(leftPoint.y - rightPoint.y, 2));
        double EAR = distanceTopBottom/distanceLeftRight;

        System.out.println("Relacion Aspecto del ojo: " + EAR);
    }

    private Mat processsingImagenFunction(Mat intImage, int variable, double value) {

        Mat outImg = new Mat();
        Imgproc.cvtColor(intImage, outImg, Imgproc.COLOR_RGBA2GRAY);

        double brightnessFactor=0;
        double contrastFactor=0;

        switch (variable) {


            case 1:
                brightnessFactor = value<20?1.2:20<=value && value<=40?1.15:1;

                Imgproc.equalizeHist(outImg,outImg);
                Core.multiply(outImg, new Scalar(brightnessFactor, brightnessFactor, brightnessFactor), outImg);
                break;
            case 2:

                contrastFactor = value<70?1.3:70<=value && value<=100?1.4:1.5;
                Imgproc.equalizeHist(outImg,outImg);
                outImg.convertTo(outImg, -1, contrastFactor, -(4*contrastFactor)); //
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

  public void fatigaDetectionFunction(){

      double PMPM=15;
      double Per = (30/numfraTot)*100;
      Log.d("Perclos: ",""+Per);
      double PPM = blinkCount;
      double BPM = yawCount;
      // CALCULO DE PERCLOS
      medPer = (numFramEyeClo / numfraTot)*100;

      //FRECUANCIA PARPADEO
      if(PPM>= PMPM){
          frecuencyBlink = true;
          fatigaStatesCount++;
          Log.d("FRECUENCIA DEL PARPADEO: ",""+PPM +"por minuto");
      }
      // PRESENCIA BOSTEZOS
      if(0<BPM){
        presenceYawn = true;
        fatigaStatesCount++;
        Log.d("BOSTEZOS: ",""+BPM +"por minuto");
      }
      //PERCLOS
      if(medPer> Per + Per*0.2){
          perclos = true;
          fatigaStatesCount++;
          Log.d("PERCLOS: ",""+medPer);
          Log.d("NUMERO DE FRAMES OJOS CERRADOS: ", ""+ numFramEyeClo);
          Log.d("NUMERO DE FRAMES TOTALES: ", ""+ numfraTot);
      }
      //DURACION DEL PARPADEO
      if(longBlinkCount>7){
          durationBlink = true;
          fatigaStatesCount++;
          Log.d("DURACION DEL PARPADEO",""+ longBlinkCount +" veces en 1 minuto");
      }


      if(fatigaStatesCount>=3){
          showState(stateTextView,"ESTADO:  FATIGA ");
          alertFatiga();
      }else if(fatigaStatesCount>=1){
          showState(stateTextView,"ESTADO:  POSIBLE FATIGA ");
      }else{
          showState(stateTextView,"ESTADO:  SIN FATIGA ");

      }

      frecuencyBlink = false;
      durationBlink = false;
      perclos = false;
      presenceYawn = false;

      numFramEyeClo = 0;
      numfraTot = 0;
      blinkCount = 0;
      yawCount = 0;
      longBlinkCount =0;
      fatigaStatesCount =0;

/*
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
        */
  }


}


