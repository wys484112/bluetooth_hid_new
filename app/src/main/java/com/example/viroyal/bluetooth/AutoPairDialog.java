package com.example.viroyal.bluetooth;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.View;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.TextView;

/**
 * TODO: document your custom view class.
 */
public class AutoPairDialog extends ProgressDialog {

    private ProgressBar mProgress;
    private TextView mMessageView;

    public AutoPairDialog(Context context) {
        super(context);
    }

    public AutoPairDialog(Context context, int theme) {
        super(context, theme);


    }

    public void setDialogText(String text){
        mMessageView.setText(text);
    }
    public void setDialogProgress(Boolean show){
        if(show){
            mProgress.setVisibility(View.VISIBLE);
        }else{
            mProgress.setVisibility(View.INVISIBLE);
        }
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init(getContext());
    }
    private void init(Context context) {
        setCancelable(true);
        setCanceledOnTouchOutside(false);
        setContentView(R.layout.loading);//loading的xml文件
        mProgress=(ProgressBar)findViewById(R.id.pb_load);
        mMessageView=(TextView)findViewById(R.id.tv_load_dialog);
        WindowManager.LayoutParams params = getWindow().getAttributes();
        params.width = WindowManager.LayoutParams.WRAP_CONTENT;
        params.height = WindowManager.LayoutParams.WRAP_CONTENT;
        getWindow().setAttributes(params);
    }
    @Override
    public void show() {//开启
        super.show();
    }
    @Override
    public void dismiss() {//关闭
        super.dismiss();
    }
}
