package org.duynguyen.atttclient.DES;

import org.duynguyen.atttclient.utils.Log;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.util.Base64;

public class DESDecryption {
    private static final String ALGORITHM = "DES";

    public static void decryptFile(String inputFile, String outputFile, String secretKeyBase64) throws Exception {
        SecretKey secretKey = new SecretKeySpec(Base64.getDecoder().decode(secretKeyBase64), ALGORITHM);
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, secretKey);

        try (FileInputStream fis = new FileInputStream(inputFile);
             CipherInputStream cis = new CipherInputStream(fis, cipher);
             FileOutputStream fos = new FileOutputStream(outputFile)) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = cis.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
            }
        }
        Log.info("File đã được giải mã: " + outputFile);
    }
}

