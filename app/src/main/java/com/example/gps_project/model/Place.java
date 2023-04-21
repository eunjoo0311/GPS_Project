package com.example.gps_project.model;

public class Place {
    private String address;

    public Place(String address) {
        this.address = address;
    }

    public Place() {
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }
}
