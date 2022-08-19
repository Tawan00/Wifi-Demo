package com.edc.kbtg.nativebroadcast;

public class EDCBroadcaster {


    public EDCBroadcaster(){
        System.loadLibrary("BroadCastDiscovery");
    }
    public native void StartEDCProcess(String tid, String mid);
    public native void StopProcess();
}
