//
// cwsdk.c
//
// Created by luoyingxing on 2018/11/15.
//

#ifdef __cplusplus
extern "C"{
#endif

#include "cwsdk.h"

int8_t ALaw_Encode(int16_t number) {
    const uint16_t ALAW_MAX = 0xFFF;
    uint16_t mask = 0x800;
    uint8_t sign = 0;
    uint8_t position = 11;
    uint8_t lsb = 0;
    if (number < 0) {
        number = -number;
        sign = 0x80;
    }
    if (number > ALAW_MAX) {
        number = ALAW_MAX;
    }
    for (; ((number & mask) != mask && position >= 5); mask >>= 1, position--);
    lsb = (number >> ((position == 4) ? (1) : (position - 4))) & 0x0f;
    return (sign | ((position - 4) << 4) | lsb) ^ 0x55;
}

int16_t ALaw_Decode(int8_t number) {
    uint8_t sign = 0x00;
    uint8_t position = 0;
    int16_t decoded = 0;
    number ^= 0x55;
    if (number & 0x80) {
        number &= ~(1 << 7);
        sign = -1;
    }
    position = ((number & 0xF0) >> 4) + 4;
    if (position != 4) {
        decoded = ((1 << position) | ((number & 0x0F) << (position - 4))
                   | (1 << (position - 5)));
    } else {
        decoded = (number << 1) | 1;
    }
    return (sign == 0) ? (decoded) : (-decoded);
}

int8_t MuLaw_Encode(int16_t number) {
    const uint16_t MULAW_MAX = 0x1FFF;
    const uint16_t MULAW_BIAS = 33;
    uint16_t mask = 0x1000;
    uint8_t sign = 0;
    uint8_t position = 12;
    uint8_t lsb = 0;
    if (number < 0) {
        number = -number;
        sign = 0x80;
    }
    number += MULAW_BIAS;
    if (number > MULAW_MAX) {
        number = MULAW_MAX;
    }
    for (; ((number & mask) != mask && position >= 5); mask >>= 1, position--);
    lsb = (number >> (position - 4)) & 0x0f;
    return (~(sign | ((position - 5) << 4) | lsb));
}

int16_t MuLaw_Decode(int8_t number) {
    const uint16_t MULAW_BIAS = 33;
    uint8_t sign = 0, position = 0;
    int16_t decoded = 0;
    number = ~number;
    if (number & 0x80) {
        number &= ~(1 << 7);
        sign = -1;
    }
    position = ((number & 0xF0) >> 4) + 5;
    decoded = ((1 << position) | ((number & 0x0F) << (position - 4))
               | (1 << (position - 5))) - MULAW_BIAS;
    return (sign == 0) ? (decoded) : (-(decoded));
}


//--------------------------------------------------------------------------------------------------------------------

static int search(int val, short *table, int size) {
    int i;
    for (i = 0; i < size; i++) {
        if (val <= *table++)
            return (i);
    }
    return (size);
}


/*********************************************************************
 * 输入参数范围 ：-32768~32767
 * 返回8位无符号整数
 * linear2alaw() - Convert a 16-bit linear PCM value to 8-bit A-law
 *
 * linear2alaw() accepts an 16-bit integer and encodes it as A-law data.
 *
 *  Linear Input Code       Compressed Code
 *  -----------------       ------------------
 *  0000000wxyza            000wxyz
 *  0000001wxyza            001wxyz
 *  000001wxyzab            010wxyz
 *  00001wxyzabc            011wxyz
 *  0001wxyzabcd            100wxyz
 *  001wxyzabcde            101wxyz
 *  01wxyzabcdef            110wxyz
 *  1wxyzabcdefg            111wxyz
 *
 * For further information see John C. Bellamy's Digital Telephony, 1982,
 * John Wiley & Sons, pps 98-111 and 472-476.
 *********************************************************************/
unsigned char linear2alaw(int pcm_val)  /* 2's complement (16-bit range) */{
    int mask;
    int seg;
    unsigned char aval;

    if (pcm_val >= 0) {
        mask = 0xD5;        /* sign (7th) bit = 1 */
    } else {
        mask = 0x55;        /* sign bit = 0 */
        //pcm_val = -pcm_val - 8;
        pcm_val = -pcm_val - 1;
    }

    /* Convert the scaled magnitude to segment number. */
    seg = search(pcm_val, seg_end, 8);  //返回pcm_val属于哪个分段

    /* Combine the sign, segment, and quantization bits. */

    if (seg >= 8)        /* out of range, return maximum value. */
        return (0x7F ^ mask);
    else {
        aval = seg << SEG_SHIFT;  //aval为每一段的偏移，分段量化后的数据需要加上该偏移（aval）
        //分段量化
        //量化方法： (pcm_val-分段值)，然后取有效的高4位   （0分段例外）
        //比如 pcm_val = 0x7000 ，那么seg=7 ，第7段的范围是0x4000~0x7FFF ，段偏移aval=7<<4=0x7F
        //0x7000-0x4000=0x3000 ，然后取有效的高4位，即右移10(seg+3)，0x3000>>10=0xC
        //上一步等效为：(0x7000>>10)&0xF=0xC 。也就是： (pcm_val >> (seg + 3)) & QUANT_MASK
        //然后加上段偏移 0x7F(aval) ，加法等效于或运算，即 |aval

        if (seg < 2)
            aval |= (pcm_val >> 4) & QUANT_MASK;  //0、1段折线的斜率一样
        else
            aval |= (pcm_val >> (seg + 3)) & QUANT_MASK;
        return (aval ^ mask);    //异或0x55，目的是尽量避免出现连续的0，或连续的1，提高传输过程的可靠性
    }
}

/*********************************************************************
 *    alaw2linear() - Convert an A-law value to 16-bit linear PCM
 *********************************************************************/
int alaw2linear(unsigned char a_val) {
    int t;
    int seg;

    a_val ^= 0x55;

    t = (a_val & QUANT_MASK) << 4;
    seg = ((unsigned) a_val & SEG_MASK) >> SEG_SHIFT;
    switch (seg) {
        case 0:
            t += 8;
            break;
        case 1:
            t += 0x108;
            break;
        default:
            t += 0x108;
            t <<= seg - 1;
    }
    return ((a_val & SIGN_BIT) ? t : -t);
}


JNIEXPORT jshortArray JNICALL
Java_com_detector_demo_PCMUtils_pcm2alaw(JNIEnv *env, jobject instance, jbyteArray array,
                                         jint length) {
    jbyte *bytes = (*env)->GetByteArrayElements(env, array, 0);

    jshortArray *arr = (*env)->NewShortArray(env, length);
    jshort *js = (*env)->GetShortArrayElements(env, arr, 0);

    for (int i = 0; i < length; i++) {
        js[i] = ALaw_Encode(bytes[i]);
    }

    (*env)->SetShortArrayRegion(env, arr, 0, length, js);

    return arr;
}


JNIEXPORT jbyteArray JNICALL
Java_com_detector_demo_PCMUtils_alaw2pcm(JNIEnv *env, jobject instance, jshortArray array,
                                         jint length) {
    jshort *js = (*env)->GetShortArrayElements(env, array, 0);

    jbyteArray *arr = (*env)->NewByteArray(env, length);
    jbyte *b = (*env)->GetByteArrayElements(env, arr, 0);

    for (int i = 0; i < length; i++) {
        b[i] = ALaw_Decode(js[i]);
    }

    (*env)->SetByteArrayRegion(env, arr, 0, length, b);

    return arr;
}

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved) {
    JNIEnv *env;

    LOGW("JNI OnLoad");

    memset(&g_ctx, 0, sizeof(g_ctx));

    g_ctx.javaVM = vm;
    if ((*vm)->GetEnv(vm, (void **) &env, JNI_VERSION_1_6) != JNI_OK) {
        return JNI_ERR; // JNI version not supported.
    }

    jniEnv = env;

    jclass tmp = (*env)->FindClass(env, JNI_RES_CLASS);
    g_res_class = (jclass) ((*env)->NewGlobalRef(env, tmp));

    return JNI_VERSION_1_6;
}

#ifdef __cplusplus
}
#endif