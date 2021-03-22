package com.h2play.canvas_magic.features.help;

import android.net.Uri;
import android.os.Bundle;
import android.widget.RadioButton;
import android.widget.VideoView;

import androidx.appcompat.app.AppCompatActivity;

import com.h2play.canvas_magic.R;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnCheckedChanged;

public class HelpActivity extends AppCompatActivity {

    @BindView(R.id.videoview)
    VideoView videoView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_help);

        ButterKnife.bind(this);

        String uriPath = "android.resource://" + getPackageName() + "/" + R.raw.how2;
        videoView = findViewById(R.id.videoview);

        videoView.setOnPreparedListener(mp -> mp.setLooping(true));
        videoView.setVideoURI(Uri.parse(uriPath));
        videoView.start();
    }


    @OnCheckedChanged({R.id.rb_detail,R.id.rb_whole})
    public void onDetailClick(RadioButton radioButton, boolean isChecked) {

        if(isChecked) {
            String uriPath = "android.resource://" + getPackageName() + "/" + R.raw.how2;
            switch (radioButton.getId()) {
                case R.id.rb_whole: {
                    uriPath = "android.resource://" + getPackageName() + "/" + R.raw.whole;
                    break;
                }
            }

            videoView.setOnPreparedListener(mp -> mp.setLooping(true));
            videoView.setVideoURI(Uri.parse(uriPath));
            videoView.start();
        }

    }

}
