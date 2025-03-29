package org.duynguyen.atttclient.utils;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;

public class DesUtils {
    private static final int BLOCK_SIZE = 8;
    private static final int[] IP = {
            58, 50, 42, 34, 26, 18, 10, 2,
            60, 52, 44, 36, 28, 20, 12, 4,
            62, 54, 46, 38, 30, 22, 14, 6,
            64, 56, 48, 40, 32, 24, 16, 8,
            57, 49, 41, 33, 25, 17, 9, 1,
            59, 51, 43, 35, 27, 19, 11, 3,
            61, 53, 45, 37, 29, 21, 13, 5,
            63, 55, 47, 39, 31, 23, 15, 7
    };

    private static final int[] FP = {
            40, 8, 48, 16, 56, 24, 64, 32,
            39, 7, 47, 15, 55, 23, 63, 31,
            38, 6, 46, 14, 54, 22, 62, 30,
            37, 5, 45, 13, 53, 21, 61, 29,
            36, 4, 44, 12, 52, 20, 60, 28,
            35, 3, 43, 11, 51, 19, 59, 27,
            34, 2, 42, 10, 50, 18, 58, 26,
            33, 1, 41, 9, 49, 17, 57, 25
    };

    private static final int[] E = {
            32, 1, 2, 3, 4, 5,
            4, 5, 6, 7, 8, 9,
            8, 9, 10, 11, 12, 13,
            12, 13, 14, 15, 16, 17,
            16, 17, 18, 19, 20, 21,
            20, 21, 22, 23, 24, 25,
            24, 25, 26, 27, 28, 29,
            28, 29, 30, 31, 32, 1
    };

    private static final int[] P = {
            16, 7, 20, 21, 29, 12, 28, 17,
            1, 15, 23, 26, 5, 18, 31, 10,
            2, 8, 24, 14, 32, 27, 3, 9,
            19, 13, 30, 6, 22, 11, 4, 25
    };

    private static final int[] PC1 = {
            57, 49, 41, 33, 25, 17, 9,
            1, 58, 50, 42, 34, 26, 18,
            10, 2, 59, 51, 43, 35, 27,
            19, 11, 3, 60, 52, 44, 36,
            63, 55, 47, 39, 31, 23, 15,
            7, 62, 54, 46, 38, 30, 22,
            14, 6, 61, 53, 45, 37, 29,
            21, 13, 5, 28, 20, 12, 4
    };

    private static final int[] PC2 = {
            14, 17, 11, 24, 1, 5, 3, 28,
            15, 6, 21, 10, 23, 19, 12, 4,
            26, 8, 16, 7, 27, 20, 13, 2,
            41, 52, 31, 37, 47, 55, 30, 40,
            51, 45, 33, 48, 44, 49, 39, 56,
            34, 53, 46, 42, 50, 36, 29, 32
    };

    private static final int[] SHIFTS = {
            1, 1, 2, 2, 2, 2, 2, 2, 1, 2, 2, 2, 2, 2, 2, 1
    };

    private static final int[][][] S_BOXES = {
            {
                    {14, 4, 13, 1, 2, 15, 11, 8, 3, 10, 6, 12, 5, 9, 0, 7},
                    {0, 15, 7, 4, 14, 2, 13, 1, 10, 6, 12, 11, 9, 5, 3, 8},
                    {4, 1, 14, 8, 13, 6, 2, 11, 15, 12, 9, 7, 3, 10, 5, 0},
                    {15, 12, 8, 2, 4, 9, 1, 7, 5, 11, 3, 14, 10, 0, 6, 13}
            },
            {
                    {15, 1, 8, 14, 6, 11, 3, 4, 9, 7, 2, 13, 12, 0, 5, 10},
                    {3, 13, 4, 7, 15, 2, 8, 14, 12, 0, 1, 10, 6, 9, 11, 5},
                    {0, 14, 7, 11, 10, 4, 13, 1, 5, 8, 12, 6, 9, 3, 2, 15},
                    {13, 8, 10, 1, 3, 15, 4, 2, 11, 6, 7, 12, 0, 5, 14, 9}
            },
            {
                    {10, 0, 9, 14, 6, 3, 15, 5, 1, 13, 12, 7, 11, 4, 2, 8},
                    {13, 7, 0, 9, 3, 4, 6, 10, 2, 8, 5, 14, 12, 11, 15, 1},
                    {13, 6, 4, 9, 8, 15, 3, 0, 11, 1, 2, 12, 5, 10, 14, 7},
                    {1, 10, 13, 0, 6, 9, 8, 7, 4, 15, 14, 3, 11, 5, 2, 12}
            },
            {
                    {7, 13, 14, 3, 0, 6, 9, 10, 1, 2, 8, 5, 11, 12, 4, 15},
                    {13, 8, 11, 5, 6, 15, 0, 3, 4, 7, 2, 12, 1, 10, 14, 9},
                    {10, 6, 9, 0, 12, 11, 7, 13, 15, 1, 3, 14, 5, 2, 8, 4},
                    {3, 15, 0, 6, 10, 1, 13, 8, 9, 4, 5, 11, 12, 7, 2, 14}
            },
            {
                    {2, 12, 4, 1, 7, 10, 11, 6, 8, 5, 3, 15, 13, 0, 14, 9},
                    {14, 11, 2, 12, 4, 7, 13, 1, 5, 0, 15, 10, 3, 9, 8, 6},
                    {4, 2, 1, 11, 10, 13, 7, 8, 15, 9, 12, 5, 6, 3, 0, 14},
                    {11, 8, 12, 7, 1, 14, 2, 13, 6, 15, 0, 9, 10, 4, 5, 3}
            },
            {
                    {12, 1, 10, 15, 9, 2, 6, 8, 0, 13, 3, 4, 14, 7, 5, 11},
                    {10, 15, 4, 2, 7, 12, 9, 5, 6, 1, 13, 14, 0, 11, 3, 8},
                    {9, 14, 15, 5, 2, 8, 12, 3, 7, 0, 4, 10, 1, 13, 11, 6},
                    {4, 3, 2, 12, 9, 5, 15, 10, 11, 14, 1, 7, 6, 0, 8, 13}
            },
            {
                    {4, 11, 2, 14, 15, 0, 8, 13, 3, 12, 9, 7, 5, 10, 6, 1},
                    {13, 0, 11, 7, 4, 9, 1, 10, 14, 3, 5, 12, 2, 15, 8, 6},
                    {1, 4, 11, 13, 12, 3, 7, 14, 10, 15, 6, 8, 0, 5, 9, 2},
                    {6, 11, 13, 8, 1, 4, 10, 7, 9, 5, 0, 15, 14, 2, 3, 12}
            },
            {
                    {13, 2, 8, 4, 6, 15, 11, 1, 10, 9, 3, 14, 5, 0, 12, 7},
                    {1, 15, 13, 8, 10, 3, 7, 4, 12, 5, 6, 11, 0, 14, 9, 2},
                    {7, 11, 4, 1, 9, 12, 14, 2, 0, 6, 10, 13, 15, 3, 5, 8},
                    {2, 1, 14, 7, 4, 10, 8, 13, 15, 12, 9, 0, 3, 5, 6, 11}
            }
    };

    public interface ProgressCallback {
        void onProgress(double progress);
    }

    public static void encrypt(InputStream input, OutputStream output, byte[] key, ProgressCallback progressCallback) throws IOException {
        processStreamParallel(input, output, key, true, progressCallback);
    }

    public static void decrypt(InputStream input, OutputStream output, byte[] key, ProgressCallback progressCallback) throws IOException {
        processStreamParallel(input, output, key, false, progressCallback);
    }

    private static void processStreamParallel(InputStream input, OutputStream output, byte[] key, boolean encrypt,
                                              ProgressCallback progressCallback) throws IOException {
        long totalSize = -1;
        if (input instanceof FileInputStream) {
            try {
                totalSize = ((FileInputStream) input).getChannel().size();
            } catch (IOException ignored) {}
        }

        long processedBytes = 0;
        int numThreads = Math.max(2, Runtime.getRuntime().availableProcessors() - 1);
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        long[] subKeys = generateSubKeys(key);
        if (!encrypt) {
            reverseSubKeys(subKeys);
        }

        try {
            int bufferSize = BLOCK_SIZE * 1024;
            byte[] dataBuffer = new byte[bufferSize];
            ByteArrayOutputStream resultBuffer = new ByteArrayOutputStream();

            int bytesRead;
            while ((bytesRead = input.read(dataBuffer)) > 0) {
                byte[] chunk = Arrays.copyOf(dataBuffer, bytesRead);
                byte[] result = processChunkParallel(chunk, subKeys, encrypt, executor);
                output.write(result);

                processedBytes += bytesRead;
                if (progressCallback != null && totalSize > 0) {
                    progressCallback.onProgress((double) processedBytes / totalSize);
                }
            }

            if (encrypt && bytesRead % BLOCK_SIZE != 0) {
                byte padValue = (byte) (BLOCK_SIZE - (bytesRead % BLOCK_SIZE));
                byte[] padding = new byte[padValue];
                Arrays.fill(padding, padValue);
                byte[] result = processChunkParallel(padding, subKeys, true, executor);
                output.write(result);
            }
        } finally {
            executor.shutdown();
            try {
                executor.awaitTermination(1, TimeUnit.MINUTES);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private static byte[] processChunkParallel(byte[] data, long[] subKeys, boolean encrypt, ExecutorService executor) throws IOException {
        int numBlocks = data.length / BLOCK_SIZE;
        if (data.length % BLOCK_SIZE != 0) {
            numBlocks++;
        }

        byte[] paddedData;
        if (data.length % BLOCK_SIZE == 0) {
            paddedData = data;
        } else {
            paddedData = new byte[numBlocks * BLOCK_SIZE];
            System.arraycopy(data, 0, paddedData, 0, data.length);
            byte padValue = (byte) (paddedData.length - data.length);
            for (int i = data.length; i < paddedData.length; i++) {
                paddedData[i] = padValue;
            }
        }
        byte[] result = new byte[numBlocks * BLOCK_SIZE];
        List<Future<BlockResult>> futures = new ArrayList<>();
        for (int i = 0; i < numBlocks; i++) {
            final int blockIndex = i;
            futures.add(executor.submit(() -> {
                int offset = blockIndex * BLOCK_SIZE;
                byte[] inputBlock = new byte[BLOCK_SIZE];
                byte[] outputBlock = new byte[BLOCK_SIZE];

                System.arraycopy(paddedData, offset, inputBlock, 0, BLOCK_SIZE);
                processBlock(inputBlock, outputBlock, subKeys);

                return new BlockResult(blockIndex, outputBlock);
            }));
        }

        try {
            for (Future<BlockResult> future : futures) {
                BlockResult blockResult = future.get();
                System.arraycopy(blockResult.data, 0, result, blockResult.index * BLOCK_SIZE, BLOCK_SIZE);
            }
        } catch (InterruptedException | ExecutionException e) {
            throw new IOException("Error processing blocks in parallel", e);
        }
        return result;
    }

    private record BlockResult(int index, byte[] data) {
    }


    private static void processBlock(byte[] input, byte[] output, long[] subKeys) {
        long block = bytesToLong(input);
        block = permute(block, IP, 64);
        int left = (int) (block >> 32);
        int right = (int) (block & 0xFFFFFFFFL);
        for (int i = 0; i < 16; i++) {
            int temp = left;
            left = right;
            right = temp ^ feistel(right, subKeys[i]);
        }
        block = ((long) right << 32) | (left & 0xFFFFFFFFL);
        block = permute(block, FP, 64);
        longToBytes(block, output);
    }


    private static int feistel(int right, long subKey) {
        long expanded = permute(right & 0xFFFFFFFFL, E, 48);
        expanded ^= subKey;
        int result = 0;
        for (int i = 0; i < 8; i++) {
            int sBoxInput = (int) ((expanded >> (42 - i * 6)) & 0x3F);
            int row = ((sBoxInput & 0x20) >> 4) | (sBoxInput & 0x01);
            int col = (sBoxInput >> 1) & 0x0F;
            int sBoxOutput = S_BOXES[i][row][col];
            result = (result << 4) | sBoxOutput;
        }
        return (int) permute(result & 0xFFFFFFFFL, P, 32);
    }

    private static long[] generateSubKeys(byte[] key) {
        long keyBits = bytesToLong(key);
        long permutedKey = permute(keyBits, PC1, 56);
        int c = (int) (permutedKey >> 28);
        int d = (int) (permutedKey & 0x0FFFFFFF);
        long[] subKeys = new long[16];
        for (int i = 0; i < 16; i++) {
            c = ((c << SHIFTS[i]) | (c >>> (28 - SHIFTS[i]))) & 0x0FFFFFFF;
            d = ((d << SHIFTS[i]) | (d >>> (28 - SHIFTS[i]))) & 0x0FFFFFFF;
            long combined = ((long) c << 28) | d;
            subKeys[i] = permute(combined, PC2, 48);
        }
        return subKeys;
    }

    private static void reverseSubKeys(long[] subKeys) {
        for (int i = 0; i < subKeys.length / 2; i++) {
            long temp = subKeys[i];
            subKeys[i] = subKeys[subKeys.length - 1 - i];
            subKeys[subKeys.length - 1 - i] = temp;
        }
    }

    private static long permute(long source, int[] table, int resultSize) {
        long result = 0;
        for (int i = 0; i < table.length; i++) {
            if (((source >> (64 - table[i])) & 1) == 1) {
                result |= (1L << (resultSize - 1 - i));
            }
        }
        return result;
    }


    private static long bytesToLong(byte[] bytes) {
        long result = 0;
        for (int i = 0; i < 8; i++) {
            result = (result << 8) | (bytes[i] & 0xFF);
        }
        return result;
    }


    private static void longToBytes(long value, byte[] bytes) {
        for (int i = 7; i >= 0; i--) {
            bytes[i] = (byte) (value & 0xFF);
            value >>= 8;
        }
    }

    public static byte[] encrypt(byte[] data, byte[] key) throws IOException {
        try (ByteArrayInputStream input = new ByteArrayInputStream(data);
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            encrypt(input, output, key, null);
            return output.toByteArray();
        }
    }

    public static byte[] decrypt(byte[] data, byte[] key) throws IOException {
        try (ByteArrayInputStream input = new ByteArrayInputStream(data);
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            decrypt(input, output, key, null);
            return removePadding(output.toByteArray());
        }
    }

    private static byte[] removePadding(byte[] data) {
        if (data.length == 0) {
            return data;
        }

        byte padValue = data[data.length - 1];
        if (padValue <= 0 || padValue > BLOCK_SIZE) {
            return data;
        }

        for (int i = data.length - padValue; i < data.length; i++) {
            if (data[i] != padValue) {
                return data;
            }
        }

        byte[] result = new byte[data.length - padValue];
        System.arraycopy(data, 0, result, 0, result.length);
        return result;
    }
}