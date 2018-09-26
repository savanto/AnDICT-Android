package com.savanto.andict;

import android.support.annotation.NonNull;
import android.support.annotation.WorkerThread;

final class NativeDict {
    static {
        System.loadLibrary("android_ffi");
    }

    @WorkerThread
    static native String[] define(
            @NonNull String server,
            int port,
            @NonNull String database,
            @NonNull String word);
}
