package com.example.viroyal.bluetooth.service.impl;

import android.util.Log;

import com.activeandroid.query.Delete;
import com.activeandroid.query.Select;
import com.activeandroid.query.Update;
import com.example.viroyal.bluetooth.database.Student;
import com.example.viroyal.bluetooth.service.StudentService;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class StudentServiceImpl implements StudentService {
    private final static String TAG = StudentServiceImpl.class.getSimpleName();
    private static StudentService mSservie;

    public StudentServiceImpl() {
    }

    public static synchronized StudentService getInstance() {
        if (mSservie == null) {
            mSservie = new StudentServiceImpl();
        }
        return mSservie;
    }

    @Override
    public int deleteByPrimaryKey(Long id) {
        new Delete().from(Student.class).where("sid=?", id).execute();
        return 0;
    }

    @Override
    public long insert(Student record) {
        return record.save();
    }

    @Override
    public int insertSelective(Student record) {
        return 0;
    }

    @Override
    public Student selectByPrimaryKey(Long id) {
        return (Student) new Select().from(Student.class).where("sid=?", id).execute().get(0);
    }

    @Override
    public int updateByPrimaryKeySelective(Student record) {
        return 0;
    }

    @Override
    public int updateByPrimaryKey(Student record) {
        new Update(Student.class).set("name=?," + "tel_no=?," + "cls_id=?", record.getName(), record.getTel_no(), record.getCls_id()).where("sid=?", record.getSid()).execute();
        return 0;
    }

    @Override
    public Set<String> findRoleByUserId(Long id) {
        return null;
    }

    @Override
    public List<Student> findAllStudent() {
        List<Student> result = new Select().from(Student.class).execute();
        for (int i = 0; i < result.size(); i++) {
            Log.e(TAG, "学生信息~~~" + result.get(i));
        }
        return null;
    }

    @Override
    public void initData() {

    }
}
