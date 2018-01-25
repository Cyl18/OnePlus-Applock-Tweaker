package com.cyl18.opapplocktweaker;

import de.robv.android.xposed.XposedHelpers;

/**
 * Created by Cyl18 on 1/25/2018.
 */

public class TrackerConnector {
    private final Object tracker;

    public TrackerConnector(Object tracker) {
        this.tracker = tracker;
    }

    public boolean getResult() {
        return XposedHelpers.getBooleanField(tracker, "mResultMatched");
    }
}
