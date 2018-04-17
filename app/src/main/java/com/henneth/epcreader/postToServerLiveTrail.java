package com.henneth.epcreader;

import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Random;
import java.util.Scanner;

import javax.net.ssl.HttpsURLConnection;

/**
 * Created by hennethcheng on 12/5/16.
 */
public class postToServerLiveTrail extends AsyncTask<String, Void, String> {

    private String position;
    private int responseCode = 0;

    public interface AsyncResponse {
        void processFinish(String output);
        void throwNoNetworkToast();
        void throwAuthenticationFailedToast();
        void errorMessageToast(String m);
    }

    public AsyncResponse delegate = null;

    public postToServerLiveTrail(AsyncResponse delegate){
        this.delegate = delegate;
    }

    protected String doInBackground(String... params) {

        responseCode = 0;
        String response = "";

        try{
            //if you are using https, make sure to import java.net.HttpsURLConnection
            URL url = new URL(params[0]);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            //you need to encode ONLY the values of the parameters
            String urlParameters="epc=" + params[1] +
                    "&time="+ params[2] +
                    "&uid="+ params[3] +
                    "&name="+ params[4];

//            String epc = params[1];
//            String time = params[2].replace(" ","T");
//            time = "2016-11-18T18:26:36.961";
//            String ckpt_name = params[3];

//            String key = "";
//            String read = "" + rawLength + rawValue + boxId + company + securityNumber + key;
//            String hashedValue = asUnsignedDecimalString(CalculateHash(read));

            Log.d("post", "livetrail post");
            position = params[5];

            byte[] postData = urlParameters.getBytes( "UTF-8" );
            int postDataLength = postData.length;
            conn.setDoOutput(true);
            conn.setInstanceFollowRedirects(false);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setRequestProperty("charset", "utf-8");
            conn.setRequestProperty("Content-Length", Integer.toString(postDataLength ));
            conn.setUseCaches(false);
            DataOutputStream wr = new DataOutputStream(conn.getOutputStream());
            wr.write( postData );

            responseCode = conn.getResponseCode();
            if (responseCode == HttpsURLConnection.HTTP_OK) {
                String line;
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                while ((line = br.readLine()) != null) {
                    response += line;
                }
                br.close();
            } else {
                response = "";
            }

            return response;
        }
        //catch some error
        catch(MalformedURLException error) {
            //Handles an incorrectly entered URL
        }
        catch(SocketTimeoutException error) {
            //Handles URL access timeout.
        }
        catch(IOException error) {
            //Handles input and output errors
        }
        return null;
    }

    protected void onPostExecute(String r){
        if (responseCode != 0){
            if (responseCode == 200 && r.equals("received")) {
                delegate.processFinish(position);
            } else {
                delegate.errorMessageToast(r);
            }
//            String status = "";
//            try {
//                JSONObject obj = new JSONObject(r);
//                status = obj.getString("status");
//                if (responseCode == 200 && status.equals("success")){
//                    delegate.processFinish(position);
//                }
//                if (responseCode == 200 && status.equals("error")){
//                    String m = obj.getString("message");
//                    delegate.errorMessageToast(m);
//                }
//            } catch (Throwable t) {
//                Log.e("My App", "Could not parse malformed JSON: \"" + r + "\"");
//            }
        } else {
            delegate.throwNoNetworkToast();
        }
    }

    // Hash
//    private long CalculateHash(String read){
//
//        long hashedValue = 12090758597L;
//        char[] charArray = read.toCharArray();
//        for (int i = 0; i < charArray.length; i++)
//        {
//            hashedValue += charArray[i];
//            hashedValue *= 19820704817L;
//        }
//        return hashedValue;
//    }

    // Signed to Unsigned
//    private static final BigInteger TWO_64 = BigInteger.ONE.shiftLeft(64);
//    private String asUnsignedDecimalString(long l) {
//        BigInteger b = BigInteger.valueOf(l);
//        if(b.signum() < 0) {
//            b = b.add(TWO_64);
//        }
//        return b.toString();
//    }

    // Get XML tag
//    private String getTagValue(String xml, String tagName){
//        return xml.split("<"+tagName+">")[1].split("</"+tagName+">")[0];
//    }

    // Random integer
//    private int randInt(int min, int max) {
//        Random rand = new Random();
//
//        // nextInt is normally exclusive of the top value,
//        // so add 1 to make it inclusive
//        int randomNum = rand.nextInt((max - min) + 1) + min;
//
//        return randomNum;
//    }
}