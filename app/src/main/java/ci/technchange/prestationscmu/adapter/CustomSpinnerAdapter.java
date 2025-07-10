package ci.technchange.prestationscmu.adapter;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.util.List;

import ci.technchange.prestationscmu.R;
import ci.technchange.prestationscmu.utils.ApiService;
import ci.technchange.prestationscmu.views.ConnexionActivity;

public class CustomSpinnerAdapter extends BaseAdapter {

    private Context context;
    private List<String> items;
    private LayoutInflater inflater;

    public CustomSpinnerAdapter(Context context, List<String> items) {
        this.context = context;
        this.items = items;
        this.inflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return items.size();
    }

    @Override
    public Object getItem(int position) {
        return items.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return createView(position, convertView, parent, false);
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        return createView(position, convertView, parent, true);
    }

    private View createView(int position, View convertView, ViewGroup parent, boolean isDropDown) {
        ViewHolder holder;

        if (convertView == null) {
            convertView = inflater.inflate(R.layout.spinner_item_with_delete, parent, false);
            holder = new ViewHolder();
            holder.textView = convertView.findViewById(R.id.spinner_text);
            holder.deleteIcon = convertView.findViewById(R.id.delete_icon);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        String item = items.get(position);

        // Afficher seulement le texte sans l'ID pour l'utilisateur
        String displayText = getDisplayText(item);
        holder.textView.setText(displayText);

        // Afficher la croix rouge seulement dans la liste déroulante et si ce n'est pas "Aucun agent inscrit"
        if (isDropDown && !item.equals("Aucun agent inscrit") && !item.equals("Erreur de chargement")) {
            holder.deleteIcon.setVisibility(View.VISIBLE);
            holder.deleteIcon.setOnClickListener(v -> {
                // Extraire l'ID, le matricule et les noms
                String id = extractId(item);
                String matricule = extractMatricule(item);
                String[] nomPrenoms = extractNomPrenoms(item);
                showDeleteConfirmationDialog(id, matricule, nomPrenoms[0], nomPrenoms[1], item, position);
            });
        } else {
            holder.deleteIcon.setVisibility(View.GONE);
        }

        return convertView;
    }

    private String getDisplayText(String formattedString) {
        // Le format est "ID|MATRICULE - NOM Prénom", on retourne "MATRICULE - NOM Prénom"
        if (formattedString != null && formattedString.contains("|")) {
            return formattedString.split("\\|")[1];
        }
        return formattedString; // Retourne la chaîne d'origine si le format n'est pas reconnu
    }

    private void showDeleteConfirmationDialog(String id, String matricule, String nom, String prenoms, String fullItem, int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Supprimer l'agent");
        builder.setMessage("ID: " + id + "\nMatricule: " + matricule + "\nNom: " + nom + "\nPrénoms: " + prenoms + "\n\nVoulez-vous vraiment supprimer cet agent ?");

        builder.setPositiveButton("Supprimer", (dialog, which) -> {
            // Afficher un spinner de chargement
            ProgressDialog progressDialog = new ProgressDialog(context);
            progressDialog.setMessage("Suppression en cours...");
            progressDialog.setCancelable(false);
            progressDialog.show();

            // Appeler l'API pour supprimer l'agent
            deleteAgent(id, matricule, nom, prenoms, position, progressDialog);
        });

        builder.setNegativeButton("Annuler", (dialog, which) -> {
            dialog.dismiss();
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void deleteAgent(String id, String matricule, String nom, String prenoms, int position, ProgressDialog progressDialog) {
        ApiService apiService = new ApiService(context);


        apiService.deleteAgent(id, matricule, nom, prenoms, new ApiService.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                // Fermer le dialog de chargement
                if (progressDialog != null && progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }

                // Supprimer de la liste et notifier l'adaptateur
                items.remove(position);
                notifyDataSetChanged();

                // Recharger la liste dans l'activité parente si possible
                if (context instanceof ConnexionActivity) {
                    ((ConnexionActivity) context).refreshAgentsList();
                }

                Toast.makeText(context, "Agent supprimé avec succès", Toast.LENGTH_SHORT).show();
                Log.d("CustomSpinnerAdapter", "Agent supprimé avec succès - ID: " + id + ", Matricule: " + matricule);
            }

            @Override
            public void onError(String message) {
                // Fermer le dialog de chargement
                if (progressDialog != null && progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }

                Toast.makeText(context, "Erreur lors de la suppression: " + message, Toast.LENGTH_LONG).show();
                Log.e("CustomSpinnerAdapter", "Erreur lors de la suppression: " + message);
            }
        });
    }



    private String extractMatricule(String formattedString) {
        // Le format est "ID|MATRICULE - NOM Prénom"
        if (formattedString != null && formattedString.contains("|")) {
            String withoutId = formattedString.split("\\|")[1]; // Enlever l'ID
            if (withoutId.contains(" - ")) {
                return withoutId.split(" - ")[0].trim();
            }
            return withoutId;
        } else if (formattedString != null && formattedString.contains(" - ")) {
            // Format ancien pour la compatibilité
            return formattedString.split(" - ")[0].trim();
        }
        return formattedString;
    }

    private String extractId(String formattedString) {
        // Le format est "ID|MATRICULE - NOM Prénom"
        if (formattedString != null && formattedString.contains("|")) {
            return formattedString.split("\\|")[0].trim();
        }
        return null; // Retourne null si pas d'ID trouvé
    }

    private String[] extractNomPrenoms(String formattedString) {
        // Le format est "ID|MATRICULE - NOM Prénom"
        String[] result = {"", ""};

        if (formattedString != null && formattedString.contains("|")) {
            String withoutId = formattedString.split("\\|")[1]; // Enlever l'ID
            if (withoutId.contains(" - ")) {
                String nomPrenoms = withoutId.split(" - ")[1].trim(); // Récupérer "NOM Prénom"
                String[] parts = nomPrenoms.split(" ", 2); // Séparer nom et prénoms
                if (parts.length >= 1) {
                    result[0] = parts[0]; // Nom
                }
                if (parts.length >= 2) {
                    result[1] = parts[1]; // Prénoms
                }
            }
        }

        return result;
    }

    static class ViewHolder {
        TextView textView;
        ImageView deleteIcon;
    }
}
