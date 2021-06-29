package com.example.samplear;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.Image;
import android.net.Uri;
import android.opengl.Visibility;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.view.OrientationEventListener;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.ar.core.exceptions.NotYetAvailableException;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.assets.RenderableSource;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.TransformableNode;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.FirebaseApp;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.label.ImageLabel;
import com.google.mlkit.vision.label.ImageLabeler;
import com.google.mlkit.vision.label.ImageLabeling;
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity {

    PreviewView cameraView;
    ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    int cameraOrientation = 0;
    Button button;
    String fileName = null;
    float maxConfidence = 0f;
    String imageLabel = "";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

//        cameraFrame = findViewById(R.id.cameraFrame);
//        arView = findViewById(R.id.arView);
        cameraView = findViewById(R.id.cameraView);
        cameraProviderFuture = ProcessCameraProvider.getInstance(getApplicationContext());

        FirebaseApp.initializeApp(this);
        button = findViewById(R.id.button);

        if(!hasCameraPermission()){
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA},10);
        }

        cameraProviderFuture.addListener(new Runnable() {
            @Override
            public void run() {
                try {
                    ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                    bindImageAnalysis(cameraProvider);
                } catch (ExecutionException | InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }, ContextCompat.getMainExecutor(this));

//        try {
//            Image image= arFragment.getArSceneView().getArFrame().acquireCameraImage();
//
//            byte[] byteArray = null;
//            byteArray = NV21toJPEG(YUV420toNV21(image),image.getWidth(),image.getHeight(),100);
//
//            Bitmap bitmap= BitmapFactory.decodeByteArray(byteArray,0,byteArray.length);
//
//            Log.i("BitmapValues", String.valueOf(bitmap));
//
//        } catch (NotYetAvailableException e) {
//            e.printStackTrace();
//        }

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fileName = "";
                maxConfidence = 0f;
                recogniseImage(cameraView.getBitmap());

                Log.i("TAGGED", "*********** " + fileName + " ***********");
            }
        });

    }

    private boolean hasCameraPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    private void bindImageAnalysis(@NonNull ProcessCameraProvider cameraProvider) {
        ImageAnalysis imageAnalysis =
                new ImageAnalysis.Builder().setTargetResolution(new Size(1920, 1080))
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build();
        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this), new ImageAnalysis.Analyzer() {
            @Override
            public void analyze(@NonNull ImageProxy image) {
                image.close();
            }
        });
        OrientationEventListener orientationEventListener = new OrientationEventListener(this) {
            @Override
            public void onOrientationChanged(int orientation) {
                cameraOrientation = orientation;
            }
        };
        orientationEventListener.enable();
        Preview preview = new Preview.Builder().build();
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK).build();
        preview.setSurfaceProvider(cameraView.getSurfaceProvider());
        cameraProvider.bindToLifecycle((LifecycleOwner)this, cameraSelector,
                imageAnalysis, preview);
    }

    public void openArView(View view) {
        if(!fileName.equalsIgnoreCase("null")) {
            Intent intent = new Intent(MainActivity.this, ArActivity.class);
            intent.putExtra("fileName", fileName);
            startActivity(intent);
        }
    }

    public void recogniseImage(Bitmap bitmap) {
        InputImage image = InputImage.fromBitmap(bitmap, 0);
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
                        fileName = imageLabel.toLowerCase();
                        Toast.makeText(getApplicationContext(), fileName, Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.i("TAGGED", "--------- ERROR ------------");
                    }
                });

    }
}