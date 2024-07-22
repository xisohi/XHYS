package com.github.catvod.utils;


import android.util.Log;


public class Shell {

    private static final String TAG = Shell.class.getSimpleName();

    public static void exec(String command) {
        try {
            int code = Runtime.getRuntime().exec(command).waitFor();
            if (code != 0) Log.d(TAG, String.format("Shell command '%s' failed with exit code '%s'", command, code));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}