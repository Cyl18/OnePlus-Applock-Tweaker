package com.cyl18.opapplocktweaker;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Environment;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;

import java.io.File;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Created by qq775 on 1/23/2018.
 */

public class AppLockHooker implements IXposedHookLoadPackage {
    private static final int ONEPLUS_APPLOCK_LAYOUT_ID = 2131558403;
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
        if (!lpparam.packageName.equals(Constants.APPLOCK_PACKAGE)) return;


        XposedHelpers.findAndHookMethod(Constants.APPLOCK_ACTIVITY_CONFIRM, lpparam.classLoader, "onResume", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                currentApplockerActivity = (Activity) param.thisObject;

                SharedPreferences preferences = getPreferences();

                boolean enable_face_recognition = preferences.getBoolean("enable_face_recognition", true);
                boolean enable_fast_password = preferences.getBoolean("enable_fast_password", false);

                if (enable_face_recognition) {
                    hookOnStart();
                }

                if (enable_fast_password) {
                    hookFastPassword(preferences);
                }
            }
        });

        XposedHelpers.findAndHookMethod(Constants.APPLOCK_ACTIVITY_CONFIRM, lpparam.classLoader, "onPause", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                SharedPreferences preferences = getPreferences();

                boolean enable_face_recognition = preferences.getBoolean("enable_face_recognition", true);

                if (enable_face_recognition)
                    hookOnStop();
            }
        });

    }

    private SharedPreferences getPreferences() {
        File dest = new File(Environment.getExternalStorageDirectory(), Constants.SHARED_SETTINGS_FILE);
        return new XSharedPreferences(dest);
    }

    private void hookFastPassword(SharedPreferences preferences) {
        String password_length = preferences.getString("password_length", "0");
        final Integer length = Integer.parseInt(password_length);
        final EditText passwordEditText = (EditText) XposedHelpers.getObjectField(currentApplockerActivity, "mPasswordEntry");

        passwordEditText.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View view, int i, KeyEvent keyEvent) {
                if (length.equals(passwordEditText.getText().length())) {
                    XposedHelpers.callMethod(currentApplockerActivity, "handleNext");
                    return true;
                }
                return false;
            }
        });
    }

    private void hookOnStop() {
        currentApplockerActivity.unbindService(connection);
    }

    private void hookOnStart() {
        Intent intent = new Intent();
        intent.setClassName(Constants.FACEUNLOCK_PACKAGE, Constants.FACEUNLOCK_SERVICE);
        currentApplockerActivity.bindService(intent, connection, Context.BIND_AUTO_CREATE);
        currentTracker = new TrackerConnector(XposedHelpers.getObjectField(currentApplockerActivity, Constants.TRACKER));
        getLayout().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FaceUnlockServiceConnector.getInstance().stopFaceUnlock();
            }
        });
    }

    private View getLayout() {
        return currentApplockerActivity.findViewById(ONEPLUS_APPLOCK_LAYOUT_ID);
    }
}

