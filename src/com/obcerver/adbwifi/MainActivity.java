package com.obcerver.adbwifi;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.obcerver.adbwifi.lib.Utility;         // Utilities of adb wifi

/**
 * adb wifi
 * @author zeyuec
 * @since 2013-07-14
 */
public class MainActivity extends Activity
{
    private final static String TAG = "adbwifi";

    private LinearLayout                 toggleButton;
    private TextView                     hint, toggleLeft, toggleRight;
    private boolean                      toggleStatus = false;
    private WifiStateReceiver            wifiStateReceiver;
    
    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        initVal();
        init();
    }
    
    @Override public void onBackPressed() {
        moveTaskToBack(true);        
    }

    @Override public void onDestroy() {
        unregisterReceiver(wifiStateReceiver);
        super.onDestroy();
    }
    
    private void initVal() {
        this.toggleButton = (LinearLayout) findViewById(R.id.toggleLayout);
        this.hint = (TextView) findViewById(R.id.hint);
        this.toggleLeft = (TextView) findViewById(R.id.toggleLeft);
        this.toggleRight = (TextView) findViewById(R.id.toggleRight);

        this.wifiStateReceiver = new WifiStateReceiver();
        registerReceiver(wifiStateReceiver, new IntentFilter(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION));
    }

    private void init() {
        if (Utility.isWifiConnected(this)) {
            resetToggleStatus();
        } else {
            lockToggle();
        }
    }

    private void resetToggleStatus() {
        toggleStatus = Utility.getAdbdStatus();
        setToggleStatus(toggleStatus);
        toggleButton.setOnClickListener(null);
        toggleButton.setOnClickListener(new ToggleClickListener());
    }

    private void lockToggle() {
        Utility.setAdbWifiStatus(false);             // try stop
        toggleStatus = false;                       
        setToggleStatus(toggleStatus);
        hint.setText("wifi is not connected");
        toggleButton.setOnClickListener(null);
    }

    private class WifiStateReceiver extends BroadcastReceiver {
        @Override public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (action.equals(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION)) {
                if (intent.getBooleanExtra(WifiManager.EXTRA_SUPPLICANT_CONNECTED, false)) {
                    // wifi is connected, waiting for ip
                    try {
                        Thread.sleep(10000);
                        int tryTimes = 0;
                        while (Utility.getIp() == null && tryTimes < 0) {
                            Thread.sleep(1000);
                        }
                        resetToggleStatus();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    // wifi connection lost
                    lockToggle();
                }
            }
        }
    }

    private class ToggleClickListener implements View.OnClickListener {
        @Override public void onClick(View v) {
            if (Utility.isWifiConnected(MainActivity.this)) {
                // try switch
                boolean ret = Utility.setAdbWifiStatus(!toggleStatus);
                if (ret) {
                    // switch successfully
                    toggleStatus = !toggleStatus;
                    setToggleStatus(toggleStatus);
                
                    if (toggleStatus) {
                        Toast.makeText(MainActivity.this, "adb wifi service started", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(MainActivity.this, "adb wifi service stopped", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    // failed
                    Toast.makeText(MainActivity.this, "something wrong", Toast.LENGTH_SHORT).show();
                }
            } else {
                lockToggle();
            }
        }
    }

    private void setToggleStatus(boolean status) {
        if (!status) {
            toggleLeft.setText("OFF");
            toggleLeft.setBackgroundColor(getResources().getColor(R.color.gray_dark));
            toggleRight.setText("");
            toggleRight.setBackgroundColor(getResources().getColor(R.color.gray_light));
            hint.setText("");
        } else {
            toggleLeft.setText("");
            toggleLeft.setBackgroundColor(getResources().getColor(R.color.gray_light));
            toggleRight.setText("ON");
            toggleRight.setBackgroundColor(getResources().getColor(R.color.blue_holo));
            hint.setText("adb connect " + Utility.getIp() + ":" + String.valueOf(Utility.getPort()));
        }
    }
}
