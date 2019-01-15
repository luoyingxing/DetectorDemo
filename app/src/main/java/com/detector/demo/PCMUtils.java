package com.detector.demo;

/**
 * PCMUtils
 * author:  luoyingxing
 * date: 2019/1/15.
 */
public class PCMUtils {

    static {
        try {
            System.loadLibrary("cwsdk");
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public static native short[] pcm2alaw(byte[] buffer, int len);

    public static native byte[] alaw2pcm(short[] buffer, int len);
}