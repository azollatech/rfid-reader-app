package com.henneth.epcreader;

import java.io.Serializable;

/**
 * Created by hennethcheng on 13/10/16.
 */
public class TagRecord implements Serializable {
    public String sEpc;
    public String time;
    public String ckpt_name;
    public String android_id;
    public String deviceName;
    public String position;

    public TagRecord(String sEpc, String time, String ckpt_name, String android_id, String deviceName, String position) {
        this.sEpc = sEpc;
        this.time = time;
        this.ckpt_name = ckpt_name;
        this.android_id = android_id;
        this.deviceName = deviceName;
        this.position = position;
    }
}
