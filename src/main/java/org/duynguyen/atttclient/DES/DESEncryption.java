package org.duynguyen.atttclient.DES;

import org.duynguyen.atttclient.utils.Log;

import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.util.Base64;

public class DESEncryption {
    private static final String ALGORITHM = "DES";

    public static void encryptFile(String inputFile, String outputFile, String secretKeyBase64) throws Exception {
        SecretKey secretKey = new SecretKeySpec(Base64.getDecoder().decode(secretKeyBase64), ALGORITHM);
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);

        try (FileInputStream fis = new FileInputStream(inputFile);
             FileOutputStream fos = new FileOutputStream(outputFile);
             CipherOutputStream cos = new CipherOutputStream(fos, cipher)) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                cos.write(buffer, 0, bytesRead);
            }
        }
        Log.info("File đã được mã hóa: " + outputFile);
    }
}

