package com.detector.demo;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.conwin.detector.entity.Node;
import com.conwin.detector.sdk.DetSDK;
import com.conwin.detector.stream.OnPlayListener;
import com.conwin.detector.view.ISurfaceView;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity {
    private ISurfaceView surfaceView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        surfaceView = findViewById(R.id.surface_view);

        findViewById(R.id.play).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playVideo();
            }
        });
        findViewById(R.id.start_record).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startRecord();
            }
        });
        findViewById(R.id.stop_record).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopRecord();
            }
        });
        findViewById(R.id.play_record).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playRecord();
            }
        });

        initRecord();
    }

    private void playVideo() {
        DetSDK.getInstance().init("http://116.204.67.11:17001");
        DetSDK.getInstance().playBack(surfaceView, "COWN-3B1-UY-4WS", 1, "2019-01-03 16:15:20", "2019-01-03 16:15:38", new OnPlayListener() {
            @Override
            public void onPrepared(int totalCount, int frame) {
                // 即将播放
            }

            @Override
            public void onBufferUpdate(int i, Node node) {

            }

            @Override
            public void onProgress(int position, Node node) {
                //当前播放进度，数值仅对回放视频有效
            }

            @Override
            public void onComplete() {
                //播放完成
            }

            @Override
            public void onError(int code) {
                //播放出错，code的取值详见 {ErrorCode.class}
            }
        });
    }

    private File recordingFile;//储存AudioRecord录下来的文件
    private boolean isRecording = false; //true表示正在录音
    private AudioRecord audioRecord = null;
    private File parent = null;//文件目录
    private String TAG = "AudioRecord";
    private int bufferSize = 0;//最小缓冲区大小

    //採样频率一般有11025HZ（11KHz），22050HZ（22KHz）、44100Hz（44KHz）
    /**
     * 采样率
     */
    private int sampleRateInHz = 11025;
    /**
     * 单声道（影响播放速度的参数）
     */
    private int channelConfig = AudioFormat.CHANNEL_CONFIGURATION_MONO;
    /**
     * 量化位数
     */
    private int audioFormat = AudioFormat.ENCODING_PCM_16BIT;

    private void initRecord() {
        //计算最小缓冲区
        bufferSize = AudioRecord.getMinBufferSize(sampleRateInHz, channelConfig, audioFormat);

        //创建AudioRecorder对象
        audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, sampleRateInHz, channelConfig, audioFormat, bufferSize);

        parent = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/DetectorDemo");
        if (!parent.exists()) {
            //创建文件夹
            parent.mkdirs();
        }
    }

    private void startRecord() {
        isRecording = true;
        new Thread(new Runnable() {
            @Override
            public void run() {
                isRecording = true;

                recordingFile = new File(parent, "audioTest.pcm");
                if (recordingFile.exists()) {
                    recordingFile.delete();
                }

                try {
                    recordingFile.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e(TAG, "创建储存音频文件出错");
                }

                try {
                    DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(recordingFile)));
                    byte[] buffer = new byte[bufferSize];

                    //开始录音
                    audioRecord.startRecording();
                    int r = 0;
//                    byte[] buff;

                    while (isRecording) {
                        int bufferReadResult = audioRecord.read(buffer, 0, bufferSize);

                        //-----pcm  to pcm-----
//                        for (int i = 0; i < bufferReadResult; i++) {
//                            dos.write(buffer[i]);
//                        }

                        //-----pcm  to alaw-----
//                        short[] buf = PCMUtils.pcm2alaw(buffer, bufferReadResult);
//
//                        for (short aBuf : buf) {
//                            dos.write(aBuf);
//                        }

//                        buff = new byte[buffer.length];

                        for (int k = 0; k < buffer.length; k++) {
                            buffer[k] = AudioCodec.aLawEncode(buffer[k]);
                        }

                        dos.write(buffer, 0, buffer.length);

                        r++;
                    }
                    audioRecord.stop();//停止录音

                    Log.i(TAG, "Recording stop");

                    dos.close();
                } catch (Throwable t) {
                    Log.e(TAG, "Recording Failed");
                }
            }
        }).start();

    }

    //停止录音
    private void stopRecord() {
        isRecording = false;
    }

    private AudioTrack audioTrack;

    private void playRecord() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    File file = new File(parent, "audioTest.pcm");
                    InputStream in = new FileInputStream(file);

                    //static type
//                    ByteArrayOutputStream out = new ByteArrayOutputStream(264848);
//                    for (int b; (b = in.read()) != -1; ) {
//                        out.write(b);
//                    }
//
//                    byte[] buffer = out.toByteArray();
//
//                    audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRateInHz, channelConfig, audioFormat, buffer.length, AudioTrack.MODE_STATIC);
//
//                    Log.i(TAG, "======== ready to play ========");
//
//                    audioTrack.write(buffer, 0, buffer.length);
//                    audioTrack.play();

                    //stream type
                    audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRateInHz, channelConfig, audioFormat, bufferSize, AudioTrack.MODE_STREAM);
                    byte[] buf = new byte[1024];
                    byte[] buff = new byte[1024];
                    while (in.read(buf) != -1) {
                        //-----pcm  to alaw-----
//                        buff = PCMUtils.alaw2pcm(buf, 1024);
                        for (int k = 0; k < 1024; k++) {
                            buff[k] = AudioCodec.aLawDecode(buf[k]);
                        }

                        Log.i(TAG, "buf.length " + buf.length);
                        audioTrack.write(buff, 0, buff.length);
                        audioTrack.play();

                        //-----pcm  to pcm-----
//                        audioTrack.write(buf, 0, buf.length);
//                        audioTrack.play();
                    }

                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
}