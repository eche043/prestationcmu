package ci.technchange.prestationscmu.utils;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

import ci.technchange.prestationscmu.models.ActeMedical;

// Créez une classe pour l'adaptateur personnalisé qui affiche seulement le code de l'acte
public class CodeActeAdapter extends ArrayAdapter<ActeMedical> {

    public CodeActeAdapter(Context context, List<ActeMedical> actes) {
        super(context, android.R.layout.simple_spinner_item, actes);
        setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = super.getView(position, convertView, parent);
        TextView textView = (TextView) view.findViewById(android.R.id.text1);

        // Afficher uniquement le code de l'acte
        ActeMedical acte = getItem(position);
        if (acte != null) {
            textView.setText(acte.getCode());
        }

        return view;
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        View view = super.getDropDownView(position, convertView, parent);
        TextView textView = (TextView) view.findViewById(android.R.id.text1);

        // Afficher uniquement le code de l'acte dans la liste déroulante
        ActeMedical acte = getItem(position);
        if (acte != null) {
            textView.setText(acte.getCode());
        }

        return view;
    }
}
