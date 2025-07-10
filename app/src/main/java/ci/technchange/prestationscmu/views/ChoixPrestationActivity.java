package ci.technchange.prestationscmu.views;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import ci.technchange.prestationscmu.R;
import ci.technchange.prestationscmu.models.FseItem;
import ci.technchange.prestationscmu.models.Patient;

public class ChoixPrestationActivity extends AppCompatActivity {

    Button fseSoinsAmb, fseDentaire, fseBioImagerie, fseHospitalisation;
    private Patient currentPatient;
    private  String numFseInitial;
    private FseItem item;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choix_prestation);

        fseSoinsAmb = findViewById(R.id.soinAmbulatoires);
        fseDentaire = findViewById(R.id.protheseDentaire);
        fseBioImagerie = findViewById(R.id.bioImagerie);
        fseHospitalisation = findViewById(R.id.hospitalisation);
        currentPatient = (Patient) getIntent().getSerializableExtra("PATIENT");
        numFseInitial = getIntent().getStringExtra("num_trans");
        item = getIntent().getParcelableExtra("PATIENT_OLD");

        fseSoinsAmb.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ChoixPrestationActivity.this, FseEditActivity.class);
                intent.putExtra("PATIENT", currentPatient);
                intent.putExtra("TYPE_FSE", "AMB");
                intent.putExtra("num_trans",numFseInitial);
                intent.putExtra("PATIENT_OLD", item);
                startActivity(intent);
            }
        });
        fseDentaire.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ChoixPrestationActivity.this, FseEditActivity.class);
                intent.putExtra("PATIENT", currentPatient);
                intent.putExtra("TYPE_FSE", "dentaire");
                intent.putExtra("num_trans",numFseInitial);
                intent.putExtra("PATIENT_OLD", item);
                startActivity(intent);
            }
        });
        fseBioImagerie.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ChoixPrestationActivity.this, FseEditActivity.class);
                intent.putExtra("PATIENT", currentPatient);
                intent.putExtra("TYPE_FSE", "biologie");
                intent.putExtra("num_trans",numFseInitial);
                intent.putExtra("PATIENT_OLD", item);
                startActivity(intent);
            }
        });
        fseHospitalisation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ChoixPrestationActivity.this, FseHospitalisationActivity.class);
                intent.putExtra("PATIENT", currentPatient);
                intent.putExtra("TYPE_FSE", "hospitalisation");
                intent.putExtra("num_trans",numFseInitial);
                intent.putExtra("PATIENT_OLD", item);
                startActivity(intent);
            }
        });
    }
}