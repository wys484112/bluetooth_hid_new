package com.example.viroyal.bluetooth;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;

import butterknife.BindView;
import butterknife.ButterKnife;


public class MainActivity2 extends Activity implements View.OnClickListener {

    private static final String TAG = "wwww";
    public static final int INPUT_DEVICE = 4;
    @BindView(R.id.bt_name)
    TextView btName;
    @BindView(R.id.bt_address)
    TextView btAddress;
    @BindView(R.id.bt_status)
    TextView btStatus;
    @BindView(R.id.img_remote)
    ImageView imgRemote;
    @BindView(R.id.img_connect)
    ImageView imgConnect;


    private BluetoothProfile mBluetoothProfile;
    private BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();

    private Context context;
    private long delayMillis = 15 * 1000;

    private Handler TimerHandler = new Handler();                   //创建一个Handler对象

    private AnimationDrawable imgRemoteAnimation;
    private AnimationDrawable imgConnectAnimation;

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
        ButterKnife.bind(this);
        context = this;

        /*这个可以同时两个动画显示*/
        imgRemote.setImageResource(R.drawable.ic_remote_anim);
        imgConnect.setImageResource(R.drawable.ic_connect_anim);
        imgRemoteAnimation = (AnimationDrawable) imgRemote.getDrawable();
        imgConnectAnimation = (AnimationDrawable) imgConnect.getDrawable();
        /*这个动画不能同时显示*/
//        imgRemote.setBackgroundResource(R.drawable.ic_remote_anim);
//        imgConnect.setBackgroundResource(R.drawable.ic_connect_anim);
//        imgRemoteAnimation = (AnimationDrawable) imgRemote.getBackground();
//        imgConnectAnimation = (AnimationDrawable) imgConnect.getBackground();
        imageViewAnimationStart();


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

    private void imageViewAnimationStart() {
        imgRemoteAnimation.start();
        imgConnectAnimation.start();
    }

    private void imageViewAnimationStop() {
        imgRemoteAnimation.stop();
        imgConnectAnimation.stop();
    }

    private void openBluetooth() {
        if (adapter == null) {
            Toast.makeText(this, "不支持蓝牙功能", Toast.LENGTH_SHORT).show();
            // 不支持蓝牙
            return;
        }
        if (mBluetoothProfile != null) {
            workThreadInit();
        } else {
            updatePhicomPERIPHERALDeviceStatus("正在开启搜索");
            adapter.getProfileProxy(context, mListener,
                    INPUT_DEVICE);
        }
        TimerHandler.postDelayed(myTimerRun, delayMillis);        //使用postDelayed方法，两秒后再调用此myTimerRun对象
    }

    /*定义一个倒计时的内部类*/
    private void delayFinishActivity(String status) {
        if (status.contains("已连接")) {
            mc = new MyCount(5000, 1000);
            mc.start();

        }
    }

    private MyCount mc;

    class MyCount extends CountDownTimer {
        public MyCount(long millisInFuture, long countDownInterval) {
            super(millisInFuture, countDownInterval);
        }

        @Override
        public void onFinish() {
//            tv.setText("finish");
            MainActivity2.this.finish();

        }

        @Override
        public void onTick(long millisUntilFinished) {
//            tv.setText("请等待30秒(" + millisUntilFinished / 1000 + ")...");
//            Toast.makeText(NewActivity.this, millisUntilFinished / 1000 + "",
//                    Toast.LENGTH_LONG).show();//toast有显示时间延迟
        }
    }


    private void workThreadInit() {
        if (adapter.isEnabled()) {
            BluetoothDevice device = hasDeviceIsConnected();
            if (device != null) {
                updatePhicomPERIPHERALDeviceStatus("已连接" + device.getName());
                btName.setText(device.getName());
                btAddress.setText(device.getAddress());
            } else {
                Log.e("wwww", "workThreadInit == startDiscovery");
                updatePhicomPERIPHERALDeviceStatus("正在搜索遥控器");
                ServiceUtils.cleanBoundedPhicomPERIPHERALDevices();
                startDiscovery();
            }
        } else {
            adapter.enable();//强制开启蓝牙
            updatePhicomPERIPHERALDeviceStatus("未连接遥控器");
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
                    Log.e("wwww", "profile == INPUT_DEVICE");
                    mBluetoothProfile = proxy;
                    workThreadInit();
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
                            updatePhicomPERIPHERALDeviceStatus("正在连接" + device.getName());
                        }
                        if (state == BluetoothProfile.STATE_CONNECTED) {//连接成功
                            updatePhicomPERIPHERALDeviceStatus("已连接" + device.getName());
                            btName.setText(device.getName());
                            btAddress.setText(device.getAddress());
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
                        } else {
                            updatePhicomPERIPHERALDeviceStatus("正在开启搜索");
                            adapter.getProfileProxy(context, mListener,
                                    INPUT_DEVICE);
                        }
                    }
                }
                break;
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
                    //发现设备
                    Log.e("wwww", "found");
                    if (isPhicomPERIPHERAL(device)) {
                        btName.setText(device.getName());
                        btAddress.setText(device.getAddress());
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


    void closeProfileProxy(BluetoothProfile mBluetoothProfile) {
        if (mBluetoothProfile != null) {
            try {
                adapter.closeProfileProxy(INPUT_DEVICE, mBluetoothProfile);
                mBluetoothProfile = null;
            } catch (Throwable t) {
                Log.e(TAG, "Error cleaning up HID proxy", t);
            }
        }
    }






//    private void readPhicomPERIPHERALDevices() {
//        BluetoothDevice device = hasDeviceIsConnected();
//        if (device != null) {
//            updatePhicomPERIPHERALDeviceStatus("已连接" + device.getName());
//            btName.setText(device.getName());
//            btAddress.setText(device.getAddress());
//        } else {
//            updatePhicomPERIPHERALDeviceStatus("未连接遥控器");
//        }
////        Set<BluetoothDevice> bondedDevices = adapter.getBondedDevices();
////
////        if (bondedDevices != null) {
////            for (BluetoothDevice device : bondedDevices) {
////                if (isPhicomPERIPHERAL(device)) {
////                    try {
////                        if (isConnected(BluetoothDevice.class, device)) {
////                            btName.setText(device.getName());
////                            btAddress.setText(device.getAddress());
////                            updatePhicomPERIPHERALDeviceStatus("已连接" + device.getName());
////                            return;
////                        }
////                    } catch (Exception e) {
////                        e.printStackTrace();
////                    }
////                }
////            }
////        }
//    }

    private void updatePhicomPERIPHERALDeviceStatus(String status) {
        delayFinishActivity(status);
        btStatus.setText(status);

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

        imageViewAnimationStop();

        unregisterReceiver(receiver);

        ServiceUtils.cancelDiscovery();

        ServiceUtils.closeProfileProxy(mBluetoothProfile);

        TimerHandler.removeCallbacks(myTimerRun);

        /*倒计时的清理*/
        if (mc != null) {
            mc.cancel();
            mc = null;
        }
    }

    @Override
    public void onClick(View v) {

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    }
}
      