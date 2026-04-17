package com.capstone.models;

public class Repair {

    private int repair_difficulty;
    private double repair_hours;
    private double labor_rate_per_hour;
    private double part_cost;
    private double labor_cost;
    private double misc_cost;
    private double total_cost;

    public Repair() {
    }

    public Repair(int repair_difficulty, double repair_hours, double labor_rate_per_hour, double part_cost,
            double labor_cost, double misc_cost, double total_cost) {
        this.repair_difficulty = repair_difficulty;
        this.repair_hours = repair_hours;
        this.labor_rate_per_hour = labor_rate_per_hour;
        this.part_cost = part_cost;
        this.labor_cost = labor_cost;
        this.misc_cost = misc_cost;
        this.total_cost = total_cost;
    }

    public int getRepair_difficulty() {
        return repair_difficulty;
    }

    public void setRepair_difficulty(int repair_difficulty) {
        this.repair_difficulty = repair_difficulty;
    }

    public double getRepair_hours() {
        return repair_hours;
    }

    public void setRepair_hours(double repair_hours) {
        this.repair_hours = repair_hours;
    }

    public double getLabor_rate_per_hour() {
        return labor_rate_per_hour;
    }

    public void setLabor_rate_per_hour(double labor_rate_per_hour) {
        this.labor_rate_per_hour = labor_rate_per_hour;
    }

    public double getPart_cost() {
        return part_cost;
    }

    public void setPart_cost(double part_cost) {
        this.part_cost = part_cost;
    }

    public double getLabor_cost() {
        return labor_cost;
    }

    public void setLabor_cost(double labor_cost) {
        this.labor_cost = labor_cost;
    }

    public double getMisc_cost() {
        return misc_cost;
    }

    public void setMisc_cost(double misc_cost) {
        this.misc_cost = misc_cost;
    }

    public double getTotal_cost() {
        return total_cost;
    }

    public void setTotal_cost(double total_cost) {
        this.total_cost = total_cost;
    }
}
