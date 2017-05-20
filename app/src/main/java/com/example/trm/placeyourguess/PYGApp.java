package com.example.trm.placeyourguess;

import android.app.Application;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import holders.SocketHolder;
import io.socket.client.Socket;

public class PYGApp extends Application {

    private static Socket mSocket;

    @Override
    public void onCreate() {
        super.onCreate();
        startService(new Intent(this, SocketService.class));
    }

    public static class SocketService extends Service {

        @Override
        public IBinder onBind(Intent intent) {
            return null;
        }

        @Override
        public int onStartCommand(Intent intent, int flags, int startId) {
            Log.d("SocketService", "Service Started");

            mSocket = SocketHolder.getInstance();

            return START_NOT_STICKY;
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            Log.d("SocketService", "Service Destroyed");
        }

        @Override
        public void onTaskRemoved(Intent rootIntent) {
            Log.e("SocketService", "END");

            if (mSocket.connected())
                mSocket.disconnect();

            stopSelf();
        }
    }
}
