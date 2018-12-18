package com.example.viroyal.bluetooth.database;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;

@Table(name="students")
public class Student extends Model {
    private final static String Tag=Student.class.getSimpleName();

    @Column(name="sid",unique = true)
    private long sid;
    @Column
    private String name;
    @Column
    private String tel_no;
    @Column
    private int cls_id;

    public String getName() {
        return name;
    }

    public long getSid() {
        return sid;
    }

    public void setSid(long sid) {
        this.sid = sid;
    }

    public Student() {
        super();
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTel_no() {
        return tel_no;
    }

    public void setTel_no(String tel_no) {
        this.tel_no = tel_no;
    }

    public int getCls_id() {
        return cls_id;
    }

    public void setCls_id(int cls_id) {
        this.cls_id = cls_id;
    }

    @Override
    public String toString() {
        return "Student{" +
                "id=" + getId() +
                ", sid=" + sid +
                ", name='" + name + '\'' +
                ", tel_no='" + tel_no + '\'' +
                ", cls_id=" + cls_id +
                '}';
    }
}
