package com.example.gps_project.model;

import com.google.firebase.firestore.DocumentId;

import java.util.Date;

public class Attendance {

    @DocumentId
    private String id = "";

    private Date timestamp = new Date();


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }
}
