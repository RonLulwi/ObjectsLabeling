package com.example.objectslabeling;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Camera;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.label.ImageLabel;
import com.google.mlkit.vision.label.ImageLabeler;
import com.google.mlkit.vision.label.ImageLabeling;
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private AppCompatTextView main_LBL_target;
    private AppCompatImageView main_IMG_image;
    private AppCompatImageButton main_BTN_camera, main_BTN_validate;
    private String[] questions;
    private int step =0;
    private ArrayList<String> labelsList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().hide();
        setContentView(R.layout.activity_main);

        findViews();
        initViews();
    }
    private void findViews() {
        main_LBL_target = findViewById(R.id.main_LBL_target);
        main_IMG_image = findViewById(R.id. main_IMG_image);
        main_BTN_camera = findViewById(R.id. main_BTN_camera);
        main_BTN_validate = findViewById(R.id. main_BTN_validate);
    }
    private void initViews() {
        questions = new String[]{"Dog", "Helmet", "Glasses"};
        main_LBL_target.setText("Take a picture of: " + questions[step]);
        main_BTN_camera.setOnClickListener(view -> requestPermissionLauncher.launch(Manifest.permission.CAMERA));
        main_BTN_validate.setOnClickListener(view -> {
            try {
                validateImage(view);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private void validateImage(View view) throws IOException {
        if(main_IMG_image.getDrawable() == null){
            Toast.makeText(MainActivity.this, "You must upload an image first", Toast.LENGTH_SHORT).show();
            return;
        }
        Bitmap bitmapImage = ((BitmapDrawable)main_IMG_image.getDrawable()).getBitmap();
        // Validate image process
        ImageLabelerOptions options = new ImageLabelerOptions.Builder().setConfidenceThreshold(0.9f).build();
        ImageLabeler imageLabeler = ImageLabeling.getClient(options);
        InputImage inputImage = InputImage.fromBitmap(bitmapImage, 0);
        labelsList = new ArrayList<>();
        imageLabeler.process(inputImage).addOnSuccessListener(new OnSuccessListener<List<ImageLabel>>() {
            @Override
            public void onSuccess(List<ImageLabel> imageLabels) {
                for (ImageLabel imageLabel: imageLabels)
                    labelsList.add(imageLabel.getText());
                if(labelsList.contains(questions[step])) {
                    Toast.makeText(MainActivity.this, "Success!", Toast.LENGTH_SHORT).show();
                    step++;
                    main_LBL_target.setText("Take a picture of: " + questions[step]);

                }else
                    Toast.makeText(MainActivity.this, "Try Again!", Toast.LENGTH_SHORT).show();
                Log.d("pttt", labelsList.toString());
            }
        });
    }

    private void uploadImage(){
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        mTakePhoto.launch(intent);
    }

    private ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    uploadImage();
                } else {
                    permissionDenied();
                }
            });

    private ActivityResultLauncher<Intent> mTakePhoto = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if(result.getResultCode() == RESULT_OK && result.getData() != null){
                    Bundle bundle = result.getData().getExtras();
                    Bitmap bitmap = (Bitmap) bundle.get("data");
                    main_IMG_image.setImageBitmap(bitmap);
                }
            });

    private void permissionDenied() {
        if(ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA))
            showCustomDialog("Permission required", "CAMERA permission is required for uploading images.", "Grant Permission", (dialog, which) -> requestPermissionLauncher.launch(Manifest.permission.CAMERA), "decline permission", null);
        else
            showCustomDialog("Permission required", "It seems you permanently declined CAMERA permission.\npress \"Grant Permission\" to go to the App settings and grant it manually.", "Grant Permission", (dialog, which) -> {
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", getPackageName(), null));
                startActivity(intent);
            }, "Not now", null);
    }

    private void showCustomDialog(String title, String massage,
                             String positiveBtnTitle, DialogInterface.OnClickListener positiveListener,
                             String negativeBtnTitle, DialogInterface.OnClickListener negativeListener){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title).
                setMessage(massage).
                setPositiveButton(positiveBtnTitle, positiveListener).
                setNegativeButton(negativeBtnTitle, negativeListener);
        builder.create().show();

    }
}