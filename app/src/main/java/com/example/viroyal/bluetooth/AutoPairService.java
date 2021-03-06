package com.example.viroyal.bluetooth;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

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
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.view.WindowManager;
import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
@SuppressLint("NewApi")
public class AutoPairService extends Service {
    // 蓝牙连接状态改变广播
    public static final String ACTION_CONNECTION_STATE_CHANGED = "android.bluetooth.input.profile.action.CONNECTION_STATE_CHANGED";
    // 蓝牙配对状态改变广播
    public static final String ACTION_BOND_STATE_CHANGED = BluetoothDevice.ACTION_BOND_STATE_CHANGED;
    // 蓝牙配对状态改变广播
    public static final String ACTION_UUID = BluetoothDevice.ACTION_UUID;
    private final static String TAG = AutoPairService.class.getSimpleName();
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
    private static final long SCAN_PERIOD = 15 * 1000 * 60;
    public boolean mScanning = false;
    private PairHandler mHandler;
    public static boolean RUNNING = false;
    public static boolean KEEP_SCAN = false;
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, ">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> onCreate");
        RUNNING = true;
        mScanning = false;
        boolean init = initBluetooth();
        if (!init) return;
        initUI();
        // 广播接收器
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_UUID);
        intentFilter.addAction(ACTION_BOND_STATE_CHANGED);
        intentFilter.addAction(ACTION_CONNECTION_STATE_CHANGED);
        registerReceiver(mBluetoothReceiver, intentFilter);
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, ">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> onStartCommand");
        //第一次启动mBluetoothProfile可能还没回调初始化，在2.step会去扫描
        //第二次启动mBluetoothProfile已经初始化完毕，代表需要直接扫描，那么此时直接开启扫描
        if(mBluetoothProfile ==null){
            //TODO 1.step 获取输出设备的操作对象BluetoothProfile
            mInputDeviceServiceListener = new InputDeviceServiceListener();
            mBluetoothAdapter.getProfileProxy(this, mInputDeviceServiceListener, INPUT_DEVICE);
        } else {
            KEEP_SCAN = true;
            scanLeDevice(true);
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
            if (mScanning) {
                mScanning = false;
                stopLeScan();
            }
        }
    };
    public void scanLeDevice(final boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.removeCallbacks(stopLeScanRunnable);
            mHandler.postDelayed(stopLeScanRunnable, SCAN_PERIOD);
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
        Log.d(TAG, ">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> startLeScan" );
        if (mBLEScanner != null)
            mBLEScanner.startScan(mScanFilterList, mScanSettings, mScanCallback);
    }
    void stopLeScan() {
        Log.d(TAG, ">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> stopLeScan" );
        if (mBLEScanner != null) mBLEScanner.stopScan(mScanCallback);
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
            // TODO 3.step 验证扫描结果
            if (mPairingDevice != null) {
                return;
            }
            mPairingDevice = device;
            scanLeDevice(false);
            Log.d(TAG, "onScanResult  <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<Name:"+mPairingDevice.getName());
            Log.d(TAG, "onScanResult  <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<MAC:"+mPairingDevice.getAddress());
            Log.d(TAG, "onScanResult  <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<RSSI:" +(0 - rssi));
            //条件是否匹配
            if (isGoodMatchRc(mPairingDevice, rssi, scanRecord)) {
                Log.d(TAG, "onScanResult  GoodGoodGoodGoodGoodGoodGoodGood isGoodMatchRc: "+mPairingDevice.getAddress());
                //找到可配对的设备，停止LE扫描
                if (createBond(device)) {
                    // TODO 4.step 等待广播mBluetoothReceiver更新结果
                    showDialog(); //Dialog和广播同步
                } else {
                    removeBond(mPairingDevice);// 若连接失败，移除它
                    mPairingDevice = null;
                }
            } else {
                mPairingDevice = null;
            }
            //无法配对设备，开启LE扫描，重新寻找
            if (mPairingDevice == null) {
                scanLeDevice(true);
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
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            if (device == null) return;
            if(mPairingDevice == null) {
                if (isHmdDevice(device)) {
                    if (action.equals(ACTION_CONNECTION_STATE_CHANGED)) {
                        int connectState = intent.getIntExtra(BluetoothProfile.EXTRA_STATE, BluetoothProfile.STATE_CONNECTED);
                        if (connectState == BluetoothProfile.STATE_CONNECTED) {// 遥控器连接后是否关闭扫描
                            scanLeDevice(false);
                        } else if (connectState == BluetoothProfile.STATE_DISCONNECTED) {// 遥控器断开后断是否开启扫描
                            if (hasDeviceIsConnected() != null) {
                                AutoPairGlobalConfig.setRcMac(device.getAddress());// 设置MAC地址
                                if(!KEEP_SCAN) //可能是替换设备，此时不能关闭扫描
                                    scanLeDevice(false);
                            } else {
                                scanLeDevice(true);
                            }
                        }
                    }
                }
            } else {
                // 配对时发生的状态改变
                if (mPairingDevice.getAddress().equals(device.getAddress())) {
                    // 接收到了UUID直接尝试连接
                    if (action.equals(ACTION_UUID)) {
                        connect(device);
                    } else  if (action.equals(ACTION_BOND_STATE_CHANGED)) { // 是否是正在连接设备的状态的改变
                        int bondState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR);
                        if (bondState == BluetoothDevice.BOND_NONE) { // 绑定失败
                            mHandler.sendEmptyMessageDelayed(PairHandler.MSG_PAIR_FAIL, 20000);
                        } else if (bondState == BluetoothDevice.BOND_BONDING) { // 正在绑定...
                            mHandler.removeMessages(PairHandler.MSG_PAIR_FAIL);
                        } else if (bondState == BluetoothDevice.BOND_BONDED) { // 绑定成功
                            mHandler.removeMessages(PairHandler.MSG_PAIR_FAIL);
                        } else if (bondState == BluetoothDevice.ERROR) {
                            mHandler.sendEmptyMessage(PairHandler.MSG_PAIR_FAIL);
                        }
                    } else if (action.equals(ACTION_CONNECTION_STATE_CHANGED)) {
                        int connectState = intent.getIntExtra(BluetoothProfile.EXTRA_STATE, BluetoothProfile.STATE_CONNECTED);
                        if (connectState == BluetoothProfile.STATE_DISCONNECTED) { // 连接失败
                            mHandler.removeMessages(PairHandler.MSG_PAIR_TIMEOUT);
                            mHandler.sendEmptyMessage(PairHandler.MSG_PAIR_FAIL);
                        } else if (connectState == BluetoothProfile.STATE_CONNECTING) { // 连接中...
                            mHandler.removeMessages(PairHandler.MSG_PAIR_FAIL);
                        } else if (connectState == BluetoothProfile.STATE_CONNECTED) { // 连接成功
                            mHandler.removeMessages(PairHandler.MSG_PAIR_TIMEOUT);
                            mHandler.removeMessages(PairHandler.MSG_PAIR_FAIL);
                            mHandler.sendEmptyMessage(PairHandler.MSG_PAIR_SUCESS);
                            AutoPairGlobalConfig.setRcMac(mPairingDevice.getAddress());// 设置MAC地址
                        }
                    }
                }
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
            } else if( System.currentTimeMillis() - matchTime > 10*1000) {
                mHandler.sendEmptyMessage(PairHandler.MSG_RSSI_LOW);
                matchTime = System.currentTimeMillis();
            }
        }
        return false;
    }
    public  boolean isHmdDevice(BluetoothDevice device) {
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
    private  BluetoothDevice hasDeviceIsConnected() {
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
    private class PairHandler extends Handler {
        private static final int MSG_SHOW_DIALOG = 0x1;
        private static final int MSG_DIS_DIALOG = 0x2;
        private static final int MSG_PAIR_TIMEOUT = 0x3;
        private static final int MSG_PAIR_SUCESS = 0x4;
        private static final int MSG_PAIR_FAIL = 0x5;
        private static final int MSG_RSSI_LOW = 0x6;
        private static final int MSG_TIMEOUIT_REBT = 0x7;
        private static final int MSG_REBT = 0x7;
        private final WeakReference<Context> mContext;
        public PairHandler(Context context) {
            mContext = new WeakReference<Context>(context);
        }
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_SHOW_DIALOG:
                    if (mPairDialog != null) {
                        mPairDialog.show();
                        showDialogProgress(true);
                        setDialogText(R.string.auto_pairng);
                    }
                    break;
                case MSG_DIS_DIALOG:
                    mHandler.removeMessages(MSG_PAIR_TIMEOUT);
                    mHandler.removeMessages(MSG_PAIR_FAIL);
                    mPairDialog.dismiss();
                    disDialogPair();
                    break;
                case MSG_PAIR_TIMEOUT:
                    mHandler.removeCallbacksAndMessages(null);
                    showDialogProgress(false);
                    setDialogText(R.string.auto_pair_timeout);
                    disDialog(3);
                    // 配对超时，可能是蓝牙服务出问题了，可以重新启动蓝牙
                    // mHandler.sendEmptyMessageDelayed(MSG_TIMEOUIT_REBT,3000);
                    break;
                case MSG_PAIR_SUCESS:
                    showDialogProgress(false);
                    setDialogText(R.string.auto_pair_sucess);
                    disDialog(3);
                    break;
                case MSG_PAIR_FAIL:
                    mHandler.removeMessages(MSG_PAIR_FAIL);
                    mHandler.removeMessages(MSG_PAIR_TIMEOUT);
                    mHandler.removeMessages(MSG_RSSI_LOW);
                    //mBluetoothAdapter.disable();//关闭蓝牙
                    //mHandler.sendEmptyMessageDelayed(MSG_TIMEOUIT_REBT,3000);
                    showDialogProgress(false);
                    setDialogText(R.string.auto_pair_fail);
                    disDialog(3);
                    break;
                case MSG_RSSI_LOW:
                    mHandler.removeMessages(MSG_RSSI_LOW);
                    showDialogProgress(false);
                    setDialogText(R.string.auto_pair_rssi_low);
                    disDialog(3);
                    break;
                case MSG_TIMEOUIT_REBT:
                    mBluetoothAdapter.enable();//重新开启蓝牙
                    break;
                default:
                    super.handleMessage(msg);
                    break;
            }
        }
    }
    public AutoPairDialog mPairDialog;
    public void initUI() {
        if (mPairDialog == null) {
            mPairDialog = new AutoPairDialog(this);
            mPairDialog.getWindow().setType((WindowManager.LayoutParams.TYPE_SYSTEM_ALERT));
            mHandler = new PairHandler(this.getBaseContext());
        }
    }
    public void showDialog() {
        mHandler.removeCallbacksAndMessages(null);
        mHandler.sendEmptyMessage(PairHandler.MSG_SHOW_DIALOG);
        mHandler.sendEmptyMessageDelayed(PairHandler.MSG_PAIR_TIMEOUT, 15 * 1000);// 15s超时
    }
    public void disDialog(int time) {
        mHandler.removeCallbacksAndMessages(null);
        mHandler.sendEmptyMessageDelayed(PairHandler.MSG_DIS_DIALOG, time * 1000);
    }
    public void setDialogText(int resid) {
        if (mPairDialog != null)
            mPairDialog.setDialogText(mPairDialog.getContext().getResources().getString(resid));
    }
    public void showDialogProgress(boolean show) {
        if (mPairDialog != null) mPairDialog.setDialogProgress(show);
    }
    public void disDialogPair() {
        if (mPairingDevice != null) {
            // 没连接成功
            if (getConnectionStatus(mPairingDevice) == BluetoothProfile.STATE_CONNECTED) {
                disOtherDeviceIsConnected();
                rmOtherDeviceIsBonded();
            } else {
                removeBond(mPairingDevice);
            }
        }
        mPairingDevice = null;
        if (hasDeviceIsConnected() != null) {
            scanLeDevice(false);
        } else {
            scanLeDevice(true);
        }
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
        AutoPairService getService() {
            return AutoPairService.this;
        }
    }
}
