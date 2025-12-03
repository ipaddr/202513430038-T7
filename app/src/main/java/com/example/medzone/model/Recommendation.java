package com.example.medzone.model;

public class Recommendation {
    public String name;
    public String dosis;
    public String note;

    // reminder fields
    public boolean reminderSet = false; // indicates if a reminder is set (merged from prefs or cloud)

    public Recommendation() { }

    public Recommendation(String name, String dosis, String note) {
        this.name = name;
        this.dosis = dosis;
        this.note = note;
        this.reminderSet = false;
    }

    public Recommendation(String name, String dosis, String note, boolean reminderSet) {
        this.name = name;
        this.dosis = dosis;
        this.note = note;
        this.reminderSet = reminderSet;
    }
}
