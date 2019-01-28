package com.detector.demo;

/**
 * AudioCodec 对PCM原始音频数据进行编解码算法
 * <p>
 * author:  luoyingxing
 * date: 2019/1/28.
 */
public class AudioCodec {
    /**
     * A-Law（A律）常量
     */
    private final static short ALAW_MAX = 0xFFF;

    /**
     * A律编码
     * <p>
     * 将PCM格式的音频按A-Law算法压缩（PCM-->ALaw）
     *
     * @param number 需要编码的short数字
     * @return byte 编码后的数字
     */
    public static byte aLawEncode(short number) {
        short mask = 0x800;
        byte sign = 0;
        byte position = 11;
        byte lsb;
        if (number < 0) {
            number = (short) -number;
            sign = (byte) 0x80;
        }

        if (number > ALAW_MAX) {
            number = ALAW_MAX;
        }

        for (; ((number & mask) != mask && position >= 5); mask >>= 1, position--) ;

        lsb = (byte) ((number >> ((position == 4) ? (1) : (position - 4))) & 0x0f);

        return (byte) ((sign | ((position - 4) << 4) | lsb) ^ 0x55);
    }

    /**
     * A律解码
     * <p>
     * 将A-Law算法压缩后的数据恢复成PCM原始格式的数据（ALaw-->PCM）
     *
     * @param number 需要解码的byte数字
     * @return byte 解码还原后为pcm收的数字
     */
    public static byte aLawDecode(byte number) {
        byte sign = 0x00;
        byte position;
        short decoded;
        number ^= 0x55;
        if ((number & 0x80) != 0) {
            number &= ~(1 << 7);
            sign = -1;
        }

        position = (byte) (((number & 0xF0) >> 4) + 4);
        if (position != 4) {
            decoded = (short) ((1 << position) | ((number & 0x0F) << (position - 4)) | (1 << (position - 5)));
        } else {
            decoded = (short) ((number << 1) | 1);
        }

        return (byte) ((sign == 0) ? (decoded) : (-decoded));
    }

    /**
     * U-Law（μ律）常量
     */
    private final static short MULAW_MAX = 0x1FFF;
    private final static short MULAW_BIAS = 33;

    /**
     * μ律编码
     * <p>
     * 将PCM格式的音频按μ-Law算法压缩（PCM-->μLaw）
     *
     * @param number 需要编码的short数字
     * @return byte 编码后的数字
     */
    public static byte mLawEncode(short number) {
        short mask = 0x1000;
        byte sign = 0;
        byte position = 12;
        byte lsb = 0;
        if (number < 0) {
            number = (short) -number;
            sign = (byte) 0x80;
        }
        number += MULAW_BIAS;
        if (number > MULAW_MAX) {
            number = MULAW_MAX;
        }
        for (; ((number & mask) != mask && position >= 5); mask >>= 1, position--) ;
        lsb = (byte) ((number >> (position - 4)) & 0x0f);
        return (byte) ~(sign | ((position - 5) << 4) | lsb);
    }

    /**
     * μ律解码
     * <p>
     * 将μ-Law算法压缩后的数据恢复成PCM原始格式的数据（μLaw-->PCM）
     *
     * @param number 需要解码的byte数字
     * @return byte 解码还原后为pcm收的数字
     */
    public static byte mLawDecode(byte number) {
        byte sign = 0, position = 0;
        short decoded = 0;
        number = (byte) ~number;
        if ((number & 0x80) != 0) {
            number &= ~(1 << 7);
            sign = -1;
        }
        position = (byte) (((number & 0xF0) >> 4) + 5);
        decoded = (short) (((1 << position) | ((number & 0x0F) << (position - 4))
                | (1 << (position - 5))) - MULAW_BIAS);
        return (byte) ((sign == 0) ? (decoded) : (-(decoded)));
    }
}