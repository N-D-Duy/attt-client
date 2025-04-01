package org.duynguyen.atttclient.network;

import org.duynguyen.atttclient.protocol.FileTransfer;
import org.duynguyen.atttclient.protocol.Message;
import org.duynguyen.atttclient.utils.Log;
import org.duynguyen.atttclient.utils.CMD;

import java.io.DataOutputStream;
import java.io.IOException;

public record Service(Session session) {

    private static final int MAX_RETRY_COUNT = 3;
    private static int retryCount = 0;

    public void sendMessage(Message ms) {
        if (this.session != null) {
            this.session.sendMessage(ms);
        }
    }

    public Message messageAuth(int command) {
        Message ms = new Message(CMD.AUTH);
        try (DataOutputStream ds = ms.writer()) {
            ds.writeByte(command);
            return ms;
        } catch (Exception ex) {
            Log.error(ex);
        }
        return null;
    }

    public Message messageNotAuth(int command) {
        Message ms = new Message(CMD.NOT_AUTH);
        try (DataOutputStream ds = ms.writer()) {
            ds.writeByte(command);
            return ms;
        } catch (Exception ex) {
            Log.error(ex);
        }
        return null;
    }

    public void login(String username, String password) {
        session.username = username;
        Message msg = messageAuth(CMD.LOGIN);
        try {
            assert msg != null;
            try (DataOutputStream ds = msg.writer()) {
                ds.writeUTF(username);
                ds.writeUTF(password);
                ds.flush();
                sendMessage(msg);
                msg.cleanup();
            }
        } catch (Exception ex) {
            Log.error(ex);
        }
    }

    public void sendHandShakeRequest(int receiveId) {
        Message msg = messageNotAuth(CMD.HANDSHAKE_REQUEST);
        if (msg != null) {
            try (DataOutputStream ds = msg.writer()) {
                ds.writeInt(session.id);
                ds.writeInt(receiveId);
                ds.flush();
                sendMessage(msg);
                msg.cleanup();
            } catch (Exception ex) {
                Log.error(ex);
            }
        }
    }

    public void sendFileInfo(String transferId, String fileName, long fileSize) {
        Message msg = messageNotAuth(CMD.FILE_INFO);
        if (msg != null) {
            try (DataOutputStream ds = msg.writer()) {
                ds.writeUTF(transferId);
                ds.writeUTF(fileName);
                ds.writeLong(fileSize);
                ds.flush();
                sendMessage(msg);
                msg.cleanup();
            } catch (Exception ex) {
                Log.error(ex);
            }
        }
    }

    public void sendFileInfoReceived() {
        Message msg = messageNotAuth(CMD.FILE_INFO_RECEIVED);
        if (msg != null) {
            try (DataOutputStream dos = msg.writer()) {
                dos.writeUTF(FileTransfer.instance.getTransferId());
                dos.flush();
                sendMessage(msg);
                msg.cleanup();
            } catch (IOException ex) {
                Log.error("sendFileInfoReceived: " + ex.getMessage());
            }
        }
    }

    public void sendHandShakeResponse(int senderId, boolean accept) {
        Message msg = messageNotAuth(accept ? CMD.HANDSHAKE_ACCEPT : CMD.HANDSHAKE_REJECT);
        if (msg != null) {
            try (DataOutputStream ds = msg.writer()) {
                ds.writeInt(senderId);
                ds.writeInt(session.id);
                ds.flush();
                sendMessage(msg);
                msg.cleanup();
            } catch (Exception ex) {
                Log.error(ex);
            }
        }
    }

    public void sendFileChunk() {
        FileTransfer transfer = FileTransfer.instance;
        if (transfer == null || !transfer.isSender()) {
            return;
        }
        byte[] data = transfer.nextChunk();
        if (data == null) {
            sendFileTransferEnd();
            return;
        }
        Message msg = messageNotAuth(CMD.FILE_CHUNK);
        if (msg != null) {
            try (DataOutputStream ds = msg.writer()) {
                ds.writeUTF(transfer.getTransferId());
                ds.writeInt(data.length);
                ds.write(data);
                ds.flush();
                sendMessage(msg);
                msg.cleanup();
                retryCount = 0;
            } catch (Exception ex) {
                Log.error("Error sending file chunk: " + ex.getMessage());
                if (++retryCount <= MAX_RETRY_COUNT) {
                    Log.info("Retrying to send chunk... Attempt " + retryCount);
                    sendFileChunk();
                } else {
                    Log.error("Max retry attempts reached for sending chunk.");
                    transfer.cancel();
                }
            }
        }
    }

    public void handleFileChunk(byte[] data) {
        FileTransfer transfer = FileTransfer.instance;
        if (transfer == null || !transfer.isReceiver()) {
            return;
        }
        try {
            transfer.writeChunk(data);
            sendChunkAck();
            retryCount = 0;
        } catch (Exception e) {
            Log.error("Error handling file chunk: " + e.getMessage());
            if (++retryCount <= MAX_RETRY_COUNT) {
                Log.info("Retrying to handle chunk... Attempt " + retryCount);
                handleFileChunk(data);
            } else {
                Log.error("Max retry attempts reached for handling chunk.");
                transfer.cancel();
            }
        }
    }

    public void sendChunkAck() {
        Message msg = messageNotAuth(CMD.CHUNK_ACK);
        if (msg != null) {
            try (DataOutputStream ds = msg.writer()) {
                ds.writeUTF(FileTransfer.instance.getTransferId());
                ds.flush();
                sendMessage(msg);
                msg.cleanup();
            } catch (Exception ex) {
                Log.error("Error sending chunk acknowledgment: " + ex.getMessage());
            }
        }
    }

    public void handleChunkAck() {
        FileTransfer transfer = FileTransfer.instance;
        if (transfer == null || !transfer.isSender()) {
            return;
        }
        sendFileChunk();
    }

    public void sendFileTransferEnd() {
        Message msg = messageNotAuth(CMD.FILE_TRANSFER_END);
        if (msg != null) {
            try (DataOutputStream ds = msg.writer()) {
                ds.writeUTF(FileTransfer.instance.getTransferId());
                ds.flush();
                sendMessage(msg);
                msg.cleanup();
            } catch (Exception ex) {
                Log.error("Error sending file transfer end: " + ex.getMessage());
            }
        }
    }

    public void handleFileTransferEnd() {
        FileTransfer transfer = FileTransfer.instance;
        if (transfer == null || !transfer.isReceiver()) {
            return;
        }
        sendFileTransferComplete(transfer.getTransferId());
        transfer.complete();
    }

    public void sendFileTransferComplete(String transferId) {
        Message msg = messageNotAuth(CMD.FILE_TRANSFER_COMPLETE);
        if (msg != null) {
            try (DataOutputStream ds = msg.writer()) {
                ds.writeUTF(transferId);
                ds.flush();
                sendMessage(msg);
                msg.cleanup();
            } catch (Exception ex) {
                Log.error("Error sending file transfer complete: " + ex.getMessage());
            }
        }
    }

    public void cancelFileTransfer(String transferId) {
        Message ms = new Message();
        ms.setCommand(CMD.NOT_AUTH);
        try (DataOutputStream dos = ms.writer()){
            dos.writeByte(CMD.FILE_TRANSFER_CANCEL);
            dos.writeUTF(transferId);
            dos.flush();
            sendMessage(ms);
        } catch (IOException e) {
            Log.error("Error sending cancel transfer request: " + e.getMessage());
        }
    }
}
