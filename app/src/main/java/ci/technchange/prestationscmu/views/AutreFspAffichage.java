package ci.technchange.prestationscmu.views;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import ci.technchange.prestationscmu.R;
import ci.technchange.prestationscmu.adapter.AutreFseAdapter;
import ci.technchange.prestationscmu.models.AutreFse;
import ci.technchange.prestationscmu.utils.AutreFspServiceDb;

public class AutreFspAffichage extends Fragment {

    private RecyclerView recyclerView;
    private AutreFseAdapter adapter;
    private TextView textViewNoData;
    private AutreFspServiceDb autreFspServiceDb;

    public AutreFspAffichage() {

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_liste_entente_prealable, container, false);


        recyclerView = view.findViewById(R.id.recyclerViewEP);
        textViewNoData = view.findViewById(R.id.textViewNoDataEP);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));


        autreFspServiceDb = AutreFspServiceDb.getInstance(getContext());


        loadAutreFspData();

        return view;
    }

    private void loadAutreFspData() {
        List<AutreFse> autreFseList = autreFspServiceDb.getAllAutreFsp();

        if (autreFseList.isEmpty()) {
            textViewNoData.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            textViewNoData.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);


            adapter = new AutreFseAdapter(autreFseList, getContext());
            recyclerView.setAdapter(adapter);
        }
    }
}