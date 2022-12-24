package com.example.qrcodev3;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraControl;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.ScaleGestureDetector;
import android.view.Surface;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;

import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity {

    private PreviewView previewView;
    private Button button;
    private LinearLayout listView;

    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private ImageCapture imageCapture;
    private BarcodeScanner barcodeScanner;

    private final ArrayList<TextView> results = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        previewView = findViewById(R.id.previewView);
        previewView.setImplementationMode(PreviewView.ImplementationMode.COMPATIBLE);
        previewView.setScaleType(PreviewView.ScaleType.FIT_CENTER);

        cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindPreview(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                // No errors need to be handled for this Future.
                // This should never be reached.
            }
        }, ContextCompat.getMainExecutor(this));

        BarcodeScannerOptions barcodeScannerOptions =
                new BarcodeScannerOptions.Builder()
                        .setBarcodeFormats(
                                Barcode.FORMAT_ALL_FORMATS)
                        .build();

        barcodeScanner = BarcodeScanning.getClient(barcodeScannerOptions);

        button = findViewById(R.id.button);
        listView = findViewById(R.id.listView);
        button.setOnClickListener(view -> {
            previewView.setVisibility(View.VISIBLE);
            listView.setVisibility(View.GONE);
            listView.removeAllViews();
            button.setVisibility(View.GONE);
            launchThread();
        });
    }

    void launchThread() {
        Bitmap bitmapPreview = previewView.getBitmap();

        new Handler(getMainLooper()).post(() -> {
            Log.w("SUCCESS", "CAPTURE");
            if(bitmapPreview == null) {
                launchThread();
                return;
            }
            InputImage inputImage = InputImage.fromBitmap(bitmapPreview, 0);
            barcodeScanner.process(inputImage)
                    .addOnSuccessListener(barcodes -> {
                        if (barcodes.size() != 0) {
                            previewView.setVisibility(View.GONE);
                            button.setVisibility(View.VISIBLE);
                            listView.setVisibility(View.VISIBLE);

                            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                            listView.setBackgroundColor(Color.BLACK);
                            params.setMargins(10,10,10,10);
                            for (Barcode barcode : barcodes) {
                                TextView textView = new TextView(getApplicationContext());
                                textView.setTextSize(30);
                                textView.setTextColor(Color.WHITE);
                                textView.setText(barcode.getDisplayValue());
                                textView.setLayoutParams(params);
                                results.add(textView);
                            }
                            for (int i = 0; i < results.size(); i++) {
                                listView.addView(results.get(results.size() - (i + 1)));
                            }
                        } else {
                            launchThread();
                        }
                    }).addOnFailureListener(error -> {
                        Log.e("Error", error.getMessage());
                        launchThread();
                    });
        });
    }

    @SuppressLint("ClickableViewAccessibility")
    void bindPreview(@NonNull ProcessCameraProvider cameraProvider) {
        Preview preview = new Preview.Builder()
                .build();

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();

        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        imageCapture =
            new ImageCapture.Builder()
                    .setTargetRotation(Surface.ROTATION_0)
                    .build();
        Camera camera = cameraProvider.bindToLifecycle((LifecycleOwner)this, cameraSelector, imageCapture,  preview);
        CameraControl cameraControl = camera.getCameraControl();
        cameraControl.setLinearZoom((float)0.3);

        launchThread();
    }
}