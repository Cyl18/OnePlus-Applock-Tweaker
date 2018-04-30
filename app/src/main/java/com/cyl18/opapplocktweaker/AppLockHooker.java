package com.cyl18.opapplocktweaker;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.text.Editable;
import android.text.TextWatcher;
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
    private ServiceConnection connection;
    private Activity currentApplockerActivity;
    private TrackerHandler currentTracker;

    public Activity getCurrentApplockerActivity() {
        return currentApplockerActivity;
    }

    public TrackerHandler getCurrentTracker() {
        return currentTracker;
    }

    public AppLockHooker() {
        connection = new MyServiceConnection(this);
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (!lpparam.packageName.equals(Constants.APPLOCK_PACKAGE)) return;


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

        XposedHelpers.findAndHookMethod(Constants.APPLOCK_ACTIVITY_CONFIRM, lpparam.classLoader, "onPause", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                SharedPreferences preferences = getPreferences();
                boolean enable_face_recognition = preferences.getBoolean("enable_face_recognition", true);

                if (enable_face_recognition)
                    hookFaceUnlockStop(true);
            }
        });

        // hook complex password
        XposedHelpers.findAndHookMethod(Constants.APPLOCK_ACTIVITY_CONFIRM_COMPLEX, lpparam.classLoader, "onCreate", Bundle.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                currentApplockerActivity = (Activity) param.thisObject;

                SharedPreferences preferences = getPreferences();
                boolean enable_fast_password = preferences.getBoolean("enable_fast_password", false);
                boolean enableReplacePassword = preferences.getBoolean("enable_replace_password", true);

                boolean shouldReplace = shouldReplace(preferences, getUnlockPackageName());

                if (enable_fast_password && !shouldReplace)
                    hookFastPassword(preferences);
                if (shouldReplace && enableReplacePassword) {
                    hookReplacePasswordComplex(preferences);
                }
            }
        });

        // hook pin
        XposedHelpers.findAndHookMethod(Constants.APPLOCK_ACTIVITY_CONFIRM_PASSWORD, lpparam.classLoader, "onCreate", Bundle.class, new XC_MethodHook() {
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
                boolean enableReplacePassword = preferences.getBoolean("enable_replace_password", true);

                String password_length = preferences.getString("password_length", "0");
                final Integer length = Integer.parseInt(password_length);
                boolean shouldReplace = shouldReplace(preferences, getUnlockPackageName());

                if (enable_fast_password && !shouldReplace)
                    onPINInput(length, thisObject);
                if (shouldReplace && enableReplacePassword) {
                    hookReplacePasswordPin(preferences, thisObject);
                }
            }
        });

        // hook custom password

        XposedHelpers.findAndHookMethod(Constants.APPLOCK_ACTIVITY_CONFIRM_PASSWORD, lpparam.classLoader, "handleNext", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                SharedPreferences pref = getPreferences();
                boolean replace = pref.getBoolean("enable_replace_password", true);
                if (replace)
                    hookReplacePassword(param, true);
            }
        });

        XposedHelpers.findAndHookMethod(Constants.APPLOCK_ACTIVITY_CONFIRM_COMPLEX, lpparam.classLoader, "handleNext", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                SharedPreferences pref = getPreferences();
                boolean replace = pref.getBoolean("enable_replace_password", true);
                if (replace)
                    hookReplacePassword(param, false);
            }
        });


        // hook fingerprint

        XposedHelpers.findAndHookMethod(Constants.APPLOCK_ACTIVITY_CONFIRM, lpparam.classLoader, "registerFingerprint", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if (getPreferences().getBoolean("disable_fingerprint", false))
                    param.setResult(null);
            }
        });

    }

    private void hookReplacePasswordPin(SharedPreferences preferences, Object thisObject) {

        if (preferences.getString("password", "").equals(((String) XposedHelpers.callMethod(thisObject, "getText")))) {
            XposedHelpers.callMethod(currentApplockerActivity, "onAuthenticated");
        }
    }

    private void hookReplacePasswordComplex(final SharedPreferences preferences) {
        final EditText passwordEditText = (EditText) XposedHelpers.getObjectField(currentApplockerActivity, "mPasswordEntry");

        passwordEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (preferences.getString("password", "").equals(passwordEditText.getText().toString())) {
                    XposedHelpers.callMethod(currentApplockerActivity, "onAuthenticated");
                }
            }
        });

    }

    private void hookReplacePassword(XC_MethodHook.MethodHookParam param, boolean isPin) {
        if (shouldReplace(getPreferences(), getUnlockPackageName())) {
            if (isPin) {
                Object mPasswordTextViewForPin = XposedHelpers.getObjectField(currentApplockerActivity, "mPasswordTextViewForPin");
                if (mPasswordTextViewForPin != null) {
                    XposedHelpers.callMethod(mPasswordTextViewForPin, "reset", true);
                    XposedHelpers.callMethod(mPasswordTextViewForPin, "setEnabled", true);
                }
                Object mPasswordEntry = XposedHelpers.getObjectField(currentApplockerActivity, "mPasswordEntry");
                if (mPasswordEntry != null)
                    XposedHelpers.callMethod(mPasswordEntry, "setText", true);
            }
            Object mPasswordEntryInputDisabler = XposedHelpers.getObjectField(currentApplockerActivity, "mPasswordEntryInputDisabler");
            if (mPasswordEntryInputDisabler != null)
                XposedHelpers.callMethod(mPasswordEntryInputDisabler, "setInputEnabled", true);
            param.setResult(null);
        }
    }

    private boolean shouldReplace(SharedPreferences preferences, String packageName) {
        boolean replace_password = preferences.getBoolean("enable_replace_password", false);
        boolean only_replace_selected = preferences.getBoolean("enable_only_replace_selected", false);
        boolean selected = preferences.getBoolean(packageName, false);
        return replace_password && !only_replace_selected || replace_password && selected;
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
        try {
            FaceUnlockServiceConnector.getInstance().stopFaceUnlock(force);
            currentApplockerActivity.unbindService(connection);
        } catch (Exception e) {

        }
    }

    private void hookFaceUnlockStart() {
        Intent intent = new Intent();
        intent.setClassName(Constants.FACEUNLOCK_PACKAGE, Constants.FACEUNLOCK_SERVICE);
        currentApplockerActivity.bindService(intent, connection, Context.BIND_AUTO_CREATE);
        currentTracker = new TrackerHandler(XposedHelpers.getObjectField(currentApplockerActivity, Constants.TRACKER));
        //getLayout().setOnClickListener(new View.OnClickListener() {
        //    @Override
        //    public void onClick(View view) {
        //        FaceUnlockServiceConnector.getInstance().stopFaceUnlock(false);
        //        FaceUnlockServiceConnector.getInstance().startFaceUnlock();
        //    }
        //});
    }


    public String getUnlockPackageName() {
        return ((String) XposedHelpers.getObjectField(currentApplockerActivity, "mPackageName"));
    }
}

