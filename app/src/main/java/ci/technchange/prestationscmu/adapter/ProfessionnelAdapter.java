package ci.technchange.prestationscmu.adapter;

import static androidx.camera.core.impl.ExtendedCameraConfigProviderStore.clear;

import static java.util.Collections.addAll;

import android.content.Context;
import android.widget.ArrayAdapter;
import android.widget.Filter;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;

import ci.technchange.prestationscmu.models.Professionel;

public class ProfessionnelAdapter extends ArrayAdapter<Professionel> {

    private List<Professionel> allItems;
    private List<String> normalizedNames; // Version normalisée (sans accents) des noms

    public ProfessionnelAdapter(Context context, int resource, List<Professionel> items) {
        super(context, resource, new ArrayList<>(items));
        this.allItems = new ArrayList<>(items);
        this.normalizedNames = new ArrayList<>();

        // Pré-normaliser tous les noms pour une recherche plus rapide
        for (Professionel professionnel : items) {
            // Nous utilisons toString() qui devrait retourner le nom complet
            // Si ce n'est pas le cas, remplacez par la méthode appropriée (getNom() par exemple)
            normalizedNames.add(normalizeText(professionnel.toString()));
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
                    List<Professionel> filteredItems = new ArrayList<>();

                    // Normaliser la contrainte pour la recherche
                    String normalizedConstraint = normalizeText(constraint.toString());

                    // Diviser la recherche en mots pour chercher chaque mot séparément
                    String[] searchWords = normalizedConstraint.split("\\s+");

                    // Pour chaque élément dans la liste complète
                    for (int i = 0; i < normalizedNames.size(); i++) {
                        String normalizedName = normalizedNames.get(i);
                        boolean matchesAllWords = true;

                        // Vérifier que chaque mot est présent dans le nom
                        for (String word : searchWords) {
                            if (!normalizedName.contains(word)) {
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

            @SuppressWarnings("unchecked")
            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                clear();
                if (results.count > 0) {
                    addAll((List<Professionel>) results.values);
                }
                notifyDataSetChanged();
            }
        };
    }
}
