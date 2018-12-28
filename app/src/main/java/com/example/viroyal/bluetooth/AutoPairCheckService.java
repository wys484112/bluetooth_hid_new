package com.example.viroyal.bluetooth;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;

import java.lang.ref.WeakReference;

public class AutoPairCheckService extends Service {
    private static final String TAG = AutoPairCheckService.class.getSimpleName();

    private Context context;
    private final IBinder mBinder = new ServiceStub(this);

    private BluetoothProfile mBluetoothProfile;
    private BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
    public static final int INPUT_DEVICE = 4;

    public AutoPairCheckService() {
    }

    private void openBluetooth() {
        if (adapter == null) {
            Toast.makeText(this, "不支持蓝牙功能", Toast.LENGTH_SHORT).show();
            return;
        }
        if (mBluetoothProfile != null) {
            if(!isConnectedToPhicomBluetooth()){
                startMainActivity2();
            }
            stopSelf();

        } else {
            adapter.getProfileProxy(context, mListener,
                    INPUT_DEVICE);
        }
    }

    private Boolean isConnectedToPhicomBluetooth(){
        BluetoothDevice device = ServiceUtils.hasDeviceIsConnected(mBluetoothProfile);
        if(device!=null){
            return true;
        }
        return false;
    }
    private BluetoothProfile.ServiceListener mListener = new BluetoothProfile.ServiceListener() {
        @Override
        public void onServiceConnected(int profile, BluetoothProfile proxy) {
            Log.i(TAG, "mConnectListener onServiceConnected");
            try {
                if (profile == INPUT_DEVICE) {
                    Log.e(TAG, "profile == INPUT_DEVICE");
                    mBluetoothProfile = proxy;
                    if(!isConnectedToPhicomBluetooth()){
                        startMainActivity2();
                    }
                    stopSelf();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onServiceDisconnected(int profile) {
            Log.i(TAG, "mConnectListener onServiceConnected");
        }
    };
    private void startMainActivity2(){
        Log.e(TAG, "startMainActivity2");

        Intent noteList = new Intent(context,MainActivity2.class);
        noteList.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(noteList);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        context = this;
        openBluetooth();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.e(TAG, "onDestroy");

        ServiceUtils.closeProfileProxy(mBluetoothProfile);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }


    /*
     * By making this a static class with a WeakReference to the Service, we
     * ensure that the Service can be GCd even when the system process still
     * has a remote reference to the stub.
     */
    static class ServiceStub extends IAutoPairCheckAidlInterface.Stub {
        WeakReference<AutoPairCheckService> mService;

        ServiceStub(AutoPairCheckService service) {
            mService = new WeakReference<AutoPairCheckService>(service);
        }

        @Override
        public void basicTypes(int anInt, long aLong, boolean aBoolean, float aFloat, double aDouble, String aString) throws RemoteException {

        }
    }
}
