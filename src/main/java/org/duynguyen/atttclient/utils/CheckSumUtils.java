package org.duynguyen.atttclient.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Utility class for various checksum, hashing, and HMAC operations.
 * Supports both synchronous and asynchronous processing with improved efficiency.
 */
public class CheckSumUtils {
    private static final int THREAD_POOL_SIZE = Runtime.getRuntime().availableProcessors();
    private static final int BUFFER_SIZE = 8192;
    private static final ExecutorService executor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
    private static final String DEFAULT_HASH_ALGORITHM = "SHA-256";
    private static final String DEFAULT_HMAC_ALGORITHM = "HmacSHA256";
    
    /**
     * Generates a cryptographically secure random key.
     * 
     * @param length The length of the key in bytes
     * @return Base64 encoded random key
     */
    public static String generateRandomKey(int length) {
        if (length <= 0) {
            throw new IllegalArgumentException("Key length must be positive");
        }
        
        SecureRandom random = new SecureRandom();
        byte[] key = new byte[length];
        random.nextBytes(key);
        return Base64.getEncoder().encodeToString(key);
    }
    
    /**
     * Hashes input string using SHA-256.
     * 
     * @param input String to hash
     * @return Base64 encoded hash
     * @throws RuntimeException if hashing algorithm is not available
     */
    public static String hash(String input) {
        return hash(input, DEFAULT_HASH_ALGORITHM);
    }
    
    /**
     * Hashes input string using specified algorithm.
     * 
     * @param input String to hash
     * @param algorithm Hashing algorithm to use
     * @return Base64 encoded hash
     * @throws RuntimeException if hashing algorithm is not available
     */
    public static String hash(String input, String algorithm) {
        if (input == null) {
            throw new IllegalArgumentException("Input cannot be null");
        }
        
        try {
            MessageDigest digest = MessageDigest.getInstance(algorithm);
            byte[] hash = digest.digest(input.getBytes());
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error initializing hash algorithm: " + algorithm, e);
        }
    }
    
    /**
     * Hashes multiple inputs in parallel using CompletableFuture.
     * 
     * @param inputs Array of strings to hash
     * @param callback Callback for when each hash completes
     */
    public static void hashInParallel(String[] inputs, HashCallback callback) {
        if (inputs == null || callback == null) {
            throw new IllegalArgumentException("Inputs and callback cannot be null");
        }
        
        for (String input : inputs) {
            CompletableFuture.supplyAsync(() -> hash(input), executor)
                .thenAccept(hashed -> callback.onHashComplete(input, hashed));
        }
    }
    
    /**
     * Generates HMAC for the input string using the provided secret key.
     * 
     * @param input Input string
     * @param secretKey Secret key for HMAC
     * @return Base64 encoded HMAC
     * @throws RuntimeException if HMAC generation fails
     */
    public static String generateHmac(String input, String secretKey) {
        return generateHmac(input, secretKey, DEFAULT_HMAC_ALGORITHM);
    }
    
    /**
     * Generates HMAC for the input string using the provided secret key and algorithm.
     * 
     * @param input Input string
     * @param secretKey Secret key for HMAC
     * @param algorithm HMAC algorithm (e.g., "HmacSHA256", "HmacSHA512")
     * @return Base64 encoded HMAC
     * @throws RuntimeException if HMAC generation fails
     */
    public static String generateHmac(String input, String secretKey, String algorithm) {
        if (input == null || secretKey == null) {
            throw new IllegalArgumentException("Input and secret key cannot be null");
        }
        
        try {
            SecretKeySpec keySpec = new SecretKeySpec(secretKey.getBytes(), algorithm);
            Mac mac = Mac.getInstance(algorithm);
            mac.init(keySpec);
            byte[] hmacBytes = mac.doFinal(input.getBytes());
            return Base64.getEncoder().encodeToString(hmacBytes);
        } catch (Exception e) {
            throw new RuntimeException("Error generating HMAC with algorithm: " + algorithm, e);
        }
    }
    
    /**
     * Verifies HMAC for the input against the expected HMAC.
     * 
     * @param input Input string
     * @param secretKey Secret key for HMAC
     * @param expectedHmac Expected HMAC value
     * @return true if HMAC matches, false otherwise
     */
    public static boolean verifyHmac(String input, String secretKey, String expectedHmac) {
        if (input == null || secretKey == null || expectedHmac == null) {
            throw new IllegalArgumentException("Input, secret key, and expected HMAC cannot be null");
        }
        
        String generatedHmac = generateHmac(input, secretKey);
        return MessageDigest.isEqual(
            Base64.getDecoder().decode(generatedHmac),
            Base64.getDecoder().decode(expectedHmac)
        );
    }
    
    /**
     * Generates HMAC for a file using the provided secret key.
     * Uses NIO for improved file reading performance.
     * 
     * @param file File to generate HMAC for
     * @param secretKey Secret key for HMAC
     * @return Base64 encoded HMAC
     * @throws RuntimeException if HMAC generation fails
     */
    public static String generateFileHmac(File file, String secretKey) {
        if (file == null || secretKey == null) {
            throw new IllegalArgumentException("File and secret key cannot be null");
        }
        if (!file.exists() || !file.isFile()) {
            throw new IllegalArgumentException("File does not exist or is not a regular file");
        }
        
        try (FileChannel channel = FileChannel.open(file.toPath(), java.nio.file.StandardOpenOption.READ)) {
            SecretKeySpec keySpec = new SecretKeySpec(secretKey.getBytes(), DEFAULT_HMAC_ALGORITHM);
            Mac mac = Mac.getInstance(DEFAULT_HMAC_ALGORITHM);
            mac.init(keySpec);
            
            ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
            while (channel.read(buffer) != -1) {
                buffer.flip();
                byte[] data = new byte[buffer.remaining()];
                buffer.get(data);
                mac.update(data);
                buffer.clear();
            }
            
            byte[] hmacBytes = mac.doFinal();
            return Base64.getEncoder().encodeToString(hmacBytes);
        } catch (Exception e) {
            throw new RuntimeException("Error generating file HMAC", e);
        }
    }
    
    /**
     * Verifies HMAC for a file against the expected HMAC.
     * 
     * @param file File to verify
     * @param secretKey Secret key for HMAC
     * @param expectedHmac Expected HMAC value
     * @return true if HMAC matches, false otherwise
     */
    public static boolean verifyFileHmac(File file, String secretKey, String expectedHmac) {
        if (file == null || secretKey == null || expectedHmac == null) {
            throw new IllegalArgumentException("File, secret key, and expected HMAC cannot be null");
        }
        
        String generatedHmac = generateFileHmac(file, secretKey);
        return MessageDigest.isEqual(
            Base64.getDecoder().decode(generatedHmac),
            Base64.getDecoder().decode(expectedHmac)
        );
    }
    
    /**
     * Generates HMAC for a file in parallel using chunk processing.
     * 
     * @param file File to generate HMAC for
     * @param secretKey Secret key for HMAC
     * @return Base64 encoded HMAC
     * @throws RuntimeException if HMAC generation fails
     */
    public static String generateFileHmacParallel(File file, String secretKey) {
        if (file == null || secretKey == null) {
            throw new IllegalArgumentException("File and secret key cannot be null");
        }
        if (!file.exists() || !file.isFile()) {
            throw new IllegalArgumentException("File does not exist or is not a regular file");
        }
        
        long fileSize = file.length();
        int chunkSize = Math.max(BUFFER_SIZE, (int)(fileSize / THREAD_POOL_SIZE));
        int numChunks = (int)Math.ceil((double)fileSize / chunkSize);
        
        try {
            byte[][] chunkHmacs = new byte[numChunks][];
            AtomicInteger completedChunks = new AtomicInteger(0);
            Object lock = new Object();

            for (int i = 0; i < numChunks; i++) {
                final int chunkIndex = i;
                final long startPosition = (long)i * chunkSize;
                final long endPosition = Math.min(startPosition + chunkSize, fileSize);
                
                CompletableFuture.runAsync(() -> {
                    try (FileInputStream fis = new FileInputStream(file)) {
                        SecretKeySpec keySpec = new SecretKeySpec(secretKey.getBytes(), DEFAULT_HMAC_ALGORITHM);
                        Mac mac = Mac.getInstance(DEFAULT_HMAC_ALGORITHM);
                        mac.init(keySpec);
                        
                        fis.skip(startPosition);
                        long remaining = endPosition - startPosition;
                        byte[] buffer = new byte[BUFFER_SIZE];
                        
                        while (remaining > 0) {
                            int bytesToRead = (int)Math.min(remaining, BUFFER_SIZE);
                            int bytesRead = fis.read(buffer, 0, bytesToRead);
                            if (bytesRead == -1) break;
                            
                            mac.update(buffer, 0, bytesRead);
                            remaining -= bytesRead;
                        }
                        
                        chunkHmacs[chunkIndex] = mac.doFinal();

                        synchronized (lock) {
                            if (completedChunks.incrementAndGet() == numChunks) {
                                lock.notifyAll();
                            }
                        }
                    } catch (Exception e) {
                        throw new RuntimeException("Error processing file chunk", e);
                    }
                }, executor);
            }

            synchronized (lock) {
                while (completedChunks.get() < numChunks) {
                    try {
                        lock.wait();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Thread interrupted while waiting for chunks to complete", e);
                    }
                }
            }

            try {
                Mac finalMac = Mac.getInstance(DEFAULT_HMAC_ALGORITHM);
                SecretKeySpec keySpec = new SecretKeySpec(secretKey.getBytes(), DEFAULT_HMAC_ALGORITHM);
                finalMac.init(keySpec);
                
                for (byte[] chunkHmac : chunkHmacs) {
                    finalMac.update(chunkHmac);
                }
                
                byte[] finalHmacBytes = finalMac.doFinal();
                return Base64.getEncoder().encodeToString(finalHmacBytes);
            } catch (Exception e) {
                throw new RuntimeException("Error combining chunk HMACs", e);
            }
        } catch (Exception e) {
            throw new RuntimeException("Error generating file HMAC in parallel", e);
        }
    }
    
    /**
     * Calculates hash of a file using the specified algorithm.
     * 
     * @param filePath Path to the file
     * @param algorithm Hash algorithm to use
     * @return Base64 encoded hash
     * @throws RuntimeException if hash generation fails
     */
    public static String hashFile(String filePath, String algorithm) {
        Path path = Paths.get(filePath);
        try {
            MessageDigest digest = MessageDigest.getInstance(algorithm);
            byte[] hash = digest.digest(Files.readAllBytes(path));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException | IOException e) {
            throw new RuntimeException("Error hashing file: " + filePath, e);
        }
    }
    
    /**
     * Shuts down the executor service.
     * Should be called when the application is shutting down.
     */
    public static void shutdown() {
        executor.shutdown();
    }
    
    /**
     * Callback interface for asynchronous hash operations.
     */
    public interface HashCallback {
        /**
         * Called when a hash operation completes.
         * 
         * @param original Original input string
         * @param hashed Resulting hash
         */
        void onHashComplete(String original, String hashed);
    }
}