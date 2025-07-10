package ci.technchange.prestationscmu.adapter;

import android.content.Context;
import android.widget.ArrayAdapter;
import android.widget.Filter;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;

public class EtablissementAdapter extends ArrayAdapter<String> {

    private List<String> allItems;
    private List<String> normalizedItems; // Version normalisée (sans accents) des items

    public EtablissementAdapter(Context context, int resource, List<String> items) {
        super(context, resource, new ArrayList<>(items));
        this.allItems = new ArrayList<>(items);
        this.normalizedItems = new ArrayList<>();

        // Pré-normaliser tous les items pour une recherche plus rapide
        for (String item : items) {
            normalizedItems.add(normalizeText(item));
        }
    }

    /**
     * Normalise le texte en supprimant les accents et en mettant en minuscules
     */
    private String normalizeText(String text) {
        String normalized = Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return normalized.toLowerCase();
    }

    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                FilterResults results = new FilterResults();

                if (constraint == null || constraint.length() == 0) {
                    // Aucune contrainte, retourne tous les éléments
                    results.values = allItems;
                    results.count = allItems.size();
                } else {
                    List<String> filteredItems = new ArrayList<>();

                    // Normaliser la contrainte pour la recherche
                    String normalizedConstraint = normalizeText(constraint.toString());

                    // Diviser la recherche en mots pour chercher chaque mot séparément
                    String[] searchWords = normalizedConstraint.split("\\s+");

                    // Pour chaque élément dans la liste complète
                    for (int i = 0; i < normalizedItems.size(); i++) {
                        String normalizedItem = normalizedItems.get(i);
                        boolean matchesAllWords = true;

                        // Vérifier que chaque mot est présent dans l'élément
                        for (String word : searchWords) {
                            if (!normalizedItem.contains(word)) {
                                matchesAllWords = false;
                                break;
                            }
                        }

                        // Si tous les mots sont présents, ajouter à la liste des résultats
                        if (matchesAllWords) {
                            filteredItems.add(allItems.get(i));
                        }
                    }

                    results.values = filteredItems;
                    results.count = filteredItems.size();
                }

                return results;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                clear();
                if (results.count > 0) {
                    addAll((List<String>) results.values);
                }
                notifyDataSetChanged();
            }
        };
    }
}
