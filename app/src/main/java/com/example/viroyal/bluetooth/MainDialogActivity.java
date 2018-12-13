package com.example.viroyal.bluetooth;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;



public class MainDialogActivity extends Activity {

    private static final String TAG = "wwww";
    private TextView txt_name, txt_address, txt_status;
    private ProgressBar mProgress;
    private Context context;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dialog);
        context = this;
        txt_name = (TextView) findViewById(R.id.txt_name2);
        txt_address = (TextView) findViewById(R.id.txt_address2);
        txt_status = (TextView) findViewById(R.id.txt_status);
        mProgress = (ProgressBar) findViewById(R.id.pb_load);
        processExtraData();

        Intent autoPairService = new Intent(context, AutoPairServiceNew.class);
            startService(autoPairService);

    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        setIntent(intent);
        processExtraData();

    }

    private void processExtraData() {
        Intent intent = getIntent();
        if (intent != null) {
            String name = intent.getStringExtra("name");
            String address = intent.getStringExtra("address");

            String status = intent.getStringExtra("status");
            Boolean show = intent.getBooleanExtra("show", false);
            if (name != null) {
                txt_name.setText(name);
            }
            if (address != null) {
                txt_address.setText(address);
            }
            if (status != null) {
                txt_status.setText(status);
            }
            if (show) {
                mProgress.setVisibility(View.VISIBLE);
            } else {
                mProgress.setVisibility(View.INVISIBLE);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

    }

}
