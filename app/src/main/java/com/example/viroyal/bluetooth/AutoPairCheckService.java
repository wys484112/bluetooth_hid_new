package com.example.viroyal.bluetooth;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;

import com.example.viroyal.bluetooth.model.BtConnectInfo;
import com.hwangjr.rxbus.RxBus;

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

public class AutoPairCheckService extends Service {
    private static final String TAG = AutoPairCheckService.class.getSimpleName();
    private static final boolean DBG = true;


    private Context context;

    private BluetoothProfile mBluetoothProfile;
    private BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
    public static final int INPUT_DEVICE = 4;

    public static BtConnectInfo getBtConnectInfo() {
        return btConnectInfo;
    }

    private static BtConnectInfo btConnectInfo = new BtConnectInfo();
    private CompositeDisposable compositeDisposable = new CompositeDisposable();

    public AutoPairCheckService() {
    }

    private void openBluetooth() {
        compositeDisposable.dispose();
        compositeDisposable = new CompositeDisposable();

        if (adapter == null) {
            Toast.makeText(this, "不支持蓝牙功能", Toast.LENGTH_SHORT).show();
            return;
        }

        if(!adapter.isEnabled()){
            adapter.enable();
        }

        if (mBluetoothProfile != null) {
            if(!isConnectedToPhicomBluetooth()){
                startMainActivity2();
            }
            loopSequence();
        } else {
            adapter.getProfileProxy(context, mListener,
                    INPUT_DEVICE);
        }
    }

    private void loopSequence(){

        Log.d(TAG, "loopSequence loopSequence");

        Disposable disposable = getDataFromServer()
                .doOnSubscribe(new Consumer<Disposable>() {
                    @Override
                    public void accept(Disposable disposable) throws Exception {
                        Log.d(TAG, "loopSequence subscribe");
                    }
                })
                .doOnNext(new Consumer<BtConnectInfo>() {
                    @Override
                    public void accept(BtConnectInfo info) throws Exception {
                        Log.d(TAG, "loopSequence doOnNext: " + info);
                        postBtConnectInfo(info);

                    }
                })
                .doOnError(new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        Log.d(TAG, "loopSequence doOnError: " + throwable.getMessage());
                    }
                })
                .delay(15, TimeUnit.SECONDS, true)       // 设置delayError为true，表示出现错误的时候也需要延迟5s进行通知，达到无论是请求正常还是请求失败，都是5s后重新订阅，即重新请求。
                .subscribeOn(Schedulers.io())
                .repeat()   // repeat保证请求成功后能够重新订阅。
                .retry()    // retry保证请求失败后能重新订阅
                .observeOn(Schedulers.newThread())
                .subscribe(new Consumer<BtConnectInfo>() {
                    @Override
                    public void accept(BtConnectInfo info) throws Exception {
                        if(info.getmStatus().contains("已连接")){
                            compositeDisposable.dispose();
                        }
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                    }
                });
        compositeDisposable.add(disposable);

    }

    private Observable<BtConnectInfo> getDataFromServer() {
        return Observable.create(new ObservableOnSubscribe<BtConnectInfo>() {
            @Override
            public void subscribe(ObservableEmitter<BtConnectInfo> emitter) throws Exception {
                if (emitter.isDisposed()) {
                    return;
                }
                BtConnectInfo info=new BtConnectInfo();
                info=workThreadInit();

//                if (emitter.isDisposed()) {
//                    return;
//                }
                emitter.onNext(info);
                emitter.onComplete();
            }
        });
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

    //开始配对
    static public boolean createBond(Class btClass, BluetoothDevice device) throws Exception {
        Method createBondMethod = btClass.getMethod("createBond");
        Boolean returnValue = (Boolean) createBondMethod.invoke(device);
        return returnValue.booleanValue();
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
                    openBluetooth();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onServiceDisconnected(int profile) {
            Log.i(TAG, "mConnectListener onServiceDisconnected");
        }
    };
    private void startMainActivity2(){
        Log.e(TAG, "startMainActivity2");

        Intent noteList = new Intent(context,MainActivity2New.class);
        noteList.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(noteList);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        context = this;
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
        Log.d(TAG, "service onCreate");

    }

    private static int count=0;
    private BtConnectInfo workThreadInit() {
        count++;
        count=count%3;
        Log.e(TAG, "workThreadInit == count=="+count);

        if (adapter.isEnabled()) {
            BluetoothDevice device = hasDeviceIsConnected();
            if (device != null) {
                btConnectInfo.setmStatus("已连接" + device.getName());
                btConnectInfo.setmName(device.getName());
                btConnectInfo.setmAddress(device.getAddress());
            } else {
                btConnectInfo.setmStatus("正在搜索遥控器");
                btConnectInfo.setmName(getString(R.string.phicom_name));
                btConnectInfo.setmAddress(getString(R.string.phicom_address));
                ServiceUtils.cleanBoundedPhicomPERIPHERALDevices();
                startDiscovery();
            }
        } else {
            adapter.enable();//强制开启蓝牙
            btConnectInfo.setmStatus("未连接遥控器");
            btConnectInfo.setmName(getString(R.string.phicom_name));
            btConnectInfo.setmAddress(getString(R.string.phicom_address));
        }
        return btConnectInfo;
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
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (DBG)
            Log.d(TAG, "onStartCommand");
        openBluetooth();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.e(TAG, "onDestroy");

        ServiceUtils.closeProfileProxy(mBluetoothProfile);
        if (compositeDisposable != null) {
            compositeDisposable.dispose();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    private void postBtConnectInfo(BtConnectInfo Info) {
        RxBus.get().post(Info);
        btConnectInfo.setmStatus(Info.getmStatus());
        btConnectInfo.setmName(Info.getmName());
        btConnectInfo.setmAddress(Info.getmAddress());
    }

    private static int reConnectcount=0;

    private BroadcastReceiver receiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            switch (action) {
                case BluetoothAdapter.ACTION_DISCOVERY_STARTED: {
                    postBtConnectInfo(new BtConnectInfo(getString(R.string.phicom_name),getString(R.string.phicom_address),"正在搜索遥控器"));
                }
                break;
                case BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED: {
                    int state = intent.getIntExtra(BluetoothAdapter.EXTRA_CONNECTION_STATE, 0);
                    int previousState = intent.getIntExtra(BluetoothAdapter.EXTRA_PREVIOUS_STATE, 0);

                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    if (isPhicomPERIPHERAL(device)) {
                        Log.i(TAG, "ACTION_CONNECTION_STATE_CHANGED state=" + state + ",device=" + device);

                        if (state == BluetoothAdapter.STATE_DISCONNECTED) {
                            postBtConnectInfo(new BtConnectInfo(device.getAddress(),device.getName(),"连接失败"));
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
                            postBtConnectInfo(new BtConnectInfo(device.getAddress(),device.getName(),"正在连接" + device.getName()));

                        }
                        if (state == BluetoothProfile.STATE_CONNECTED) {//连接成功
                            reConnectcount=0;
                            postBtConnectInfo(new BtConnectInfo(device.getAddress(),device.getName(),"已连接" + device.getName()));
                        } else if (state == BluetoothProfile.STATE_DISCONNECTED) {//连接失败
                            Log.i(TAG, "device.getBondState()=" + device.getBondState());
                            postBtConnectInfo(new BtConnectInfo(device.getAddress(),device.getName(),"连接失败"));
                        }
                    }
                }
                break;
                case BluetoothAdapter.ACTION_STATE_CHANGED: {
                    int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1);
                    int previousState = intent.getIntExtra(BluetoothAdapter.EXTRA_PREVIOUS_STATE, -1);

                    if (previousState != BluetoothAdapter.STATE_ON && state == BluetoothAdapter.STATE_ON) {
                        openBluetooth();
                    }else if(state != BluetoothAdapter.STATE_ON){
                        openBluetooth();
                    }
                }
                break;
                case BluetoothDevice.ACTION_BOND_STATE_CHANGED: {
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    int state = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, -1);
                    int previousState = intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, -1);

                    if (isPhicomPERIPHERAL(device)) {
                        if (state == BluetoothDevice.BOND_BONDING) {
                            postBtConnectInfo(new BtConnectInfo(device.getAddress(),device.getName(),"配对中"));
                        }
                        if (state == BluetoothDevice.BOND_BONDED) {
                            postBtConnectInfo(new BtConnectInfo(device.getAddress(),device.getName(),"已配对"));

                        }
                        if (state == BluetoothDevice.BOND_BONDED && previousState == BluetoothDevice.BOND_BONDING) {
                            postBtConnectInfo(new BtConnectInfo(device.getAddress(),device.getName(),"正在连接"));

                            connect(device);//连接设备
                        }
                    }
                }
                break;
                case BluetoothDevice.ACTION_FOUND: {
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    //发现设备
//                    Log.e(TAG, "found");
                    if (isPhicomPERIPHERAL(device)) {
                        if (adapter.isDiscovering()) {
                            adapter.cancelDiscovery();
                        }

                        if (device.getBondState() == BluetoothDevice.BOND_NONE) {
                            try {
                                createBond(BluetoothDevice.class, device);
                                postBtConnectInfo(new BtConnectInfo(device.getAddress(),device.getName(),"正在配对"));

                            } catch (Exception e) {
                                postBtConnectInfo(new BtConnectInfo(device.getAddress(),device.getName(),"配对失败"));

                                e.printStackTrace();
                            }
                            //未配对
                        } else if (device.getBondState() == BluetoothDevice.BOND_BONDED) {
                            //已配对
                            try {
                                postBtConnectInfo(new BtConnectInfo(device.getAddress(),device.getName(),"已配对，正在连接"));

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






    private final IBinder mBinder = new ServiceStub(this);

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
