package ci.technchange.prestationscmu.views;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ci.technchange.prestationscmu.R;
import ci.technchange.prestationscmu.adapter.FseAdapter;
import ci.technchange.prestationscmu.models.FseAmbulatoire;
import ci.technchange.prestationscmu.models.FseItem;
import ci.technchange.prestationscmu.printing.generatePdfForFSE;
import ci.technchange.prestationscmu.utils.FseServiceDb;

public class ListeAllFse extends Fragment implements FseAdapter.OnItemClickListener{

    private RecyclerView recyclerView;
    private FseAdapter adapter;
    private List<FseItem> dataList;

    private FseServiceDb fseServiceDb;

    public ListeAllFse() {

        // Required empty public constructor
    }



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view =inflater.inflate(R.layout.fragment_liste_all_fse, container, false);
        // Initialisation de la RecyclerView
        recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        TextView textViewNoData = view.findViewById(R.id.textViewNoDataAll);

        fseServiceDb = FseServiceDb.getInstance(getContext());
        List<FseAmbulatoire> fseList = fseServiceDb.getAllFseAmbulatoire();


        // Initialisation de la liste de données
        dataList = new ArrayList<>();


        for (FseAmbulatoire fse : fseList) {

            String statusText = "";

            if (fse.iStatusProgres() == 0) {
                statusText = "Editée";
            } else if (fse.iStatusProgres() == 1) {
                statusText = "Finalisée";
            } else if (fse.iStatusProgres() == 2) {
                statusText = "En attente d'Entente préalable";
            } else if (fse.iStatusProgres() == -1) {
                statusText = "Entente préalable refusé";
            }
            else {
                statusText = "Statut inconnu"; // En cas de valeur non prévue
            }
            Log.d("status", "Status: " + fse.iStatusProgres() + ", NumSecurite: " + fse.getNumTrans());
            dataList.add(new FseItem(
                    fse.getNumTrans(),
                    fse.getNumSecu(),
                    fse.getNomComplet(),
                    statusText ,
                    fse.getType_fse(),
                    fse.getDateNaissance(),
                    fse.getNomEtablissement(),
                    fse.getId(),
                    fse.getDate_soins()
            ));
        }
        //dataList.add(new FseItem("12345", "987654321", "Jean Dupont", "Finalisée", "Chirurgie", "01/01/1990", "Hôpital A" , 1));
       // dataList.add(new FseItem("12346", "987654322", "Marie Dubois", "Editée", "Consultation", "15/05/1985", "Hôpital B" , 2));
       // dataList.add(new FseItem("12347", "987654323", "Paul Martin", "Finalisée", "Radiologie", "22/08/1979", "Clinique C", 3));

        if (dataList.isEmpty()) {
            textViewNoData.setVisibility(View.VISIBLE);  // Affiche "Aucune donnée"
            recyclerView.setVisibility(View.GONE);       // Cache le RecyclerView
        } else {
            textViewNoData.setVisibility(View.GONE);    // Cache "Aucune donnée"
            recyclerView.setVisibility(View.VISIBLE);   // Affiche la liste
        }


        // Création de l'adaptateur et l'assigner au RecyclerView
        adapter = new FseAdapter(dataList, this);
        recyclerView.setAdapter(adapter);
        return view ;
    }

    public void onPrintButtonClick(FseItem item) {
        Log.e("Impression", "il est rentre " );
        impression(item);
    }

    private void impression(FseItem item) {
        try {

            String numTrans = item.getTransactionNumber();
            String nomComplet = item.getFullName();
            String numSecu = item.getSecurityNumber();
            String dateNaiss = item.getDateNaissance();
            String etablissement = item.getEtablissement();
            String sexe = item.getSexe();
            String codeEts = item.getCodeEts();
            String dateSoins = item.getDateSoins();
            String numFsInitial = item.getNumFsInitial();
            Boolean etablissementCmr = item.isEtablissementCmr();
            Boolean etablissementAutre = item.isEtablismentAutre();
            Boolean etablissementRef = item.isEtablismentRef();
            Boolean etablissementEloignement = item.isEtablissmentEloignement();
            Boolean etablissementUrgent = item.isEtablissementUrgent();
            /*String etablissementUrgent = item.getEtablissementUrgent();
            String etablissementRef = item.getEtablissementRef();
            String etablissementEloignement = item.getEtablissementEloignement();**/


            Map<String, Object> data = new HashMap<>();
            data.put("dateSoins", dateSoins);
            data.put("OGD", "OGD Value");
            data.put("numFsInitiale", numFsInitial != null ? numFsInitial : "");
            data.put("numTransaction", numTrans);
            data.put("numEntentePrealable", "345678");
            data.put("numEntentePrealableAC", "901234");
            data.put("nomPrenomsAssure", nomComplet);
            data.put("numSecu",numSecu);
            data.put("dateNaissance", dateNaiss);
            data.put("genre", sexe);
            data.put("numAssureAC", "074852496241");
            data.put("codeAC", "AC123");
            data.put("nomAC", "Nom AC");
            data.put("codeEtablissement", codeEts);
            data.put("nomEtablissement", etablissement);
            data.put("typeFSE", "Type FSE");
                data.put("cocheCMR", etablissementCmr ? "Oui" : "Non");
            data.put("cocheUrgence", etablissementUrgent ? "Oui" : "Non");
            data.put("cocheEloignement", etablissementEloignement ? "Oui":"Non");
            data.put("cochereference", etablissementRef ? "Oui" : "Non");

            data.put("cocheAutre", etablissementAutre ? "Oui": "Non");
            data.put("precisionEtablissementAccueil", false);
            data.put("codeProfessionnelSante", "PS123");
            data.put("nomProfessionnelSante", "Dr. John Doe");
            data.put("specialiteProfessionnelSante", "Généraliste");
            data.put("infoComp_Maternite", "Info Maternité");
            data.put("infoComp_AVP", "Info AVP");
            data.put("infoComp_ATMP", "Info ATMP");
            data.put("infoComp_AUTRE", "Info Autre");
            data.put("infoComp_PROGSPECIAL", "Programme Spécial");
            data.put("infoComp_CODE", "Code Info");
            data.put("infoComp_IMMVEH", "Imm Véhicule");
            data.put("infoComp_Observation", "Observation");
            data.put("codeAffection1", "Aff1");
            data.put("codeAffection2", "Aff2");

            List<Map<String, Object>> prestations = new ArrayList<>();
            Map<String, Object> prestation1 = new HashMap<>();
            prestation1.put("codeActe", "Acte001");
            prestation1.put("designation", "Désignation 1");
            prestation1.put("dateDebut", "2025-02-01");
            prestation1.put("dateFin", "2025-02-05");
            prestation1.put("numDent", "12");
            prestation1.put("quantite", 1);
            prestation1.put("montant", 100.0);
            prestation1.put("partCmu", 50.0);
            prestation1.put("partAC", 30.0);
            prestation1.put("partAssure", 20.0);
            prestations.add(prestation1);

            data.put("prestations", prestations);
            Log.d("Impression", "Impression de la FSE : " + numTrans);
            Gson gson = new Gson();
            String jsonData = gson.toJson(data);
           // generatePdfForFSE fsp = new generatePdfForFSE(jsonData);
            Log.d("Impression", "Impression fin");
            Toast.makeText(getContext(), "Impression de la FSE : " + nomComplet, Toast.LENGTH_SHORT).show();


        } catch (Exception e) {
            Toast.makeText(getContext(), "Erreur lors de l'impression : " + e.getMessage(), Toast.LENGTH_LONG).show();
            Log.d("Impression", "Erreur : " + e.getMessage());
        }
    }
}