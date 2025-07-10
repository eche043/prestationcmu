package ci.technchange.prestationscmu.utils;

import android.content.Context;
import android.telephony.SmsManager;
import java.util.ArrayList;

public class DataSMSMetriqueManager {
    //private static final String SERVER_PHONE = "+12172900867";
    private static final String SERVER_PHONE = "0768260566";
    private Context context;

    public DataSMSMetriqueManager(Context context) {
        this.context = context;
    }

    public void sendDataMetriqueViaSMS(String lettreCle, String codeEts, String idFamoco,
                                       String nombre_recherche, String nombre_fse_edit,
                                       String nombre_fse_finalise,String date_rapport, String code_agac,String nombre_fse_non_finalise) {
        try {
            String message = String.format(
                    "TC_%s:%s:%s:%s:%s:%s:%s:%s:%s",
                    lettreCle, codeEts, idFamoco, nombre_recherche, nombre_fse_edit, nombre_fse_finalise,date_rapport,code_agac,nombre_fse_non_finalise
            );

            SmsManager smsManager = SmsManager.getDefault();
            ArrayList<String> parts = smsManager.divideMessage(message);
            smsManager.sendMultipartTextMessage(SERVER_PHONE, null, parts, null, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}