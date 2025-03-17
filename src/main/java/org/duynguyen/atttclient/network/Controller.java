package org.duynguyen.atttclient.network;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import lombok.Setter;
import org.duynguyen.atttclient.models.FileTransfer;
import org.duynguyen.atttclient.models.User;
import org.duynguyen.atttclient.presentation.MainController;
import org.duynguyen.atttclient.presentation.StartupController;
import org.duynguyen.atttclient.presentation.widgets.FileTransferDialog;
import org.duynguyen.atttclient.presentation.widgets.ToastMessage;
import org.duynguyen.atttclient.protocol.Message;
import org.duynguyen.atttclient.utils.CMD;
import org.duynguyen.atttclient.utils.Log;

import java.io.DataInputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class Controller implements IMessageHandler {
    @Setter
    public Service service;
    public Session session;

    public Controller(Session session) {
        this.session = session;
        this.service = session.getService();
    }

    @Override
    public void onMessage(Message mss) {
        if (mss != null) {
            try {
                int command = mss.getCommand();
                switch (command) {
                    case CMD.NOT_AUTH:
                        messageNotAuth(mss);
                        break;
                    case CMD.AUTH:
                        messageAuth(mss);
                        break;
                    case CMD.SERVER_DIALOG:
                        StartupController.getInstance().showDialog("SERVER ALERT", mss.reader().readUTF());
                        break;
                    case CMD.SERVER_MESSAGE:
                        ToastMessage.showMessage(mss.reader().readUTF());
                        break;
                    default:
                        Log.info("CMD: " + mss.getCommand());
                        break;
                }
            } catch (Exception e) {
                Log.error("onMessage: " + e.getMessage());
            }
        } else {
            Log.info("message is null");
        }
    }

    @Override
    public void onConnectionFail() {
        Log.info("Connection failed");
    }

    @Override
    public void onDisconnected() {
        Log.info("Disconnected");
    }

    @Override
    public void onConnectOK() {
        Log.info("Connected");
    }

    @Override
    public void newMessage(Message ms) {

    }

    @Override
    public void messageAuth(Message ms) {
        if (ms != null) {
            try (DataInputStream dis = ms.reader()) {
                byte command = dis.readByte();
                switch (command) {
                    case CMD.LOGIN_OK:
                        if (session.getSaveCredentialsListener() != null) {
                            session.getSaveCredentialsListener().onLoginSuccess();
                        }
                        StartupController.getInstance().onLoginSuccess(extractUsers(ms));
                        break;
                    case CMD.UPDATE_USER_LIST:
                        MainController.getInstance().setUsers(extractUsers(ms));
                        break;
                    case CMD.REGISTER_OK:
                        break;
                    default:
                        Log.info("default: " + command);
                        break;
                }
            } catch (Exception e) {
                Log.error("message auth: " + e.getMessage());
            }
        }
    }

    @Override
    public void messageNotAuth(Message ms) {
        if (ms != null) {
            try (DataInputStream dis = ms.reader()) {
                byte command = dis.readByte();
                switch (command) {
                    case CMD.HANDSHAKE_REQUEST:
                        int senderId = dis.readInt();
                        String fromUser = MainController.getInstance().getUsers().stream()
                                .filter(user -> user.id() == senderId)
                                .findFirst()
                                .map(User::username)
                                .orElse("Unknown");
                        Platform.runLater(() -> showHandshakeDialog(fromUser, senderId));
                        break;
                    case CMD.HANDSHAKE_SUCCESS:
                        try (DataInputStream _dis = ms.reader()) {
                            String _transferId = _dis.readUTF();
                            int _senderId = _dis.readInt();
                            int _receiverId = _dis.readInt();
                            int keySize = _dis.readInt();
                            byte[] keyDes = new byte[keySize];
                            for (int i = 0; i < keySize; i++) {
                                keyDes[i] = _dis.readByte();
                            }
                            new FileTransfer(_senderId, _receiverId, _transferId, keyDes);
                            if (_senderId == session.id) {
                                MainController.getInstance().onHandshakeSuccess();
                            }
                        }
                        break;
                    case CMD.HANDSHAKE_REJECT:
                        MainController.getInstance().onHandshakeFailed();
                        break;
                    case CMD.FILE_INFO:
                        try (DataInputStream _dis = ms.reader()) {
                            String transferId = _dis.readUTF();
                            String fileName = _dis.readUTF();
                            long fileSize = _dis.readLong();
                            Log.info("file info: " + fileName + " " + fileSize);
                            FileTransfer fileTransfer = FileTransfer.instance;
                            fileTransfer.prepareForReceiving(fileName, fileSize);

                            Platform.runLater(() -> {
                                FileTransferDialog.getInstance()
                                        .show(fileTransfer)
                                        .onCancel(transfer -> {
                                            service.cancelFileTransfer(transfer.getTransferId());
                                        })
                                        .onComplete(transfer -> {
                                            ToastMessage.showMessage("File received: " + transfer.getFileName());
                                        });
                            });
                            service.sendFileInfoReceived();
                        }
                        break;
                    case CMD.FILE_INFO_RECEIVED:
                        try (DataInputStream _dis = ms.reader()) {
                            String transferId = _dis.readUTF();
                            Platform.runLater(() -> {
                                FileTransferDialog.getInstance()
                                        .show(FileTransfer.instance)
                                        .onCancel(transfer -> {
                                            service.cancelFileTransfer(transfer.getTransferId());
                                        })
                                        .onComplete(transfer -> {
                                            ToastMessage.showMessage("File sent successfully: " + transfer.getFileName());
                                        });
                            });
                            service.sendFileChunk();
                        }
                        break;
                    case CMD.FILE_CHUNK:
                        try (DataInputStream _dis = ms.reader()) {
                            String transferId = _dis.readUTF();
                            int size = _dis.readInt();
                            byte[] chunk;
                            chunk = _dis.readNBytes(size);
                            service.handleFileChunk(chunk);
                        }
                        break;
                    case CMD.CHUNK_ACK:
                        service.handleChunkAck();
                        break;
                    case CMD.FILE_TRANSFER_COMPLETE:
                        try (DataInputStream _dis = ms.reader()) {
                            String transferId = _dis.readUTF();
                            Log.info("File transfer complete: " + transferId);
                            if (FileTransfer.instance != null &&
                                    FileTransfer.instance.getTransferId().equals(transferId) &&
                                    FileTransfer.instance.isSender()) {
                                FileTransfer.instance.complete();
                            }
                        } catch (Exception e) {
                            Log.error("Error processing FILE_TRANSFER_COMPLETE: " + e.getMessage());
                        }
                        break;
                    case CMD.FILE_TRANSFER_END:
                        Log.info("File transfer end");
                        service.handleFileTransferEnd();
                        break;
                    case CMD.CHUNK_ERROR:
                        Log.info("Chunk error");
                        break;
                    case CMD.FILE_TRANSFER_CANCEL:
                        Log.info("File transfer cancel");
                        break;
                    default:
                        Log.info("default: " + command);
                        break;
                }
            } catch (Exception e) {
                Log.error("message not auth: " + e.getMessage());
            }
        }
    }

    private List<User> extractUsers(Message ms) {
        try (DataInputStream dis = ms.reader()) {
            List<User> users = new ArrayList<>();
            try {
                int size = dis.readInt();
                for (int i = 0; i < size; i++) {
                    int id = dis.readInt();
                    String username = dis.readUTF();
                    users.add(new User(id, username));
                    if (session.username.equals(username)) {
                        session.id = id;
                    }
                }
            } catch (Exception e) {
                Log.error("extractUsers: " + e.getMessage());
            }
            return users;
        } catch (Exception e) {
            Log.error("extractUsers: " + e.getMessage());
        }
        return null;
    }

    private void showHandshakeDialog(String fromUser, int senderId) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Handshake Request");
        alert.setHeaderText("New handshake request from " + fromUser);
        alert.setContentText("Do you want to accept the request?");

        ButtonType acceptButton = new ButtonType("Accept", ButtonBar.ButtonData.OK_DONE);
        ButtonType rejectButton = new ButtonType("Reject", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(acceptButton, rejectButton);

        Optional<ButtonType> result = alert.showAndWait();
        sendHandshakeResponse(senderId, result.isPresent() && result.get() == acceptButton);
    }

    private void sendHandshakeResponse(int senderId, boolean accept) {
        session.getService().sendHandShakeResponse(senderId, accept);
    }
}
