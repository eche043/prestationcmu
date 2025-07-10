package ci.technchange.prestationscmu.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import java.util.List;

import ci.technchange.prestationscmu.R;
import ci.technchange.prestationscmu.models.AutreFse;

public class AutreFseAdapter extends RecyclerView.Adapter<AutreFseAdapter.AutreFseViewHolder> {
    private final List<AutreFse> autreFseList;
    private final Context context;

    public AutreFseAdapter(List<AutreFse> autreFseList, Context context) {
        this.autreFseList = autreFseList;
        this.context = context;
    }

    @NonNull
    @Override
    public AutreFseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_fsp, parent, false);
        return new AutreFseViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AutreFseViewHolder holder, int position) {
        AutreFse item = autreFseList.get(position);


        holder.txtTypeSoin.setText("Autre feuille");
        holder.txtTransactionNumber.setText(String.format("N° Transaction : %s", item.getNumTransaction()));


        Glide.with(context)
                .load(item.getUrlPhoto())
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(holder.imgAction);
    }

    @Override
    public int getItemCount() {
        return autreFseList.size();
    }

    static class AutreFseViewHolder extends RecyclerView.ViewHolder {
        final TextView txtTransactionNumber;
        final TextView txtTypeSoin;
        final ImageView imgAction;

        public AutreFseViewHolder(@NonNull View itemView) {
            super(itemView);
            txtTransactionNumber = itemView.findViewById(R.id.txtTransactionNumberOld);
            txtTypeSoin = itemView.findViewById(R.id.txtTypeSoinOld);
            imgAction = itemView.findViewById(R.id.imgAction);


            imgAction.setClickable(false);
            imgAction.setFocusable(false);
        }
    }
}