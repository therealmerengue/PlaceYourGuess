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
                IO.Options options = new IO.Options();
                options.timeout = 500;
                options.reconnectionAttempts = 2;
                mSocket = IO.socket("http://192.168.4.1:8080", options);
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
        }

        return mSocket;
    }
}

//LBN: http://192.168.4.1:8080