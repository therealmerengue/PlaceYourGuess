package com.example.trm.placeyourguess;

public final class Settings {

    private Settings() {}

    private static int numOfRounds = 5;
    private static int timer = 30;

    private static boolean streetNamesEnabled = true;

    public static boolean isStreetNamesEnabled() { return streetNamesEnabled; }

    public static void setStreetNamesEnabled(boolean streetNamesEnabled) { Settings.streetNamesEnabled = streetNamesEnabled; }

    public static int getNumOfRounds() { return numOfRounds; }
}
