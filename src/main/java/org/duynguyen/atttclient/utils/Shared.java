package org.duynguyen.atttclient.utils;

import org.duynguyen.atttclient.protocol.FileTransfer;

import java.util.concurrent.ConcurrentHashMap;

public class Shared {
    public static final ConcurrentHashMap<String, FileTransfer> fileTransferSessions = new ConcurrentHashMap<>();

    public static FileTransfer getFileTransferSession(String transferId) {
        return fileTransferSessions.get(transferId);
    }

    public static void addFileTransferSession(FileTransfer fileTransfer) {
        fileTransferSessions.put(fileTransfer.getTransferId(), fileTransfer);
    }

    public static void removeFileTransferSession(String transferId) {
        fileTransferSessions.remove(transferId);
    }

    public static void clearFileTransferSessions() {
        fileTransferSessions.clear();
    }

    public static boolean containsFileTransferSession(String transferId) {
        return fileTransferSessions.containsKey(transferId);
    }
}
