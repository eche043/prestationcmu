package ci.technchange.prestationscmu.adapter;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import ci.technchange.prestationscmu.R;
import ci.technchange.prestationscmu.models.FseItem;
import ci.technchange.prestationscmu.views.ChoixPrestationActivity;
import ci.technchange.prestationscmu.views.FseEditActivity;

public class FseAdapterOld extends RecyclerView.Adapter<FseAdapterOld.FseViewHolder> {
    private List<FseItem> fseList;
    public Context context;





    public FseAdapterOld(List<FseItem> fseList,Context context) {
        this.fseList = fseList;
        this.context = context;
    }

    @NonNull
    @Override
    public FseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_fse_old, parent, false);
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

        holder.btnAction.setPadding(0, 8, 0, 8);
        GradientDrawable shape = new GradientDrawable();
        shape.setCornerRadius(8); // en pixels - ajustez selon vos besoins
        shape.setColor(android.graphics.Color.parseColor("#1ba805"));
        holder.btnAction.setBackground(shape);

        // GradientDrawable drawable = (GradientDrawable)  holder.btnAction.getBackground();

        holder.btnAction.setOnClickListener(v -> {
            Intent intent = new Intent(context, ChoixPrestationActivity.class);
            intent.putExtra("num_trans", item.getTransactionNumber());
            intent.putExtra("PATIENT_OLD",  item);
            context.startActivity(intent);
        });


    }

    @Override
    public int getItemCount() {
        return fseList.size();
    }

    public static class FseViewHolder extends RecyclerView.ViewHolder {
        TextView txtTransactionNumber, txtFullName , txtTypeSoin , txtSecurite ,dateNaiss ,txtEtablissement;
        Button btnAction , btnFinalisation ;
        public FseViewHolder(@NonNull View itemView) {
            super(itemView);
            txtTransactionNumber = itemView.findViewById(R.id.txtTransactionNumberOld);
            txtFullName = itemView.findViewById(R.id.txtFullNameOld);
            txtTypeSoin = itemView.findViewById(R.id.txtTypeSoinOld);
            txtSecurite = itemView.findViewById(R.id.txtSecuriteOld);
            dateNaiss = itemView.findViewById(R.id.dateNaissOld);
            txtEtablissement = itemView.findViewById(R.id.txtEtablissementOld);
            btnAction = itemView.findViewById(R.id.btnSuivantOld);

        }
    }
}

