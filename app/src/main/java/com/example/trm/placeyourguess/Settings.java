package com.example.trm.placeyourguess;

public final class Settings {

    private Settings() {}

    private static int numOfRounds = 5;
    private static int timer = 30;
    private static boolean timerEnabled = false;
    private static boolean hintsEnabled = false;
    private static boolean streetNamesEnabled = true;

    public static void setNumOfRounds(int numOfRounds) {
        Settings.numOfRounds = numOfRounds;
    }

    public static int getNumOfRounds() { return numOfRounds; }

    public static int getTimer() {
        return timer;
    }

    public static void setTimer(int timer) {
        Settings.timer = timer;
    }

    public static boolean isTimerEnabled() {
        return timerEnabled;
    }

    public static void setTimerEnabled(boolean timerEnabled) {
        Settings.timerEnabled = timerEnabled;
    }

    public static boolean isHintsEnabled() {
        return hintsEnabled;
    }

    public static void setHintsEnabled(boolean hintsEnabled) {
        Settings.hintsEnabled = hintsEnabled;
    }

    public static boolean isStreetNamesEnabled() { return streetNamesEnabled; }

    public static void setStreetNamesEnabled(boolean streetNamesEnabled) { Settings.streetNamesEnabled = streetNamesEnabled; }
}
