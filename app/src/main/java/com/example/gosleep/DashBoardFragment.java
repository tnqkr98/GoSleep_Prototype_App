package com.example.gosleep;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import app.akexorcist.bluetotohspp.library.BluetoothSPP;
import app.akexorcist.bluetotohspp.library.BluetoothState;

public class DashBoardFragment extends Fragment {
    TextView tem, hum;
    private static Handler mHandler;
    Bundle receivbundle;
    View view;
    Button back,next;
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
        view = inflater.inflate(R.layout.fragment_dashboard, container, false);

        tem = (TextView)view.findViewById(R.id.txt_tem);
        hum = (TextView)view.findViewById(R.id.txt_hum);
        back = (Button)view.findViewById(R.id.backbt);
        next = (Button)view.findViewById(R.id.nextbt);

        receivbundle = this.getArguments();
        mHandler = new Handler(){
            @Override
            public void handleMessage(@NonNull Message msg) {
                try {
                    if (msg.what == 0) {
                        tem.setText(((GoSleepActivity) getActivity()).tem);
                        hum.setText(((GoSleepActivity) getActivity()).hum);
                    }
                }catch (Exception e){}


            }
        };


        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ((GoSleepActivity)getActivity()).bt.send("mback",true);
                Log.d("dddd", "Send back");
            }
        });

        next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ((GoSleepActivity)getActivity()).bt.send("mnext",true);
                Log.d("dddd", "Send next");
            }
        });

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
                    try {
                        ((GoSleepActivity) getActivity()).bt.send("a", true);
                        //Log.d("dddd","ack!!");
                        // 일종의 ack. 아두이노에서는 이것을 5초이상 못받으면 통신이 끊긴것으로 간주.(블루투스 송수신 거리초과로 인한 예외처리)
                    }catch (Exception e){}
                    Thread.sleep(1000);
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
