package com.example.gosleep;

import android.graphics.Color;
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
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import app.akexorcist.bluetotohspp.library.BluetoothSPP;
import app.akexorcist.bluetotohspp.library.BluetoothState;

public class DashBoardFragment extends Fragment {
    TextView tem, hum, mode,co2,tx_cds;
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
        co2 = (TextView)view.findViewById(R.id.co2);
        mode = (TextView)view.findViewById(R.id.mode_txt);
        tx_cds = (TextView)view.findViewById(R.id.tx_cds);

        back = (Button)view.findViewById(R.id.backbt);
        next = (Button)view.findViewById(R.id.nextbt);

        receivbundle = this.getArguments();
        mHandler = new Handler(){
            @Override
            public void handleMessage(@NonNull Message msg) {
                if (msg.what == 0) {
                    tem.setText(((GoSleepActivity) getActivity()).tem);
                    hum.setText(((GoSleepActivity) getActivity()).hum);
                    co2.setText(((GoSleepActivity)getActivity()).co2);
                    tx_cds.setText(((GoSleepActivity)getActivity()).cds);

                    switch (((GoSleepActivity) getActivity()).current_mode){
                        case 2:
                            back.setText("");
                            back.setBackgroundResource(R.drawable.bt_type_gray);
                            next.setText("Sleep");
                            next.setBackgroundResource(R.drawable.bt_type_blue);
                            mode.setText("Current Mode : Waiting Mode");
                            mode.setTextColor(Color.WHITE);
                            break;
                        case 3:
                            back.setText("Stop");
                            back.setBackgroundResource(R.drawable.bt_type_red);
                            next.setText("");
                            next.setBackgroundResource(R.drawable.bt_type_gray);
                            mode.setText("Current Mode : Distance Mode");
                            mode.setTextColor(Color.WHITE);
                            break;
                        case 4:
                            back.setText("Stop");
                            back.setBackgroundResource(R.drawable.bt_type_red);
                            next.setText("Pause");
                            next.setBackgroundResource(R.drawable.bt_type_blue);
                            mode.setText("Current Mode : Sleep Mode");
                            mode.setTextColor(Color.WHITE);
                            break;
                        case 5:
                            if(((GoSleepActivity) getActivity()).past_mode ==4
                            && ((GoSleepActivity) getActivity()).current_mode ==5) {
                                back.setText("");
                                back.setBackgroundResource(R.drawable.bt_type_gray);
                                next.setText("Wake Up");
                                next.setBackgroundResource(R.drawable.bt_type_blue);
                                mode.setText("Current Mode : Sensing Mode");
                                mode.setTextColor(Color.WHITE);
                            }
                            break;
                        case 6:
                            if(((GoSleepActivity) getActivity()).past_mode ==5
                                    && ((GoSleepActivity) getActivity()).current_mode ==6) {
                                back.setText("");
                                back.setBackgroundResource(R.drawable.bt_type_gray);
                                next.setText("Stop Alarm");
                                next.setBackgroundResource(R.drawable.bt_type_blue);
                                mode.setText("Current Mode : Wake Mode");
                                mode.setTextColor(Color.WHITE);
                            }
                            break;
                        case 8:
                            back.setText("");
                            back.setBackgroundResource(R.drawable.bt_type_gray);
                            next.setText("Restart");
                            next.setBackgroundResource(R.drawable.bt_type_blue);
                            mode.setTextColor(Color.GREEN);
                            mode.setText("Current Mode : Pause");
                            break;
                        case 9:
                            mode.setTextColor(Color.RED);
                            mode.setText("Current Mode : Emergency");
                            break;
                    }
                 }
            }
        };

        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ((GoSleepActivity)getActivity()).bt.send("mb",true);
                Log.d("dddd", "Send back");
            }
        });

        next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ((GoSleepActivity)getActivity()).bt.send("mn",true);
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
