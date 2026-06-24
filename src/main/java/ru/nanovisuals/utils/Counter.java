package ru.nanovisuals.utils;

public class Counter {

    private long lastReset = System.currentTimeMillis();

    public void reset() {
        lastReset = System.currentTimeMillis();
    }

    public long getPassedTimeMs() {
        return System.currentTimeMillis() - lastReset;
    }

    public boolean passedMs(long ms) {
        return getPassedTimeMs() >= ms;
    }
}
