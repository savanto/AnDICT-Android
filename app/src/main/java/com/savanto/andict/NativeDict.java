package com.savanto.andict;

import android.support.annotation.NonNull;
import android.support.annotation.WorkerThread;

final class NativeDict {
    static {
        System.loadLibrary("android_ffi");
    }

    @WorkerThread
    static native Definition[] define(
            @NonNull String server,
            int port,
            @NonNull String database,
            @NonNull String word);

    @WorkerThread
    static native Definition[] defineWithStrategy(
            @NonNull String server,
            int port,
            @NonNull String database,
            @NonNull String strategy,
            @NonNull String word);
}
