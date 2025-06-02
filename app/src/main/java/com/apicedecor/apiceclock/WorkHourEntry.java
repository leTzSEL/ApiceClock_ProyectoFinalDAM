package com.apicedecor.apiceclock;

public class WorkHourEntry {
    String date;
    String name;
    String surname;
    String email;
    long startTimestamp;
    long endTimestamp;
    int totalMinutes;

    public WorkHourEntry(String date, String name, String surname, String email, long startTimestamp, long endTimestamp) {
        this.date = date;
        this.name = name;
        this.surname = surname;
        this.email = email;
        this.startTimestamp = startTimestamp;
        this.endTimestamp = endTimestamp;
        this.totalMinutes = (int) ((endTimestamp - startTimestamp) / (60 * 1000)); // minutos
    }
}
