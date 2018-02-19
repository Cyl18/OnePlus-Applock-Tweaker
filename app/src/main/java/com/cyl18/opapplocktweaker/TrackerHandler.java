package com.cyl18.opapplocktweaker;

import de.robv.android.xposed.XposedHelpers;

/**
 * Created by Cyl18 on 1/25/2018.
 */

public class TrackerHandler {
    private final Object tracker;

    public TrackerHandler(Object tracker) {
        this.tracker = tracker;
    }

    public boolean getResult() {
        return XposedHelpers.getBooleanField(tracker, "mResultMatched");
    }
}
