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
import de.robv.android.xposed.XC_MethodReplacement;
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

        SharedPreferences globalPref = getPreferences();
        boolean enableReplacePassword = globalPref.getBoolean("enable_replace_password", true);

        // hook face recognition
        XposedHelpers.findAndHookMethod(Constants.APPLOCK_ACTIVITY_CONFIRM, lpparam.classLoader, "onResume", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                currentApplockerActivity = (Activity) param.thisObject;

                SharedPreferences preferences = getPreferences();
                boolean enable_face_recognition = preferences.getBoolean("enable_face_recognition", true);

                if (enable_face_recognition)
                    hookFaceUnlockStart();
            }
        });

        XposedHelpers.findAndHookMethod(Constants.APPLOCK_ACTIVITY_CONFIRM, lpparam.classLoader, "onAuthenticated", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                SharedPreferences preferences = getPreferences();
                boolean enable_face_recognition = preferences.getBoolean("enable_face_recognition", true);

                if (enable_face_recognition)
                    hookFaceUnlockStop(true);
            }
        });

        // hook complex password
        XposedHelpers.findAndHookMethod(Constants.APPLOCK_ACTIVITY_CONFIRM_COMPLEX, lpparam.classLoader, "onResume", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                currentApplockerActivity = (Activity) param.thisObject;

                SharedPreferences preferences = getPreferences();
                boolean enable_fast_password = preferences.getBoolean("enable_fast_password", false);

                if (enable_fast_password)
                    hookFastPassword(preferences);
            }
        });

        // hook pin
        XposedHelpers.findAndHookMethod(Constants.APPLOCK_ACTIVITY_CONFIRM_PASSWORD, lpparam.classLoader, "onResume", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                currentApplockerActivity = (Activity) param.thisObject;
            }
        });

        XposedHelpers.findAndHookMethod(Constants.VIEW_PIN, lpparam.classLoader, "append", char.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Object thisObject = param.thisObject;

                SharedPreferences preferences = getPreferences();
                boolean enable_fast_password = preferences.getBoolean("enable_fast_password", false);

                String password_length = preferences.getString("password_length", "0");
                final Integer length = Integer.parseInt(password_length);

                if (enable_fast_password)
                    onPINInput(length, thisObject);
            }
        });

        // hook custom password
        if (enableReplacePassword) {
            XposedHelpers.findAndHookMethod(Constants.APPLOCK_CHOOSE_PASSWORD, lpparam.classLoader, "launchConfirmationActivity",
                    int.class, CharSequence.class, CharSequence.class, CharSequence.class, boolean.class, boolean.class, boolean.class, long.class, boolean.class, String.class, int.class, int.class, new XC_MethodReplacement() {
                        @Override
                        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {

                            return true;
                        }

                    });
        }


    }


    private void hookFastPassword(SharedPreferences preferences) {
        String password_length = preferences.getString("password_length", "0");
        final Integer length = Integer.parseInt(password_length);
        final EditText passwordEditText = (EditText) XposedHelpers.getObjectField(currentApplockerActivity, "mPasswordEntry");

        passwordEditText.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View view, int i, KeyEvent keyEvent) {
                if (length.equals(passwordEditText.getText().toString().length())) {
                    XposedHelpers.callMethod(currentApplockerActivity, "handleNext");
                    return true;
                }
                return false;
            }
        });
    }

    private void onPINInput(Integer length, Object thisObject) {

        if (length.equals(((String) XposedHelpers.callMethod(thisObject, "getText")).length())) {
            XposedHelpers.callMethod(currentApplockerActivity, "handleNext");
        }
    }

    private SharedPreferences getPreferences() {
        File dest = new File(Environment.getExternalStorageDirectory(), Constants.SHARED_SETTINGS_FILE);
        return new XSharedPreferences(dest);
    }

    private void hookFaceUnlockStop(boolean force) {
        FaceUnlockServiceConnector.getInstance().stopFaceUnlock(force);
        currentApplockerActivity.unbindService(connection);
    }

    private void hookFaceUnlockStart() {
        Intent intent = new Intent();
        intent.setClassName(Constants.FACEUNLOCK_PACKAGE, Constants.FACEUNLOCK_SERVICE);
        currentApplockerActivity.bindService(intent, connection, Context.BIND_AUTO_CREATE);
        currentTracker = new TrackerConnector(XposedHelpers.getObjectField(currentApplockerActivity, Constants.TRACKER));
        getLayout().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FaceUnlockServiceConnector.getInstance().stopFaceUnlock(false);
                FaceUnlockServiceConnector.getInstance().startFaceUnlock();
            }
        });
    }

    private View getLayout() {
        return currentApplockerActivity.findViewById(ONEPLUS_APPLOCK_LAYOUT_ID);
    }
}

