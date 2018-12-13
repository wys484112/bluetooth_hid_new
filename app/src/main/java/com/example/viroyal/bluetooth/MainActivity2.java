package com.example.viroyal.bluetooth;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;


public class MainActivity2 extends Activity implements View.OnClickListener {

    private static final String TAG = "wwww";
    public static final int INPUT_DEVICE = 4;
    private BluetoothProfile mBluetoothProfile;
    private BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();

    private TextView txt_name, txt_address, txt_status;
    private Context context;
    private long delayMillis = 15 * 1000;

    private Handler TimerHandler = new Handler();                   //创建一个Handler对象
    private CountDownTimer mTimer;


    Runnable myTimerRun = new Runnable()                //创建一个runnable对象
    {
        @Override
        public void run() {
            workThreadInit();
            TimerHandler.postDelayed(this, delayMillis);      //再次调用myTimerRun对象，实现每两秒一次的定时器操作
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context = this;
        txt_name = (TextView) findViewById(R.id.txt_name2);
        txt_address = (TextView) findViewById(R.id.txt_address2);
        txt_status = (TextView) findViewById(R.id.txt_status);


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
        registerReceiver(receiver, filter);


        openBluetooth();

    }

    private void openBluetooth() {
        if (adapter == null) {
            Toast.makeText(this, "不支持蓝牙功能", Toast.LENGTH_SHORT).show();
            // 不支持蓝牙
            return;
        }
        if (mBluetoothProfile != null) {
            workThreadInit();
            readPhicomPERIPHERALDevices();
        } else {
            updatePhicomPERIPHERALDeviceStatus("正在开启搜索");
            adapter.getProfileProxy(context, mListener,
                    INPUT_DEVICE);
        }
        TimerHandler.postDelayed(myTimerRun, delayMillis);        //使用postDelayed方法，两秒后再调用此myTimerRun对象
    }

    private void workThreadInit() {
        if (adapter.isEnabled()) {
            if (hasDeviceIsConnected()!=null) {
                finish();//已经连接 退出界面；
            } else {
                Log.e("wwww","workThreadInit == startDiscovery");
                updatePhicomPERIPHERALDeviceStatus("正在搜索遥控器");
                cleanBoundedPhicomPERIPHERALDevices();
                startDiscovery();
            }
        } else {
            adapter.enable();//强制开启蓝牙
        }
    }

    private void startDiscovery() {
        if (adapter.isEnabled()) {
            if (adapter.isDiscovering()) {
                return;
            }
            adapter.startDiscovery();
        }

    }
    // 是否有连接的遥控器
    private BluetoothDevice hasDeviceIsConnected() {
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
    private BluetoothProfile.ServiceListener mListener = new BluetoothProfile.ServiceListener() {
        @Override
        public void onServiceConnected(int profile, BluetoothProfile proxy) {
            Log.i(TAG, "mConnectListener onServiceConnected");
            //BluetoothProfile proxy这个已经是BluetoothInputDevice类型了
            try {
                if (profile == INPUT_DEVICE) {
                    Log.e("wwww","profile == INPUT_DEVICE");
                    mBluetoothProfile = proxy;
                    workThreadInit();
                    readPhicomPERIPHERALDevices();
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

    private BroadcastReceiver receiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            switch (action) {

                case BluetoothAdapter.ACTION_DISCOVERY_STARTED: {
                    updatePhicomPERIPHERALDeviceStatus("正在搜索遥控器");

                }
                break;
                case BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED: {
                    int state = intent.getIntExtra(BluetoothAdapter.EXTRA_CONNECTION_STATE, 0);
                    int previousState = intent.getIntExtra(BluetoothAdapter.EXTRA_PREVIOUS_STATE, 0);

                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    if (isPhicomPERIPHERAL(device)) {
                        if (state == BluetoothAdapter.STATE_DISCONNECTED) {
                            updatePhicomPERIPHERALDeviceStatus("连接失败");
                        }
                    }
                }
                break;
                case "android.bluetooth.input.profile.action.CONNECTION_STATE_CHANGED": {
                    int state = intent.getIntExtra(BluetoothProfile.EXTRA_STATE, 0);
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    Log.i(TAG, "state=" + state + ",device=" + device);
                    if (isPhicomPERIPHERAL(device)) {
                        if (state == BluetoothProfile.STATE_CONNECTING) {//连接成功
                            Toast.makeText(MainActivity2.this, "正在连接", Toast.LENGTH_LONG).show();

                        }

                        if (state == BluetoothProfile.STATE_CONNECTED) {//连接成功
                            updatePhicomPERIPHERALDeviceStatus("已连接" + device.getName());
                            finish();

                        } else if (state == BluetoothProfile.STATE_DISCONNECTED) {//连接失败
                            updatePhicomPERIPHERALDeviceStatus("连接失败");

                        }
                    }

                }
                break;
                case BluetoothAdapter.ACTION_STATE_CHANGED: {
                    int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1);
                    int previousState = intent.getIntExtra(BluetoothAdapter.EXTRA_PREVIOUS_STATE, -1);
                    if (previousState != BluetoothAdapter.STATE_ON && state == BluetoothAdapter.STATE_ON) {
                        if (mBluetoothProfile != null) {
                            workThreadInit();
                            readPhicomPERIPHERALDevices();
                        } else {
                            updatePhicomPERIPHERALDeviceStatus("正在开启搜索");
                            adapter.getProfileProxy(context, mListener,
                                    INPUT_DEVICE);
                        }

                    }
                }

                break;
//                case BluetoothAdapter.ACTION_SCAN_MODE_CHANGED:
//                    break;


                case BluetoothDevice.ACTION_BOND_STATE_CHANGED: {
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    int state = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, -1);
                    int previousState = intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, -1);

                    if (isPhicomPERIPHERAL(device)) {
                        if (state == BluetoothDevice.BOND_BONDING) {
                            updatePhicomPERIPHERALDeviceStatus("配对中");

                        }
                        if (state == BluetoothDevice.BOND_BONDED) {
                            updatePhicomPERIPHERALDeviceStatus("已配对");

                        }
                        if (state == BluetoothDevice.BOND_BONDED && previousState == BluetoothDevice.BOND_BONDING) {
                            updatePhicomPERIPHERALDeviceStatus("正在连接");
                            connect(device);//连接设备
                        }
                    }


                }

                break;

                case BluetoothDevice.ACTION_FOUND: {
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    //外部设备
                    Log.e("wwww", "found");
                    if (isPhicomPERIPHERAL(device)) {
                        txt_name.setText(device.getName());
                        txt_address.setText(device.getAddress());
                        if (adapter.isDiscovering()) {
                            adapter.cancelDiscovery();
                        }

                        if (device.getBondState() == BluetoothDevice.BOND_NONE) {
                            try {
                                createBond(BluetoothDevice.class, device);
                                updatePhicomPERIPHERALDeviceStatus("正在配对");

                            } catch (Exception e) {
                                updatePhicomPERIPHERALDeviceStatus("配对失败");

                                e.printStackTrace();
                            }
                            //未配对
                        } else if (device.getBondState() == BluetoothDevice.BOND_BONDED) {
                            //已配对
                            try {
                                updatePhicomPERIPHERALDeviceStatus("已配对，正在连接");
                                connect(device);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }


                    }
                }


                break;
                case BluetoothDevice.ACTION_PAIRING_REQUEST:
                    break;
            }
        }
    };


    void closeProfileProxy() {
        if (mBluetoothProfile != null) {
            try {
                adapter.closeProfileProxy(INPUT_DEVICE, mBluetoothProfile);
                mBluetoothProfile = null;
            } catch (Throwable t) {
                Log.e(TAG, "Error cleaning up HID proxy", t);
            }
        }
    }


    private void cleanBoundedPhicomPERIPHERALDevices() {
        Set<BluetoothDevice> bondedDevices = adapter.getBondedDevices();
        if (bondedDevices != null) {
            for (BluetoothDevice device : bondedDevices) {
                if (isPhicomPERIPHERAL(device)) {
                    if (device.getBondState() == BluetoothDevice.BOND_BONDING) {
                        try {
                            cancelBondProcess(BluetoothDevice.class, device);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                    }
                    if (device.getBondState() != BluetoothDevice.BOND_NONE) {
                        final BluetoothDevice dev = device;
                        if (dev != null) {
                            try {
                                final boolean successful = cancelBondProcess(BluetoothDevice.class, device);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }

                        }
                    }
                }
            }
        }
    }


    private void readPhicomPERIPHERALDevices() {
        Set<BluetoothDevice> bondedDevices = adapter.getBondedDevices();
        if (bondedDevices != null) {
            for (BluetoothDevice device : bondedDevices) {
                if (isPhicomPERIPHERAL(device)) {
                    try {
                        if (isConnected(BluetoothDevice.class, device)) {
                            txt_name.setText(device.getName());
                            txt_address.setText(device.getAddress());
                            updatePhicomPERIPHERALDeviceStatus("已连接" + device.getName());
                            return;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        updatePhicomPERIPHERALDeviceStatus("未连接遥控器");
    }

    private void updatePhicomPERIPHERALDeviceStatus(String status) {
        txt_status.setText(status);

    }

    private Boolean isPhicomPERIPHERAL(BluetoothDevice device) {
        if (device != null && device.getBluetoothClass().getMajorDeviceClass() == BluetoothClass.Device.Major.PERIPHERAL) {
            if (device.getName() != null && !device.getName().isEmpty()) {
                if (device.getName().equals("斐讯遥控器")) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * 连接设备
     *
     * @param bluetoothDevice
     */
    public void connect(final BluetoothDevice device) {
        Log.i(TAG, "connect device:" + device);
        try {
            //得到BluetoothInputDevice然后反射connect连接设备
            Method method = mBluetoothProfile.getClass().getMethod("connect",
                    new Class[]{BluetoothDevice.class});
            method.invoke(mBluetoothProfile, device);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    //自动配对设置Pin值
    static public boolean autoBond(Class btClass, BluetoothDevice device, String strPin) throws Exception {
        Method autoBondMethod = btClass.getMethod("setPin", new Class[]{byte[].class});
        Boolean result = (Boolean) autoBondMethod.invoke(device, new Object[]{strPin.getBytes()});
        return result;
    }

    //开始配对
    static public boolean createBond(Class btClass, BluetoothDevice device) throws Exception {
        Method createBondMethod = btClass.getMethod("createBond");
        Boolean returnValue = (Boolean) createBondMethod.invoke(device);
        return returnValue.booleanValue();

    }

    //取消配对
    static public boolean cancelBondProcess(Class btClass, BluetoothDevice device) throws Exception {
        Method cancelBondProcessMethod = btClass.getMethod("cancelBondProcess");
        Boolean returnValue = (Boolean) cancelBondProcessMethod.invoke(device);
        return returnValue.booleanValue();

    }


    //移除配对
    static public boolean removeBond(Class btClass, BluetoothDevice device) throws Exception {
        Method removeBondMethod = btClass.getMethod("removeBond");
        Boolean returnValue = (Boolean) removeBondMethod.invoke(device);
        return returnValue.booleanValue();

    }

    //是否连接
    static public boolean isConnected(Class btClass, BluetoothDevice device) throws Exception {
        Method isConnectedMethod = btClass.getMethod("isConnected");
        Boolean returnValue = (Boolean) isConnectedMethod.invoke(device);
        return returnValue.booleanValue();

    }


    /**
     * 断开连接
     *
     * @param BluetoothDevice
     */
    public void disConnect(BluetoothDevice device) {
        Log.i(TAG, "disConnect device:" + device);
        try {
            if (device != null) {
                Method method = mBluetoothProfile.getClass().getMethod("disconnect",
                        new Class[]{BluetoothDevice.class});
                method.invoke(mBluetoothProfile, device);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.e("wwww", "ondestroy");
        unregisterReceiver(receiver);
        if (adapter.isDiscovering()) {
            adapter.cancelDiscovery();
        }

        closeProfileProxy();
        TimerHandler.removeCallbacks(myTimerRun);
    }

    @Override
    public void onClick(View v) {

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    }
}
      