package org.duynguyen.atttclient.network;

import org.duynguyen.atttclient.protocol.Message;

public interface IMessageHandler {

    void onMessage(Message message);

    void onConnectionFail();

    void onDisconnected();

    void onConnectOK();

    void newMessage(Message ms);

    void messageAuth(Message ms);

    void messageNotAuth(Message ms);
}
