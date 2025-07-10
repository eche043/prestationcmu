package ci.technchange.prestationscmu.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import ci.technchange.prestationscmu.utils.UploadQueueManager;

public class NetworkChangeReceiver extends BroadcastReceiver {
    private static final String TAG = "NetworkChangeReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (ConnectivityManager.CONNECTIVITY_ACTION.equals(intent.getAction())) {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();

            Log.d(TAG, "Changement d'état réseau détecté: " + (isConnected ? "connecté" : "déconnecté"));

            if (isConnected) {
                // Si une connexion est disponible, vérifier la file d'attente
                UploadQueueManager.getInstance(context).startRetryScan();
            }
        }
    }
}