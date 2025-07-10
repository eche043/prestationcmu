package ci.technchange.prestationscmu.views;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import ci.technchange.prestationscmu.R;
import ci.technchange.prestationscmu.utils.ReferentielService;
import timber.log.Timber;

public class InscriptionActivity extends AppCompatActivity {
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int PERMISSION_CAMERA = 101;
    private static final int REQUEST_STORAGE_PERMISSION = 103;
    private static final int REQUEST_EMPREINTES = 2;

    private String photoPath;
    private String nomUtilisateur;
    private String prenomUtilisateur;
    private String numeroUtilisateur;
    private String centreSanteUtilisateur;
    private Bitmap photoUtilisateur;
    private boolean empreintesEnregistrees = false;

    private EditText editNom, editPrenom, editNumero;
    private AutoCompleteTextView autoCompleteCentreSante;
    private Button btnPrendrePhoto, btnEnregistrerEmpreinte, btnInscription;
    private ImageView imgUtilisateur;
    private LinearLayout layoutPhotoButton, layoutEmpreinteButton, layoutEmpreintesCapturees;

    private ReferentielService referentielService;
    private Uri photoUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inscription);

        referentielService = new ReferentielService(this);
        initializeViews();
        loadCentreSante();
        setupListeners();
    }

    private void initializeViews() {
        editNom = findViewById(R.id.editNom);
        editPrenom = findViewById(R.id.editPrenom);
        editNumero = findViewById(R.id.editNumeroTelephone);
        autoCompleteCentreSante = findViewById(R.id.autoCompleteCodeAffec1);
        btnPrendrePhoto = findViewById(R.id.btnPrendrePhoto);
        btnEnregistrerEmpreinte = findViewById(R.id.btnEnregistrerEmpreinte);
        btnInscription = findViewById(R.id.btnInscription);
        imgUtilisateur = findViewById(R.id.imagePhotoProfil);
        layoutPhotoButton = findViewById(R.id.layoutPhotoButton);
        layoutEmpreinteButton = findViewById(R.id.layoutEmpreinteButton);
        layoutEmpreintesCapturees = findViewById(R.id.layoutEmpreintesCapturees);

        btnPrendrePhoto.setEnabled(false);
        btnInscription.setEnabled(false);
    }

    private File createImageFile()  {
        System.out.println("Dans fonction createImageFile");
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "photo_user"+"_"+editNumero.getText().toString()+"_"+ timeStamp;
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        System.out.println("DOSSIER: "+storageDir.getAbsolutePath().toString());
        if (!storageDir.exists()) {
            storageDir.mkdirs();
        }
        if (storageDir == null) {
            Log.e("Camera", "Storage directory is null. Cannot create image file.");
            return null;
        }

        File image = null;
        try {
            //image = File.createTempFile(imageFileName, ".jpg", storageDir);
            System.out.println("AVANT new File");
            //image = new File(storageDir, imageFileName);
            image =  File.createTempFile(
                    imageFileName,  /* prefix */
                    ".jpg",         /* suffix */
                    storageDir      /* directory */
            );
            photoPath = image.getAbsolutePath();
        } catch (Exception e) {
            System.out.println("******************$");
            e.printStackTrace();
            System.out.println("******************$");
            //throw null;
        }

        return image;
    }

    private Uri getImageUri(File imageFile) {
        return FileProvider.getUriForFile(
                this,
                getPackageName() + ".fileprovider",
                imageFile
        );
    }

    private void loadCentreSante() {
        List<String> etablissements = referentielService.getAllEtablissements();
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                etablissements
        );
        autoCompleteCentreSante.setAdapter(adapter);
        autoCompleteCentreSante.setThreshold(1);
    }

    private void setupListeners() {
        autoCompleteCentreSante.setOnItemClickListener((parent, view, position, id) -> {
            centreSanteUtilisateur = (String) parent.getItemAtPosition(position);
            btnPrendrePhoto.setEnabled(true);
        });

        btnPrendrePhoto.setOnClickListener(v -> {
            if (checkCameraPermission()) {
                dispatchTakePictureIntent();
            }
        });

        btnEnregistrerEmpreinte.setOnClickListener(v -> {
            if (validatePersonalData()) {
                // Préparer les données à passer à l'activité des empreintes
                //Intent intent = new Intent(InscriptionActivity.this, RecupererEmpreinteMainActivity.class);
                Intent intent = new Intent(InscriptionActivity.this, EnrollmentActivity.class);

                // Passer les données via l'intent
                intent.putExtra("NOM", nomUtilisateur);
                intent.putExtra("PRENOM", prenomUtilisateur);
                intent.putExtra("TELEPHONE", numeroUtilisateur);
                intent.putExtra("CENTRE_SANTE", centreSanteUtilisateur);
                intent.putExtra("PHOTO_PATH", photoPath);

                startActivityForResult(intent, REQUEST_EMPREINTES);
            }
        });

        btnInscription.setOnClickListener(v -> {
            if (validateAndSubmitForm()) {
                enregistrerDonnees();
            }
        });
    }

    private void dispatchTakePictureIntent() {
        //if (checkStoragePermission()) {
            Toast.makeText(this, "Dans le code de dispatcher", Toast.LENGTH_SHORT).show();
            //Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            //if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                /*File photoFile = null;
                try {
                    photoFile = createImageFile();
                } catch (IOException ex) {
                    Toast.makeText(this, "Erreur création fichier", Toast.LENGTH_SHORT).show();
                }
                if (photoFile != null) {
                    Uri photoUri = getImageUri(photoFile);
                    takePictureIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
                    startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
                }*/
                File photoFile = createImageFile();
                if (photoFile != null) {
                    photoUri = FileProvider.getUriForFile(this, "ci.technchange.prestationscmuym.fileprovider", photoFile);
                    Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
                    startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
                }
            /*}else{
                System.out.println("Retour de fonction INTENT");
            }*/
        //}
    }

    private boolean checkStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        REQUEST_STORAGE_PERMISSION);
                return false;
            }
        }
        return true;
    }

    private boolean checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    PERMISSION_CAMERA);
            return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PERMISSION_CAMERA:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    dispatchTakePictureIntent();
                } else {
                    Toast.makeText(this, "Permission caméra refusée", Toast.LENGTH_SHORT).show();
                }
                break;
            case REQUEST_STORAGE_PERMISSION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    dispatchTakePictureIntent();
                }
                break;
        }
    }

    private boolean validatePersonalData() {
        nomUtilisateur = editNom.getText().toString().trim();
        prenomUtilisateur = editPrenom.getText().toString().trim();
        numeroUtilisateur = editNumero.getText().toString().trim();

        if (TextUtils.isEmpty(nomUtilisateur)) {
            editNom.setError("Le nom est requis");
            return false;
        }

        if (TextUtils.isEmpty(prenomUtilisateur)) {
            editPrenom.setError("Le prénom est requis");
            return false;
        }

        if (TextUtils.isEmpty(numeroUtilisateur)) {
            editNumero.setError("Le numéro de téléphone est requis");
            return false;
        }

        if (TextUtils.isEmpty(centreSanteUtilisateur)) {
            Toast.makeText(this, "Le centre de santé est requis", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (photoUtilisateur == null) {
            Toast.makeText(this, "La photo est requise", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bitmap bitmap = BitmapFactory.decodeFile(photoPath);
            if (bitmap != null) {
                // Mettre à jour l'image et la rendre visible
                imgUtilisateur.setImageBitmap(bitmap);
                imgUtilisateur.setVisibility(View.VISIBLE);

                // Masquer le layout du bouton caméra
                layoutPhotoButton.setVisibility(View.INVISIBLE);

                photoUtilisateur = bitmap;
                updateInscriptionButton();
                Toast.makeText(this, "Photo sauvegardée", Toast.LENGTH_SHORT).show();
            }
        }
        if (requestCode == REQUEST_EMPREINTES && resultCode == RESULT_OK) {
            if (data != null) {
                boolean empreintesValides = data.getBooleanExtra("EMPREINTES_VALIDES", false);
                if (empreintesValides) {
                    // Basculer vers la vue des checkboxes
                    layoutEmpreinteButton.setVisibility(View.GONE);
                    layoutEmpreintesCapturees.setVisibility(View.VISIBLE);

                    // Mettre à jour l'état des cases à cocher en fonction des empreintes sélectionnées
                    CheckBox checkboxIndexDroit = findViewById(R.id.checkboxIndexDroit);
                    CheckBox checkboxIndexGauche = findViewById(R.id.checkboxIndexGauche);
                    CheckBox checkboxPouceDroit = findViewById(R.id.checkboxPouceDroit);
                    CheckBox checkboxPouceGauche = findViewById(R.id.checkboxPouceGauche);

                    checkboxIndexDroit.setChecked(data.getBooleanExtra("INDEX_DROIT", false));
                    checkboxIndexGauche.setChecked(data.getBooleanExtra("INDEX_GAUCHE", false));
                    checkboxPouceDroit.setChecked(data.getBooleanExtra("POUCE_DROIT", false));
                    checkboxPouceGauche.setChecked(data.getBooleanExtra("POUCE_GAUCHE", false));

                    empreintesEnregistrees = true;
                    updateInscriptionButton();
                    Toast.makeText(this, "Empreintes enregistrées avec succès", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void updateInscriptionButton() {
        // Activer le bouton d'inscription uniquement si la photo ET les empreintes sont enregistrées
        btnInscription.setEnabled(photoUtilisateur != null && empreintesEnregistrees);
    }

    private boolean validateAndSubmitForm() {
        nomUtilisateur = editNom.getText().toString().trim();
        prenomUtilisateur = editPrenom.getText().toString().trim();
        numeroUtilisateur = editNumero.getText().toString().trim();

        if (TextUtils.isEmpty(nomUtilisateur)) {
            editNom.setError("Le nom est requis");
            return false;
        }

        if (TextUtils.isEmpty(prenomUtilisateur)) {
            editPrenom.setError("Le prénom est requis");
            return false;
        }

        if (TextUtils.isEmpty(numeroUtilisateur)) {
            editNumero.setError("Le numéro de téléphone est requis");
            return false;
        }

        if (TextUtils.isEmpty(centreSanteUtilisateur)) {
            Toast.makeText(this, "Le centre de santé est requis", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (photoUtilisateur == null) {
            Toast.makeText(this, "La photo est requise", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (!empreintesEnregistrees) {
            Toast.makeText(this, "Les empreintes sont requises", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private void enregistrerDonnees() {
        StringBuilder sb = new StringBuilder();
        sb.append("Inscription réussie!\n");
        sb.append("Nom: ").append(nomUtilisateur).append("\n");
        sb.append("Prénom: ").append(prenomUtilisateur).append("\n");
        sb.append("Numéro de téléphone: ").append(numeroUtilisateur).append("\n");
        sb.append("Centre de santé: ").append(centreSanteUtilisateur).append("\n");
        sb.append("Photo: ").append(photoUtilisateur != null ? "Oui" : "Non").append("\n");
        sb.append("Empreintes: ").append(empreintesEnregistrees ? "Oui" : "Non");

        new AlertDialog.Builder(this)
                .setTitle("Résumé d'inscription")
                .setMessage(sb.toString())
                .setPositiveButton("OK", null)
                .show();
    }
}