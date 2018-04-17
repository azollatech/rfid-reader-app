package com.henneth.epcreader;

import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Random;
import java.util.Scanner;

/**
 * Created by hennethcheng on 12/5/16.
 */
public class postToServer extends AsyncTask<String, Void, String> {

    private String position;

    public interface AsyncResponse {
        void processFinish(String output);
        void throwNoNetworkToast();
        void throwAuthenticationFailedToast();
    }

    public AsyncResponse delegate = null;

    public postToServer(AsyncResponse delegate){
        this.delegate = delegate;
    }

    protected String doInBackground(String... params) {

        try{
            //if you are using https, make sure to import java.net.HttpsURLConnection
            URL url = new URL(params[0]);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            //you need to encode ONLY the values of the parameters
//            String param="epc=" + URLEncoder.encode(params[1], "UTF-8")+
//                    "&time="+URLEncoder.encode(params[2], "UTF-8")+
//                    "&uid="+URLEncoder.encode(params[3], "UTF-8")+
//                    "&name="+URLEncoder.encode(params[4], "UTF-8");

            String epc = params[1];
            String time = params[2].replace(" ","T");
//            time = "2016-11-18T18:26:36.961";
            String ckpt_name = params[3];

            ArrayList<String> raw = new ArrayList<String>();
            raw.add("event.tag.best tag_id=0x"+epc+", first="+time+", antenna=4, rssi=-729, eventid=0");

            int rawLength = raw.size();
            String rawValue = "";
            if (rawLength > 0) {
                rawValue += raw.get(0);
            }
            if (rawLength > 1) {
                rawValue += raw.get(rawLength - 1);
            }

            String boxId = ckpt_name;
            int company = 3;
            int securityNumber = randInt(1,1000);
            String key = "950a1f40ce3f3fda1695bea415338604";

            String read = "" + rawLength + rawValue + boxId + company + securityNumber + key;
//            Log.i("read", read);
            String hashedValue = asUnsignedDecimalString(CalculateHash(read));

            String body="<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                    "<soap12:Envelope xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:soap12=\"http://www.w3.org/2003/05/soap-envelope\">\n" +
                    "<soap12:Body>\n" +
                    "<AddRawTimes xmlns=\"http://livetime.sportstiming.dk/\">\n" +
                    "<raw>\n" +
                    "<string>" + raw.get(0) + "</string>\n" +
                    "</raw>\n" +
                    "<boxId>"+boxId+"</boxId>\n" +
                    "<company>"+company+"</company>\n" +
                    "<securityNumber>"+securityNumber+"</securityNumber>\n" +
                    "<hash>"+hashedValue+"</hash>\n" +
                    "</AddRawTimes>\n" +
                    "</soap12:Body>\n" +
                    "</soap12:Envelope>";
            Log.i("body", body);

            position = params[5];

            conn.setRequestMethod("POST");
            conn.setDoOutput(true);

            //Android documentation suggested that you set the length of the data you are sending to the server, BUT
            // do NOT specify this length in the header by using conn.setRequestProperty(“Content-Length”, length);
            //use this instead.
            conn.setFixedLengthStreamingMode(body.getBytes().length);
//            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setRequestProperty("Host", "livetime.sportstiming.dk");
            conn.setRequestProperty("Content-Type", "application/soap+xml; charset=utf-8");
//            conn.setRequestProperty("Content-Length", "length");

            //send the POST out
            PrintWriter out = new PrintWriter(conn.getOutputStream());
            out.print(body);
            out.close();

            String responseCode = String.valueOf(conn.getResponseCode());
            Log.i("Response Code", responseCode);

            //build the string to store the response text from the server
            String response= "";

            //start listening to the stream
            Scanner inStream = new Scanner(conn.getInputStream());

            //process the stream and store it in StringBuilder
            while(inStream.hasNextLine())
                response+=(inStream.nextLine());

//            Log.i("Response", response);
            String result;
            try {
                result = getTagValue(response, "AddRawTimesResult");
            }
            catch(Exception error){
                result = "";
            }
            Log.i("result", result);
            return responseCode+result;
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

    protected void onPostExecute(String responseCodeAndResponse){
        if (responseCodeAndResponse != null){
            String responseCode = responseCodeAndResponse.substring(0,3);
            Log.i("responseCode",responseCode);
            String result = responseCodeAndResponse.substring(3);
            Log.i("result",result);
            if (responseCode.equals("200")){
                if (result.equals("AUTHENTICATION FAILED")){
                    delegate.throwAuthenticationFailedToast();
                } else {
                    delegate.processFinish(position);
                }
            }
        } else {
            delegate.throwNoNetworkToast();
        }
    }

    // Hash
    private long CalculateHash(String read){

        long hashedValue = 12090758597L;
        char[] charArray = read.toCharArray();
        for (int i = 0; i < charArray.length; i++)
        {
            hashedValue += charArray[i];
            hashedValue *= 19820704817L;
        }
        return hashedValue;
    }

    // Signed to Unsigned
    private static final BigInteger TWO_64 = BigInteger.ONE.shiftLeft(64);
    private String asUnsignedDecimalString(long l) {
        BigInteger b = BigInteger.valueOf(l);
        if(b.signum() < 0) {
            b = b.add(TWO_64);
        }
        return b.toString();
    }

    // Get XML tag
    private String getTagValue(String xml, String tagName){
        return xml.split("<"+tagName+">")[1].split("</"+tagName+">")[0];
    }

    // Random integer
    private int randInt(int min, int max) {
        Random rand = new Random();

        // nextInt is normally exclusive of the top value,
        // so add 1 to make it inclusive
        int randomNum = rand.nextInt((max - min) + 1) + min;

        return randomNum;
    }
}