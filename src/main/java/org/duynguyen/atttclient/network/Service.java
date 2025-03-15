package org.duynguyen.atttclient.network;

import org.duynguyen.atttclient.protocol.Message;
import org.duynguyen.atttclient.utils.Log;
import org.duynguyen.atttclient.utils.CMD;

import java.io.DataOutputStream;

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
}
