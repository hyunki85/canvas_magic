package com.h2play.canvas_magic.features.help;

import android.net.Uri;
import android.os.Bundle;
import android.widget.RadioButton;
import android.widget.VideoView;

import androidx.appcompat.app.AppCompatActivity;

import com.h2play.canvas_magic.R;

public class HelpActivity extends AppCompatActivity {

    private VideoView videoView;
    private RadioButton rbDetail;
    private RadioButton rbWhole;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_help);

        // Initialize views using findViewById instead of ButterKnife
        videoView = findViewById(R.id.videoview);
        rbDetail = findViewById(R.id.rb_detail);
        rbWhole = findViewById(R.id.rb_whole);
        
        // Set up listeners for RadioButtons instead of using @OnCheckedChanged
        rbDetail.setOnCheckedChangeListener((buttonView, isChecked) -> onDetailClick(rbDetail, isChecked));
        rbWhole.setOnCheckedChangeListener((buttonView, isChecked) -> onDetailClick(rbWhole, isChecked));

        String uriPath = "android.resource://" + getPackageName() + "/" + R.raw.how2;

        videoView.setOnPreparedListener(mp -> mp.setLooping(true));
        videoView.setVideoURI(Uri.parse(uriPath));
        videoView.start();
    }

    public void onDetailClick(RadioButton radioButton, boolean isChecked) {
        if(isChecked) {
            String uriPath = "android.resource://" + getPackageName() + "/" + R.raw.how2;
            
            if (radioButton.getId() == R.id.rb_whole) {
                uriPath = "android.resource://" + getPackageName() + "/" + R.raw.whole;
            }

            videoView.setOnPreparedListener(mp -> mp.setLooping(true));
            videoView.setVideoURI(Uri.parse(uriPath));
            videoView.start();
        }
    }
}
