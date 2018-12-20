package com.example.viroyal.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class AutoPairBroadcast extends BroadcastReceiver {
    private final String TAG = "wwww";
    private BluetoothAdapter bluetoothAdapter;

    @Override
    public void onReceive(Context context, Intent intent) {
        // TODO: This method is called when the BroadcastReceiver is receiving
        // an Intent broadcast.
        String action = intent.getAction();
//        if (action == null) return;
//        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
//        if (bluetoothAdapter == null) return;
        if(action.equals(Intent.ACTION_BOOT_COMPLETED)){
//            Log.e("wwww","ACTION_BOOT_COMPLETED");
//            Intent activityintent=new Intent(context,MainActivity2.class);
//            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//            context.startActivity(activityintent);



//            Intent i = new Intent();
//            ComponentName cn = new ComponentName("com.example.viroyal.bluetooth",  "com.example.viroyal.bluetooth.MainActivity2");
//            i.setComponent(cn);
//            i.setAction("android.intent.action.MAIN");
//            context.startActivity(i); //or startActivityForResult(i, RESULT_OK);

//            Intent it = new Intent(" com.qylk.call.main "); context.startActivity(it);
            Log.d("wwww","boot complete");
            Intent noteList = new Intent(context,MainActivity2.class);
            noteList.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(noteList);
//            enablePairService(context, true);
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
