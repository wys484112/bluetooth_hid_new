package com.example.viroyal.bluetooth.model;

public class BtConnectInfo {
    private String mAddress;
    private String mName;
    private String mStatus;

    public BtConnectInfo() {
    }
    public BtConnectInfo(String mAddress, String mName, String mStatus) {
        this.mAddress = mAddress;
        this.mName = mName;
        this.mStatus = mStatus;
    }

    public String getmAddress() {
        return mAddress;
    }

    public void setmAddress(String mAddress) {
        this.mAddress = mAddress;
    }

    public String getmName() {
        return mName;
    }

    public void setmName(String mName) {
        this.mName = mName;
    }

    public String getmStatus() {
        return mStatus;
    }

    public void setmStatus(String mStatus) {
        this.mStatus = mStatus;
    }

    @Override
    public String toString() {
        return "BtConnectInfo{" +
                "mAddress='" + mAddress + '\'' +
                ", mName='" + mName + '\'' +
                ", mStatus='" + mStatus + '\'' +
                '}';
    }
}
