package com.cyl18.opapplocktweaker;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Created by qq775 on 1/23/2018.
 */

public class AppLockHooker implements IXposedHookLoadPackage {

    private static final String APPLOCK_PACKAGE = "com.oneplus.applocker";

    private static ServiceConnection connection = new MyServiceConnection();
    private static Activity currentApplockerActivity;

    public static Activity getCurrentApplockerActivity() {
        return currentApplockerActivity;
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (!lpparam.packageName.equals(APPLOCK_PACKAGE)) return;

        XposedHelpers.findAndHookMethod("com.oneplus.applocker.ApplockerConfirmActivity", lpparam.classLoader, "onCreate", Bundle.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                hookOnCreate(param);
            }
        });

        XposedHelpers.findAndHookMethod("com.oneplus.applocker.ApplockerConfirmActivity", lpparam.classLoader, "onResume", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                hookOnCreate(param);
            }
        });

        XposedHelpers.findAndHookMethod("com.oneplus.applocker.ApplockerConfirmActivity", lpparam.classLoader, "onDestroy", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                hookOnDestroy();
            }
        });
    }

    private void hookOnDestroy() {
        currentApplockerActivity.unbindService(connection);
    }

    private void hookOnCreate(XC_MethodHook.MethodHookParam param) {
        currentApplockerActivity = (Activity) param.thisObject;
        Intent intent = new Intent();
        intent.setClassName("com.oneplus.faceunlock", "com.oneplus.faceunlock.FaceUnlockService");
        currentApplockerActivity.bindService(intent, connection, Context.BIND_AUTO_CREATE);
    }


}

