package com.example.vipayee.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.vipayee.AppConstants;
import com.example.vipayee.R;
import com.example.vipayee.utils.BaseActivity;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import java.util.HashMap;
import java.util.Map;

public class GenerateQrActivity extends BaseActivity {

    private ImageView qrImage;
    private TextView tvUpiId;
    private Bitmap finalQrBitmap;
    String UpiName, upiId, upiUri ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_generate_qr);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        qrImage = findViewById(R.id.ivQr);
        tvUpiId = findViewById(R.id.tvUpi);

        upiId = AppConstants.UPI_ID;
        UpiName = AppConstants.UPI_NAME;
        upiUri = AppConstants.UPI_URI;

        tvUpiId.setText("UPI ID: " + upiId);

        generateQrWithLogo(upiUri);
    }

    private void generateQrWithLogo(String data) {
        try {
            int size = 600;

            // 🔹 High error correction (IMPORTANT for logo)
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);

            BitMatrix matrix = new MultiFormatWriter()
                    .encode(data, BarcodeFormat.QR_CODE, size, size, hints);

            Bitmap qrBitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);

            for (int x = 0; x < size; x++) {
                for (int y = 0; y < size; y++) {
                    qrBitmap.setPixel(x, y,
                            matrix.get(x, y) ? 0xFF000000 : 0xFFFFFFFF);
                }
            }

            // 🔹 Load Logo
            Bitmap logo = BitmapFactory.decodeResource(getResources(), R.drawable.bgmain);

            // 🔹 Scale logo (20% of QR size)
            int logoSize = size / 5;
            Bitmap scaledLogo = Bitmap.createScaledBitmap(
                    logo, logoSize, logoSize, false);

            // 🔹 Combine QR + Logo
            finalQrBitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(finalQrBitmap);
            canvas.drawBitmap(qrBitmap, 0, 0, null);

            int left = (size - logoSize) / 2;
            int top = (size - logoSize) / 2;

            canvas.drawBitmap(scaledLogo, left, top, new Paint(Paint.FILTER_BITMAP_FLAG));

            qrImage.setImageBitmap(finalQrBitmap);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 🔹 Menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_share_qr, menu);
        return true;
    }

    // 🔹 Share QR
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_share) {
            shareQr();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void shareQr() {
        try {
            String path = MediaStore.Images.Media.insertImage(
                    getContentResolver(),
                    finalQrBitmap,
                    "UPI_QR",
                    "ViPayee UPI QR"
            );

            Uri uri = Uri.parse(path);

            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("image/*");
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            intent.putExtra(Intent.EXTRA_TEXT,
                    "Scan this QR to pay to " + upiId);

            startActivity(Intent.createChooser(intent, "Share QR via"));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}














//package com.example.vipayee.activities;
//
//import android.graphics.Bitmap;
//import android.os.Bundle;
//import android.widget.ImageView;
//import android.widget.TextView;
//
//import androidx.appcompat.app.AppCompatActivity;
//
//import com.example.vipayee.AppConstants;
//import com.example.vipayee.R;
//import com.google.zxing.BarcodeFormat;
//import com.google.zxing.MultiFormatWriter;
//import com.google.zxing.common.BitMatrix;
//
//public class GenerateQrActivity extends AppCompatActivity {
//
//    private ImageView qrImage;
//    private TextView tvUpiId;
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_generate_qr);
//
//        qrImage = findViewById(R.id.ivQr);
//        tvUpiId = findViewById(R.id.tvUpi);
//
//        // 🔹 Sample UPI Data (can be dynamic)
//        String upiId = AppConstants.UPI_ID;
//        String name = AppConstants.UPI_NAME;
//
//        String upiUri = AppConstants.UPI_URI;
//
//        tvUpiId.setText("UPI ID: " + upiId);
//
//        generateQrCode(upiUri);
//    }
//
//    private void generateQrCode(String data) {
//        try {
//            BitMatrix matrix = new MultiFormatWriter()
//                    .encode(data, BarcodeFormat.QR_CODE, 600, 600);
//
//            Bitmap bitmap = Bitmap.createBitmap(600, 600, Bitmap.Config.RGB_565);
//
//            for (int x = 0; x < 600; x++) {
//                for (int y = 0; y < 600; y++) {
//                    bitmap.setPixel(x, y,
//                            matrix.get(x, y) ? 0xFF000000 : 0xFFFFFFFF);
//                }
//            }
//
//            qrImage.setImageBitmap(bitmap);
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//}
