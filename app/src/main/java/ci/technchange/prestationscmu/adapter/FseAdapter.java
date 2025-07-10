package ci.technchange.prestationscmu.adapter;

import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.TextView;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import ci.technchange.prestationscmu.R;
import ci.technchange.prestationscmu.models.FseItem;
import ci.technchange.prestationscmu.views.FseFiniliastionSoinsAmbulatoire;

public class FseAdapter extends RecyclerView.Adapter<FseAdapter.FseViewHolder> {
    private List<FseItem> fseList;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onPrintButtonClick(FseItem item);
    }



    public FseAdapter(List<FseItem> fseList, OnItemClickListener listener) {
        this.fseList = fseList;
        this.listener =listener;
    }

    @NonNull
    @Override
    public FseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_fse, parent, false);
        return new FseViewHolder(view);
    }


    @Override
    public void onBindViewHolder(@NonNull FseViewHolder holder, int position) {
        FseItem item = fseList.get(position);
        holder.txtTypeSoin.setText(item.getTypesoins());
        holder.txtTransactionNumber.setText("N° Transaction : " + item.getTransactionNumber());
        holder.txtFullName.setText("Nom : " + item.getFullName());
        holder.txtSecurite.setText("N°Securite : "+item.getSecurityNumber());
        holder.dateNaiss.setText("date naissance : "+item.getDateNaissance());
        holder.txtEtablissement.setText("Etablissement : "+item.getEtablissement());
        holder.txtDateSoins.setText("Date de soins: "+item.getDateSoins());
        holder.txtStatus.setText(item.getStatus());

        // GradientDrawable drawable = (GradientDrawable)  holder.btnAction.getBackground();



        // Modifier la couleur du statut
        if ("Finalisée".equals(item.getStatus())) {
            holder.btnAction.setText("Imprimer");
            holder.txtStatus.setTextColor(android.graphics.Color.parseColor("#1ba805"));

            holder.btnAction.setPadding(0, 8, 0, 8);
            //holder.btnAction.setBackgroundColor(android.graphics.Color.parseColor("#1ba805"));
            GradientDrawable shape = new GradientDrawable();
            shape.setCornerRadius(8); // en pixels - ajustez selon vos besoins
            shape.setColor(android.graphics.Color.parseColor("#1ba805"));
            holder.btnAction.setBackground(shape);

            holder.btnAction.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onPrintButtonClick(item); // Appeler la méthode de l'interface
                }
            });

        } else if("En attente d'Entente préalable".equals(item.getStatus())){
            holder.btnAction.setText("Demande");
            holder.txtStatus.setTextColor(android.graphics.Color.parseColor("#f67d0b")); // Orange
            //holder.btnAction.setBackgroundColor(android.graphics.Color.parseColor("#f67d0b"));
            holder.btnAction.setPadding(0, 8, 0, 8);
            GradientDrawable shape = new GradientDrawable();
            shape.setCornerRadius(8); // en pixels - ajustez selon vos besoins
            shape.setColor(android.graphics.Color.parseColor("#f67d0b"));
            holder.btnAction.setBackground(shape);

        }
        else {
            holder.btnAction.setText("Imprimer");
            holder.btnFinalisation.setVisibility(View.VISIBLE);
            holder.btnFinalisation.setPadding(0, 8, 0, 8);

            holder.txtStatus.setTextColor(android.graphics.Color.parseColor("#f67d0b")); // Orange
            //holder.btnAction.setBackgroundColor(android.graphics.Color.parseColor("#f67d0b"));
            holder.btnAction.setPadding(0, 8, 0, 8);
            GradientDrawable shape1 = new GradientDrawable();
            shape1.setCornerRadius(8); // en pixels - ajustez selon vos besoins
            shape1.setColor(android.graphics.Color.parseColor("#1ba805"));
            holder.btnAction.setBackground(shape1);

            GradientDrawable shape = new GradientDrawable();
            shape.setCornerRadius(8); // en pixels - ajustez selon vos besoins
            shape.setColor(android.graphics.Color.parseColor("#f67d0b"));
            //holder.btnAction.setBackground(shape);
            holder.btnFinalisation.setBackground(shape);

            holder.btnFinalisation.setOnClickListener(v -> {
                // Récupérer l'ID de l'élément sélectionné
                int itemId = item.getFseId();  // Remplacez getId() par la méthode qui retourne l'ID dans votre modèle FseItem
                String num = item.getTransactionNumber()  ;
                // Créer un Intent pour démarrer l'activité cible
                Intent intent = new Intent(v.getContext(), FseFiniliastionSoinsAmbulatoire.class); // Remplacez NouvelleActivite par l'activité cible

                // Passer l'ID de l'élément via l'Intent
                intent.putExtra("ITEM_ID", itemId);
                intent.putExtra("NUM_TRANSACTION", num);

                // Démarrer l'activité
                v.getContext().startActivity(intent);
            });


            holder.btnAction.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onPrintButtonClick(item);
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return fseList.size();
    }

    public static class FseViewHolder extends RecyclerView.ViewHolder {
        TextView txtTransactionNumber, txtFullName, txtStatus , txtTypeSoin , txtSecurite ,dateNaiss ,txtEtablissement, txtDateSoins;
        Button btnAction , btnFinalisation ;
        public FseViewHolder(@NonNull View itemView) {
            super(itemView);
            txtTransactionNumber = itemView.findViewById(R.id.txtTransactionNumber);
            txtFullName = itemView.findViewById(R.id.txtFullName);
            txtStatus = itemView.findViewById(R.id.txtStatus);
            txtTypeSoin = itemView.findViewById(R.id.txtTypeSoin);
            txtSecurite = itemView.findViewById(R.id.txtSecurite);
            dateNaiss = itemView.findViewById(R.id.dateNaiss);
            txtEtablissement = itemView.findViewById(R.id.txtEtablissement);
            txtDateSoins= itemView.findViewById(R.id.txtDateSoins);
            btnAction = itemView.findViewById(R.id.btnAction);
            btnFinalisation = itemView.findViewById(R.id.btnFinalisation);
        }
    }
}

