package com.example.kylehirschfelder.wifi_p2pgroupclient;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends AppCompatActivity implements WifiP2pManager.PeerListListener, WifiP2pManager.ConnectionInfoListener, WifiP2pManager.GroupInfoListener {

    WifiP2pManager mManager;
    WifiP2pManager.Channel mChannel;
    MyBroadcastReceiver mReceiver;
    public  static final String TAG = "log";
    IntentFilter mIntentFilter;
    Button btn, btnConnect;
    int i = 0;

    public List peers = new ArrayList();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btn = (Button) findViewById(R.id.button);
        btnConnect = (Button) findViewById(R.id.btn2);

        //Handles Wifi connection
        WifiManager manager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        manager.setWifiEnabled(false);
        Log.println(Log.ASSERT, TAG, "Resetting WIFI");
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        manager.setWifiEnabled(true);

        mManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        mChannel = mManager.initialize(this,getMainLooper(),null);
        mReceiver = new MyBroadcastReceiver(mManager,mChannel,this);

        deletePersistentGroups();

        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        btnConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                connect();
            }
        });

        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {
                        Log.println(Log.ASSERT,TAG,"Discovery process success");
                    }

                    @Override
                    public void onFailure(int i) {
                        Log.println(Log.ASSERT,TAG,"Discovery process failed");
                        Log.println(Log.ASSERT,TAG,String.valueOf(i));
                    }
                });
            }
        });
    }

    @Override   //Register broadcast receiver with intent values
    protected void onResume(){
        super.onResume();
        registerReceiver(mReceiver,mIntentFilter);
    }

    @Override   //Register broadcast receiver with intent values
    protected void onPause(){
        super.onPause();
        unregisterReceiver(mReceiver);
    }

    @Override
    public void onPeersAvailable(WifiP2pDeviceList wifiP2pDeviceList) {

        peers.clear();
        peers.addAll(wifiP2pDeviceList.getDeviceList());

        if (peers.size() == 0) {
            Log.println(Log.ASSERT, TAG, "0 Devices found");
            return;
        } else {
            Log.println(Log.ASSERT, TAG, String.valueOf(peers.size()) + " Devices found");
            //connect();
        }
    }

    public void connect(){

        // Picking the first device found on the network.
        //if(peers.size() < 1) return;
        WifiP2pDevice device = (WifiP2pDevice) peers.get(0);
        WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = device.deviceAddress;
        config.wps.setup = WpsInfo.PBC;
        config.groupOwnerIntent = 0;
        Log.println(Log.ASSERT,"LOG","Before connecting to the tablet");

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        mManager.connect(mChannel, config, new WifiP2pManager.ActionListener() {

            @Override
            public void onSuccess() {
                // WiFiDirectBroadcastReceiver will notify us. Ignore for now.
                Log.println(Log.ASSERT,"Log","success connecting");

            }
            @Override
            public void onFailure(int reason) {
                // Toast.makeText(WiFiDirectActivity.this, "Connect failed. Retry.",
                // Toast.LENGTH_SHORT).show();
                Log.println(Log.ASSERT, "log", "connecting failed " + String.valueOf(reason));
            }
        });
    }

    @Override
    public void onConnectionInfoAvailable(final WifiP2pInfo info){
        InetAddress groupOwnerAddress = info.groupOwnerAddress;
        Log.println(Log.ASSERT,TAG,"Connection info available");
        if(info.groupFormed) {
            Log.println(Log.ASSERT, TAG, "This is group client");
            Log.println(Log.ASSERT, TAG, groupOwnerAddress.getHostAddress());
/*
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
*/

            new FileServerAsyncTask(getApplicationContext(),groupOwnerAddress.getHostAddress(),this).execute();

            mManager.stopPeerDiscovery(mChannel, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    Log.println(Log.ASSERT,"Log","Peer discovery stopped");
                }

                @Override
                public void onFailure(int reason) {
                    Log.println(Log.ASSERT,"Log","Peer discovery continues");

                }
            });
        }
    }

    @Override
    public void onGroupInfoAvailable(WifiP2pGroup group){
        //connect();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    public void deletePersistentGroups(){
        try {
            Method[] methods = WifiP2pManager.class.getMethods();

            for (int i = 0; i < methods.length; i++) {
                if (methods[i].getName().equals("deletePersistentGroup")) {
                    // Delete any persistent group
                    for (int netid = 0; netid < 32; netid++) {
                        methods[i].invoke(mManager, mChannel, netid, null);
                    }
                }
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    public static class FileServerAsyncTask extends AsyncTask {

        Context context;
        String host;
        int port = 8888;
        int len;
        Socket socket = new Socket();
        byte buf[] = new byte[1024];
        //Activity main;

        MainActivity mActivity;
        public FileServerAsyncTask(Context context, String host, MainActivity mActivity) {
            //  this.main = main;
            this.context = context;
            this.host = host;
            this.mActivity = mActivity;
        }

        @Override
        protected Object doInBackground(Object[] objects) {
            Log.println(Log.ASSERT, "Tag", "Here");

            if (host != null) {
                Log.println(Log.ASSERT, "HOST:", host);
                try {
                    String data = "Sending this motherfucking string";
                    socket.bind(null);
                    socket.connect((new InetSocketAddress(host, port)), 8000);
                    //String data ="This is the data";

                    HashMap<String,String> map = new HashMap<String, String>();
                    map.put("Mayank","Kale");
                    map.put("Kyle","CS GOD");

                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    ObjectOutput out = null;
                    out = new ObjectOutputStream(bos);
                    out.writeObject(map);
                    byte[] array = bos.toByteArray();
                    // buf = data.getBytes();
                    OutputStream outputStream = socket.getOutputStream();
                    int len = array.length;
                    outputStream.write(array, 0 , len);

                    outputStream.close();
                    bos.close();
                    //main.function_count++;
                    //MyAppApplication.count++;
/*
                    buf = data.getBytes();
                    int len = buf.length;
                    OutputStream outputStream = socket.getOutputStream();
                    outputStream.write(buf, 0, len);
                    outputStream.close();
*/
                    socket.close();
                    mActivity.deletePersistentGroups();

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }
    }
}