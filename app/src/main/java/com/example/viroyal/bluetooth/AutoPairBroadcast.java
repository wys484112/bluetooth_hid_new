package com.example.viroyal.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class AutoPairBroadcast extends BroadcastReceiver {
    private final String TAG = "AutoPairBroadcast";
    private BluetoothAdapter bluetoothAdapter;

    @Override
    public void onReceive(Context context, Intent intent) {
        // TODO: This method is called when the BroadcastReceiver is receiving
        // an Intent broadcast.
        String action = intent.getAction();
        if (action == null) return;
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) return;
        if(action.equals(Intent.ACTION_BOOT_COMPLETED)){
            enablePairService(context, true);
        }
    }

    public void enablePairService(Context context, boolean pair) {
        Log.d(TAG, " =================== >> enablePairService:  " + pair);
        Intent autoPairService = new Intent(context, AutoPairServiceNew.class);
        if(pair){
            context.startService(autoPairService);
        } else {
            context.stopService(autoPairService);
        }
    }

}
