package com.example.samplear;

import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.label.ImageLabel;
import com.google.mlkit.vision.label.ImageLabeler;
import com.google.mlkit.vision.label.ImageLabeling;
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions;

import java.util.List;

public class RecogniseImage extends AsyncTask<Void, Void, Void> {
    private final Bitmap bitmap;
    private int cameraOrientation = 0;
    public String imageLabel="NULL";
    private float maxConfidence = 0f;

    public RecogniseImage(Bitmap bitmap, int cameraOrientation) {
        this.bitmap = bitmap;
        this.cameraOrientation = cameraOrientation;
    }

    public String recogniseImage() {
        InputImage image = InputImage.fromBitmap(bitmap, cameraOrientation);
        ImageLabeler labeler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS);
        String msg = "";
        labeler.process(image)
                .addOnSuccessListener(new OnSuccessListener<List<ImageLabel>>() {
                    @Override
                    public void onSuccess(List<ImageLabel> labels) {
                        StringBuilder msg= new StringBuilder();
                        for (ImageLabel label : labels) {
                            String text = label.getText();
                            float confidence = label.getConfidence();
                            int index = label.getIndex();
                            if(label.getConfidence() > 0.2) {
                                msg.append(text).append(": ").append(confidence).append("\n");
                            }
                            if(confidence > maxConfidence){
                                maxConfidence = confidence;
                                imageLabel = text;
                            }
                            Log.i("TAGGED", index + ": " + text + ": " + confidence + "\n");
                        }
                        Log.i("TAGGED", "********" + imageLabel + "*********");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // Task failed with an exception
                        // ...
                    }
                });
        return imageLabel;

    }

    @Override
    protected Void doInBackground(Void... voids) {
        recogniseImage();
        return null;
    }
}
