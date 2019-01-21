package com.example.viroyal.bluetooth;

import android.app.Activity;
import android.app.ActivityManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.ComponentName;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.ServiceConnection;
import android.util.Log;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

public class ServiceUtils {
    private static final String TAG = ServiceUtils.class.getSimpleName();
    private static final boolean DBG = true;

    public static final int INPUT_DEVICE = 4;



    public static boolean isServiceRunning(Context context, String ServiceName) {
        if (("").equals(ServiceName) || ServiceName == null)
            return false;
        ActivityManager myManager = (ActivityManager) context
                .getSystemService(Context.ACTIVITY_SERVICE);
        ArrayList<ActivityManager.RunningServiceInfo> runningService = (ArrayList<ActivityManager.RunningServiceInfo>) myManager
                .getRunningServices(Integer.MAX_VALUE);
        for (int i = 0; i < runningService.size(); i++) {
            Log.e(TAG, "isServiceRunning =="+runningService.get(i).service.getClassName().toString());

            if (runningService.get(i).service.getClassName().toString()
                    .contains(ServiceName)) {
                return true;
            }
        }
        return false;
    }

    // 是否有连接的遥控器
    public static BluetoothDevice hasDeviceIsConnected(BluetoothProfile mBluetoothProfile) {
        if (mBluetoothProfile != null) {
            List<BluetoothDevice> deviceList = mBluetoothProfile.getConnectedDevices();
            for (BluetoothDevice device : deviceList) {
                if (isPhicomPERIPHERAL(device)) {
                    return device;
                }
            }
        }
        return null;
    }
    public static Boolean isPhicomPERIPHERAL(BluetoothDevice device) {
        if (device != null && device.getBluetoothClass().getMajorDeviceClass() == BluetoothClass.Device.Major.PERIPHERAL)
        {
            if (device.getName() != null && !device.getName().isEmpty()) {
                if (device.getName().equals("斐讯遥控器")) {
                    return true;
                }
            }
        }
        return false;
    }
    public static void closeProfileProxy(BluetoothProfile mBluetoothProfile) {
        if (mBluetoothProfile != null) {
            try {
                BluetoothAdapter.getDefaultAdapter().closeProfileProxy(INPUT_DEVICE, mBluetoothProfile);
                mBluetoothProfile = null;
            } catch (Throwable t) {
                Log.e(TAG, "Error cleaning up HID proxy", t);
            }
        }
    }


    public static void removeBond(BluetoothDevice device) {
        if (device != null) {
            int state = device.getBondState();
            if (state == BluetoothDevice.BOND_BONDING) {
                try {
                    Method method = BluetoothDevice.class.getMethod("cancelBondProcess");
                    method.invoke(device);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            state = device.getBondState();
            if (state != BluetoothDevice.BOND_NONE) {
                try {
                    Method method = BluetoothDevice.class.getMethod("removeBond");
                    method.invoke(device);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void cleanBoundedPhicomPERIPHERALDevices() {
        Set<BluetoothDevice> bondedDevices = BluetoothAdapter.getDefaultAdapter().getBondedDevices();

        if (bondedDevices != null) {
            for (BluetoothDevice device : bondedDevices) {
                if (isPhicomPERIPHERAL(device)) {
                    removeBond(device);
                }
            }
        }
    }


    public static void cancelDiscovery() {
        if (BluetoothAdapter.getDefaultAdapter() != null) {
            if (BluetoothAdapter.getDefaultAdapter().isDiscovering()) {
                BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
            }
        }
    }


/****************************************************************************************************
 * ***********************bind  start  service*******************************************************
 * **************************************************************************************************
 * */
    public static IAutoPairCheckAidlInterface sService = null;
    private static HashMap<Context, ServiceBinder> sConnectionMap = new HashMap<Context, ServiceBinder>();

    public static class ServiceToken {
        ContextWrapper mWrappedContext;
        ServiceToken(ContextWrapper context) {
            mWrappedContext = context;
        }
    }

    public static ServiceToken bindToService(Activity context) {
        return bindToService(context, null);
    }

    public static ServiceToken bindToService(Activity context, ServiceConnection callback) {
        Activity realActivity = context.getParent();
        if (realActivity == null) {
            realActivity = context;
        }
        ContextWrapper cw = new ContextWrapper(realActivity);
        cw.startService(new Intent(cw, AutoPairCheckService.class));
        ServiceBinder sb = new ServiceBinder(callback);
        if (cw.bindService((new Intent()).setClass(cw, IAutoPairCheckAidlInterface.class), sb, 0)) {
            sConnectionMap.put(cw, sb);
            if (DBG)
                Log.e(TAG, "bind to service");
            return new ServiceToken(cw);
        }
        if (DBG)
            Log.e(TAG, "Failed to bind to service");
        return null;
    }

    public static void unbindFromService(ServiceToken token) {
        if (token == null) {
            Log.e(TAG, "Trying to unbind with null token");
            return;
        }
        ContextWrapper cw = token.mWrappedContext;
        ServiceBinder sb = sConnectionMap.remove(cw);
        if (sb == null) {
            if (DBG)
                Log.e(TAG, "Trying to unbind for unknown Context");
            return;
        }
        cw.unbindService(sb);
        if (sConnectionMap.isEmpty()) {
            // presumably there is nobody interested in the service at this point,
            // so don't hang on to the ServiceConnection
            if (DBG)
                Log.e(TAG, "unbind to service");

            sService = null;
        }
    }

    private static class ServiceBinder implements ServiceConnection {
        ServiceConnection mCallback;
        ServiceBinder(ServiceConnection callback) {
            mCallback = callback;
        }

        public void onServiceConnected(ComponentName className, android.os.IBinder service) {
            sService = IAutoPairCheckAidlInterface.Stub.asInterface(service);
//			initAlbumArtCache();
            if (mCallback != null) {
                mCallback.onServiceConnected(className, service);
            }
        }

        public void onServiceDisconnected(ComponentName className) {
            if (mCallback != null) {
                mCallback.onServiceDisconnected(className);
            }
            sService = null;
        }
    }

}
