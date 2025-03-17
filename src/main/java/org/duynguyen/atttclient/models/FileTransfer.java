package org.duynguyen.atttclient.models;

import lombok.Getter;
import org.duynguyen.atttclient.network.Session;
import org.duynguyen.atttclient.utils.DesUtils;
import org.duynguyen.atttclient.utils.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
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
            Log.error("Lỗi khi chuẩn bị file để gửi: " + e.getMessage());
        }
    }

    public void prepareForReceiving(String fileName, long fileSize) {
        try {
            String appDirectory = System.getProperty("user.dir");
            File targetDirectory = new File(appDirectory, "received_files");

            if (!targetDirectory.exists()) {
                boolean created = targetDirectory.mkdirs();
                if (!created) {
                    Log.error("Không thể tạo thư mục: " + targetDirectory.getAbsolutePath());
                    this.state = FileTransferState.FAILED;
                    return;
                }
            }
            this.fileName = fileName;
            this.fileSize = fileSize;
            this.targetFile = new File(targetDirectory, fileName);
            this.encryptedFile = new File(targetDirectory, fileName + ".des");
            this.fileOutputStream = new FileOutputStream(encryptedFile);
            this.state = FileTransferState.TRANSFERRING;
        } catch (Exception e) {
            this.state = FileTransferState.FAILED;
            Log.error("Lỗi khi chuẩn bị nhận file: " + e.getMessage());
        }
    }

    public File encryptFile(File file) throws Exception {
        if (file == null || !file.exists()) {
            throw new IllegalArgumentException("File không tồn tại!");
        }
        File encryptedFile = new File(file.getParent(), file.getName() + ".des");
        try (FileInputStream fis = new FileInputStream(file);
             FileOutputStream fos = new FileOutputStream(encryptedFile)) {
            DesUtils.encrypt(fis, fos, keyDes);
        }
        return encryptedFile;
    }

    public void decryptFile() {
        try {
            this.state = FileTransferState.DECRYPTING;
            try (FileInputStream fis = new FileInputStream(encryptedFile);
                 FileOutputStream fos = new FileOutputStream(targetFile)) {
                DesUtils.decrypt(fis, fos, keyDes);
            }
            if (encryptedFile.exists()) {
                if (encryptedFile.delete()) {
                    Log.info("File mã hóa đã được xóa");
                } else {
                    Log.error("Không thể xóa file mã hóa");
                }
            }
            this.state = FileTransferState.COMPLETED;
        } catch (Exception e) {
            this.state = FileTransferState.FAILED;
            Log.error("Lỗi khi giải mã file: " + e.getMessage());
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
            return Arrays.copyOf(buffer, bytesRead);
        } catch (IOException e) {
            Log.error("Lỗi khi đọc chunk file: " + e.getMessage());
            state = FileTransferState.FAILED;
            return null;
        }
    }

    public void writeChunk(byte[] data) {
        if (state != FileTransferState.TRANSFERRING) {
            return;
        }
        try {
            fileOutputStream.write(data);
            bytesTransferred += data.length;

            if (bytesTransferred >= fileSize) {
                fileOutputStream.close();
            }
        } catch (IOException e) {
            state = FileTransferState.FAILED;
            Log.error("Lỗi khi ghi chunk file: " + e.getMessage());
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
                decryptFile();
            } else {
                state = FileTransferState.COMPLETED;
                if (listener != null) {
                    listener.onTransferComplete();
                }
            }
        } catch (IOException e) {
            state = FileTransferState.FAILED;
            Log.error("Lỗi khi hoàn thành truyền file: " + e.getMessage());
            if (listener != null) {
                listener.onTransferFailed(e.getMessage());
            }
        }
    }
}