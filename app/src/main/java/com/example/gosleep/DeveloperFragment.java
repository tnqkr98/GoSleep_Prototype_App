package com.example.gosleep;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class DeveloperFragment extends Fragment {
    View view;
    Switch velveSwit,fanSwit,heatSwit;
    Button fanButton,zpButton;
    EditText eFan;
    TextView fanSpeed, distance, co2, illuminance;

    private static Handler mHandler;
    ArduinoThread thread;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        thread = new ArduinoThread();
        thread.start();
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_developer, container, false);
        eFan = (EditText)view.findViewById(R.id.fan_speed);
        eFan.setText("80");

        fanSpeed = (TextView)view.findViewById(R.id.fanspeedtxt);
        distance = (TextView)view.findViewById(R.id.txt_dist);
        co2 = (TextView)view.findViewById(R.id.txt_co2);
        illuminance = (TextView)view.findViewById(R.id.txt_illu);

        fanButton = (Button)view.findViewById(R.id.bt_fanset);
        fanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int speed = Integer.parseInt(eFan.getText().toString());            /// 여기 에러
                if(speed<10)
                    ((GoSleepActivity) getActivity()).bt.send("fs00" + speed, true);
                else if(speed >=10 && speed <100)
                    ((GoSleepActivity) getActivity()).bt.send("fs0" + speed, true);
                else
                    ((GoSleepActivity) getActivity()).bt.send("fs" + speed, true);
            }
        });

        velveSwit = (Switch)view.findViewById(R.id.velve_switch);
        velveSwit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(velveSwit.isChecked())
                    ((GoSleepActivity) getActivity()).bt.send("v1", true);
                else
                    ((GoSleepActivity) getActivity()).bt.send("v0", true);
            }
        });

        fanSwit = (Switch)view.findViewById(R.id.fan_switch);
        fanSwit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(fanSwit.isChecked())
                    ((GoSleepActivity) getActivity()).bt.send("f1", true);
                else
                    ((GoSleepActivity) getActivity()).bt.send("f0", true);
            }
        });

        heatSwit = (Switch)view.findViewById(R.id.heat_switch);
        heatSwit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(heatSwit.isChecked())
                    ((GoSleepActivity)getActivity()).bt.send("h1", true);
                else
                    ((GoSleepActivity)getActivity()).bt.send("h0", true);
            }
        });

        zpButton = (Button) view.findViewById(R.id.zpbutton);
        zpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((GoSleepActivity)getActivity()).bt.send("z", true);
            }
        });


        mHandler = new Handler(){
            @Override
            public void handleMessage(@NonNull Message msg) {
                try {
                    if (msg.what == 0) {
                        //tem.setText(((GoSleepActivity) getActivity()).tem);
                        //hum.setText(((GoSleepActivity) getActivity()).hum);
                        fanSpeed.setText(((GoSleepActivity) getActivity()).fanspeed);
                        co2.setText(((GoSleepActivity)getActivity()).co2);
                        distance.setText(((GoSleepActivity)getActivity()).dist);
                        illuminance.setText(((GoSleepActivity)getActivity()).cds);

                        if (((GoSleepActivity)getActivity()).moduleControlCMD > 0) {
                            if(((GoSleepActivity)getActivity()).fanOn) fanSwit.setChecked(true);
                            else fanSwit.setChecked(false);
                            if(((GoSleepActivity)getActivity()).heatOn) heatSwit.setChecked(true);
                            else heatSwit.setChecked(false);
                            if(((GoSleepActivity)getActivity()).velveOn) velveSwit.setChecked(true);
                            else velveSwit.setChecked(false);
                            ((GoSleepActivity)getActivity()).moduleControlCMD--;
                        }
                    }
                }catch (Exception e){}
            }
        };

        return view;
    }

    private class ArduinoThread extends Thread{
        public void run(){
            while(!Thread.currentThread().isInterrupted()){
                try {
                    if (((GoSleepActivity) getActivity()).arduinoDataRecievOn)
                        mHandler.sendEmptyMessage(0);
                }catch(Exception e){}

                try {
                    Thread.sleep(100);
                }catch (InterruptedException e){
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        thread.interrupt();
    }
}
