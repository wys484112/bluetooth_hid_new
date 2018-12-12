package com.example.viroyal.bluetooth;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;

public class Main2Activity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

//        loding("aaaaa",true);
        Intent autoPairService = new Intent(this, AutoPairServiceNew.class);
        this.startService(autoPairService);

    }
    AutoPairDialog loading;

    public void loding(String txt,Boolean show) {//点击加载并按钮模仿网络请求
        loading = new AutoPairDialog(this, R.style.CustomDialog);
        loading.show();
        loading.setDialogText(txt);
        loading.setDialogProgress(show);
    }
}
