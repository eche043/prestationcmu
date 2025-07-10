package ci.technchange.prestationscmu.utils;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.telephony.SmsManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import ci.technchange.prestationscmu.models.MetriqueConnexion;

public class DataSMSManager {
    private static final String TAG = "DataSMSManager";
    //private static final String SERVER_PHONE = "+12172900867";
    private static final String SERVER_PHONE = "0768260566";

    // Interface pour le callback
    public interface SMSSendCallback {
        void onSMSSendSuccess();
        void onSMSSendFailure(String errorMessage);
    }


    public void sendDataViaSMS(String lettreCle,String identifiant, String numero_transaction, String code_affection1, String code_affection2, String code_acte1, String code_acte2, String code_acte3,String date, String code_ets, String id, String idFamoco, String code_agac, String heure, String lat, String longitude, String typeBon) {

        Log.d("sendDataViaSMS","Envoi de la feuille par SMS");
        try {

            String heure_finalisation = "";
            if (heure != null) {
                String heureOriginale = heure;
                // Vérifier si l'heure est au format "HH:MM:SS"
                if (heureOriginale.matches("\\d{1,2}:\\d{2}:\\d{2}")) {
                    String[] parts = heureOriginale.split(":");
                    heure_finalisation = parts[0] + "h" + parts[1] + "m" + parts[2] + "s";
                }
                // Vérifier si l'heure est au format "HH:MM"
                else if (heureOriginale.matches("\\d{1,2}:\\d{2}")) {
                    String[] parts = heureOriginale.split(":");
                    heure_finalisation = parts[0] + "h" + parts[1] + "m00s";
                }
                else {
                    heure_finalisation = heureOriginale; // Utiliser la valeur originale si le format ne correspond pas
                }
            } else {
                heure_finalisation = "00h00m00s"; // Valeur par défaut si l'heure est null
            }
            String message = String.format(
                    "TC_%s:%s:%s:%s:%s:%s:%s:%s:%s:%s:%s:%s:%s:%s:%s:%s:%s",
                    lettreCle,identifiant, numero_transaction, code_affection1, code_affection2,code_acte1,code_acte2,code_acte3,date,code_ets,id,idFamoco,code_agac,heure_finalisation,lat,longitude,typeBon
            );
            SmsManager smsManager = SmsManager.getDefault();
            ArrayList<String> parts = smsManager.divideMessage(message);
            smsManager.sendMultipartTextMessage(SERVER_PHONE, null, parts, null, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void updateProfilSMS(Context context, String lettreCle,String ancien_code, String nom, String prenoms,
                                         String matricule, String contact,
                                         SMSSendCallback callback) {
        try {
            String message = String.format(
                    "TC_%s:%s:%s:%s:%s:%s",
                    lettreCle, ancien_code,nom, prenoms, matricule,contact
            );

            // Générer des actions uniques pour éviter les conflits
            String uniqueId = UUID.randomUUID().toString();
            String SENT_ACTION = "SMS_SENT_ACTION_" + uniqueId;
            String DELIVERED_ACTION = "SMS_DELIVERED_ACTION_" + uniqueId;

            // Créer les PendingIntent avec les drapeaux appropriés
            int flags = PendingIntent.FLAG_ONE_SHOT;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                flags |= PendingIntent.FLAG_IMMUTABLE;
            }

            PendingIntent sentPI = PendingIntent.getBroadcast(context, 0,
                    new Intent(SENT_ACTION), flags);
            PendingIntent deliveredPI = PendingIntent.getBroadcast(context, 0,
                    new Intent(DELIVERED_ACTION), flags);

            // Créer les receveurs de diffusion
            BroadcastReceiver sentReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    switch (getResultCode()) {
                        case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                            Log.e(TAG, "Échec général");
                            callback.onSMSSendFailure("Échec général");
                            break;
                        case SmsManager.RESULT_ERROR_NO_SERVICE:
                            Log.e(TAG, "Pas de service");
                            callback.onSMSSendFailure("Pas de service");
                            break;
                        case SmsManager.RESULT_ERROR_NULL_PDU:
                            Log.e(TAG, "PDU null");
                            callback.onSMSSendFailure("PDU null");
                            break;
                        case SmsManager.RESULT_ERROR_RADIO_OFF:
                            Log.e(TAG, "Radio éteinte");
                            callback.onSMSSendFailure("Radio éteinte");
                            break;
                        case Activity.RESULT_OK: // Utilisation de Activity.RESULT_OK au lieu de SmsManager.RESULT_OK
                            Log.d(TAG, "SMS envoyé avec succès");
                            // On n'appelle pas encore le callback car on attend la confirmation de livraison
                            break;
                        default:
                            Log.d(TAG, "Résultat inconnu: " + getResultCode());
                            break;
                    }

                    try {
                        context.unregisterReceiver(this);
                    } catch (Exception e) {
                        Log.e(TAG, "Erreur lors du désabonnement: " + e.getMessage());
                    }
                }
            };

            BroadcastReceiver deliveredReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    // Pour le DELIVERED, on considère OK par défaut
                    if (getResultCode() == Activity.RESULT_OK) { // Utilisation de Activity.RESULT_OK
                        Log.d(TAG, "SMS livré avec succès");
                        callback.onSMSSendSuccess();
                    } else {
                        Log.e(TAG, "SMS non livré, code: " + getResultCode());
                        callback.onSMSSendFailure("SMS non livré");
                    }

                    try {
                        context.unregisterReceiver(this);
                    } catch (Exception e) {
                        Log.e(TAG, "Erreur lors du désabonnement: " + e.getMessage());
                    }
                }
            };

            // Enregistrer les receveurs de diffusion
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // Pour Android 13+, utiliser RECEIVER_NOT_EXPORTED
                context.registerReceiver(sentReceiver, new IntentFilter(SENT_ACTION), Context.RECEIVER_NOT_EXPORTED);
                context.registerReceiver(deliveredReceiver, new IntentFilter(DELIVERED_ACTION), Context.RECEIVER_NOT_EXPORTED);
            } else {
                // Pour les versions antérieures
                context.registerReceiver(sentReceiver, new IntentFilter(SENT_ACTION));
                context.registerReceiver(deliveredReceiver, new IntentFilter(DELIVERED_ACTION));
            }

            // Envoyer le SMS
            SmsManager smsManager = SmsManager.getDefault();
            ArrayList<String> parts = smsManager.divideMessage(message);

            ArrayList<PendingIntent> sentIntents = new ArrayList<>();
            ArrayList<PendingIntent> deliveredIntents = new ArrayList<>();

            for (int i = 0; i < parts.size(); i++) {
                sentIntents.add(sentPI);
                deliveredIntents.add(deliveredPI);
            }

            smsManager.sendMultipartTextMessage(SERVER_PHONE, null, parts, sentIntents, deliveredIntents);

        } catch (Exception e) {
            Log.e(TAG, "Erreur lors de l'envoi du SMS: " + e.getMessage());
            callback.onSMSSendFailure("Exception: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /*public void sendAgentAndCentreViaSMS(String lettreCle,String nom, String prenoms, String matricule, String code_ets,String idFamoco, String date,String heure, String contact, String idString, String lat, String lng, String latInt, String lngInt) {
        try {
            String message = String.format(
                    "TC_%s:%s:%s:%s:%s:%s:%s:%s:%s:%s:%s:%s:%s:%s",
                    lettreCle,nom, prenoms, matricule, code_ets,idFamoco,date, heure, contact, idString, lat, lng, latInt, lngInt
            );
            SmsManager smsManager = SmsManager.getDefault();
            ArrayList<String> parts = smsManager.divideMessage(message);
            smsManager.sendMultipartTextMessage(SERVER_PHONE, null, parts, null, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }*/

    public void sendAgentAndCentreViaSMS(Context context, String lettreCle, String nom, String prenoms,
                                         String matricule, String code_ets, String idFamoco,
                                         String date, String heure, String contact, String idString,
                                         String lat, String lng, String latInt, String lngInt,
                                         SMSSendCallback callback) {
        try {
            String message = String.format(
                    "TC_%s:%s:%s:%s:%s:%s:%s:%s:%s:%s:%s:%s:%s:%s",
                    lettreCle, nom, prenoms, matricule, code_ets, idFamoco, date, heure,
                    contact, idString, lat, lng, latInt, lngInt
            );

            // Générer des actions uniques pour éviter les conflits
            String uniqueId = UUID.randomUUID().toString();
            String SENT_ACTION = "SMS_SENT_ACTION_" + uniqueId;
            String DELIVERED_ACTION = "SMS_DELIVERED_ACTION_" + uniqueId;

            // Créer les PendingIntent avec les drapeaux appropriés
            int flags = PendingIntent.FLAG_ONE_SHOT;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                flags |= PendingIntent.FLAG_IMMUTABLE;
            }

            PendingIntent sentPI = PendingIntent.getBroadcast(context, 0,
                    new Intent(SENT_ACTION), flags);
            PendingIntent deliveredPI = PendingIntent.getBroadcast(context, 0,
                    new Intent(DELIVERED_ACTION), flags);

            // Créer les receveurs de diffusion
            BroadcastReceiver sentReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    switch (getResultCode()) {
                        case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                            Log.e(TAG, "Échec général");
                            callback.onSMSSendFailure("Échec général");
                            break;
                        case SmsManager.RESULT_ERROR_NO_SERVICE:
                            Log.e(TAG, "Pas de service");
                            callback.onSMSSendFailure("Pas de service");
                            break;
                        case SmsManager.RESULT_ERROR_NULL_PDU:
                            Log.e(TAG, "PDU null");
                            callback.onSMSSendFailure("PDU null");
                            break;
                        case SmsManager.RESULT_ERROR_RADIO_OFF:
                            Log.e(TAG, "Radio éteinte");
                            callback.onSMSSendFailure("Radio éteinte");
                            break;
                        case Activity.RESULT_OK: // Utilisation de Activity.RESULT_OK au lieu de SmsManager.RESULT_OK
                            Log.d(TAG, "SMS envoyé avec succès");
                            // On n'appelle pas encore le callback car on attend la confirmation de livraison
                            break;
                        default:
                            Log.d(TAG, "Résultat inconnu: " + getResultCode());
                            break;
                    }

                    try {
                        context.unregisterReceiver(this);
                    } catch (Exception e) {
                        Log.e(TAG, "Erreur lors du désabonnement: " + e.getMessage());
                    }
                }
            };

            BroadcastReceiver deliveredReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    // Pour le DELIVERED, on considère OK par défaut
                    if (getResultCode() == Activity.RESULT_OK) { // Utilisation de Activity.RESULT_OK
                        Log.d(TAG, "SMS livré avec succès");
                        callback.onSMSSendSuccess();
                    } else {
                        Log.e(TAG, "SMS non livré, code: " + getResultCode());
                        callback.onSMSSendFailure("SMS non livré");
                    }

                    try {
                        context.unregisterReceiver(this);
                    } catch (Exception e) {
                        Log.e(TAG, "Erreur lors du désabonnement: " + e.getMessage());
                    }
                }
            };

            // Enregistrer les receveurs de diffusion
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // Pour Android 13+, utiliser RECEIVER_NOT_EXPORTED
                context.registerReceiver(sentReceiver, new IntentFilter(SENT_ACTION), Context.RECEIVER_NOT_EXPORTED);
                context.registerReceiver(deliveredReceiver, new IntentFilter(DELIVERED_ACTION), Context.RECEIVER_NOT_EXPORTED);
            } else {
                // Pour les versions antérieures
                context.registerReceiver(sentReceiver, new IntentFilter(SENT_ACTION));
                context.registerReceiver(deliveredReceiver, new IntentFilter(DELIVERED_ACTION));
            }

            // Envoyer le SMS
            SmsManager smsManager = SmsManager.getDefault();
            ArrayList<String> parts = smsManager.divideMessage(message);

            ArrayList<PendingIntent> sentIntents = new ArrayList<>();
            ArrayList<PendingIntent> deliveredIntents = new ArrayList<>();

            for (int i = 0; i < parts.size(); i++) {
                sentIntents.add(sentPI);
                deliveredIntents.add(deliveredPI);
            }

            smsManager.sendMultipartTextMessage(SERVER_PHONE, null, parts, sentIntents, deliveredIntents);

        } catch (Exception e) {
            Log.e(TAG, "Erreur lors de l'envoi du SMS: " + e.getMessage());
            callback.onSMSSendFailure("Exception: " + e.getMessage());
            e.printStackTrace();
        }
    }





    public void connexionViaSMS(Context context, MetriqueConnexion metrique,String lettreCle, String lat,String lng,String idFamoco, SMSSendCallback callback) {
        try {

            String heure = "";
            if (metrique.getHeureConnexion() != null) {
                String heureOriginale = metrique.getHeureConnexion();
                // Vérifier si l'heure est au format "HH:MM"
                if (heureOriginale.matches("\\d{1,2}:\\d{2}")) {
                    String[] parts = heureOriginale.split(":");
                    if (parts.length >= 2) {
                        heure = parts[0] + "h" + parts[1] + "m00s";
                    } else {
                        heure = heureOriginale; // Utiliser la valeur originale si le format est inattendu
                    }
                } else {
                    heure = heureOriginale; // Utiliser la valeur originale si le format ne correspond pas
                }
            } else {
                heure = "00h00m00s"; // Valeur par défaut si l'heure est null
            }

            Log.d("Heure format1", metrique.getHeureConnexion());
            Log.d("Heure format2", heure);
            Log.d("latitude", lat);
            Log.d("longitude", lng);
            String message = String.format(
                    "TC_%s:%s:%s:%s:%s:%s:%s:%s:%s:%s:%s",
                    lettreCle, metrique.getCodeEts(), metrique.getIdRegion(), metrique.getNomComplet(), metrique.getDateConnexion(), heure, metrique.getCodeAgac(), lat, lng,idFamoco,"0"
            );

            // Générer des actions uniques pour éviter les conflits
            String uniqueId = UUID.randomUUID().toString();
            String SENT_ACTION = "SMS_SENT_ACTION_" + uniqueId;
            String DELIVERED_ACTION = "SMS_DELIVERED_ACTION_" + uniqueId;

            // Créer les PendingIntent avec les drapeaux appropriés
            int flags = PendingIntent.FLAG_ONE_SHOT;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                flags |= PendingIntent.FLAG_IMMUTABLE;
            }

            PendingIntent sentPI = PendingIntent.getBroadcast(context, 0,
                    new Intent(SENT_ACTION), flags);
            PendingIntent deliveredPI = PendingIntent.getBroadcast(context, 0,
                    new Intent(DELIVERED_ACTION), flags);

            // Créer les receveurs de diffusion
            BroadcastReceiver sentReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    switch (getResultCode()) {
                        case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                            Log.e(TAG, "Échec général");
                            callback.onSMSSendFailure("Échec général");
                            break;
                        case SmsManager.RESULT_ERROR_NO_SERVICE:
                            Log.e(TAG, "Pas de service");
                            callback.onSMSSendFailure("Pas de service");
                            break;
                        case SmsManager.RESULT_ERROR_NULL_PDU:
                            Log.e(TAG, "PDU null");
                            callback.onSMSSendFailure("PDU null");
                            break;
                        case SmsManager.RESULT_ERROR_RADIO_OFF:
                            Log.e(TAG, "Radio éteinte");
                            callback.onSMSSendFailure("Radio éteinte");
                            break;
                        case Activity.RESULT_OK: // Utilisation de Activity.RESULT_OK au lieu de SmsManager.RESULT_OK
                            Log.d(TAG, "SMS envoyé avec succès");
                            // On n'appelle pas encore le callback car on attend la confirmation de livraison
                            break;
                        default:
                            Log.d(TAG, "Résultat inconnu: " + getResultCode());
                            break;
                    }

                    try {
                        context.unregisterReceiver(this);
                    } catch (Exception e) {
                        Log.e(TAG, "Erreur lors du désabonnement: " + e.getMessage());
                    }
                }
            };

            BroadcastReceiver deliveredReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    // Pour le DELIVERED, on considère OK par défaut
                    if (getResultCode() == Activity.RESULT_OK) { // Utilisation de Activity.RESULT_OK
                        Log.d(TAG, "SMS livré avec succès");
                        callback.onSMSSendSuccess();
                    } else {
                        Log.e(TAG, "SMS non livré, code: " + getResultCode());
                        callback.onSMSSendFailure("SMS non livré");
                    }

                    try {
                        context.unregisterReceiver(this);
                    } catch (Exception e) {
                        Log.e(TAG, "Erreur lors du désabonnement: " + e.getMessage());
                    }
                }
            };

            // Enregistrer les receveurs de diffusion
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // Pour Android 13+, utiliser RECEIVER_NOT_EXPORTED
                context.registerReceiver(sentReceiver, new IntentFilter(SENT_ACTION), Context.RECEIVER_NOT_EXPORTED);
                context.registerReceiver(deliveredReceiver, new IntentFilter(DELIVERED_ACTION), Context.RECEIVER_NOT_EXPORTED);
            } else {
                // Pour les versions antérieures
                context.registerReceiver(sentReceiver, new IntentFilter(SENT_ACTION));
                context.registerReceiver(deliveredReceiver, new IntentFilter(DELIVERED_ACTION));
            }

            // Envoyer le SMS
            SmsManager smsManager = SmsManager.getDefault();
            ArrayList<String> parts = smsManager.divideMessage(message);

            ArrayList<PendingIntent> sentIntents = new ArrayList<>();
            ArrayList<PendingIntent> deliveredIntents = new ArrayList<>();

            for (int i = 0; i < parts.size(); i++) {
                sentIntents.add(sentPI);
                deliveredIntents.add(deliveredPI);
            }

            smsManager.sendMultipartTextMessage(SERVER_PHONE, null, parts, sentIntents, deliveredIntents);

        } catch (Exception e) {
            Log.e(TAG, "Erreur lors de l'envoi du SMS: " + e.getMessage());
            callback.onSMSSendFailure("Exception: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
