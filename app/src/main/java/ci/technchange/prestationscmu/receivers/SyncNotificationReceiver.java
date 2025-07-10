package ci.technchange.prestationscmu.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;

import androidx.fragment.app.FragmentActivity;

import com.google.android.material.snackbar.Snackbar;

import ci.technchange.prestationscmu.utils.UploadQueueManager;

public class SyncNotificationReceiver extends BroadcastReceiver {
    private static final String TAG = "SyncNotificationReceiver";
    private final View rootView;
    private final Handler mainHandler;

    public SyncNotificationReceiver(View rootView) {
        this.rootView = rootView;
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.d(TAG, "Received broadcast action: " + action);

        if (UploadQueueManager.ACTION_SYNC_STARTED.equals(action)) {
            mainHandler.post(() -> {
                if (rootView != null) {
                    Snackbar.make(rootView, "Synchronisation FSE en cours...", Snackbar.LENGTH_LONG).show();
                } else {
                    Log.e(TAG, "rootView est NULL pour SYNC_STARTED");
                }
            });
        } else if (UploadQueueManager.ACTION_SYNC_COMPLETED.equals(action)) {
            mainHandler.post(() -> {
                if (rootView != null) {
                    Snackbar.make(rootView, "Synchronisation FSE terminée", Snackbar.LENGTH_SHORT).show();
                } else {
                    Log.e(TAG, "rootView est NULL pour SYNC_COMPLETED");
                }
            });
        }
    }
}