package ci.technchange.prestationscmu.utils;

import android.content.Context;

import java.io.InputStream;
import java.io.ObjectInputStream;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.RSAPublicKeySpec;

import javax.crypto.Cipher;

import ci.technchange.prestationscmu.R;

//import ai.tech5.hdbarcodedemo.R;

public class Encrypt {

    public static final String RSA_ALGORITHM = "RSA/ECB/PKCS1Padding";
    private static PublicKey pubKey;

    public Encrypt(Context context) {

        InputStream headerStream = null;
        ObjectInputStream objstream = null;
        try {
            headerStream = context.getResources().openRawResource(R.raw.public_key);

            objstream = new ObjectInputStream(headerStream);
            BigInteger m1 = (BigInteger) objstream.readObject();
            BigInteger e1 = (BigInteger) objstream.readObject();
            RSAPublicKeySpec keySpec = new RSAPublicKeySpec(m1, e1);
            KeyFactory fact = KeyFactory.getInstance("RSA");
            pubKey = fact.generatePublic(keySpec);
        } catch (Exception et) {
            et.printStackTrace();
        } finally {
            try {

                if (objstream != null) {
                    objstream.close();
                }
                if (headerStream != null) {
                    headerStream.close();
                }
            } catch (Exception e) {

            }
        }
    }

    public byte[] encryptData(byte[] data) throws Exception {


        // Encrypt data by using RSA ALGORITHM
        Cipher cipherRSA = Cipher.getInstance(RSA_ALGORITHM);
        cipherRSA.init(Cipher.ENCRYPT_MODE, pubKey);
        byte[] encryptedData = cipherRSA.doFinal(data);
        return encryptedData;

    }


}
