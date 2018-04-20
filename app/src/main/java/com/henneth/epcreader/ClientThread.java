package com.henneth.epcreader;

import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import android.os.Handler;
import android.os.Message;

class ClientThread implements Runnable {

    private Socket socket;
    private BufferedReader input;
    private int SERVERPORT;
    private String SERVER_IP;
    private String TAG = "clientThread";
    private String msg;
    private String position;
    private Handler handler;

    ClientThread(int serverport, String serverip, String msg, String position, Handler handler) {
        SERVERPORT = serverport;
        SERVER_IP = serverip;
        this.msg = msg;
        this.position = position;
        this.handler = handler;
    }

    @Override
    public void run() {

        try {
            InetAddress serverAddr = InetAddress.getByName(SERVER_IP);
            socket = new Socket(serverAddr, SERVERPORT);
            Log.i(TAG, "run");

            this.sendMessage(msg);

//            while (!Thread.currentThread().isInterrupted()) {
//
//                Log.i(TAG, "Waiting for message from server...");
//
//                this.input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
//                String message = input.readLine();
//                Log.i(TAG, "Message received from the server : " + message);
//
//                if (null == message || "Disconnect".contentEquals(message)) {
//                    Thread.interrupted();
//                    message = "Server Disconnected.";
//                    Log.i(TAG, message);
//                    break;
//                }
//
//                if (message.equals("received")) {
//                    Message msg = Message.obtain(); // Creates an new Message instance
//                    msg.obj = position; // Put the string into Message, into "obj" field.
//                    msg.setTarget(handler); // Set the Handler
//                    msg.sendToTarget();
//                    Log.i(TAG, message);
//
//                }
//
//            }

        } catch (UnknownHostException e1) {
            e1.printStackTrace();
        } catch (IOException e1) {
            e1.printStackTrace();
        }

    }

    void sendMessage(String message) {
        try {
            if (null != socket) {

                Log.i(TAG, "Send message");
                PrintWriter out = new PrintWriter(new BufferedWriter(
                        new OutputStreamWriter(socket.getOutputStream())),
                        true);
                out.println(message);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }


    }

}