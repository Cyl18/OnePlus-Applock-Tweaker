package com.cyl18.opapplocktweaker;

import android.content.Context;
import android.hardware.display.DisplayManager;
import android.os.IBinder;
import android.os.RemoteException;
import android.view.Display;

import com.android.internal.policy.IOPFacelockCallback;
import com.android.internal.policy.IOPFacelockService;

import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

/**
 * Created by Cyl18 on 1/23/2018.
 */

public class FaceUnlockServiceConnector {
    private IOPFacelockService facelockService;
    private IOPFacelockCallback facelockCallback;
    private static FaceUnlockServiceConnector instance;
    private boolean started = false;
    private IBinder currentBinder;

    public static FaceUnlockServiceConnector getInstance() {
        return instance;
    }

    public FaceUnlockServiceConnector() {
        instance = this;
    }

    public void startFaceUnlock(IBinder binder) {
        if (started) return;
        started = true;
        currentBinder = binder;
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

    public void startFaceUnlock() {
        if (currentBinder == null) throw new RuntimeException("currentBinder is null!");
        startFaceUnlock(currentBinder);
    }

    public void stopFaceUnlock() {
        if (!started) return;
        try {
            facelockService.stopFaceUnlock(0);
            facelockService.unregisterCallback(this.facelockCallback);
            facelockService.release();
            started = false;
        } catch (RemoteException e) {
            XposedBridge.log(e);
        }

        if (!AppLockHooker.getCurrentTracker().getResult() &&
                isScreenOn())
            startFaceUnlock();
    }

    private boolean isScreenOn() {
        DisplayManager dm = (DisplayManager) AppLockHooker.getCurrentApplockerActivity().getSystemService(Context.DISPLAY_SERVICE);
        Display[] displays = dm.getDisplays();
        for (Display display : displays) {
            if (display.getState() == Display.STATE_ON
                    || display.getState() == Display.STATE_UNKNOWN) {
                return true;
            }
        }
        return false;
    }

    class Callback extends IOPFacelockCallback.Stub {
        private static final int RESULT_SUCCESSFUL = 0;

        public void onBeginRecognize(int i) throws RemoteException {

        }

        @Override
        public void onCompared(int faceId, int userId, int result, int compareTimeMillis, int score) throws RemoteException {
            if (result != RESULT_SUCCESSFUL) return;
            Object activity = AppLockHooker.getCurrentApplockerActivity();
            try {
                XposedHelpers.callMethod(activity, "onAuthenticated");
            } catch (Throwable e) {
                XposedBridge.log(e);
            }
        }


        public void onEndRecognize(int i, int i1, int i2) throws RemoteException {
            FaceUnlockServiceConnector.getInstance().stopFaceUnlock();

        }


    }
}
