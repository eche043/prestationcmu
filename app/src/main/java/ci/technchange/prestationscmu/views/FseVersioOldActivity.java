package ci.technchange.prestationscmu.views;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;


import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import ci.technchange.prestationscmu.R;
import ci.technchange.prestationscmu.adapter.FseAdapterOld;
import ci.technchange.prestationscmu.models.FseAmbulatoire;
import ci.technchange.prestationscmu.models.FseItem;
import ci.technchange.prestationscmu.models.Patient;
import ci.technchange.prestationscmu.utils.FseServiceDb;

public class FseVersioOldActivity extends AppCompatActivity {
    private Patient currentPatient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fse_versio_old);

        Intent intent = getIntent();
        String nomComplet = intent.getStringExtra("nom_complet");
        Log.d("nom_complet", nomComplet);
        currentPatient = (Patient) getIntent().getSerializableExtra("PATIENT");

        FseServiceDb fseServiceDb = FseServiceDb.getInstance(this);
        List<FseAmbulatoire> fseAmbulatoire = fseServiceDb.getFseAmbulatoireByNomComplet(nomComplet);

        RecyclerView recyclerView = findViewById(R.id.recyclerViewFseOld);

        TextView textInfo = findViewById(R.id.textFseOld);
        Button btnNouvelleFse = findViewById(R.id.btnNouvelleFseOld);

        btnNouvelleFse.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                launchFseEditActivity();
            }
        });


        if (fseAmbulatoire == null || fseAmbulatoire.isEmpty()) {
            Log.d("Info", "vide ");
            launchFseEditActivity();

        } else {
            Log.d("Info", "Données trouvées pour nom_complet : " + nomComplet);

            textInfo.setVisibility(View.GONE);

            List<FseItem> fseList = new ArrayList<>();
            for (FseAmbulatoire ambulatoire : fseAmbulatoire) {
                FseItem fseItem = new FseItem();
                fseItem.setTransactionNumber(ambulatoire.getNumTrans());
                fseItem.setFullName(ambulatoire.getNomComplet());
                fseItem.setSecurityNumber(ambulatoire.getNumSecu());
                fseItem.setDateNaissance(ambulatoire.getDateNaissance());
                fseItem.setEtablissement(ambulatoire.getNomEtablissement());
                fseItem.setSexe(ambulatoire.getSexe());
                fseItem.setEtablissementCmr(ambulatoire.isEtablissementCmr());
                fseItem.setGuid(ambulatoire.getNumGuid());
                //fseItem.setStatus(ambulatoire.iStatusProgres());
                fseList.add(fseItem);
            }


            FseAdapterOld adapter = new FseAdapterOld(fseList, this);
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
            recyclerView.setAdapter(adapter);
        }
    }

    private void launchFseEditActivity() {
        if (currentPatient != null) {
            Intent intent = new Intent(FseVersioOldActivity.this, ChoixPrestationActivity.class);
            intent.putExtra("PATIENT", currentPatient);
            startActivity(intent);
            finish();
        } else {
            Toast.makeText(FseVersioOldActivity.this, "Aucun patient sélectionné", Toast.LENGTH_SHORT).show();
        }
    }
}