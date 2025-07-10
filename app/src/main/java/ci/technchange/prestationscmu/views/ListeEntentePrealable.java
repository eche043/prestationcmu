package ci.technchange.prestationscmu.views;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import ci.technchange.prestationscmu.R;
import ci.technchange.prestationscmu.adapter.FseAdapter;
import ci.technchange.prestationscmu.models.FseAmbulatoire;
import ci.technchange.prestationscmu.models.FseItem;
import ci.technchange.prestationscmu.utils.FseServiceDb;


public class ListeEntentePrealable extends Fragment implements FseAdapter.OnItemClickListener{

    private RecyclerView recyclerView;
    private FseAdapter adapter;
    private List<FseItem> dataList;

    private FseServiceDb fseServiceDb;
    public ListeEntentePrealable() {
        // Required empty public constructor
    }



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view =inflater.inflate(R.layout.fragment_liste_entente_prealable, container, false);
        // Initialisation de la RecyclerView
        recyclerView = view.findViewById(R.id.recyclerViewEP);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        TextView textViewNoData = view.findViewById(R.id.textViewNoDataEP);

        fseServiceDb = FseServiceDb.getInstance(getContext());
        List<FseAmbulatoire> fseList = fseServiceDb.getFseAmbulatoireByStatusProgres(2);


        // Initialisation de la liste de données
        dataList = new ArrayList<>();


        for (FseAmbulatoire fse : fseList) {


            dataList.add(new FseItem(
                    fse.getNumTrans(),
                    fse.getNumSecu(),
                    fse.getNomComplet(),
                    "En attente d'Entente préalable" ,
                    "HOSPITALISATION ET SOINS ",
                    fse.getDateNaissance(),
                    fse.getNomEtablissement(),
                    fse.getId(),
                    fse.getDate_soins()
            ));
        }
       // dataList.add(new FseItem("12345", "987654321", "Jean Dupont", "En attente d'Entente préalable", "Chirurgie", "01/01/1990", "Hôpital A" , 1));
       // dataList.add(new FseItem("12346", "987654322", "Marie Dubois", "En attente d'Entente préalable", "Consultation", "15/05/1985", "Hôpital B" , 2));
       // dataList.add(new FseItem("12347", "987654323", "Paul Martin", "En attente d'Entente préalable", "Radiologie", "22/08/1979", "Clinique C", 3));

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
        // on met rien ici enfin je pense
    }
}