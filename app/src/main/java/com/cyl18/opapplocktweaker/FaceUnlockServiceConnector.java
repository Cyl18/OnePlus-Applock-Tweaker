package com.cyl18.opapplocktweaker;

import android.os.IBinder;
import android.os.RemoteException;

import com.android.internal.policy.IOPFacelockCallback;
import com.android.internal.policy.IOPFacelockService;

import de.robv.android.xposed.XposedBridge;

/**
 * Created by Cyl18 on 1/23/2018.
 */

public class FaceUnlockServiceConnector {
    private IOPFacelockService facelockService;
    private IOPFacelockCallback facelockCallback;
    private static FaceUnlockServiceConnector instance;

    public static FaceUnlockServiceConnector getInstance() {
        return instance;
    }

    public FaceUnlockServiceConnector() {
        instance = this;
    }

    public void onFaceUnlockServiceConnected(IBinder binder) {
        facelockService = IOPFacelockService.Stub.asInterface(binder);
        facelockCallback = new Callback();
        try {
            facelockService.prepare();
            facelockService.registerCallback(this.facelockCallback);
            facelockService.startFaceUnlock(0);
        } catch (RemoteException e) {
            XposedBridge.log(e);
        }
    }

    public void onFaceUnlockServiceDisconnected() {
        try {
            facelockService.stopFaceUnlock(0);
            facelockService.unregisterCallback(this.facelockCallback);
            facelockService.release();

        } catch (RemoteException e) {
            XposedBridge.log(e);
        }
    }


    class Callback extends IOPFacelockCallback.Stub {
        private static final int RESULT_SUCCESSFUL = 0;

        public void onBeginRecognize(int i) throws RemoteException {

        }

        @Override
        public void onCompared(int faceId, int userId, int result, int compareTimeMillis, int score) throws RemoteException {
            if (result != RESULT_SUCCESSFUL) return;
            Object activity = AppLockHooker.getCurrentApplockerActivity();
            Class<?> aClass = activity.getClass();
            try {
                aClass.getMethod("onAuthenticated").invoke(activity, null);
            } catch (Throwable e) {
                XposedBridge.log(e);
            }
        }


        public void onEndRecognize(int i, int i1, int i2) throws RemoteException {
            FaceUnlockServiceConnector.getInstance().onFaceUnlockServiceDisconnected();
        }


    }
}
