package com.example.viroyal.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.util.Log;

import java.util.List;

public class ServiceUtils {
    private static final String TAG = ServiceUtils.class.getSimpleName();

    public static final int INPUT_DEVICE = 4;

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
        if (device != null && device.getBluetoothClass().getMajorDeviceClass() == BluetoothClass.Device.Major.PERIPHERAL) {
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
}
