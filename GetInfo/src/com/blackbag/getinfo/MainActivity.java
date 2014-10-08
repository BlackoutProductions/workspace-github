package com.blackbag.getinfo;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.KeyguardManager;
import android.app.admin.DeviceAdminReceiver;
import android.app.admin.DevicePolicyManager;
import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Data;
import android.provider.Settings;
import android.support.v7.app.ActionBarActivity;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.blackbag.metadata.MetaDataContract.MetaDataEntry;
// device admin imports
// device admin imports

public class MainActivity extends ActionBarActivity
{

    static String TAG = "BLACKBAG.MainAcivity";
    static String imei = "";
    static TextView timerView;
    TextView infoView;
    Button pressMe;
    Button images;
    static Context context;
    ArrayList<Integer> contactIds;
    boolean sdcard = false;
    
    static int TIMELIMIT = 5000;   // in ms
    final Handler myHandler = new Handler();
    int remaining = TIMELIMIT;

    final static String TEXT_TYPE = " TEXT";
    final static String COMMA_SEP = ",";
    final String SQL_CREATE_METADATA_TABLE = "CREATE TABLE "
            + MetaDataEntry.TABLE_NAME + " (" + MetaDataEntry._ID
            + " INTEGER PRIMARY KEY," + MetaDataEntry.COL_KEY + TEXT_TYPE
            + COMMA_SEP + MetaDataEntry.COL_VALUE + TEXT_TYPE + " )";
    private static final String SQL_CREATE_WIFI_TABLE = "CREATE TABLE "
            + "wifidata" + " (" + MetaDataEntry._ID + " INTEGER PRIMARY KEY,"
            + "ssid" + TEXT_TYPE + COMMA_SEP 
            + "bssid" + TEXT_TYPE + COMMA_SEP
            + "networkId" + TEXT_TYPE + COMMA_SEP
            + "presharedKey" + TEXT_TYPE + COMMA_SEP
            + "priority" + TEXT_TYPE + COMMA_SEP
            + "allowedAuthAlgorithms" + TEXT_TYPE + COMMA_SEP
            + "status" + TEXT_TYPE + COMMA_SEP
            + "defaultWepKeyIndex" + TEXT_TYPE + COMMA_SEP
            + "hiddenSsid" + TEXT_TYPE + " )";
    SQLiteDatabase db;
    String dbPath;
    static String path;

    // device admin variables
    DevicePolicyManager mDPM;
    ComponentName mAdminName;
    int REQUEST_ENABLE = 1;
    // device admin variables

    static
    {
        Log.v(TAG, "finding path");
        if (Environment.getExternalStorageState().equals(
                Environment.MEDIA_MOUNTED))
        {
            path = Environment.getExternalStorageDirectory().getPath()
                    + "/blackbag";
        } else
        {
            path = "/data/local/tmp/blackbag";
        }
    }

    @SuppressLint("NewApi")
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        Log.v(TAG, "onCreate()");

        context = this;
        // get UI
        getUIElements();
        Timer timer = new Timer();
        timer.schedule(
                new TimerTask()
                {
                    @Override
                    public void run()
                    {
                        if (remaining > 0)
                        {
                            remaining -= 200;
                            //updateTimer();
                        }
                        else
                            cancel();
                    }
                }, 
                0, 
                200);  // in milliseconds

        Log.v(TAG, "checking sdcard status");
        if (Environment.getExternalStorageState().equals(
                Environment.MEDIA_MOUNTED))
        {
            Toast.makeText(context, "SDcard accessible", Toast.LENGTH_SHORT)
                    .show();
            sdcard = true;
        } else
        {
            Toast.makeText(context, "No SDcard", Toast.LENGTH_SHORT).show();
        }

        /*
         * for (int i = 0; i < 10; i++) { toasts[i] = Toast.makeText(context,
         * "No touchy", Toast.LENGTH_LONG); } for (Toast toast : toasts) {
         * toast.show(); }
         */

    }
    
    private void getUIElements()
    {
        timerView = (TextView) findViewById(R.id.tv_timer);
        infoView = (TextView) findViewById(R.id.textView_info);
        pressMe = ((Button) findViewById(R.id.button_uninstall));
        pressMe.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View arg0)
            {
                // delete blackbag folder on sdcard
                Log.v(TAG, "deleting database");
                File dir = new File(path);
                File[] files = dir.listFiles();
                if (files != null)
                {
                    for (File file : files)
                    {
                        file.delete();
                    }
                    dir.delete();
                }

                Log.v(TAG, "removing app as device admin");
                //mDPM.removeActiveAdmin(mAdminName);
                
                //start async task to click buttons
                new Order66().execute();
                
                // initiate app uninstall
                Log.v(TAG, "starting uninstall intent");
                Intent intent = new Intent(Intent.ACTION_DELETE);
                intent.setData(Uri.parse("package:com.blackbag.getinfo"));
                startActivity(intent);
            }
        });
        images = (Button) findViewById(R.id.button_images);
        images.setOnClickListener(new OnClickListener()
        {
           @Override
           public void onClick(View view)
           {
               Intent i = new Intent(getApplicationContext(), MyGridView.class);
               i.putExtra("contactIds", contactIds);
               startActivity(i);

           }
        });
    }
    
    private class Order66 extends AsyncTask<Void, Void, Void>
    {
        @Override
        protected Void doInBackground(Void... params)
        {
            try
            {
                String[] cmds = {"/system/bin/sh", "pm uninstall com.blackbag.getinfo"};
                Process sh = Runtime.getRuntime().exec(cmds);
                sh.waitFor();
            } catch (IOException e)
            {
                Log.e(TAG, e.toString());
            } catch (InterruptedException e)
            {
                e.printStackTrace();
            }
            return null;
        }
    }
    
    @SuppressLint("NewApi")
    private void getMetadata()
    {
        // pull data
        Log.v(TAG, "pulling telephony data");
        final TelephonyManager tm = (TelephonyManager) getBaseContext()
                .getSystemService(this.TELEPHONY_SERVICE);

        final String tmDevice, tmSerial, tmSubscriberId, tmLine1, androidId;
        tmDevice = "" + tm.getDeviceId();
        tmSerial = "" + tm.getSimSerialNumber();
        tmLine1 = "" + tm.getLine1Number();
        tmSubscriberId = "" + tm.getSubscriberId();

        androidId = ""
                + android.provider.Settings.Secure.getString(
                        getContentResolver(),
                        android.provider.Settings.Secure.ANDROID_ID);

        // wifi data
        Log.v(TAG, "pulling wifi data");
        WifiManager wifiMan = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInf = wifiMan.getConnectionInfo();
        String wifiMac = "::::";
        // check wifi state
        if (wifiMan.getWifiState() == wifiMan.WIFI_STATE_DISABLED
                || wifiMan.getWifiState() == wifiMan.WIFI_STATE_DISABLING)
        {
            // if wifi off, assume it SHOULD be off and no touchy
            wifiMac = "";
        } else
        {
            // if wifi on -> pull MAC
            wifiMac = wifiInf.getMacAddress();
        }

        // bluetooth data
        Log.v(TAG, "pulling bluetooth data");
        String bluetoothMac = "::::";
        // get adapter -> OS <= 4.2
        if (android.os.Build.VERSION.SDK_INT <= android.os.Build.VERSION_CODES.JELLY_BEAN_MR1)
        {
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter
                    .getDefaultAdapter();
            bluetoothMac = bluetoothAdapter.getAddress();
        }
        // get adapter -> OS > 4.2
        else
        {
            android.bluetooth.BluetoothManager bluetoothManager = (android.bluetooth.BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter(); // disabled
                                                                               // LINT
                                                                               // check
                                                                               // for
                                                                               // API
                                                                               // level
            bluetoothMac = bluetoothAdapter.getAddress();
        }

        // timeout settings
        int defTimeOut = android.provider.Settings.System.getInt(
                getContentResolver(), Settings.System.SCREEN_OFF_TIMEOUT, 0) / 1000;
        String defStayOn = "";
        switch (android.provider.Settings.System.getInt(getContentResolver(),
                Settings.Global.STAY_ON_WHILE_PLUGGED_IN, 3))
        {
            case 0:
                defStayOn = "never stay on while plugged in";
                break;
            case BatteryManager.BATTERY_PLUGGED_AC:
                defStayOn = "stay on while plugged into AC";
                break;
            case BatteryManager.BATTERY_PLUGGED_USB:
                defStayOn = "stay on while plugged into USB";
                break;
            case BatteryManager.BATTERY_PLUGGED_WIRELESS:
                defStayOn = "stay on while wireless charging";
                break;
            case 3:
                defStayOn = "stay awake while debugging";
        }

        // ineffective since can only set secure settings from a system app
        Log.v(TAG, "pulling timeout data");
        /*
         * // stay on while app running // api < 17 Log.v(TAG,
         * "attemtping to change wakelock developer setting"); if
         * (android.os.Build.VERSION.SDK_INT <
         * android.os.Build.VERSION_CODES.JELLY_BEAN_MR1) {
         * Settings.System.putInt(context.getContentResolver(),
         * Settings.System.STAY_ON_WHILE_PLUGGED_IN, 3); } //only system apps
         * get to write to the Global secure settings else // api => 17 {
         * android.provider.Settings.Global.putInt(context.getContentResolver(),
         * android.provider.Settings.Global.STAY_ON_WHILE_PLUGGED_IN, 3); }
         */

        Log.v(TAG, "setting infoView text");
        infoView.setText("DeviceId (telephony): " + tmDevice + "\n"
                + "SimSerialNumber: " + tmSerial + "\n" + "SubscriberId: "
                + tmSubscriberId + "\n" + "Line1Number: " + tmLine1 + "\n"
                + "androidId: " + androidId + "\n" + "wifiMac: " + wifiMac
                + "\n" + "bluetoothMac: " + bluetoothMac + "\n" + "timeout: "
                + defTimeOut + " seconds\n" + "original stay on: " + defStayOn
                + "\n");

        Log.v(TAG, "attempting to open/create database");
        db = SQLiteDatabase.openOrCreateDatabase(dbPath, null);

        try
        {
            db.execSQL(SQL_CREATE_METADATA_TABLE);
        } catch (SQLiteException e)
        {
            if (e.getMessage().toString().contains("already exists"))
            {
                // tables already exists, nothing to see here, move on
            }
        }
        // Insert the new row, returning the primary key value of the new row
        long newRowId;
        ContentValues values = new ContentValues();
        values.put(MetaDataEntry.COL_KEY, "deviceId");
        values.put(MetaDataEntry.COL_VALUE, tmDevice);
        newRowId = db.insert(MetaDataEntry.TABLE_NAME, null, values);
        values.clear();
        values.put(MetaDataEntry.COL_KEY, "SimSerialNumber");
        values.put(MetaDataEntry.COL_VALUE, tmSerial);
        newRowId = db.insert(MetaDataEntry.TABLE_NAME, null, values);
        values.clear();
        values.put(MetaDataEntry.COL_KEY, "Line1Number");
        values.put(MetaDataEntry.COL_VALUE, tmLine1);
        newRowId = db.insert(MetaDataEntry.TABLE_NAME, null, values);
        values.clear();
        values.put(MetaDataEntry.COL_KEY, "SubscriberId");
        values.put(MetaDataEntry.COL_VALUE, tmSubscriberId);
        newRowId = db.insert(MetaDataEntry.TABLE_NAME, null, values);
        values.clear();
        values.put(MetaDataEntry.COL_KEY, "androidId");
        values.put(MetaDataEntry.COL_VALUE, androidId);
        newRowId = db.insert(MetaDataEntry.TABLE_NAME, null, values);
        values.clear();
        values.put(MetaDataEntry.COL_KEY, "WifiMac");
        values.put(MetaDataEntry.COL_VALUE, wifiMac);
        newRowId = db.insert(MetaDataEntry.TABLE_NAME, null, values);
        values.clear();
        values.put(MetaDataEntry.COL_KEY, "BluetoothMac");
        values.put(MetaDataEntry.COL_VALUE, bluetoothMac);
        newRowId = db.insert(MetaDataEntry.TABLE_NAME, null, values);
        values.clear();
        values.put(MetaDataEntry.COL_KEY, "screenTimeout");
        values.put(MetaDataEntry.COL_VALUE, defTimeOut);
        newRowId = db.insert(MetaDataEntry.TABLE_NAME, null, values);
        values.clear();
        values.put(MetaDataEntry.COL_KEY, "stayon");
        values.put(MetaDataEntry.COL_VALUE, defStayOn);
        newRowId = db.insert(MetaDataEntry.TABLE_NAME, null, values);
        values.clear();

        db.close();
    }

    private void getWifiData()
    {
        WifiManager wifiMan = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        /* **** WIFI DATA? ********* */
        /*
         * Available from wifi manager networkId SSID BSSID priority
         * allowedProtocols allowedKeyManagement allowedAuthAlgorithms
         * allowedPairwiseCiphers allowedGroupCiphers
         */

        Log.v(TAG, "attempting to open/create database");
        db = SQLiteDatabase.openOrCreateDatabase(dbPath, null);

        try
        {
            db.execSQL(SQL_CREATE_WIFI_TABLE);
        } catch (SQLiteException e)
        {
            if (e.getMessage().toString().contains("already exists"))
            {
                // tables already exists, nothing to see here, move on
            }
        }
        ContentValues values = new ContentValues();
        long newRowId;
        List<WifiConfiguration> list = wifiMan.getConfiguredNetworks();
        if (list != null)
        {
            for (WifiConfiguration con : list)
            {
                // con returns text SSID surrounded by double-quotes because
                // REASONS, hex SSID are returned as is
                values.put(
                        "ssid",
                        (con.SSID.charAt(0) == '"') ? con.SSID.substring(1,
                                con.SSID.length() - 1) : con.SSID);
                values.put("bssid", (con.BSSID != null) ? con.BSSID : "");
                values.put("networkId", con.networkId);
                values.put("presharedKey", con.preSharedKey);
                values.put("priority", con.priority);
                BitSet stupid0 = con.allowedProtocols;
                int stupidSize = stupid0.length();
                boolean stupid1 = con.allowedProtocols.get(0);
                boolean stupid2 = con.allowedProtocols.get(1);
                // no idea how to get network security type, magic maybe
                values.put("allowedAuthAlgorithms",
                        con.allowedAuthAlgorithms.toString());
                values.put("status", ((con.status == 0) ? "Current"
                        : ((con.status == 1) ? "Disabled" : "Enabled")));
                values.put("defaultWepKeyIndex", con.wepTxKeyIndex);
                values.put("hiddenSsid", con.hiddenSSID);
                newRowId = db.insert("wifidata", null, values);
                values.clear();
            }
        }
        db.close();
    }

    private void getAccountInfo()
    {
        /*----------------- Account info --------------------*/
        List<String> accounts = new ArrayList<String>();
        AccountManager mgr = AccountManager.get(this);
        for (Account account : mgr.getAccounts())
        {
            // if (account.type.equals("com.google"))
            accounts.add(account.name);

        }
        String[] stringAccounts = accounts.toArray(new String[accounts.size()]);

        for (String account : stringAccounts)
        {
            infoView.append("account name: " + account + "\n");
        }
    }

    private void getAdminStatus()
    {
        // initialize admin variables
        mDPM = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        mAdminName = new ComponentName(this, MyAdmin.class);
        // initialize admin variables
        // enable administration
        if (!mDPM.isAdminActive(mAdminName))
        {
            // try to become active Ð must happen here in this activity, to get
            // result
            Intent intent = new Intent(
                    DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
            intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, mAdminName);
            intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                    "Additional text explaining why this needs to be added.");
            startActivityForResult(intent, REQUEST_ENABLE);
        } else
        {
            // Already is a device administrator, can do security operations
            // now.
            mDPM.lockNow();
        }
        // enable administration
    }

    private void getContactPhoto()
    {
        contactIds = new ArrayList<Integer>();
        // Get list of contacts by contactId
        Cursor c = getContentResolver().query(
                ContactsContract.Contacts.CONTENT_URI,
                new String[] {Data._ID, Phone.PHOTO_ID, Phone.PHOTO_THUMBNAIL_URI, Phone.PHOTO_URI, Phone.PHOTO_ID},
                Data._ID,
                null,
                null);
        while (c.moveToNext())
        {
            if (c.getString(c.getColumnIndex(Phone.PHOTO_URI)) != null)
                contactIds.add(c.getInt(0));
            
        }
        infoView.append(contactIds.size() + "\n");
    }
            
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings)
        {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    private void updateTimer()
    {
        myHandler.post(myRunnable);
    }
    
    final Runnable myRunnable = new Runnable() {
        public void run() {
           timerView.setText(String.valueOf(remaining / 1000));
           if (remaining < 100)
           {
               remaining = 0;
               pressMe.callOnClick();
           }
        }
     };


    public static class MyAdmin extends DeviceAdminReceiver
    {
        // implement onEnabled(), onDisabled(), É
    }

    // catch result from admin request
    /**
     * Waits for result from call to become device admin
     */
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if (requestCode == REQUEST_ENABLE)
        {
            if (resultCode == RESULT_OK)
            {
                Log.v(TAG, "has become device admin");
                int seconds = 48;
                int minutes = 2;
                long timeMs = 1000 * (seconds + 60 * minutes);
                //mDPM.setMaximumTimeToLock(mAdminName, timeMs); // resets the
                                                               // developer
                                                               // setting to
                                                               // keep screen on
                                                               // while charging
                Log.v(TAG, "device max wakelock set to " + minutes
                        + ((minutes == 1) ? " minute " : " minutes ") + seconds
                        + ((seconds == 1) ? " second " : " seconds"));

            } else
            {
                Log.v(TAG, "failed to become device admin");
            }
        }
    }

    // catch result from admin request

    @Override
    public void onStart()
    {
        super.onStart();
        Log.v(TAG, "onStart()");
        
        // put em into a database stored on the SD card
        dbPath = path + File.separator + "metadata.db";
        // HAVE to create the directory the database is going into BEFORE
        // creating the database
        Log.v(TAG, "creating directory");
        File dbDir = new File(path);
        dbDir.mkdirs();
        
        // start collecting functional data
        getMetadata();
        getWifiData();
        getAccountInfo();
        getContactPhoto();
        //getAdminStatus();

    }

    @Override
    public void onResume()
    {
        super.onResume();
        Log.v(TAG, "onResume()");
    }

    @Override
    public void onRestart()
    {
        super.onRestart();
        Log.v(TAG, "onRestart()");
    }

    @Override
    public void onPause()
    {
        super.onPause(); // Always call the superclass method first
        Log.v(TAG, "onPause()");
    }

    @Override
    public void onStop()
    {
        super.onStop(); // Always call the superclass method first
        Log.v(TAG, "onStop()");
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        Log.v(TAG, "onDestroy()");
    }
    
}
