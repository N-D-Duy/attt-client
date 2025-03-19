package org.duynguyen.atttclient.network;

import org.duynguyen.atttclient.protocol.FileTransfer;
import org.duynguyen.atttclient.protocol.Message;
import org.duynguyen.atttclient.utils.Log;
import org.duynguyen.atttclient.utils.CMD;

import java.io.DataOutputStream;
import java.io.IOException;

public class Service {
    public final Session session;
    public Service(Session session) {
        this.session = session;
    }

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

    public void login (String username, String password) {
        session.username = username;
        Message msg = messageAuth(CMD.LOGIN);
        try(DataOutputStream ds =  msg.writer()){
            ds.writeUTF(username);
            ds.writeUTF(password);
            ds.flush();
            sendMessage(msg);
            msg.cleanup();
        } catch (Exception ex) {
            Log.error(ex);
        }
    }

    public void sendHandShakeRequest(int receiveId){
        Message msg = messageNotAuth(CMD.HANDSHAKE_REQUEST);
        try(DataOutputStream ds =  msg.writer()){
            ds.writeInt(session.id);
            ds.writeInt(receiveId);
            ds.flush();
            sendMessage(msg);
            msg.cleanup();
        } catch (Exception ex) {
            Log.error(ex);
        }
    }

    public void sendFileInfo(String transferId, String fileName, long fileSize) {
        Message msg = messageNotAuth(CMD.FILE_INFO);
        try(DataOutputStream ds =  msg.writer()){
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

    public void sendFileInfoReceived(){
        Message msg = messageNotAuth(CMD.FILE_INFO_RECEIVED);
        try(DataOutputStream dos = msg.writer()){
            dos.writeUTF(FileTransfer.instance.getTransferId());
            dos.flush();
            sendMessage(msg);
            msg.cleanup();
        } catch (IOException ex){
            Log.error("sendFileInfoReceived: " + ex.getMessage());
        }
    }
    public void sendHandShakeResponse(int senderId, boolean accept){
        Message msg = messageNotAuth(accept ? CMD.HANDSHAKE_ACCEPT : CMD.HANDSHAKE_REJECT);
        try(DataOutputStream ds =  msg.writer()){
            ds.writeInt(senderId);
            ds.writeInt(session.id);
            ds.flush();
            sendMessage(msg);
            msg.cleanup();
        } catch (Exception ex) {
            Log.error(ex);
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
        try (DataOutputStream ds = msg.writer()) {
            ds.writeUTF(transfer.getTransferId());
            ds.writeInt(data.length);
            ds.write(data);
            ds.flush();
            sendMessage(msg);
            msg.cleanup();
        } catch (Exception ex) {
            Log.error("Lỗi khi gửi chunk file: " + ex.getMessage());
        }
    }

    public void handleFileChunk(byte[] data) {
        FileTransfer transfer = FileTransfer.instance;
        if (transfer == null || !transfer.isReceiver()) {
            return;
        }
        transfer.writeChunk(data);
        sendChunkAck();
    }

    public void sendChunkAck() {
        Message msg = messageNotAuth(CMD.CHUNK_ACK);
        try (DataOutputStream ds = msg.writer()) {
            ds.writeUTF(FileTransfer.instance.getTransferId());
            ds.flush();
            sendMessage(msg);
            msg.cleanup();
        } catch (Exception ex) {
            Log.error("Lỗi khi gửi xác nhận chunk: " + ex.getMessage());
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
        try (DataOutputStream ds = msg.writer()) {
            ds.writeUTF(FileTransfer.instance.getTransferId());
            ds.flush();
            sendMessage(msg);
            msg.cleanup();
        } catch (Exception ex) {
            Log.error("Lỗi khi gửi kết thúc truyền file: " + ex.getMessage());
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
        try (DataOutputStream ds = msg.writer()) {
            ds.writeUTF(transferId);
            ds.flush();
            sendMessage(msg);
            msg.cleanup();
        } catch (Exception ex) {
            Log.error("Lỗi khi gửi hoàn thành truyền file: " + ex.getMessage());
        }
    }
    
public void cancelFileTransfer(String transferId) {
    try {
        Message ms = new Message();
        ms.setCommand(CMD.NOT_AUTH);
        DataOutputStream dos = ms.writer();
        dos.writeByte(CMD.FILE_TRANSFER_CANCEL);
        dos.writeUTF(transferId);
        dos.flush();
        sendMessage(ms);
        Log.info("Sending cancel transfer request for " + transferId);
    } catch (IOException e) {
        Log.error("Error sending cancel transfer request: " + e.getMessage());
    }
}
}
