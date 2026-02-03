package com.example.vipayee.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import android.media.Image;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.*;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.vipayee.R;
import com.example.vipayee.utils.SessionManager;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.*;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ScanQrActivity extends AppCompatActivity {

    private static final String TAG = "MLKIT_QR";
    private static final int CAMERA_REQUEST = 1001;

    private PreviewView previewView;
    private boolean scannedOnce = false;

    private ExecutorService cameraExecutor;
    private BarcodeScanner barcodeScanner;

    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan_qr);

        previewView = findViewById(R.id.previewView);
        sessionManager = new SessionManager(this);

        cameraExecutor = Executors.newSingleThreadExecutor();

        BarcodeScannerOptions options =
                new BarcodeScannerOptions.Builder()
                        .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                        .build();

        barcodeScanner = BarcodeScanning.getClient(options);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.CAMERA},
                    CAMERA_REQUEST
            );
        }
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                ImageAnalysis analysis =
                        new ImageAnalysis.Builder()
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build();

                analysis.setAnalyzer(cameraExecutor, imageProxy -> {
                    if (scannedOnce) {
                        imageProxy.close();
                        return;
                    }

                    @SuppressWarnings("UnsafeOptInUsageError")
                    Image mediaImage = imageProxy.getImage();

                    if (mediaImage != null) {
                        InputImage image =
                                InputImage.fromMediaImage(
                                        mediaImage,
                                        imageProxy.getImageInfo().getRotationDegrees()
                                );

                        barcodeScanner.process(image)
                                .addOnSuccessListener(this::handleResult)
                                .addOnCompleteListener(task -> imageProxy.close());
                    } else {
                        imageProxy.close();
                    }
                });

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(
                        this,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        analysis
                );

            } catch (Exception e) {
                Log.e(TAG, "Camera start failed", e);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void handleResult(List<Barcode> barcodes) {
        if (barcodes.isEmpty()) return;

        Barcode barcode = barcodes.get(0);
        String rawText = barcode.getRawValue();
        if (rawText == null) return;

        scannedOnce = true;
        Log.d(TAG, "QR RAW = " + rawText);

        Uri uri = Uri.parse(rawText);
        String pa = uri.getQueryParameter("pa");
        String pn = uri.getQueryParameter("pn");

        if (pa != null && !pa.isEmpty()) {

            // ✅ STORE UPI DETAILS FOR FUTURE USE
            sessionManager.saveUpiDetails(pa, pn != null ? pn : "");

            Intent intent = new Intent(this, EnterAmountActivity.class);
            intent.putExtra("upi_uri", rawText);
            startActivity(intent);
            finish();

        } else {
            Toast.makeText(this, "Not a valid UPI QR", Toast.LENGTH_LONG).show();
            scannedOnce = false;
        }
    }

    @Override
    protected void onDestroy() {
        cameraExecutor.shutdown();
        barcodeScanner.close();
        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults
    ) {
        if (requestCode == CAMERA_REQUEST &&
                grantResults.length > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            Toast.makeText(this,
                    "Camera permission required",
                    Toast.LENGTH_LONG).show();
            finish();
        }
    }
}
