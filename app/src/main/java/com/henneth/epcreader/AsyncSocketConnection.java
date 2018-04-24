package com.henneth.epcreader;

/**
 * Created by henneth on 20/4/2018.
 */

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import android.util.Log;

/**
 * The AsyncConnection class is an AsyncTask that can be used to open a socket connection with a server and to write/read data asynchronously.
 *
 * The socket connection is initiated in the background thread of the AsyncTask which will stay alive reading data in a while loop
 * until disconnect() method is called from outside or the connection has been lost.
 *
 * When the socket reads data it sends it to the ConnectionHandler via didReceiveData() method in the same thread of AsyncTask.
 * To write data to the server call write() method from outside thread. As the input and output streams are separate there will be no problem with synchronisation.
 *
 * A useful tip: if you wish to avoid connection timeout to happen while the application is inactive try to write some meaningless data periodically as a heartbeat.
 * A useful tip: if you wish to keep the connection alive for longer that the activity  life cycle than consider using services.
 *
 * Created by StarWheel on 10/08/13.
 *
 */
public class AsyncSocketConnection extends android.os.AsyncTask<String, String, List<String>> {
    private String url;
    private int port;
    private int timeout;

    private BufferedReader in;
    private BufferedWriter out;
    private Socket socket;
    public AsyncResponse delegate = null;
    private boolean interrupted = false;
    private String TAG = getClass().getName();

    public interface AsyncResponse {
        void processFinish(String output);
        void toast(String msg);
    }

    public AsyncSocketConnection(String url, int port, int timeout, AsyncResponse delegate) {
        this.url = url;
        this.port = port;
        this.timeout = timeout;
        this.delegate = delegate;
    }

    @Override
    protected void onProgressUpdate(String... values) {
        super.onProgressUpdate(values);
    }

    @Override
    protected void onPostExecute(List<String> list) {
        if (list.get(0).equals("1")) {
            Log.d(TAG, "Finished communication with the socket. Result = " + list.get(1));
            delegate.processFinish(list.get(1));
        } else {
            Log.d(TAG, "Error: " + list.get(1));
            delegate.toast(list.get(1));
        }
    }

    @Override
    protected List<String> doInBackground(String... params) {
        String result = "0";
        List<String> list = new ArrayList<>();
        Exception error = null;

        try {
            Log.d(TAG, "Opening socket connection.");
            socket = new Socket();
            socket.connect(new InetSocketAddress(url, port), timeout);

            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

            this.write(params[0]);

            while(!interrupted) {
                String line = in.readLine();
                if (line != null) {
                    Log.d(TAG, "Received:" + line);
                    result = "1";
                    list.add(result);
                    list.add(line);
                    return list;
                }
            }
        } catch (UnknownHostException ex) {
            Log.e(TAG, "doInBackground(): " + ex.toString());
            error = interrupted ? null : ex;
            result = "2";
        } catch (IOException ex) {
            Log.d(TAG, "doInBackground(): " + ex.toString());
            error = interrupted ? null : ex;
            result = "3";
        } catch (Exception ex) {
            Log.e(TAG, "doInBackground(): " + ex.toString());
            error = interrupted ? null : ex;
            result = "4";
        } finally {
            try {
                socket.close();
                out.close();
                in.close();
            } catch (Exception ex) {}
        }

        list.add(result);
        list.add(error.toString());
        return list;
    }

    public void write(final String data) {
        try {
            Log.d(TAG, "writ(): data = " + data);
            out.write(data + "\n");
            out.flush();
        } catch (IOException ex) {
            Log.e(TAG, "write(): " + ex.toString());
        } catch (NullPointerException ex) {
            Log.e(TAG, "write(): " + ex.toString());
        }
    }

    public void disconnect() {
        try {
            Log.d(TAG, "Closing the socket connection.");

            interrupted = true;
            if(socket != null) {
                socket.close();
            }
            if(out != null & in != null) {
                out.close();
                in.close();
            }
        } catch (IOException ex) {
            Log.e(TAG, "disconnect(): " + ex.toString());
        }
    }
}