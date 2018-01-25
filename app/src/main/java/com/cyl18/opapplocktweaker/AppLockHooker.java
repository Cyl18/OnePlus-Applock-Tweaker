package com.cyl18.opapplocktweaker;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Created by qq775 on 1/23/2018.
 */

public class AppLockHooker implements IXposedHookLoadPackage {
    private static ServiceConnection connection = new MyServiceConnection();
    private static Activity currentApplockerActivity;
    private static TrackerConnector currentTracker;

    public static Activity getCurrentApplockerActivity() {
        return currentApplockerActivity;
    }

    public static TrackerConnector getCurrentTracker() {
        return currentTracker;
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (!lpparam.packageName.equals(Constants.APPLOCK_PACKAGE_NAME)) return;

        XposedHelpers.findAndHookMethod(Constants.ACTIVITY_CONFIRM_NAME, lpparam.classLoader, "onResume", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                hookOnStart(param);
            }
        });

        XposedHelpers.findAndHookMethod(Constants.ACTIVITY_CONFIRM_NAME, lpparam.classLoader, "onPause", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                hookOnStop();
            }
        });
    }

    private void hookOnStop() {
        currentApplockerActivity.unbindService(connection);
    }

    private void hookOnStart(XC_MethodHook.MethodHookParam param) {
        currentApplockerActivity = (Activity) param.thisObject;
        Intent intent = new Intent();
        intent.setClassName("com.oneplus.faceunlock", "com.oneplus.faceunlock.FaceUnlockService");
        currentApplockerActivity.bindService(intent, connection, Context.BIND_AUTO_CREATE);
        currentTracker = new TrackerConnector(XposedHelpers.getObjectField(currentApplockerActivity, "mCredentialCheckResultTracker"));
        //currentApplockerActivity.findViewById((int)2131558403).setOnClickListener(new View.OnClickListener() {
        //    @Override
        //    public void onClick(View view) {
        //        FaceUnlockServiceConnector.getInstance().startFaceUnlock();
        //    }
        //});
    }
}

