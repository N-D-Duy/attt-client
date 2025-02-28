package org.duynguyen.atttclient.keygen;

import org.duynguyen.atttclient.utils.Log;

import java.security.*;
import java.util.Base64;
import java.nio.file.*;

public class RSAKeyGen {
    private static final String PRIVATE_KEY_PATH = "private.pem";
    private static final String PUBLIC_KEY_PATH = "public.pem";

    public static void gen() throws Exception {
        if (!Files.exists(Path.of(PRIVATE_KEY_PATH))) {
            generateKeyPair();
        } else {
            Log.warn("Khóa RSA đã tồn tại.");
        }
    }

    private static void generateKeyPair() throws Exception {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        KeyPair keyPair = keyGen.generateKeyPair();

        String privateKeyStr = Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded());
        String publicKeyStr = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());

        Files.write(Path.of(PRIVATE_KEY_PATH), privateKeyStr.getBytes());
        Files.write(Path.of(PUBLIC_KEY_PATH), publicKeyStr.getBytes());

        Log.info("Khóa RSA đã được tạo.");
    }
}
