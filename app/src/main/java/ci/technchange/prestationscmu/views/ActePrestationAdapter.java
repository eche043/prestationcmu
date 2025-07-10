package ci.technchange.prestationscmu.views;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

import java.util.List;

import ci.technchange.prestationscmu.R;
import ci.technchange.prestationscmu.models.ActeMedical;

public class ActePrestationAdapter extends BaseAdapter {
    private Context context;
    private List<ActeMedical> prestationList;

    public ActePrestationAdapter(Context context, List<ActeMedical> prestationList) {
        this.context = context;
        this.prestationList = prestationList;
    }

    @Override
    public int getCount() {
        return prestationList.size();
    }

    @Override
    public Object getItem(int position) {
        return prestationList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(context);
            convertView = inflater.inflate(R.layout.liste_acte_prestation, parent, false);
        }

        ActeMedical prestation = prestationList.get(position);

        // Associer les TextView aux données
        TextView titreDesignation = convertView.findViewById(R.id.titreDesignation);
        TextView infoCodeActe = convertView.findViewById(R.id.infoCodeActe);
        TextView infoMontant = convertView.findViewById(R.id.infoMontant);
        TextView infoPartCMU = convertView.findViewById(R.id.infoPartCMU);
        TextView infoAssure = convertView.findViewById(R.id.infoAssure);

        titreDesignation.setText(prestation.getTitre());
        infoCodeActe.setText(prestation.getCode());
        infoMontant.setText(String.valueOf(prestation.getTarif()));
        infoPartCMU.setText(String.valueOf(prestation.getTarif()*70/100));
        infoAssure.setText(String.valueOf(prestation.getTarif()*30/100));

        // Gestion du bouton de suppression
        ImageButton deleteButton = convertView.findViewById(R.id.deleteButton);
        deleteButton.setOnClickListener(v -> {
            prestationList.remove(position);
            notifyDataSetChanged();
        });

        return convertView;
    }
}

