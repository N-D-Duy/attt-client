package org.duynguyen.atttclient.network;

import org.duynguyen.atttclient.models.User;
import org.duynguyen.atttclient.presentation.MainController;
import org.duynguyen.atttclient.presentation.StartupController;
import org.duynguyen.atttclient.presentation.ToastMessage;
import org.duynguyen.atttclient.protocol.Message;
import org.duynguyen.atttclient.utils.CMD;
import org.duynguyen.atttclient.utils.Log;

import java.io.DataInputStream;
import java.util.ArrayList;
import java.util.List;

public record Controller(Session session) implements IMessageHandler {

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
                        Log.info("Login OK");
                        if (session.getSaveCredentialsListener() != null) {
                            session.getSaveCredentialsListener().onLoginSuccess();
                        }
                        StartupController.getInstance().onLoginSuccess(extractUsers(ms));
                        break;
                    case CMD.UPDATE_USER_LIST:
                        Log.info("Updating user list");
                        MainController.getInstance().setUsers(extractUsers(ms));
                        break;
                    case CMD.REGISTER_OK:
                        break;
                    default:
                        Log.info("default: " + command);
                        break;
                }
            } catch (Exception e) {
                Log.error("messageNotLogin: " + e.getMessage());
            }
        }
    }

    @Override
    public void messageNotAuth(Message ms) {
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

}
