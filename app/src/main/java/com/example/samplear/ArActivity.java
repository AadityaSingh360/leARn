package com.example.samplear;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.assets.RenderableSource;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.TransformableNode;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.ByteBuffer;

public class ArActivity extends AppCompatActivity {
    ModelRenderable renderable;
    private String fileName = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ar);

        fileName = getIntent().getStringExtra("fileName");

        FirebaseStorage firebaseStorage=FirebaseStorage.getInstance();
        StorageReference modelref = firebaseStorage.getReference().child(fileName + ".glb");
        ArFragment arFragment= (ArFragment) getSupportFragmentManager()
                .findFragmentById(R.id.arFragment);


        Log.i("TAGGED", "***********************" + fileName);

        findViewById(R.id.modelButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(getApplicationContext(), "Downloading " + fileName + ".glb", Toast.LENGTH_SHORT).show();
                try {
                    assert fileName != null;
                    File file= File.createTempFile(fileName,"glb");
                    modelref.getFile(file).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                            buildModel(file);
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(getApplicationContext(), "No such model present", Toast.LENGTH_SHORT).show();
                }
            }
        });

        assert arFragment != null;
        arFragment.setOnTapArPlaneListener((hitResult, plane, motionEvent) -> {
            AnchorNode anchorNode=new AnchorNode(hitResult.createAnchor());
//            anchorNode.setRenderable(renderable);
            anchorNode.setParent(arFragment.getArSceneView().getScene());
//            arFragment.getArSceneView().getScene().addChild(anchorNode);

            TransformableNode node = new TransformableNode(arFragment.getTransformationSystem());
//            Maxscale must be greater than minscale
            node.getScaleController().setMaxScale(0.10f);
            node.getScaleController().setMinScale(0.01f);

            node.setParent(anchorNode);
            node.setRenderable(renderable);
            node.select();

        });

    }

    private static byte[] imageToByte(Image image){
        byte[] byteArray = null;
        byteArray = NV21toJPEG(YUV420toNV21(image),image.getWidth(),image.getHeight(),100);
        return byteArray;
    }

    private static byte[] NV21toJPEG(byte[] nv21, int width, int height, int quality) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        YuvImage yuv = new YuvImage(nv21, ImageFormat.NV21, width, height, null);

        yuv.compressToJpeg(new Rect(0, 0, width, height), quality, out);
        return out.toByteArray();
    }

    private static byte[] YUV420toNV21(Image image) {
        byte[] nv21;
        // Get the three planes.
        ByteBuffer yBuffer = image.getPlanes()[0].getBuffer();
        ByteBuffer uBuffer = image.getPlanes()[1].getBuffer();
        ByteBuffer vBuffer = image.getPlanes()[2].getBuffer();

        int ySize = yBuffer.remaining();
        int uSize = uBuffer.remaining();
        int vSize = vBuffer.remaining();


        nv21 = new byte[ySize + uSize + vSize];

        //U and V are swapped
        yBuffer.get(nv21, 0, ySize);
        vBuffer.get(nv21, ySize, vSize);
        uBuffer.get(nv21, ySize + vSize, uSize);

        return nv21;
    }

    private void buildModel(File file) {
        RenderableSource renderableSource=RenderableSource
                .builder()
                .setSource(this, Uri.parse(file.getPath()), RenderableSource.SourceType.GLB)
                .setRecenterMode(RenderableSource.RecenterMode.ROOT)
                .build();
        ModelRenderable
                .builder()
                .setSource(this,renderableSource)
                .setRegistryId(file.getPath())
                .build()
                .thenAccept(modelRenderable -> {
                    Toast.makeText(this,"Model Downloaded",Toast.LENGTH_SHORT).show();
                    renderable=modelRenderable;
                });
    }

    public void openCameraView(View view){
        Intent intent = new Intent(ArActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}