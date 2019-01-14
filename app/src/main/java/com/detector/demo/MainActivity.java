package com.detector.demo;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import com.conwin.detector.entity.Node;
import com.conwin.detector.sdk.DetSDK;
import com.conwin.detector.stream.OnPlayListener;
import com.conwin.detector.view.ISurfaceView;

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
        findViewById(R.id.record).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                record();
            }
        });


    }

    private void playVideo() {
        DetSDK.getInstance().init("http://116.204.67.11:17001");
        DetSDK.getInstance().playBack(surfaceView, "COWN-3B1-UY-4WS", 1, "2019-01-03 16:15:24", "2019-01-03 16:15:34", new OnPlayListener() {
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

    private void record() {

    }

}
