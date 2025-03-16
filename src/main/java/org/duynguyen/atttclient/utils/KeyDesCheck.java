package org.duynguyen.atttclient.utils;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class KeyDesCheck {
    public static void main(String[] args) {
        byte[] keyDes = {19, 19, -118, -43, 28, 22, -33, -14};

        try {
            SecretKey key = new SecretKeySpec(keyDes, "DES");
            System.out.println("✅ Key hợp lệ và có thể sử dụng cho DES.");
        } catch (Exception e) {
            System.out.println("❌ Key không hợp lệ: " + e.getMessage());
        }
    }
}
