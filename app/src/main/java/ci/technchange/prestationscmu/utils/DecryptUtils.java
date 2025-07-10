package ci.technchange.prestationscmu.utils;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.security.PrivateKey;
import javax.crypto.Cipher;

public class DecryptUtils {
    public static void decryptDatabase(File encryptedFile, File decryptedFile, PrivateKey privateKey) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.DECRYPT_MODE, privateKey);

        FileInputStream fis = new FileInputStream(encryptedFile);
        FileOutputStream fos = new FileOutputStream(decryptedFile);

        byte[] buffer = new byte[256];
        int length;
        while ((length = fis.read(buffer)) != -1) {
            byte[] decryptedBytes = cipher.doFinal(buffer, 0, length);
            fos.write(decryptedBytes);
        }

        fis.close();
        fos.close();
    }
}
