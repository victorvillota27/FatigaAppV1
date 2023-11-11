package com.example.fatigaapp.detection;

import android.content.Context;
import android.util.Log;

import org.opencv.objdetect.CascadeClassifier;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class CascadeClassifierLoader {

    public static CascadeClassifier loadCascade(Context context, int resId, String outFilNam) {
        CascadeClassifier cascadeClassifier = new CascadeClassifier();

        try {
            InputStream is = context.getResources().openRawResource(resId);
            File cascadeDir = context.getDir("cascade", Context.MODE_PRIVATE);
            File cascadeFile = new File(cascadeDir, outFilNam);
            FileOutputStream os = new FileOutputStream(cascadeFile);

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }

            is.close();
            os.close();

            cascadeClassifier.load(cascadeFile.getAbsolutePath());

            if (cascadeClassifier.empty()) {
                Log.e("CASCADE", "Failed to load cascade classifier");
            } else {
                Log.d("CASCADE", "Loaded cascade classifier");
            }
        } catch (IOException e) {
            e.printStackTrace();
            Log.e("CASCADE", "Error copying cascade classifier");
        }

        return cascadeClassifier;
    }
}
