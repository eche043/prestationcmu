package ci.technchange.prestationscmu.views;

import android.Manifest;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import ci.technchange.prestationscmu.R;
import ci.technchange.prestationscmu.receivers.SyncNotificationReceiver;
import ci.technchange.prestationscmu.utils.UploadQueueManager;

public class ScannerFseActivity extends AppCompatActivity {


    private static final int CAMERA_PERMISSION_REQUEST_CODE = 100;
    private FloatingActionButton fab;

    private SyncNotificationReceiver syncReceiver;
    private View rootView;  // La vue principale de votre activité

    String numTrans, numSecu, guid, affection1, affection2, acte1, acte2, acte3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scanner_fse);

        // Initialiser la vue racine
        rootView = findViewById(android.R.id.content);

        // Récupérer l'intent
        Intent intent = getIntent();

        // Récupérer les données
        numTrans = intent.getStringExtra("NUM_TRANS");


        // Configurer la Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.app_title);
        }

        // Utiliser le FAB au lieu du bouton standard
        fab = findViewById(R.id.fab);
        fab.setImageResource(R.drawable.ic_camera); // Vous devrez créer cette icône
        fab.setOnClickListener(v -> checkPermissionsAndStartCamera());
    }

    private void checkPermissionsAndStartCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
        } else {
            startCameraActivity();
        }
    }

    private void startCameraActivity() {
        Log.d("TAG*******#", "startCameraActivity: ");
        System.out.println(numTrans);
        Intent intent = new Intent(this, CameraActivity.class);
        intent.putExtra("NUM_TRANS", numTrans);
        startActivity(intent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCameraActivity();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Enregistrer le récepteur pour les notifications de synchronisation
        syncReceiver = new SyncNotificationReceiver(rootView);
        registerReceiver(syncReceiver, new IntentFilter("com.example.scancmu.SYNC_STARTED"));

        // Vérifier s'il y a des uploads en attente et démarrer la synchronisation
        if (!UploadQueueManager.getInstance(this).isQueueEmpty()) {
            UploadQueueManager.getInstance(this).startRetryScan();
        }
    }
    @Override
    protected void onPause() {
        super.onPause();

        // Désenregistrer le récepteur
        if (syncReceiver != null) {
            unregisterReceiver(syncReceiver);
            syncReceiver = null;
        }
    }
}