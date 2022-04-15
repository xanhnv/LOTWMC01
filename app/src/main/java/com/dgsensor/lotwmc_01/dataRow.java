package com.dgsensor.lotwmc_01;

public class dataRow {
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getVol() {
        return vol;
    }

    public void setVol(String vol) {
        this.vol = vol;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    String id;
    String vol;
    String time;
    public dataRow(String id, String vol, String time) {
        this.id = id;
        this.vol = vol;
        this.time = time;
    }

}
