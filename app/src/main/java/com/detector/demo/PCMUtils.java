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

    /**
     * 编码
     * @param buffer
     * @param len
     * @return
     */
    public static native short[] pcm2alaw(byte[] buffer, int len);

    /**
     * 解码
     * @param buffer
     * @param len
     * @return
     */
    public static native byte[] alaw2pcm(byte[] buffer, int len);
}