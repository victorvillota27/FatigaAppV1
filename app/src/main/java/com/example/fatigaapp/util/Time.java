package com.example.fatigaapp.util;

public class Time {
    private long timIni=0;
    private long timFin=0;
    private long timAct =0;
    public Time() {
    }

    public void setTimIni(long timIni) { this.timIni = timIni; }

    public void setTimFin(long timFin) { this.timFin = timFin; }

    public long getTimFin() { return timFin; }
    public long getTimIni() { return timIni; }
    public void setTimAct(long timAct) { this.timAct = timAct; }
    public long getTimAct() {
        timAct = timFin - timIni;
        return timAct;
    }
}
