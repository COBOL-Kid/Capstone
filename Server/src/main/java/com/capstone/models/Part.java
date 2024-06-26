package com.capstone.models;

public class Part {

    private String desc;
    private String manufacturer;
    private double price;
    private int qty;

    public Part() {
    }

    public Part(String desc, String manufacturer, double price, int qty) {
        this.desc = desc;
        this.manufacturer = manufacturer;
        this.price = price;
        this.qty = qty;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public void setManufacturer(String manufacturer) {
        this.manufacturer = manufacturer;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public int getQty() {
        return qty;
    }

    public void setQty(int qty) {
        this.qty = qty;
    }
}
