package org.duynguyen.atttclient.protocol;

import javafx.application.Platform;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import lombok.Getter;
import lombok.Setter;
import org.duynguyen.atttclient.network.Session;
import org.duynguyen.atttclient.utils.DesUtils;
import org.duynguyen.atttclient.utils.Log;

import java.io.*;
import java.util.Arrays;
import java.util.concurrent.locks.ReentrantLock;

@Getter
public class FileTransfer {
    public static FileTransfer instance;
    private final Session session = Session.getInstance();
    private final String transferId;
    private String fileName;
    private long fileSize;
    private final int senderId;
    private final int receiverId;
    private long bytesTransferred;
    private FileTransferState state;
    private final byte[] keyDes;
    private FileInputStream fileInputStream;
    private FileOutputStream fileOutputStream;
    private File sourceFile;
    private File targetFile;
    private File encryptedFile;
    private long startTime;
    private String estimatedTimeRemaining = "Calculating...";
    public interface ProgressListener {
        void onProgressUpdate(double progress, String estimatedTime);
    }

    @Setter
    private ProgressListener progressListener;

    public interface TransferCompleteListener {
        void onTransferComplete();
        void onTransferFailed(String reason);
    }

    private TransferCompleteListener listener;

    public void setTransferCompleteListener(TransferCompleteListener listener) {
        this.listener = listener;
    }


    private static final int DEFAULT_CHUNK_SIZE = 64 * 1024;
    private final ReentrantLock lock = new ReentrantLock();
    private static final int MAX_RETRY_COUNT = 3;
    private int retryCount = 0;

    public enum FileTransferState {
        INITIALIZED,
        ENCRYPTING,
        TRANSFERRING,
        COMPLETED,
        DECRYPTING,
        FAILED
    }

    public double getProgress() {
        lock.lock();
        try {
            return (double) bytesTransferred / fileSize;
        } finally {
            lock.unlock();
        }
    }

    public FileTransfer(int senderId, int receiverId, String transferId, byte[] keyDes) {
        this.transferId = transferId;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.bytesTransferred = 0;
        this.state = FileTransferState.INITIALIZED;
        this.keyDes = keyDes;
        instance = this;
    }

    public boolean isSender() {
        return senderId == session.id;
    }

    public boolean isReceiver() {
        return receiverId == session.id;
    }

    public void prepareForSending(File file) {
        try {
            this.sourceFile = file;
            this.fileName = file.getName();
            this.fileSize = file.length();
            this.state = FileTransferState.ENCRYPTING;

            this.encryptedFile = encryptFile(file);
            this.fileInputStream = new FileInputStream(encryptedFile);
            this.state = FileTransferState.TRANSFERRING;
        } catch (Exception e) {
            this.state = FileTransferState.FAILED;
            Log.error("Error preparing file for sending: " + e.getMessage());
        }
    }

    public void prepareForReceiving(String fileName, long fileSize) {
        try {
            this.fileName = fileName;
            this.fileSize = fileSize;

            String tempDir = System.getProperty("java.io.tmpdir");
            this.encryptedFile = new File(tempDir, fileName + ".des");
            this.fileOutputStream = new FileOutputStream(encryptedFile);
            this.state = FileTransferState.TRANSFERRING;
        } catch (Exception e) {
            this.state = FileTransferState.FAILED;
            Log.error("Error preparing to receive file: " + e.getMessage());
        }
    }

    public File encryptFile(File file) throws Exception {
        if (file == null || !file.exists()) {
            throw new IllegalArgumentException("File does not exist!");
        }

        String encryptDir = System.getProperty("user.dir") + File.separator + "ATTTClient_Files";
        File directory = new File(encryptDir);
        if (!directory.exists()) {
            directory.mkdirs();
        }

        File encryptedFile = new File(directory, file.getName() + ".des");
        File encryptedTxtFile = new File(directory, file.getName() + ".txt");

        startTime = System.currentTimeMillis();

        try (FileInputStream fis = new FileInputStream(file);
             FileOutputStream fos = new FileOutputStream(encryptedFile)) {
            DesUtils.encrypt(fis, fos, keyDes, this::updateProgress);
        }

        try (FileInputStream desInput = new FileInputStream(encryptedFile);
             FileOutputStream txtOutput = new FileOutputStream(encryptedTxtFile)) {
            byte[] buffer = new byte[8192];
            int bytesRead;

            while ((bytesRead = desInput.read(buffer)) != -1) {
                byte[] chunk = Arrays.copyOf(buffer, bytesRead);
                String base64 = java.util.Base64.getEncoder().encodeToString(chunk);
                txtOutput.write(base64.getBytes());
            }
        }

        Log.info(Arrays.toString(keyDes));

        return encryptedFile;
    }

    private void updateProgress(double progress) {
        if (progress > 0) {
            long elapsedTime = System.currentTimeMillis() - startTime;
            long totalEstimatedTime = (long) (elapsedTime / progress);
            long remainingTime = totalEstimatedTime - elapsedTime;

            estimatedTimeRemaining = formatTime(remainingTime);
        }

        if (progressListener != null) {
            Platform.runLater(() ->
                    progressListener.onProgressUpdate(progress, estimatedTimeRemaining));
        }
    }

    private String formatTime(long millis) {
        if (millis < 1000) {
            return "< 1 second";
        }

        long seconds = millis / 1000;
        long minutes = seconds / 60;
        seconds = seconds % 60;

        if (minutes > 0) {
            return String.format("%d minutes %d seconds", minutes, seconds);
        } else {
            return String.format("%d seconds", seconds);
        }
    }

    public boolean hasMoreChunks() {
        return bytesTransferred < fileSize;
    }

    public byte[] nextChunk() {
        if (!hasMoreChunks() || state != FileTransferState.TRANSFERRING) {
            return null;
        }

        byte[] buffer = new byte[DEFAULT_CHUNK_SIZE];
        try {
            int bytesRead = fileInputStream.read(buffer);
            if (bytesRead == -1) {
                fileInputStream.close();
                return null;
            }
            bytesTransferred += bytesRead;
            retryCount = 0;
            return Arrays.copyOf(buffer, bytesRead);
        } catch (IOException e) {
            Log.error("Error reading file chunk: " + e.getMessage());
            if (++retryCount <= MAX_RETRY_COUNT) {
                Log.info("Retrying to read chunk... Attempt " + retryCount);
                return nextChunk();
            } else {
                state = FileTransferState.FAILED;
                Log.error("Max retry attempts reached for reading chunk.");
                return null;
            }
        }
    }

    public void writeChunk(byte[] data) {
        if (state != FileTransferState.TRANSFERRING) {
            return;
        }
        try {
            fileOutputStream.write(data);
            bytesTransferred += data.length;
            retryCount = 0;

            if (bytesTransferred >= fileSize) {
                fileOutputStream.close();
            }
        } catch (IOException e) {
            Log.error("Error writing file chunk: " + e.getMessage());
            if (++retryCount <= MAX_RETRY_COUNT) {
                Log.info("Retrying to write chunk... Attempt " + retryCount);
                writeChunk(data);
            } else {
                state = FileTransferState.FAILED;
                Log.error("Max retry attempts reached for writing chunk.");
            }
        }
    }

    public void complete() {
        try {
            if (fileInputStream != null) {
                fileInputStream.close();
            }
            if (fileOutputStream != null) {
                fileOutputStream.close();
            }

            if (isReceiver()) {
                this.state = FileTransferState.DECRYPTING;

                Platform.runLater(() -> {
                    try {
                        FileChooser fileChooser = new FileChooser();
                        fileChooser.setTitle("Save Received File");
                        fileChooser.setInitialFileName(fileName);

                        String extension = getFileExtension(fileName);
                        if (!extension.isEmpty()) {
                            FileChooser.ExtensionFilter filter =
                                    new FileChooser.ExtensionFilter(
                                            extension.toUpperCase() + " files", "*." + extension);
                            fileChooser.getExtensionFilters().add(filter);
                        }

                        Stage stage = new Stage();
                        this.targetFile = fileChooser.showSaveDialog(stage);

                        if (targetFile != null) {
                            new Thread(this::decryptToTargetLocation).start();
                        } else {
                            state = FileTransferState.FAILED;
                            if (listener != null) {
                                listener.onTransferFailed("File save canceled by user");
                            }
                        }
                    } catch (Exception e) {
                        state = FileTransferState.FAILED;
                        Log.error("Error selecting save location: " + e.getMessage());
                        if (listener != null) {
                            listener.onTransferFailed(e.getMessage());
                        }
                    }
                });
            } else {
                state = FileTransferState.COMPLETED;
                if (listener != null) {
                    listener.onTransferComplete();
                }
            }
        } catch (IOException e) {
            state = FileTransferState.FAILED;
            Log.error("Error completing file transfer: " + e.getMessage());
            if (listener != null) {
                listener.onTransferFailed(e.getMessage());
            }
        }
    }

    private void decryptToTargetLocation() {
        try {
            startTime = System.currentTimeMillis();

            try (FileInputStream fis = new FileInputStream(encryptedFile);
                 FileOutputStream fos = new FileOutputStream(targetFile)) {
                DesUtils.decrypt(fis, fos, keyDes, this::updateProgress);
                fos.flush();
            }

            if (encryptedFile.exists()) {
                if (encryptedFile.delete()) {
                    Log.info("Encrypted file has been deleted");
                } else {
                    Log.error("Cannot delete encrypted file");
                }
            }

            state = FileTransferState.COMPLETED;

            if (listener != null) {
                Platform.runLater(() -> listener.onTransferComplete());
            }
        } catch (Exception e) {
            state = FileTransferState.FAILED;
            Log.error("Error decrypting file: " + e.getMessage());

            if (listener != null) {
                Platform.runLater(() -> listener.onTransferFailed(e.getMessage()));
            }
        }
    }

    private String getFileExtension(String fileName) {
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex > 0) {
            return fileName.substring(lastDotIndex + 1);
        }
        return "";
    }

    public void cancel() {
        try {
            if (fileInputStream != null) {
                fileInputStream.close();
            }
            if (fileOutputStream != null) {
                fileOutputStream.close();
            }
            if (encryptedFile != null && encryptedFile.exists()) {
                encryptedFile.delete();
            }
            state = FileTransferState.FAILED;
            if (listener != null) {
                listener.onTransferFailed("File transfer has been canceled");
            }
        } catch (IOException e) {
            Log.error("Error canceling file transfer: " + e.getMessage());
        }
    }
}
