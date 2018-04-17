package com.henneth.epcreader;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.ref.WeakReference;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import rfid.ivrjacku1.IvrJackAdapter;
import rfid.ivrjacku1.IvrJackService;
import rfid.ivrjacku1.IvrJackStatus;

public class MainActivity extends AppCompatActivity implements IvrJackAdapter, postToServerRTS.AsyncResponse, postToServerLiveTrail.AsyncResponse {

    private boolean bFirstLoad = true;
    private ImageView imgPlugout = null;
    private TextView txtStatus = null;
    private TextView txtTotal = null;
    private TextView txtDate = null;

    private TextView lblEPC = null;
    private TextView lblTimes = null;

    private Button btnSetting = null;
    private Button clearScreen;

    private ProgressDialogEx pd;
    private boolean bCancel = false;
    private boolean bOpened = false;
    private MHandler handler = null;
    private Handler handler1 = null;

    public static String DEVICE_ADDRESS = "device_address";
    private static String mConnectedDeviceName = null;
    private static String deviceName;
    private static String ckpt_name;
    private static boolean isConnected;

    // For referencing this activity
    static Activity thisActivity = null;

    // Tag List
    private ArrayAdapter<String> tagAdapter;
    private ArrayAdapter<String> shadowAdapter;
    ListView tagListView;

    // Tracking tag statuses
    ArrayList<String> list = new ArrayList<String>();
    ArrayList<TagRecord> incompleteTagRecord = new ArrayList<TagRecord>();
    ArrayList<TagRecord> fullList = new ArrayList<TagRecord>();
    ArrayList<String> doneList = new ArrayList<>();
    int numOfTags;
//    ArrayList<TagRecord> tagTimeList = new ArrayList<TagRecord>();

    // ConnectivityBroadcastReceiver
    BroadcastReceiver receiver;

    // Others
    private Button btnQuery = null;
    private String cMsg;
    private boolean bSuccess;
    public static IvrJackService reader = null;

    byte[] batteryData = new byte[20];
    public static String batteryLevel = "";

//    private CustomListAdapter seqAdapter;
//    private ArrayList<seqTag> seqArray = new ArrayList<seqTag>();
    private ArrayList<String> tagArray = new ArrayList<String>();
    private boolean bUpdateRequired = false;
    final private int REQUEST_CODE_ASK_PERMISSIONS = 123;

    public static String title = "Disconnected";
    private Menu menu;
    public static String toggleTitle;

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 1: {

                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay!
                    initApp();
                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Toast.makeText(MainActivity.this, "App cannot function without permission to record audio.", Toast.LENGTH_SHORT).show();
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }
    private void requestForRecordAudioPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, 1);
    }
    private boolean checkIfAlreadyhavePermission() {
        int result = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO);
        if (result == PackageManager.PERMISSION_GRANTED) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        int MyVersion = Build.VERSION.SDK_INT;
        if (MyVersion > Build.VERSION_CODES.LOLLIPOP_MR1) {
            if (!checkIfAlreadyhavePermission()) {
                requestForRecordAudioPermission();
                return;
            }
        }

        initApp();
    }
    public void initApp() {
        setContentView(R.layout.activity_get_rfid);
        this.setTitle(title);
        thisActivity = this;

//        epclist = ((ListView)findViewById(R.id.tagListView));
//        epclist.setCacheColorHint(Color.TRANSPARENT);
//        seqAdapter = new CustomListAdapter(this, R.layout.tag_list, this.seqArray);
//        epclist.setAdapter(this.seqAdapter);

        reader = new IvrJackService();
        reader.open(this, this);

        lblEPC = (TextView)findViewById(R.id.textView1);
        lblTimes = (TextView)findViewById(R.id.textView11);

        handler = new MHandler(this);

        tagListView = (ListView) findViewById(R.id.tagListView);
        shadowAdapter = new ArrayAdapter<String>(this, R.layout.tag_list);
        tagAdapter = new CustomAdapter(thisActivity, R.layout.tag_list, list);
        tagListView.setAdapter(tagAdapter);

        try {
            String filename = "shadowAdapter.txt";
            File sdcardPath = Environment.getExternalStorageDirectory();
            File FilePath = new File(sdcardPath, filename);

            Log.d("msgStr", FilePath.toString());

            FileInputStream fis = new FileInputStream(FilePath);
            ObjectInputStream ois = new ObjectInputStream(fis);
            ArrayList<String> returnlist = (ArrayList<String>) ois.readObject();
            ois.close();

            for (String sEpc : returnlist){
                shadowAdapter.add(sEpc);
            }
        } catch (Exception e) {
            Log.d("msgStr", "Fail to get previous shadowAdapter data.");
        }

        try {
            String filename = "fullList.txt";
            File sdcardPath = Environment.getExternalStorageDirectory();
            File FilePath = new File(sdcardPath, filename);

            Log.d("msgStr", FilePath.toString());

            FileInputStream fis = new FileInputStream(FilePath);
            ObjectInputStream ois = new ObjectInputStream(fis);
            ArrayList<TagRecord> returnlist = (ArrayList<TagRecord>) ois.readObject();
            ois.close();
            for (TagRecord t : returnlist){
                fullList.add(t);
            }
        } catch (Exception e) {
            Log.d("msgStr", "Fail to get previous fullList data.");
        }

        try {
            String filename = "tagRecord.txt";
            File sdcardPath = Environment.getExternalStorageDirectory();
            File FilePath = new File(sdcardPath, filename);

            FileInputStream fis = new FileInputStream(FilePath);
            ObjectInputStream ois = new ObjectInputStream(fis);
            ArrayList<String> returnlist = (ArrayList<String>) ois.readObject();
            ois.close();

            numOfTags = returnlist.size();
            for (String tag : returnlist){
                tagAdapter.add(tag);
            }

            for (int i = 0; i < numOfTags; i++){
                doneList.add(String.valueOf(i));
            }
        } catch (Exception e) {
            Log.d("msgStr", "Fail to get previous tagAdapter data.");
        }

        try {
            String filename = "incompleteTagRecord.txt";
            File sdcardPath = Environment.getExternalStorageDirectory();
            File FilePath = new File(sdcardPath, filename);

            FileInputStream fis = new FileInputStream(FilePath.toString());
            ObjectInputStream ois = new ObjectInputStream(fis);
            ArrayList<TagRecord> returnlist = (ArrayList<TagRecord>) ois.readObject();
            ois.close();

            for (TagRecord t : returnlist){
                incompleteTagRecord.add(t);
                doneList.remove(t.position);
            }

            ((CustomAdapter)tagAdapter).setWholeDoneList(doneList);
            ((CustomAdapter)tagListView.getAdapter()).notifyDataSetChanged();

        } catch (Exception e) {
            Log.d("msgStr", "Fail to get previous incompleteTagRecord data.");
        }
        registerConnectivityBroadcastReceiver();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.overflow_menu, menu);
        this.menu = menu;
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
//        logoutUser();
        switch (item.getItemId()) {
            case R.id.battery:

                if (bOpened) {
                    showToast("Please disconnect the reader first.");
                    return true;
                }

                pd = ProgressDialogEx.show(this, "Reading battery level...");
                new Thread(){
                    @Override
                    public void run() {
                        int ret = 0;
                        try {
                            cMsg = "Device communication error, make sure that is plugged.";
                            bSuccess = false;
                            ret = reader.getBatteryBuzzer(batteryData);
                            if (ret == 0) {
                                bSuccess = true;
                            }
                            else if (ret > 2) {
                                cMsg = "Read battery level failure";
                            }
                        } catch (Exception e) {
                            cMsg = "Unknown error.";
                            bSuccess = false;
                        }
                        finally {
                            Log.d("msgStr", String.valueOf(ret));
                        }
                        handler.sendEmptyMessage(200);
                    }}.start();
                return true;
            case R.id.settings:
//                Intent intent = new Intent(this, SettingsActivity.class);
//                startActivity(intent);
                Intent intent = new Intent();
                intent.setClassName(this, "com.henneth.epcreader.SettingsActivity");
                startActivity(intent);
//                toast("settings clicked");
                return true;
            case R.id.disconnect:
                if (reader != null) {
                    toggleConnection();
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onStart() {
        Log.d("onStart", "onStart");
        super.onStart();
    }

    /**
     * 用Handler来更新UI
     */
    static class MHandler extends Handler {
        WeakReference<MainActivity> outerClass;

        MHandler(MainActivity activity) {
            outerClass = new WeakReference<MainActivity>(activity);
        }

        @Override
        public void handleMessage(android.os.Message msg) {
            MainActivity theClass = outerClass.get();
            switch (msg.what) {
                case 1:
                    theClass.pd.dismiss(); // 关闭ProgressDialog
//                    theClass.btnQuery.setEnabled(true);
                    if (theClass.bCancel) break;
                    if (theClass.bSuccess) {
                        theClass.bOpened = !theClass.bOpened;
//                        if (!theClass.bOpened)
//                            theClass.btnQuery.setText(">>>>Start<<<<");
//                        else
//                            theClass.btnQuery.setText(">>>>Stop<<<<");
                    } else {
                        if (theClass.cMsg != null)
                            theClass.showToast(theClass.cMsg);
                    }
                    break;

                case 100:
//                    theClass.seqAdapter.notifyDataSetChanged();
                    //theClass.epclist.setSelection(theClass.epclist.getAdapter().getCount() - 1);
                    theClass.bUpdateRequired = false;
                    break;

                case 104:
                    theClass.txtTotal.setText("Total:" + theClass.tagArray.size());
                    theClass.bUpdateRequired = false;
                    break;

                case 200:
                    // check battery level
                    theClass.pd.dismiss(); // �ر�ProgressDialog
                    if (!theClass.bSuccess && theClass.cMsg != null)
                    {
                        theClass.showToast(theClass.cMsg);
                    }
                    else if (theClass.bSuccess) {
                        theClass.batteryLevel = String.valueOf(theClass.batteryData[0]);
                        Log.d("msgStr", theClass.batteryLevel);
                        theClass.showBatteryLevel();
                    }
                    break;
            }
        }
    }
    private String[] separateEpc(String sEpc) {
        Log.d("toSeparate", "sEpc: " + sEpc);
        if (sEpc.length() > 24) {
            return sEpc.split(";");
        } else {
            String[] epc_list = new String[1];
            epc_list[0] = sEpc;
            return epc_list;
        }
    }

    private boolean checkIfShort(String sEpc, int period) {
        boolean tooshort = false;
        String time = getTime();
        for (TagRecord t : fullList) {
            if (t.sEpc.equals(sEpc)) {
                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
                Date pastTime = null;
                try {
                    pastTime = format.parse(t.time);
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                Date currentTime = null;
                try {
                    currentTime = format.parse(time);
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                if ((currentTime.getTime() - pastTime.getTime()) <= period) {
                    Log.d("time interval", "time interval: " + (currentTime.getTime() - pastTime.getTime()));
                    Log.d("too short", "past: " + pastTime + "now: " + currentTime);
                    tooshort = true;
                } else {
                    Log.d("time interval", "time interval: " + (currentTime.getTime() - pastTime.getTime()));
                    Log.d("not short", "past: " + pastTime + "now: " + currentTime);
                    tooshort = false;
                    t.time = time;
                }
            }
        }
        return tooshort;
    }
    private boolean checkPostion(String sEpc) {
        int latestPosition = -1;
        for(int i=0 ; i<shadowAdapter.getCount() ; i++){
            Object obj = shadowAdapter.getItem(i);
            if (String.valueOf(obj).equals(sEpc)){
                latestPosition = i;
            }
        }
        return (shadowAdapter.getPosition(sEpc) < 0 || shadowAdapter.getCount() - latestPosition > 5);
    }
    private void ListRefresh(String sEpc) {

//        if(shadowAdapter.getPosition(sEpc) > 0 && shadowAdapter.getCount() - shadowAdapter.getPosition(sEpc) < 10){
//            Toast.makeText(thisActivity, sEpc+ " has been scanned recently", Toast.LENGTH_SHORT).show();
//        }
        if (checkIfShort(sEpc, 2000)) {
            return;
        }

        boolean limitTimeOrNot =  PreferenceManager.getDefaultSharedPreferences(this).getBoolean("pref_15_mins", false);
        Log.d("limitTimeOrNot", "limitTimeOrNot: " + limitTimeOrNot);
        if (limitTimeOrNot == true) {
            if (checkIfShort(sEpc, 900000)) {
                return;
            }
        }
        boolean alternationLimitOrNot =  PreferenceManager.getDefaultSharedPreferences(this).getBoolean("pref_10_chips", false);
        Log.d("alternationLimitOrNot", "alternationLimitOrNot: " + alternationLimitOrNot);
        if (alternationLimitOrNot == true) {
            if (!checkPostion(sEpc)) {
                return;
            }
        }

//        int latestPosition = -1;
//        for(int i=0 ; i<shadowAdapter.getCount() ; i++){
//            Object obj = shadowAdapter.getItem(i);
//            Log.d("obj", "obj: " + obj);
//            if (String.valueOf(obj).equals(sEpc)){
//                latestPosition = i;
//            }
//        }

//        Log.d("sEpc", String.valueOf(shadowAdapter.getCount() - shadowAdapter.getPosition(sEpc))); not include self correct
//        if(shadowAdapter.getPosition(sEpc) < 0 || shadowAdapter.getCount() - latestPosition > 10){
//        if(shadowAdapter.getPosition(sEpc) < 0){
        try{
            // save Epc for future checking
            shadowAdapter.add(sEpc);
            // make a backup copy of shadowAdapter
            ArrayList<String> shadowAdapterList = new ArrayList<String>();
            for (int i = 0; i < shadowAdapter.getCount(); i++)
                shadowAdapterList.add(shadowAdapter.getItem(i));
            writeToFile("shadowAdapter.txt", shadowAdapterList);

            // get current time
            String time = getTime();

            // add information to list
            tagAdapter.insert(sEpc + "\n" + time, 0);
            // make a backup copy of tagAdapter
            ArrayList<String> tagAdapterList = new ArrayList<String>();
            for (int i = 0; i < tagAdapter.getCount(); i++)
                tagAdapterList.add(tagAdapter.getItem(i));
            writeToFile("tagRecord.txt", tagAdapterList);

            // get position
            String position = String.valueOf(tagAdapter.getCount()-1);

            // get device ID
            String android_id = Settings.Secure.getString(thisActivity.getContentResolver(), Settings.Secure.ANDROID_ID);

            // get value from settings
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            String ckpt_name = prefs.getString("pref_ckpt_name", "empty");
            String server = prefs.getString("pref_server", "empty");

            Log.d("server", server);
            Log.d("server", ckpt_name);

            // save for resend
            TagRecord tagRecord = new TagRecord(sEpc, time, ckpt_name, android_id, deviceName, position);
            incompleteTagRecord.add(tagRecord);
            writeToFile("incompleteTagRecord.txt", incompleteTagRecord);
            fullList.add(tagRecord);
            writeToFile("fullList.txt", fullList);

            // post to server
            if (server.equals("RTS")) {
                new postToServerRTS(this).execute("http://m.racetimingsolutions.com/rfid-gun", sEpc, time, android_id, ckpt_name, position);
//                } else if (server.equals("Sports Timing")) {
//                    new postToServer(this).execute("http://livetime.sportstiming.dk/LiveTimeService.asmx", sEpc, time, ckpt_name, deviceName, position);
            } else if (server.equals("Live Trail")) {
                String port = prefs.getString("pref_port", "");
                new postToServerLiveTrail(this).execute("http://livetrail.net:" + port + "/rts?c=bvTvMJqxcQn5D2Fk", sEpc, time, android_id, ckpt_name, position);
            } else {
                toast("Please set the destination server.");
            }
        } catch(Exception ex) {
            Toast.makeText(thisActivity,ex.toString()+ "|-" +tagAdapter.getCount() , Toast.LENGTH_SHORT).show();
        }
    }
    public String getTime() {
//        Calendar c = Calendar.getInstance();
//        int second = c.get(Calendar.SECOND);
//        int minute = c.get(Calendar.MINUTE);
//        int hour = c.get(Calendar.HOUR_OF_DAY);
//        return hour + ":" + minute + ":" + second;
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date());
    }
    private void writeToFile(String name, ArrayList list) {
        try {
            File sdcardPath = Environment.getExternalStorageDirectory();
            File FilePath = new File(sdcardPath, name);
            Log.d("msgNew", FilePath.toString());
//            FileOutputStream fos = new FileOutputStream(sdcardPath.toString() + "/" + name);
            FileOutputStream fos = new FileOutputStream(FilePath);
//            FileOutputStream fos = thisActivity.openFileOutput(name, Context.MODE_PRIVATE);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(list);
            oos.close();
            Log.d("msgNew", "Saved to file.");
        } catch (IOException e) {
            Log.e("Exception", "File write failed: " + e.toString());
        }
    }
    public void registerConnectivityBroadcastReceiver(){
        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        receiver = new ConnectivityBroadcastReceiver();
        this.registerReceiver(receiver, filter);
    }

    private class ConnectivityBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
//            boolean noConnectivity = intent.getBooleanExtra(ConnectivityManager
//                    .EXTRA_NO_CONNECTIVITY, false);
//            NetworkInfo info1 = (NetworkInfo) intent.getParcelableExtra(ConnectivityManager
//                    .EXTRA_NETWORK_INFO);
//            NetworkInfo info2 = (NetworkInfo) intent.getParcelableExtra(ConnectivityManager
//                    .EXTRA_OTHER_NETWORK_INFO);
            ConnectivityManager cm =
                    (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);

            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            isConnected = activeNetwork != null &&
                    activeNetwork.isConnectedOrConnecting();

            if (isConnected) {
                sendOneIncompleteTag();
            }
            Log.d("IsConnected", String.valueOf(isConnected));
        }
    }

    private void showToast(String msg) {
        showToast(msg, R.drawable.icon_info, true);
    }

    private void showToast(String msg, int resID, boolean bError) {
        View toastRoot = getLayoutInflater().inflate(R.layout.toast, null);
        Toast toast = new Toast(getApplicationContext());
        toast.setGravity(Gravity.CENTER, 0, 0);
        //if (bError)
        //	toast.setDuration(Toast.LENGTH_LONG);
        //else
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.setView(toastRoot);
        TextView tv = (TextView)toastRoot.findViewById(R.id.toastbox_message);
        tv.setText(msg);
        if (resID > 0) {
            ImageView iv = (ImageView)toastRoot.findViewById(R.id.toastbox_icon);
            iv.setImageResource(resID);
        }
        toast.show();
    }
    private void showToast(String msg, int resID) {
        showToast(msg, resID, false);
    }

    private void showBatteryLevel(){
        AlertDialog.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder = new AlertDialog.Builder(thisActivity, android.R.style.Theme_Material_Dialog_Alert);
        } else {
            builder = new AlertDialog.Builder(thisActivity);
        }
        builder.setTitle("Reader Battery Level")
                .setMessage("Battery level is: " + batteryLevel + "%")
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                })
                .setIcon(android.R.drawable.ic_dialog_info)
                .show();
    }

    /**
     * Toast
     */
    private void toast(String message){
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void processFinish(String position) {
        for (TagRecord t : incompleteTagRecord) {
            if (t.position.equals(position)) {
                incompleteTagRecord.remove(t);
                break;
            }
        }
        writeToFile("incompleteTagRecord.txt", incompleteTagRecord);

        if (!incompleteTagRecord.isEmpty()) {
            TagRecord t = incompleteTagRecord.get(0);

            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            String ckpt_name = prefs.getString("pref_ckpt_name", "empty");
            String server = prefs.getString("pref_server", "empty");

            // post to server
            if (server.equals("RTS")) {
                new postToServerRTS(this).execute("http://m.racetimingsolutions.com/rfid-gun", t.sEpc, t.time, t.android_id, ckpt_name, t.position);
//            } else if (server.equals("Sports Timing")) {
//                new postToServer(this).execute("http://livetime.sportstiming.dk/LiveTimeService.asmx", t.sEpc, t.time, ckpt_name, t.deviceName, t.position);
            } else if (server.equals("Live Trail")) {
                String port = prefs.getString("pref_port", "");
                new postToServerLiveTrail(this).execute("http://livetrail.net:" + port + "/rts?c=bvTvMJqxcQn5D2Fk", t.sEpc, t.time, t.android_id, ckpt_name, t.position);
            } else {
                toast("Please set the destination server.");
            }
//            new postToServer(this).execute("http://m.racetimingsolutions.com/rfid-gun", t.sEpc, t.time, t.android_id, t.deviceName, t.position);
//            new postToServer(this).execute("http://livetime.sportstiming.dk/LiveTimeService.asmx", t.sEpc, t.time, ckpt_name, t.deviceName, t.position);
        }
        ((CustomAdapter)tagAdapter).setDoneList(position);
        ((CustomAdapter)tagListView.getAdapter()).notifyDataSetChanged();
    }

    @Override
    public void throwNoNetworkToast() {
        Toast.makeText(thisActivity, "No Network Connection." , Toast.LENGTH_SHORT).show();
    }

    @Override
    public void throwAuthenticationFailedToast() {
        Toast.makeText(thisActivity, "Authentication Failed." , Toast.LENGTH_SHORT).show();
    }

    public void errorMessageToast(String m) {
        Toast.makeText(thisActivity, m , Toast.LENGTH_SHORT).show();
    }

    public void sendOneIncompleteTag() {
        // get value from settings
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String ckpt_name = prefs.getString("pref_ckpt_name", "empty");
        String server = prefs.getString("pref_server", "empty");

        if (!incompleteTagRecord.isEmpty()) {
            TagRecord t = incompleteTagRecord.get(0);

            // post to server
            if (server.equals("RTS")) {
                new postToServerRTS(this).execute("http://m.racetimingsolutions.com/rfid-gun", t.sEpc, t.time, t.android_id, ckpt_name, t.position);
//            } else if (server.equals("Sports Timing")) {
//                new postToServer(this).execute("http://livetime.sportstiming.dk/LiveTimeService.asmx", t.sEpc, t.time, ckpt_name, t.deviceName, t.position);
            } else if (server.equals("Live Trail")) {
                String port = prefs.getString("pref_port", "");
                new postToServerLiveTrail(this).execute("http://livetrail.net:" + port + "/rts?c=bvTvMJqxcQn5D2Fk", t.sEpc, t.time, t.android_id, ckpt_name, t.position);
            } else {
                toast("Please set the destination server.");
            }
//            new postToServer(this).execute("http://m.racetimingsolutions.com/rfid-gun", t.sEpc, t.time, t.android_id, t.deviceName, t.position);
//            new postToServer(this).execute("http://livetime.sportstiming.dk/LiveTimeService.asmx", t.sEpc, t.time, ckpt_name, t.deviceName, t.position);
        }
    }

    public void toggleConnection() {
        if (!bOpened)
            pd = ProgressDialogEx.show(MainActivity.this, "Connecting...");
        else
            pd = ProgressDialogEx.show(MainActivity.this, "Disconnecting...");
        new Thread(){
            @Override
            public void run() {
                Log.d("try0","try0");
                int ret = 0;
                try {
                    Log.d("try1","try1");
                    cMsg = "Device communication error, make sure that is plugged.";
                    bSuccess = false;
//                    title = "Disconnected";
                    ret = reader.readEPC(!bOpened);
                    if (ret == 0) {
                        Log.d("try2", "try2");
                        bSuccess = true;
                        if (!bOpened) {
                            title = "Ready for scanning";
                        } else {
                            title = "Disconnected";
                        }
                    }
                    else if (ret == -1) {
                        Log.d("try3","try3");
                        cMsg = "Device is running low battery, please charge!";
                    }
                } catch (Exception e) {
                    Log.d("error", "value: " + e);
                    Log.d("try4","try4");
                    cMsg = "Unknown error.";
                    bSuccess = false;
                }
                finally {
                    Log.d("ret","value: " + ret);
//                    Log.d("cMsg","value: " + cMsg);
                    Log.d("finally","finally");
                }
                Message msg = Message.obtain(); // Creates an new Message instance
                msg.obj = title; // Put the string into Message, into "obj" field.
                handler1.sendMessage(msg);
                handler.sendEmptyMessage(1);
            }
        }.start();

        handler1 = new Handler(){
            @Override
            public void handleMessage(Message msg) {
                Log.d("title", "title: " + msg.obj);
                setTitle((String) msg.obj);
                MenuItem overflowMenuItem = menu.findItem(R.id.disconnect);
                if (((String) msg.obj) == "Ready for scanning") {
                    overflowMenuItem.setTitle("Disconnect");
                } else {
                    overflowMenuItem.setTitle("Connect");
                }
            }
        };
    }

    @Override
    public void onConnect(String s) {
        Log.d("onConnect", "onConnect");
//        imgPlugout.setVisibility(View.GONE);
//        btnQuery.setVisibility(View.VISIBLE);
//        btnSetting.setVisibility(View.VISIBLE);
//        txtTotal.setVisibility(View.VISIBLE);
//        lblEPC.setVisibility(View.VISIBLE);
//        lblTimes.setVisibility(View.VISIBLE);
//        clearScreen.setVisibility(View.VISIBLE);
        tagListView.setVisibility(View.VISIBLE);
//        txtStatus.setText("Welcome");
        showToast("Recognized.", R.drawable.toastbox_auth_success);
        title = "Connecting...";
        toggleConnection();
        Message msg = Message.obtain(); // Creates an new Message instance
        msg.obj = title; // Put the string into Message, into "obj" field.
        handler1.sendMessage(msg);
    }
    @Override
    public void onResume() {
        Log.d("onResume","onResume");
        super.onResume();
        tagAdapter.notifyDataSetChanged();
    }
    @Override
    public void onStop() {
        Log.d("onStop","onStop");
        super.onStop();
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d("onDestroy","onDestroy");
        if (reader != null) {
            reader.close();
            Log.i("HEX", "reader close");
        }
        this.unregisterReceiver(receiver);
    }

    @Override
    public void onDisconnect() {
        Log.d("onDisconnect","onDisconnect");
        tagListView.setVisibility(View.INVISIBLE);
        if (!bFirstLoad) {
            showToast("Plugout!", R.drawable.toastbox_remove);
        }
        bFirstLoad = false;
        bCancel = false;
        bOpened = false;

        // title
        title = "Disconnected";
        this.setTitle(title);

        // overflow menu item
        if (menu != null) {
            MenuItem overflowMenuItem = menu.findItem(R.id.disconnect);
            overflowMenuItem.setTitle("Connect");
        }
    }

    @Override
    public void onStatusChange(IvrJackStatus arg0) {
        Log.d("onStatusChange","onStatusChange");
        switch (arg0) {
            case ijsDetecting:
                pd = ProgressDialogEx.show(MainActivity.this, "Detecting...");
                break;

            case ijsRecognized:
                pd.dismiss();
                break;

            case ijsUnRecognized:
                pd.dismiss();
                Toast.makeText(this, "Unrecognized!", Toast.LENGTH_SHORT).show();
                break;
            default:
                break;
        }
    }

    @Override
    public void onInventory(String arg0) {
        Log.d("onInventory","onInventory");
        String[] epc_list = separateEpc(arg0);
        for (String sEpc : epc_list){
            ListRefresh(sEpc);
        }
    }

    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
    }

}

