package com.didi.virtualapk.utils;

import android.util.Log;

public class MethodUtils {
    private static final String TAG = "MethodUtils-App";

    public static void logStackTrace() {
        //用于追踪调用堆栈
        Throwable throwable = new Throwable();
        StackTraceElement[] stackTrace = throwable.getStackTrace();
        for (StackTraceElement stackTraceElement : stackTrace) {
            Log.d(TAG, "request: stackTraceElement=" + stackTraceElement);
        }
    }

}
