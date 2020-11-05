package com.example.gosleep;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.WindowManager;

public class GoSleepSplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_go_sleep_splash);
        Handler hd = new Handler();
        hd.postDelayed(new splash(),1500);
    }

    private class splash implements Runnable{
        @Override
        public void run() {
            startActivity(new Intent(getApplication(),GoSleepActivity.class));
            GoSleepSplashActivity.this.finish();
        }
    }
}