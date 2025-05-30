package com.apicedecor.apiceclock;

public class WorkHourEntry {
    public String date;
    public String time;
    public String name;
    public String surname;
    public String email;
    public boolean isStart;

    public WorkHourEntry() { }

    public WorkHourEntry(String date, String time, String name, String surname, String email, boolean isStart) {
        this.date = date;
        this.time = time;
        this.name = name;
        this.surname = surname;
        this.email = email;
        this.isStart = isStart;
    }
}
