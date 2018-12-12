package com.example.viroyal.bluetooth;

public class AutoPairGlobalConfig {
    public static final String DEF_NAME="斐讯遥控器";
    public static String  rcName="";
    public static String  rcMac="";

    public static String getRcName() {
        return rcName;
    }

    public static void setRcName(String rcName) {
        AutoPairGlobalConfig.rcName = rcName;
    }

    public static String getRcMac() {
        return rcMac;
    }

    public static void setRcMac(String rcMac) {
        AutoPairGlobalConfig.rcMac = rcMac;
    }
}
