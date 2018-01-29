package com.cyl18.opapplocktweaker;

import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.IBinder;

public class MyServiceConnection implements ServiceConnection {
    private static FaceUnlockServiceConnector faceUnlockServiceConnector;

    public MyServiceConnection(AppLockHooker hooker) {
        faceUnlockServiceConnector = new FaceUnlockServiceConnector(hooker);
    }

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        faceUnlockServiceConnector.startFaceUnlock(iBinder);
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {
        faceUnlockServiceConnector.stopFaceUnlock(true);
    }
}
