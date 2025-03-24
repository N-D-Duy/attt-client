package org.duynguyen.atttclient.network;
import javafx.application.Platform;
import lombok.Getter;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;

import lombok.Setter;
import org.duynguyen.atttclient.listeners.SaveCredentialsListener;
import org.duynguyen.atttclient.protocol.Message;
import org.duynguyen.atttclient.utils.CMD;
import org.duynguyen.atttclient.utils.Log;

public class Session implements ISession {
    public String username;
    public int id;
    public static Session instance;
    private byte[] key;
    public Socket sc;
    public DataInputStream dis;
    public DataOutputStream dos;
    public IMessageHandler controller;
    @Getter
    private Service service;
    private byte curR, curW;
    private final Sender sender = new Sender();
    private Thread collectorThread;
    protected Thread sendThread;
    public String IPAddress;
    public boolean getKeyCompleted;
    public boolean isClosed;
    public boolean connected;
    public boolean connecting;
    public String host;
    public int port;
    protected Thread initThread;
    @Setter
    @Getter
    public SaveCredentialsListener saveCredentialsListener;

    public Session(String host, int port) {
        this.host = host;
        this.port = port;
        service = new Service(this);
        controller = new Controller(this);
        instance = this;
    }

    public static Session getInstance() {
        if(instance == null) {
            instance = new Session("20.243.124.24", 1609);
        }
        return instance;
    }
    public boolean connect() {
        if(!connected && !connecting){
            getKeyCompleted = false;
            sc = null;
            initThread = new Thread(this::networkInit);
            initThread.start();
        }
        return connected;
    }

    public void networkInit(){
        connecting = true;
        connected = true;
        Thread.currentThread().setPriority(Thread.MAX_PRIORITY);

        try{
            doConnect();
            controller.onConnectOK();
        } catch (Exception e) {
            Log.error(e);
            close();
            controller.onConnectionFail();
        }
    }

    public void doConnect() throws IOException {
        sc = new Socket(host, port);
        sc.setTcpNoDelay(true);
        dos = new DataOutputStream(sc.getOutputStream());
        dis = new DataInputStream(sc.getInputStream());
        IPAddress = sc.getInetAddress().getHostAddress();
        collectorThread = new Thread(new MessageCollector());
        collectorThread.start();
        sendThread = new Thread(sender);
        sendThread.start();
        connected = true;
        connecting = false;
        doSendMessage(new Message(CMD.GET_SESSION_ID));
    }


    @Override
    public boolean isConnected() {
        return connected;
    }

    @Override
    public void setHandler(IMessageHandler messageHandler) {
        this.controller = messageHandler;
    }

    @Override
    public void setService(Service service) {
        this.service = service;
    }

    @Override
    public void sendMessage(Message message) {
        if (connected) {
            sender.addMessage(message);
        }
    }

    private void doSendMessage(Message m) {
        try {
            byte[] data = m.getData();
            byte value = m.getCommand();
            int num = data.length;

            byte b = value;
            if (getKeyCompleted) {
                b = writeKey(value);
            }

            dos.writeByte(b);
            if (getKeyCompleted) {
                dos.writeByte(writeKey((byte) (num >> 24)));
                dos.writeByte(writeKey((byte) (num >> 16)));
                dos.writeByte(writeKey((byte) (num >> 8)));
                dos.writeByte(writeKey((byte) (num & 255)));
            } else {
                dos.writeByte((num >> 24) & 0xFF);
                dos.writeByte((num >> 16) & 0xFF);
                dos.writeByte((num >> 8) & 0xFF);
                dos.writeByte(num & 0xFF);
            }

            if (getKeyCompleted) {
                for (int i = 0; i < num; i++) {
                    data[i] = writeKey(data[i]);
                }
            }
            dos.write(data);
            dos.flush();
        } catch (Exception e) {
            Log.error("doSendMessage err", e);
        }
    }


    public byte readKey(byte b) {
        byte b2 = this.curR;
        this.curR = (byte) (b2 + 1);
        byte result = (byte) ((key[b2] & 255) ^ (b & 255));
        if (this.curR >= key.length) {
            this.curR %= (byte) key.length;
        }
        return result;
    }

    public byte writeKey(byte b) {
        byte b2 = this.curW;
        this.curW = (byte) (b2 + 1);
        byte result = (byte) ((key[b2] & 255) ^ (b & 255));
        if (this.curW >= key.length) {
            this.curW %= (byte) key.length;
        }
        return result;
    }

    @Override
    public void close() {
        cleanNetwork();
    }

    private void cleanNetwork() {
        try {
            key = null;
            curR = 0;
            curW = 0;
            connected = false;
            connecting = false;
            if (sc != null) {
                sc.close();
                sc = null;
            }
            if (dos != null) {
                dos.close();
                dos = null;
            }
            if (dis != null) {
                dis.close();
                dis = null;
            }
            if (sendThread != null) {
                sendThread.interrupt();
                sendThread = null;
            }
            if (collectorThread != null) {
                collectorThread.interrupt();
                collectorThread = null;
            }
            controller = null;
            service = null;
            // System.gc();
        } catch (IOException e) {
            Log.error("cleanNetwork err", e);
        }
    }

    public void getKey(Message message) {
        try (DataInputStream dos = message.reader()){
            byte b = dos.readByte();
            key = new byte[b];
            for(int i = 0; i < b; i++) {
                key[i] = dos.readByte();
            }
            for(int j = 0; j < key.length-1; j++) {
                key[j+1] ^= key[j];
            }
            getKeyCompleted = true;
        } catch (IOException e) {
            Log.error("sendKey err", e);
        }
    }
    public void disconnect() {
        try {
            if (sc != null) {
                sc.close();
                connected = false;
            }
        } catch (Exception e) {
            Log.error("disconnect err", e);
        } finally {
            Platform.runLater(() -> {
                Log.info("Server disconnected. Closing client...");
                Platform.exit();
                System.exit(0);
            });
        }
    }


    public void setName(String name) {
        if (collectorThread != null) {
            collectorThread.setName(name);
        }
        if (sendThread != null) {
            sendThread.setName(name);
        }
    }

    private void processMessage(Message ms) {
        if (!isClosed) {
            controller.onMessage(ms);
        }
    }

    private class Sender implements Runnable {

        private final ArrayList<Message> sendingMessage;

        public Sender() {
            sendingMessage = new ArrayList<>();
        }

        public void addMessage(Message message) {
            sendingMessage.add(message);
        }

        @Override
        public void run() {
            while (connected) {
                if (getKeyCompleted) {
                    while (!sendingMessage.isEmpty()) {
                        try {
                            Message m = sendingMessage.get(0);
                            if (m != null) {
                                doSendMessage(m);
                            }
                            sendingMessage.remove(0);
                        } catch (Exception e) {
                            disconnect();
                            return;
                        }
                    }
                }
                try {
                    Thread.sleep(10L);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }

    class MessageCollector implements Runnable {
        @Override
        public void run() {
            Message message;
            try {
                while (connected) {
                    message = readMessage();
                    if (message != null) {
                        try {
                            if (!getKeyCompleted || message.getCommand() == CMD.GET_SESSION_ID) {
                                getKey(message);
                            } else {
                                processMessage(message);
                            }
                        } catch (Exception e) {
                            Log.error("MessageCollector err", e);
                        }
                    } else {
                        break;
                    }
                }
            } catch (Exception ex) {
            }
            if(!connected){
                return;
            }
            if(sc!=null){
                try {
                    cleanNetwork();
                } catch (Exception e) {
                    Log.error("MessageCollector err", e);
                }
            }
        }

        private Message readMessage() throws Exception {
            try {
                byte cmd = dis.readByte();
                if (getKeyCompleted) {
                    cmd = readKey(cmd);
                }

                int size;
                if (getKeyCompleted) {
                    byte b1 = dis.readByte();
                    byte b2 = dis.readByte();
                    byte b3 = dis.readByte();
                    byte b4 = dis.readByte();
                    size = (readKey(b1) & 255) << 24 | (readKey(b2) & 255) << 16 |
                            (readKey(b3) & 255) << 8  | (readKey(b4) & 255);
                } else {
                    size = (dis.readByte() & 255) << 24 | (dis.readByte() & 255) << 16 |
                            (dis.readByte() & 255) << 8  | (dis.readByte() & 255);
                }

                byte[] data = new byte[size];
                int len = 0;
                int byteRead = 0;
                while (len != -1 && byteRead < size) {
                    len = dis.read(data, byteRead, size - byteRead);
                    if (len > 0) {
                        byteRead += len;
                    }
                }

                if (getKeyCompleted) {
                    for (int i = 0; i < data.length; i++) {
                        data[i] = readKey(data[i]);
                    }
                }
                return new Message(cmd, data);
            } catch (EOFException e) {
                return null;
            }
        }
    }
}


