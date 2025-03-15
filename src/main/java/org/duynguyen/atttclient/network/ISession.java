package org.duynguyen.atttclient.network;

import org.duynguyen.atttclient.protocol.Message;

public interface ISession {

    boolean isConnected();

    void setHandler(IMessageHandler messageHandler);

    void setService(Service service);

    void sendMessage(Message message);

    void close();

}
