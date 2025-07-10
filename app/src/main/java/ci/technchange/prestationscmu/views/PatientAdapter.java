package ci.technchange.prestationscmu.views;

import static androidx.core.content.ContextCompat.startActivity;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.List;

import ci.technchange.prestationscmu.R;
import ci.technchange.prestationscmu.models.Metrique;
import ci.technchange.prestationscmu.models.Patient;
import ci.technchange.prestationscmu.utils.ActivityTracker;
import ci.technchange.prestationscmu.utils.MetriqueServiceDb;
import ci.technchange.prestationscmu.utils.RegionUtils;
import ci.technchange.prestationscmu.utils.SharedPrefManager;
import ci.technchange.prestationscmu.utils.UtilsInfosAppareil;

public class PatientAdapter extends BaseAdapter {
    public Context context;
    public List<Patient> item_patient;
    private static final String API_URL = "http://51.38.224.233:8080/api/v1/saveFSE";
    private ActivityTracker activityTracker;
    private UtilsInfosAppareil utilsInfos;
    private MetriqueServiceDb metriqueServiceDb;
    private SimpleDateFormat dateFormat;
    private SharedPrefManager sharedPrefManager;

    public PatientAdapter(Context context, List<Patient> item_patient){
        this.context = context;
        this.item_patient = item_patient;
        this.activityTracker = new ActivityTracker(context);
        this.utilsInfos = new UtilsInfosAppareil(context);
        this.metriqueServiceDb =  MetriqueServiceDb.getInstance(context);
        this.dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        this.sharedPrefManager = new SharedPrefManager(context);
    }

    public int getCount(){return item_patient.size();}
    public Object getItem(int position){return item_patient.get(position);}
    public long getItemId(int position) {
        return position;
    }

    public View getView(int position, View patientView, ViewGroup parent) {
        if (patientView == null) {
            Log.i("HANDLESEARCH -> PATIENTADAPTER: ","patientview is null donct inflate layout liste patien_trouve");
            patientView = LayoutInflater.from(context).inflate(R.layout.liste_patient_trouve, parent, false);
        }else{
            Log.i("HANDLESEARCH -> PATIENTADAPTER: ","patientview is not null donct RIEN");
        }

        Patient patient = item_patient.get(position);

        TextView nomCompletPatient = patientView.findViewById(R.id.infoNomComplet);
        TextView dateNaissancePatient = patientView.findViewById(R.id.infoDateNaissance);
        TextView telephonePatient = patientView.findViewById(R.id.infoTelephone);
        TextView lieuNaissancePatient = patientView.findViewById(R.id.infoLieuNaissance);

        nomCompletPatient.setText(patient.getNom() +" "+ patient.getPrenoms());
        dateNaissancePatient.setText(patient.getDateNaissance());
        telephonePatient.setText(patient.getTelephone());
        lieuNaissancePatient.setText(patient.getLieuNaissance());

        patientView.setOnClickListener(v -> {
          //  Intent intent = new Intent(context, ChoixPrestationActivity.class);
            Intent intent = new Intent(context, FseVersioOldActivity.class);
            activityTracker.enregistrerDateFin();
            saveMetrique();
            intent.putExtra("PATIENT", patient);
            intent.putExtra("nom_complet",patient.getNom()+" "+patient.getPrenoms());
            context.startActivity(intent);
        });

        return patientView;
    }

    private boolean saveMetrique() {
        try {
            Metrique metrique = new Metrique();
            String downloadedFileName = sharedPrefManager.getDownloadedFileName();
            String idFamoco = utilsInfos.recupererIdAppareil();
            Log.d("Famoco", "ID famoco est : "+idFamoco);
            int id = RegionUtils.getRegionid(downloadedFileName);
            String activite = activityTracker.getLastActivity();
            String date_debut = activityTracker.getDateDebut();
            String date_fin =activityTracker.getDateFin();
            metrique.setActivite(activite);
            metrique.setDateDebut(date_debut);
            metrique.setDateFin(date_fin);
            metrique.setIdRegion(id);
            metrique.setIdFamoco(idFamoco);
            metrique.setStatusSynchro(0);


            Log.d("Metrique", metrique.toString());

            long result = metriqueServiceDb.insertMetrique(metrique);

            if (result != -1) {
                Log.d("metrique_info", "La metrique pour l'activité "+activite+" est enregistré");
                return true;
            } else {
                Log.d("metrique_info", "La metrique pour l'activité "+activite+"n'a pas pu être enregistrer");
                return false;
            }
        } catch (Exception e) {
            Log.e("metrique_info", "Erreur :"+e.getMessage());
            //Toast.makeText(this, "Erreur: " + e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
            return false;
        }
    }
}
