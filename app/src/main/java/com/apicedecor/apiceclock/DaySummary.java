package com.apicedecor.apiceclock;

public class DaySummary {
    private String date;
    private String totalHours;
    private String name;
    private String surname;
    private String email;

    public DaySummary() {
        // Necesario para Firestore
    }

    public DaySummary(String date, String totalHours) {
        this.date = date;
        this.totalHours = totalHours;
    }

    public String getDate() {
        return date;
    }

    public String getTotalHours() {
        return totalHours;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public void setTotalHours(String totalHours) {
        this.totalHours = totalHours;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setName(String nombre) {
        this.name = nombre;
    }

    public void setSurname(String apellido) {
        this.surname = apellido;
    }
}

