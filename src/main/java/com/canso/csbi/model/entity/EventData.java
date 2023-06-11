package com.canso.csbi.model.entity;

import java.text.SimpleDateFormat;
import java.util.Date;

public class EventData {
    private Date timestamp;

    public EventData(Date timestamp) {
        this.timestamp = timestamp;
    }

    public String getData() {
        return "data: " + new SimpleDateFormat("hh:mm:ss").format(timestamp) + "\n\n";
    }
}