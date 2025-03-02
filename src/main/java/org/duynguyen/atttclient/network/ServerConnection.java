package org.duynguyen.atttclient.network;

import org.duynguyen.atttclient.models.User;

import java.util.ArrayList;
import java.util.List;

public class ServerConnection {
    public boolean connect(String uuid, String displayName) {
        // Kết nối đến server với UUID và tên hiển thị
        // Đây chỉ là giả định, bạn cần thay thế bằng code thực tế
        return true;
    }

    public List<User> getOnlineUsers() {
        ArrayList<User> users = new ArrayList<>();
        users.add(new User("Alice"));
        users.add(new User("Bob"));
        users.add(new User("Charlie"));
        return users;
    }
}
