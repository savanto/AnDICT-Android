package com.savanto.andict;

final class NativeDict {
    static {
        System.loadLibrary("android_ffi");
    }

    static native String[] define(String server, int port, String database, String word);
}
