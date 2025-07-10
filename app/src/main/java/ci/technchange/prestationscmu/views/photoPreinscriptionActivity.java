package ci.technchange.prestationscmu.views;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import ci.technchange.prestationscmu.R;
import ci.technchange.prestationscmu.models.Malade;

public class photoPreinscriptionActivity extends AppCompatActivity {

    private static final int REQUEST_IMAGE_CAPTURE_RECTO = 1;
    private static final int REQUEST_IMAGE_CAPTURE_VERSO = 2;
    private static final int REQUEST_IMAGE_CAPTURE_PATIENT = 3;
    private static final int REQUEST_CAMERA_PERMISSION = 100;

    private ImageView imageViewRecto, imageViewVerso, imageViewPatient;
    private CheckBox checkBoxNoPiece;
    private Button buttonContinuer, buttonRetour;

    private Bitmap rectoBitmap, versoBitmap, patientBitmap;
    private boolean hasNoPiece = false;

    private Malade malade;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_preinscription);


        malade = (Malade) getIntent().getSerializableExtra("malade");


        imageViewRecto = findViewById(R.id.imageViewRecto);
        imageViewVerso = findViewById(R.id.imageViewVerso);
        imageViewPatient = findViewById(R.id.imageViewPatient);
        checkBoxNoPiece = findViewById(R.id.checkBoxNoPiece);
        buttonContinuer = findViewById(R.id.buttonContinuer);
        buttonRetour = findViewById(R.id.retourHome);


        imageViewRecto.setOnClickListener(v -> dispatchTakePictureIntent(REQUEST_IMAGE_CAPTURE_RECTO));
        imageViewVerso.setOnClickListener(v -> dispatchTakePictureIntent(REQUEST_IMAGE_CAPTURE_VERSO));
        imageViewPatient.setOnClickListener(v -> dispatchTakePictureIntent(REQUEST_IMAGE_CAPTURE_PATIENT));


        checkBoxNoPiece.setOnCheckedChangeListener((buttonView, isChecked) -> {
            hasNoPiece = isChecked;
            updateContinuerButtonState();
        });


        buttonContinuer.setOnClickListener(v -> {
            if (hasNoPiece || rectoBitmap != null) {
                String rectoPath = saveImage(rectoBitmap, "recto.jpg");
                String versoPath = saveImage(versoBitmap, "verso.jpg");
                String patientPath = saveImage(patientBitmap, "patient.jpg");
                malade.setRectoPath(rectoPath);
                malade.setVersoPath(versoPath);
                malade.setPhotoPath(patientPath);


                Intent intent = new Intent(this, RecapPreinscriptionActivity.class);
                intent.putExtra("malade", malade);
                startActivity(intent);
            }
        });

        buttonRetour.setOnClickListener(v -> finish());
    }


    private void checkCameraPermissionAndTakePhoto(int requestCode) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
        } else {

            dispatchTakePictureIntent(requestCode);
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                dispatchTakePictureIntent(REQUEST_IMAGE_CAPTURE_RECTO);
            } else {

                Toast.makeText(this, "La permission de la caméra est nécessaire pour prendre des photos.", Toast.LENGTH_SHORT).show();
            }
        }
    }


    private void dispatchTakePictureIntent(int requestCode) {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, requestCode);
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null) {
            Bundle extras = data.getExtras();
            Bitmap imageBitmap = (Bitmap) extras.get("data");

            switch (requestCode) {
                case REQUEST_IMAGE_CAPTURE_RECTO:
                    rectoBitmap = imageBitmap;
                    imageViewRecto.setImageBitmap(rectoBitmap);
                    break;
                case REQUEST_IMAGE_CAPTURE_VERSO:
                    versoBitmap = imageBitmap;
                    imageViewVerso.setImageBitmap(versoBitmap);
                    break;
                case REQUEST_IMAGE_CAPTURE_PATIENT:
                    patientBitmap = imageBitmap;
                    imageViewPatient.setImageBitmap(patientBitmap);
                    break;
            }
            updateContinuerButtonState();
        }
    }

    private void updateContinuerButtonState() {
        buttonContinuer.setEnabled(hasNoPiece || rectoBitmap != null);
    }

    private String saveImage(Bitmap bitmap, String imageName) {
        if (bitmap == null) return "";

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String fileName = imageName.replace(".jpg", "_" + timeStamp + ".jpg");

        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File imageFile = new File(storageDir, fileName);

        try (FileOutputStream out = new FileOutputStream(imageFile)) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
            Toast.makeText(this, "Photo enregistrée à : " + imageFile.getAbsolutePath(), Toast.LENGTH_SHORT).show();
            return imageFile.getAbsolutePath();
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
    }
}