package ci.technchange.prestationscmu.utils;

import android.content.Context;
import android.os.Environment;
import android.util.Base64;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.logging.FileHandler;

//import ci.technchange.prestationscmu.BuildConfig;
import ci.technchange.prestationscmu.utils.Encrypt;

public class Loger {

    //Encrypt encrypt = null;

    public Loger() {
       // encrypt = new Encrypt(context);
    }


    private final static String TAG = Loger.class.getSimpleName();
    public static FileHandler logger = null;
    public static final String LOG_FILE_NAME = "T5BBCLog.txt";


    public void logException(String tag, Exception e) {
        e.printStackTrace();
        addToLog(tag, Log.getStackTraceString(e));
    }

    /**
     * Adding message to the Log file
     *
     * @param tag     tag
     * @param message message
     */
    public void addToLog(String tag, String message) {
        System.out.println(tag+" => "+message);
        /*if (tag == null) {
            tag = "T5-Idencode";
        }
        Log.i(tag, message);

        //  File userDIR = FingerprintUtils.createExternalDirectory("BBC_LOGS");
        String storageDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOCUMENTS).getAbsolutePath();
        String userDIR = storageDir + File.separator + "BBC_LOGS";
        File var2 = new File(userDIR);
        if (!var2.exists()) {
            var2.mkdirs();
        }
        File logFile = new File(userDIR, LOG_FILE_NAME);
        if (!logFile.exists()) {
            try {
                Log.i(TAG, "File created");
                logFile.createNewFile();
            } catch (IOException e) {
                Log.e(TAG, e.getLocalizedMessage());
            }
        }
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy_HH:mm:ss", Locale.US);
            String currentDateandTime = sdf.format(new Date());

            //BufferedWriter for performance, true to set append to file flag
            BufferedWriter buf = new BufferedWriter(new FileWriter(logFile, true));

            String data = currentDateandTime + "   " + tag + "   " + message;
            try {
                if (BuildConfig.DEBUG) {
                    String encryptedData = Base64.encodeToString(encrypt.encryptData(data.getBytes()), Base64.NO_WRAP);
                    buf.write(encryptedData + "\r\n");
                }else{
                    buf.write(data + "\r\n");
                }
            } catch (Exception e) {
                buf.write(data + "\r\n");
            }
            //buf.append(message);
            buf.newLine();
            buf.flush();
            buf.close();
        } catch (IOException e) {
            Log.e(TAG, e.getLocalizedMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}*/
    }
}