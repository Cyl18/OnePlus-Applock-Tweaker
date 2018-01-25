package com.cyl18.opapplocktweaker;

import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.IBinder;

public class MyServiceConnection implements ServiceConnection {
    public static FaceUnlockServiceConnector faceUnlockServiceConnector = new FaceUnlockServiceConnector();

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        faceUnlockServiceConnector.startFaceUnlock(iBinder);
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {
        faceUnlockServiceConnector.stopFaceUnlock();
    }
}
