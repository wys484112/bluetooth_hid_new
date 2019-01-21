package com.example.viroyal.bluetooth;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.viroyal.bluetooth.model.BtConnectInfo;
import com.hwangjr.rxbus.RxBus;
import com.hwangjr.rxbus.annotation.Subscribe;

import java.lang.reflect.Method;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.schedulers.Schedulers;


public class MainActivity2New extends Activity {
    private static final String TAG = MainActivity2New.class.getSimpleName();
    private static final boolean DBG = true;
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

    private AnimationDrawable imgRemoteAnimation;
    private AnimationDrawable imgConnectAnimation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        /*动画显示*/
        imgRemote.setImageResource(R.drawable.ic_remote_anim);
        imgConnect.setImageResource(R.drawable.ic_connect_anim);
        imgRemoteAnimation = (AnimationDrawable) imgRemote.getDrawable();
        imgConnectAnimation = (AnimationDrawable) imgConnect.getDrawable();

        imageViewAnimationStart();

        RxBus.get().register(this);

        if (ServiceUtils.isServiceRunning(this, AutoPairCheckService.class.getName())) {
            if (DBG)
                Log.d(TAG, "isServiceRunning");
        } else {
            Intent autoPairService = new Intent(this, AutoPairCheckService.class);
            startService(autoPairService);
            if (DBG)
                Log.d(TAG, "startService");
        }
        if (AutoPairCheckService.getBtConnectInfo() != null) {
            BtConnectInfo info = AutoPairCheckService.getBtConnectInfo();
            if (info.getmStatus() != null) {
                btStatus.setText(info.getmStatus());
            }
            if (info.getmName() != null) {
                btName.setText(info.getmName());
            }
            if (info.getmAddress() != null) {
                btAddress.setText(info.getmAddress());
            }
        }
    }

    private void imageViewAnimationStart() {
        imgRemoteAnimation.start();
        imgConnectAnimation.start();
    }

    private void imageViewAnimationStop() {
        imgRemoteAnimation.stop();
        imgConnectAnimation.stop();
    }

    @Subscribe
    public void updatePhicomPERIPHERALDeviceStatus(BtConnectInfo btConnectInfo) {
        if (DBG)
            Log.d(TAG, "updatePhicomPERIPHERALDeviceStatus  btConnectInfo==" + btConnectInfo);
        delayFinishActivity(btConnectInfo.getmStatus());
        btStatus.setText(btConnectInfo.getmStatus());
        btName.setText(btConnectInfo.getmName());
        btAddress.setText(btConnectInfo.getmAddress());

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (DBG)
            Log.d(TAG, "onDestroy");

        imageViewAnimationStop();
        RxBus.get().unregister(this);

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
            MainActivity2New.this.finish();

        }

        @Override
        public void onTick(long millisUntilFinished) {

        }
    }
}
      