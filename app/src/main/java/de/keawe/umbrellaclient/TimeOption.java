package de.keawe.umbrellaclient;

import androidx.annotation.NonNull;

public class TimeOption {
    private final int minutes;
    private final String text;

    public TimeOption(String text, int minutes){
        this.text = text;
        this.minutes = minutes;
    }

    @NonNull
    @Override
    public String toString() {
        return text;
    }

    public int minutes(){
        return minutes;
    }
}
