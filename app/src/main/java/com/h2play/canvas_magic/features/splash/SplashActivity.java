package com.h2play.canvas_magic.features.splash;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import com.h2play.canvas_magic.features.menu.MenuActivity;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = new Intent(this, MenuActivity.class);
        startActivity(intent);
        finish();
    }

}
