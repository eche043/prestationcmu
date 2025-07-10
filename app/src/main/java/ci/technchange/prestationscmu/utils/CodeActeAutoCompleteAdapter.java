// Assurez-vous que votre classe CodeActeAutoCompleteAdapter est correctement implémentée
// et qu'elle est dans le bon package (ci.technchange.prestationscmu.utils)

package ci.technchange.prestationscmu.utils;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import ci.technchange.prestationscmu.models.ActeMedical;

public class CodeActeAutoCompleteAdapter extends ArrayAdapter<ActeMedical> {
    private List<ActeMedical> actesList;
    private List<ActeMedical> allActes;

    public CodeActeAutoCompleteAdapter(Context context, List<ActeMedical> actes) {
        super(context, android.R.layout.simple_dropdown_item_1line, actes);
        this.allActes = new ArrayList<>(actes);
        this.actesList = new ArrayList<>(actes);
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

        // Afficher le code suivi du libellé dans la liste déroulante pour plus de clarté
        ActeMedical acte = getItem(position);
        if (acte != null) {
            textView.setText(acte.getCode() + " - " + acte.getLibelle());
        }

        return view;
    }

    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                FilterResults results = new FilterResults();

                // Si aucune contrainte, retourner tous les éléments
                if (constraint == null || constraint.length() == 0) {
                    results.values = allActes;
                    results.count = allActes.size();
                } else {
                    List<ActeMedical> filteredItems = new ArrayList<>();
                    String filterPattern = constraint.toString().toLowerCase().trim();

                    // Filtrer les actes dont le code ou le libellé contient la contrainte
                    for (ActeMedical acte : allActes) {
                        if (acte.getCode().toLowerCase().contains(filterPattern) ||
                                acte.getLibelle().toLowerCase().contains(filterPattern)) {
                            filteredItems.add(acte);
                        }
                    }

                    results.values = filteredItems;
                    results.count = filteredItems.size();
                }

                return results;
            }

            @SuppressWarnings("unchecked")
            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                actesList.clear();
                if (results.values != null) {
                    actesList.addAll((List<ActeMedical>) results.values);
                }
                notifyDataSetChanged();

                clear();
                if (results.values != null) {
                    addAll(actesList);
                }
                notifyDataSetInvalidated();
            }
        };
    }
}