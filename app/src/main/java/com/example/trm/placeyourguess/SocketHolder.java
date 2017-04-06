package com.example.trm.placeyourguess;

import java.net.URISyntaxException;

import io.socket.client.IO;
import io.socket.client.Socket;

public class SocketHolder {
    private static Socket mSocket;
    private SocketHolder() {}

    public static Socket getInstance() {
        if (mSocket == null) {
            try {
                mSocket = IO.socket("http://192.168.1.102:8080");
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
        }

        return mSocket;
    }
}
