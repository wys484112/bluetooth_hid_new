package com.example.viroyal.bluetooth;

import android.annotation.SuppressLint;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@SuppressLint("NewApi")
public class AutoPairServiceNew extends Service {
    // 蓝牙连接状态改变广播
    public static final String ACTION_CONNECTION_STATE_CHANGED = "android.bluetooth.input.profile.action.CONNECTION_STATE_CHANGED";
    // 蓝牙配对状态改变广播
    public static final String ACTION_BOND_STATE_CHANGED = BluetoothDevice.ACTION_BOND_STATE_CHANGED;
    // 蓝牙配对状态改变广播
    public static final String ACTION_UUID = BluetoothDevice.ACTION_UUID;
    private final static String TAG = "wwww";
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothDevice mPairingDevice;
    private BluetoothProfile mBluetoothProfile;
    private InputDeviceServiceListener mInputDeviceServiceListener;
    private BluetoothLeScanner mBLEScanner;
    private ScanSettings mScanSettings;
    private List<ScanFilter> mScanFilterList;
    private BleDeviceScanCallback mScanCallback;
    public static final int INPUT_DEVICE = 4;
    public static final int PRIORITY_AUTO_CONNECT = 1000;
    //开启扫描时间，一直开启BT会影响WIFI速率，也没必要一直开启，在15分后关闭扫描
    private static final long SCAN_PERIOD = 1000 * 20;
    public boolean mScanning = false;
    private Handler mHandler;
    public static boolean RUNNING = false;
    public static boolean KEEP_SCAN = false;
    private Context context;

    @Override
    public void onCreate() {
        super.onCreate();
        context = this;
        Log.d(TAG, ">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> onCreate");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        RUNNING = true;
        mScanning = false;
        boolean init = initBluetooth();
        initUI();


        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED); //监听搜索完毕
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);   //监听搜索开始
        filter.addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED); //监听连接状态变化
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);              //监听蓝牙打开状态
        filter.addAction(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);           //监听扫描模式

        filter.addAction("android.bluetooth.input.profile.action.CONNECTION_STATE_CHANGED");               //监听hid是否连接
        filter.addAction(BluetoothDevice.ACTION_FOUND);               //监听是否配对
        filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED); //监听配对状态
        filter.addAction(BluetoothDevice.ACTION_PAIRING_REQUEST);     //监听配对的状态
        registerReceiver(mBluetoothReceiver, filter);

        if (init) {
            Log.d(TAG, ">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> onStartCommand");
            //第一次启动mBluetoothProfile可能还没回调初始化，在2.step会去扫描
            //第二次启动mBluetoothProfile已经初始化完毕，代表需要直接扫描，那么此时直接开启扫描
            mPairingDevice = null;
            if (mBluetoothProfile == null) {
                //TODO 1.step 获取输出设备的操作对象BluetoothProfile
                mInputDeviceServiceListener = new InputDeviceServiceListener();
                mBluetoothAdapter.getProfileProxy(this, mInputDeviceServiceListener, INPUT_DEVICE);
            } else {
                KEEP_SCAN = true;
                cleanBoundDevices();
                scanLeDevice(true);
            }
            mHandler.removeCallbacks(stopLeScanRunnable);
            mHandler.postDelayed(stopLeScanRunnable, SCAN_PERIOD);
        } else {
            loding(null, null, "没有蓝牙", false);
        }

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, ">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> onDestroy");
        unregisterReceiver(mBluetoothReceiver);
        scanLeDevice(false);
        closeProfileProxy();
        RUNNING = false;
        super.onDestroy();
    }

    public boolean initBluetooth() {
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }
        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }
        if (!mBluetoothAdapter.isEnabled()) {
            mBluetoothAdapter.enable();//强制开启蓝牙
        }

        mBLEScanner = mBluetoothAdapter.getBluetoothLeScanner();
        if (mBLEScanner == null) {
            Log.e(TAG, "Unable to obtain a BluetoothLeScanner.");
            return false;
        }
        mScanFilterList = new ArrayList<ScanFilter>();
        mScanSettings = new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build();
        mScanCallback = new BleDeviceScanCallback();
        return true;
    }

    void closeProfileProxy() {
        if (mBluetoothProfile != null) {
            try {
                mBluetoothAdapter.closeProfileProxy(INPUT_DEVICE, mBluetoothProfile);
                mBluetoothProfile = null;
            } catch (Throwable t) {
                Log.e(TAG, "Error cleaning up HID proxy", t);
            }
        }
    }

    private final class InputDeviceServiceListener implements BluetoothProfile.ServiceListener {
        @Override
        public void onServiceConnected(int profile, BluetoothProfile proxy) {
            try {
                if (profile == INPUT_DEVICE) {
                    mBluetoothProfile = proxy;
                    Log.i(TAG, "InputDeviceServiceListener onServiceConnected");
                    // TODO 2.step
                    if (hasDeviceIsConnected() == null) {
                        cleanBoundDevices();
                        scanLeDevice(true);
                    } else {
                        scanLeDevice(false);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onServiceDisconnected(int profile) {
            Log.i(TAG, "InputDeviceServiceListener onServiceDisconnected");
        }
    }

    Runnable stopLeScanRunnable = new Runnable() {
        @Override
        public void run() {



            if (mBluetoothAdapter.isEnabled()) {
                if (hasDeviceIsConnected() == null) {
                    mPairingDevice = null;
                    if (mBluetoothProfile == null) {
                        //TODO 1.step 获取输出设备的操作对象BluetoothProfile
                        mInputDeviceServiceListener = new InputDeviceServiceListener();
                        mBluetoothAdapter.getProfileProxy(context, mInputDeviceServiceListener, INPUT_DEVICE);
                    } else {
                        KEEP_SCAN = true;
                        cleanBoundDevices();
                        scanLeDevice(true);
                    }

                } else {
                    if (mPairingDevice != null) {
                        if (getConnectionStatus(mPairingDevice) == BluetoothProfile.STATE_CONNECTED) {
                            disOtherDeviceIsConnected();
                            rmOtherDeviceIsBonded();
                        }
                    }
                    scanLeDevice(false);
                }
            } else {
                mBluetoothAdapter.enable();//强制开启蓝牙
            }

            mHandler.postDelayed(stopLeScanRunnable, SCAN_PERIOD);


        }
    };

    private void cleanBoundDevices() {
        Set<BluetoothDevice> bondedDevices = mBluetoothAdapter.getBondedDevices();
        if (bondedDevices != null) {
            for (BluetoothDevice device : bondedDevices) {
                if (isHmdDevice(device)) {
                    removeBond(device);
                }
            }
        }
    }

    public void scanLeDevice(final boolean enable) {
        if (enable) {
            if (!mScanning) {
                mScanning = true;
                startLeScan();
            }
        } else {
            if (mScanning) {
                mScanning = false;
                stopLeScan();
            }
        }
    }

    void startLeScan() {
        Log.d(TAG, ">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> startLeScan");
        if (mBLEScanner != null)
            mBLEScanner.startScan(mScanFilterList, mScanSettings, mScanCallback);
    }

    void stopLeScan() {
        Log.d(TAG, ">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> stopLeScan");
        if (mBLEScanner != null) mBLEScanner.stopScan(mScanCallback);
    }


    public void loding(String name, String address, String status, Boolean show) {//点击加载并按钮模仿网络请求


        Intent intent1 = new Intent(this, MainDialogActivity.class);
        intent1.putExtra("name", name);
        intent1.putExtra("address", address);
        intent1.putExtra("status", status);
        intent1.putExtra("show", show);
        startActivity(intent1);


    }

    private final class BleDeviceScanCallback extends ScanCallback {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            Log.d(TAG, " onScanResult <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
            BluetoothDevice device = result.getDevice();
            int rssi = result.getRssi();
            byte[] scanRecord = result.getScanRecord().getBytes();
            if (device.getBondState() != BluetoothDevice.BOND_NONE) {
                return;
            }

            mPairingDevice = device;
            scanLeDevice(false);
            Log.d(TAG, "onScanResult  <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<Name:" + mPairingDevice.getName());
            Log.d(TAG, "onScanResult  <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<MAC:" + mPairingDevice.getAddress());
            Log.d(TAG, "onScanResult  <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<RSSI:" + (0 - rssi));
            //条件是否匹配
            if (isGoodMatchRc(device, rssi, scanRecord)) {
                Log.d(TAG, "onScanResult  GoodGoodGoodGoodGoodGoodGoodGood isGoodMatchRc: " + mPairingDevice.getAddress());
                //找到可配对的设备，停止LE扫描
                if (device.getBondState() == BluetoothDevice.BOND_NONE) {
                    //未配对
                    if (createBond(device)) {
                        // TODO 4.step 等待广播mBluetoothReceiver更新结果
                        mHandler.removeCallbacks(stopLeScanRunnable);
                        mHandler.postDelayed(stopLeScanRunnable, SCAN_PERIOD);

                    } else {
                        removeBond(mPairingDevice);// 若连接失败，移除它
                        mPairingDevice = null;
                        scanLeDevice(true);
                    }
                } else if (device.getBondState() == BluetoothDevice.BOND_BONDED) {
                    //已配对
                    mHandler.removeCallbacks(stopLeScanRunnable);
                    mHandler.postDelayed(stopLeScanRunnable, SCAN_PERIOD);
                    connect(device);
                }
            } else {
                mPairingDevice = null;
            }

        }

        @Override
        public void onScanFailed(int errorCode) {
        }
    }

    // createBond 匹配状态 已经接受ACTION_UUID后进行连接
    private final BroadcastReceiver mBluetoothReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            switch (action) {
                case BluetoothAdapter.ACTION_DISCOVERY_STARTED: {
                    loding(null,null,"正在搜索遥控器", true);
                }
                break;
                case "android.bluetooth.input.profile.action.CONNECTION_STATE_CHANGED": {
                    int state = intent.getIntExtra(BluetoothProfile.EXTRA_STATE, 0);
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    Log.i(TAG, "state=" + state + ",device=" + device);
                    if (mPairingDevice != null && mPairingDevice.getAddress().equals(device.getAddress())) {
                        if (isHmdDevice(device)) {
                            if (state == BluetoothProfile.STATE_CONNECTING) {//正在连接
                                loding(device.getName(),device.getAddress(),"正在连接" + device.getName(), true);
                            }
                            if (state == BluetoothProfile.STATE_CONNECTED) {//连接成功
                                loding(device.getName(),device.getAddress(),"已连接" + device.getName(), false);
                                AutoPairGlobalConfig.setRcMac(mPairingDevice.getAddress());// 设置MAC地址

                            } else if (state == BluetoothProfile.STATE_DISCONNECTED) {//连接失败
                                loding(device.getName(),device.getAddress(),"连接失败", false);

                            }
                        }
                    }

                }
                break;
                case BluetoothAdapter.ACTION_STATE_CHANGED: {
                    int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1);
                    int previousState = intent.getIntExtra(BluetoothAdapter.EXTRA_PREVIOUS_STATE, -1);
                    if (previousState != BluetoothAdapter.STATE_ON && state == BluetoothAdapter.STATE_ON) {
                        mPairingDevice = null;
                        if (mBluetoothProfile == null) {
                            //TODO 1.step 获取输出设备的操作对象BluetoothProfile
                            mInputDeviceServiceListener = new InputDeviceServiceListener();
                            mBluetoothAdapter.getProfileProxy(context, mInputDeviceServiceListener, INPUT_DEVICE);
                        } else {
                            KEEP_SCAN = true;
                            cleanBoundDevices();
                            scanLeDevice(true);
                        }

                        loding(null,null,"正在搜索遥控器", true);
                    }
                }
                break;
                case BluetoothDevice.ACTION_BOND_STATE_CHANGED: {
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    int state = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, -1);
                    int previousState = intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, -1);

                    if (mPairingDevice != null && mPairingDevice.getAddress().equals(device.getAddress())) {
                        if (isHmdDevice(device)) {
                            if (state == BluetoothDevice.BOND_BONDING) {
                                loding(device.getName(),device.getAddress(),"配对中", true);
                            }
                            if (state == BluetoothDevice.BOND_BONDED) {
                                loding(device.getName(),device.getAddress(),"已配对", false);

                            }
                            if (state == BluetoothDevice.BOND_BONDED && previousState == BluetoothDevice.BOND_BONDING) {
                                loding(device.getName(),device.getAddress(),"正在连接", true);
                                connect(device);//连接设备
                            }
                        }
                    }

                }
                break;
            }
        }
    };

    private void removeBond(BluetoothDevice device) {
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

    public boolean createBond(BluetoothDevice device) {
        if (device != null) {
            if (device.getBondState() == BluetoothDevice.BOND_NONE) {
                return device.createBond();
            }
        }
        return false;
    }

    public void connect(BluetoothDevice device) {
        if (mBluetoothProfile != null && device != null) {
            try {
                Method method = mBluetoothProfile.getClass().getMethod("connect", new Class[]{BluetoothDevice.class});
                method.invoke(mBluetoothProfile, device);
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                Method method = mBluetoothProfile.getClass().getMethod("setPriority", new Class[]{BluetoothDevice.class, int.class});
                method.invoke(mBluetoothProfile, device, PRIORITY_AUTO_CONNECT);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    void disconnect(final BluetoothDevice device) {
        try {
            Method method = mBluetoothProfile.getClass().getMethod("disconnect", new Class[]{BluetoothDevice.class});
            method.invoke(mBluetoothProfile, device);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public int getConnectionStatus(BluetoothDevice device) {
        if (mBluetoothProfile == null) {
            return BluetoothProfile.STATE_DISCONNECTED;
        }
        return mBluetoothProfile.getConnectionState(device);
    }

    // ########################################  配对过滤条件  ###########################################
    private static final int BLE_RSSI = 100;
    private static final int COMPLETE_NAME_FLAG = 0x09;
    private static final int UUID16_SERVICE_FLAG_MORE = 0x02;
    private static final int UUID16_SERVICE_FLAG_COMPLETE = 0x03;
    private static final int UUID32_SERVICE_FLAG_MORE = 0x04;
    private static final int UUID128_SERVICE_FLAG_COMPLETE = 0x07;
    private static final int HOGP_UUID16 = 0x1812;
    private long matchTime = 0;

    private boolean isGoodMatchRc(BluetoothDevice device, final int rssi, byte[] scanRecord) {
        boolean isHIMEDIARc;
        if (isHmdDevice(device)) {
            isHIMEDIARc = true;
        } else {
            isHIMEDIARc = isNameMatchExName(AutoPairGlobalConfig.getRcName(), scanRecord);
        }
        boolean isHOGPDevice = containHogpUUID(scanRecord);
        if (isHIMEDIARc && isHOGPDevice) {
            if ((0 - rssi) <= BLE_RSSI) {
                return true;
            } else if (System.currentTimeMillis() - matchTime > 10 * 1000) {
                loding(device.getName(),device.getAddress(),"信号弱",false);
                matchTime = System.currentTimeMillis();
            }
        }
        return false;
    }

    public boolean isHmdDevice(BluetoothDevice device) {
        if (device != null) {
            if (device.getAddress().equals(AutoPairGlobalConfig.getRcMac())) {
                return true;
            }
            if (device.getName() != null) {
                if (device.getName().startsWith(AutoPairGlobalConfig.DEF_NAME)) return true;
                if (device.getName().startsWith(AutoPairGlobalConfig.getRcName())) return true;
            }
        }
        return false;
    }

    // 是否有连接的遥控器
    private BluetoothDevice hasDeviceIsConnected() {
        if (mBluetoothProfile != null) {
            List<BluetoothDevice> deviceList = mBluetoothProfile.getConnectedDevices();
            for (BluetoothDevice device : deviceList) {
                if (isHmdDevice(device)) {
                    return device;
                }
            }
        }
        return null;
    }

    // 是否有已绑定的遥控器
    private BluetoothDevice hasDeviceIsBonded() {
        if (mBluetoothAdapter != null) {
            Set<BluetoothDevice> deviceList = mBluetoothAdapter.getBondedDevices();
            for (BluetoothDevice device : deviceList) {
                if (isHmdDevice(device)) {
                    return device;
                }
            }
        }
        return null;
    }

    /* we only care 16bit UUID now */
    public static synchronized boolean containHogpUUID(byte[] scanRecord) {
        int i, j, length = scanRecord.length;
        i = 0;
        int uuid = 0;
        while (i < length - 2) {
            int element_len = scanRecord[i];
            byte element_type = scanRecord[i + 1];
            if (element_type == UUID16_SERVICE_FLAG_MORE || element_type == UUID16_SERVICE_FLAG_COMPLETE) {
                for (j = 0; j < element_len - 1; j++, j++) {
                    uuid = scanRecord[i + j + 2] + (scanRecord[i + j + 3] << 8);
                    if (uuid == HOGP_UUID16) {
                        return true;
                    }
                }
            } else if (element_type >= UUID32_SERVICE_FLAG_MORE && element_type >= UUID128_SERVICE_FLAG_COMPLETE) {
            }
            i += element_len + 1;
        }
        return false;
    }

    public static synchronized boolean isNameMatchExName(String name, byte[] scanRecord) {
        int i = 0;
        int length = scanRecord.length;
        byte[] byteName = new byte[50];
        String decodedName = null;
        while (i < length - 2) {
            int element_len = scanRecord[i];
            byte element_type = scanRecord[i + 1];
            if (element_type == COMPLETE_NAME_FLAG) {
                System.arraycopy(scanRecord, i + 2, byteName, 0, element_len - 1);
                try {
                    decodedName = new String(byteName, "UTF-8");
                } catch (UnsupportedEncodingException e) {
                }
                if (decodedName != null) {
                    if (decodedName.startsWith(name)) {
                        return true;
                    }
                }
            }
            i += element_len + 1;
        }
        return false;
    }

    // ########################################  UI  ###########################################

    public void initUI() {
        mHandler = new Handler();
    }


    // 断开有连接的遥控器
    private void disOtherDeviceIsConnected() {
        if (mBluetoothProfile != null) {
            List<BluetoothDevice> deviceList = mBluetoothProfile.getConnectedDevices();
            for (BluetoothDevice device : deviceList) {
                if (isHmdDevice(device)) {
                    if (mPairingDevice != null && mPairingDevice.getAddress().equals(device.getAddress())) {
                        continue;
                    }
                    disconnect(device);
                }
            }
        }
    }

    private void rmOtherDeviceIsBonded() {
        Set<BluetoothDevice> deviceList = mBluetoothAdapter.getBondedDevices();
        for (BluetoothDevice device : deviceList) {
            if (isHmdDevice(device)) {
                if (mPairingDevice != null && mPairingDevice.getAddress().equals(device.getAddress())) {
                    continue;
                }
                removeBond(device);
            }
        }
    }

    // #################################################################################################
    // AIDL 可以调用
    private final IBinder mBinder = new LocalBinder();

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    public class LocalBinder extends Binder {
        AutoPairServiceNew getService() {
            return AutoPairServiceNew.this;
        }
    }
}
