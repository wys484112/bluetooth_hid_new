package com.example.viroyal.bluetooth;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.activeandroid.ActiveAndroid;
import com.example.viroyal.bluetooth.database.Student;
import com.example.viroyal.bluetooth.service.StudentService;
import com.example.viroyal.bluetooth.service.impl.StudentServiceImpl;
import com.jakewharton.rxbinding3.view.RxView;

import java.util.Random;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import kotlin.Unit;

public class Main3Activity extends AppCompatActivity {
    private static final String TAG = Main3Activity.class.getSimpleName();

    @BindView(R.id.button3)
    Button button3;
    @BindView(R.id.button4)
    Button button4;
    @BindView(R.id.textView2)
    TextView textView2;
    @BindView(R.id.textView3)
    TextView textView3;
    private Observable<Unit> btInsertObservable;
    private Observable<Unit> btReadObservable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main3);
        ButterKnife.bind(this);


        //rxjava//rxandroid使用实例 begin
        btInsertObservable = RxView.clicks(button3).throttleFirst(1, TimeUnit.SECONDS)
                .subscribeOn(AndroidSchedulers.mainThread())
                .doOnNext(new Consumer<Unit>() {
                    @Override
                    public void accept(Unit unit) throws Exception {
                        Log.d(TAG, "doOnNext");

                    }
                });
        btInsertObservable.subscribe(new Consumer<Unit>() {
            @Override
            public void accept(Unit unit) throws Exception {

                //事务 处理多条信息是用事务
                ActiveAndroid.beginTransaction();
                try {
                    for (int i = 0; i < 5; i++) {
                        Log.d(TAG, "insert student:" + i);
                        mockStudent(i);
                    }
                    ActiveAndroid.setTransactionSuccessful();
                }
                finally {
                    ActiveAndroid.endTransaction();
                }


            }
        });


        btReadObservable = RxView.clicks(button4).throttleFirst(1, TimeUnit.SECONDS)
                .subscribeOn(AndroidSchedulers.mainThread());
        btReadObservable.subscribe(new Consumer<Unit>() {
            @Override
            public void accept(Unit unit) throws Exception {

                Student temp=new Student();
                temp.setSid(2);
                temp.setName("update_student_");
                temp.setCls_id(555555555);
                temp.setTel_no(String.valueOf(555555));
                StudentServiceImpl.getInstance().updateByPrimaryKey(temp);

                //事务
                StudentServiceImpl.getInstance().findAllStudent();

            }
        });

        //rxjava//rxandroid使用实例 end


    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        btInsertObservable.unsubscribeOn(AndroidSchedulers.mainThread());
    }

    public void mockStudent(int i) {
        Student student = new Student();
        student.setSid(i);
        student.setName("student_" + i);
        student.setCls_id(new Random().nextInt(5));
        student.setTel_no(String.valueOf(new Random().nextLong()));
        StudentServiceImpl.getInstance().insert(student);
    }

}
