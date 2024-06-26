package com.capstone.models;

import java.util.List;

public class Maintenance {
    private String desc;
    private int due_mileage;
    private int due_km;
    private boolean is_oem;
    private boolean is_cycle;
    private int cycle_mileage;
    private int cycle_km;
    private Repair repair;
    private List<Part> parts;

    public Maintenance() {
    }

    public Maintenance(String desc, int due_mileage, int due_km, boolean is_oem, boolean is_cycle, int cycle_mileage, int cycle_km, Repair repair, List<Part> parts) {
        this.desc = desc;
        this.due_mileage = due_mileage;
        this.due_km = due_km;
        this.is_oem = is_oem;
        this.is_cycle = is_cycle;
        this.cycle_mileage = cycle_mileage;
        this.cycle_km = cycle_km;
        this.repair = repair;
        this.parts = parts;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public int getDue_mileage() {
        return due_mileage;
    }

    public void setDue_mileage(int due_mileage) {
        this.due_mileage = due_mileage;
    }

    public int getDue_km() {
        return due_km;
    }

    public void setDue_km(int due_km) {
        this.due_km = due_km;
    }

    public boolean isIs_oem() {
        return is_oem;
    }

    public void setIs_oem(boolean is_oem) {
        this.is_oem = is_oem;
    }

    public boolean isIs_cycle() {
        return is_cycle;
    }

    public void setIs_cycle(boolean is_cycle) {
        this.is_cycle = is_cycle;
    }

    public int getCycle_mileage() {
        return cycle_mileage;
    }

    public void setCycle_mileage(int cycle_mileage) {
        this.cycle_mileage = cycle_mileage;
    }

    public int getCycle_km() {
        return cycle_km;
    }

    public void setCycle_km(int cycle_km) {
        this.cycle_km = cycle_km;
    }

    public Repair getRepair() {
        return repair;
    }

    public void setRepair(Repair repair) {
        this.repair = repair;
    }

    public List<Part> getParts() {
        return parts;
    }

    public void setParts(List<Part> parts) {
        this.parts = parts;
    }
}
