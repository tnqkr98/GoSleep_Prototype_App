package com.example.gosleep;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.DragEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.larswerkman.holocolorpicker.ColorPicker;
import com.larswerkman.holocolorpicker.SVBar;
import com.larswerkman.holocolorpicker.SaturationBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class MoodFragment extends Fragment {
    View view;

    Switch lightonoff;
    SeekBar brightness;
    ColorPicker picker;

    Boolean isLEDon = false;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_mood, container, false);

        picker = (ColorPicker)view.findViewById(R.id.picker);
        SVBar svBar = (SVBar) view.findViewById(R.id.svbar);
        picker.addSVBar(svBar);
        picker.setShowOldCenterColor(false);
        picker.setOnColorChangedListener(new ColorPicker.OnColorChangedListener() {
            @Override
            public void onColorChanged(int color) {
                int argb = picker.getColor();
                Log.d("dddd","drag~~~~~~");
                if(isLEDon) {   // 한번에 많은 터치 인식 이슈 해결 필요
                    ((GoSleepActivity) getActivity()).bt.send("lp" + threeChar(getRed(argb)) + threeChar(getGreen(argb)) + threeChar(getBlue(argb)), true);
                    try {
                        Thread.sleep(50); //동작이 너무 빠르면 아두이노 통신 상 큐에 너무쌓여서 출력 딜레이발생.
                    }catch (Exception e){}
                }
            }
        });

        lightonoff = (Switch)view.findViewById(R.id.switch1);
        brightness = (SeekBar)view.findViewById(R.id.seekBar);
        brightness.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) { }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) { }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if(isLEDon) {   // 한번에 많은 터치 인식 이슈 해결 필요
                    //seekBar.getProgress();
                    ((GoSleepActivity) getActivity()).bt.send("lb", true);
                    try {
                        Thread.sleep(50);
                    }catch (Exception e){}
                }
            }
        });

        lightonoff.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                int argb = picker.getColor();
                if(b) {
                    isLEDon = true;
                    Log.d("dddd","ledset :"+threeChar(getRed(argb))+threeChar(getGreen(argb))+threeChar(getBlue(argb)));
                    ((GoSleepActivity) getActivity()).bt.send("lp"+threeChar(getRed(argb))+threeChar(getGreen(argb))+threeChar(getBlue(argb)),true);
                }
                else {
                    ((GoSleepActivity) getActivity()).bt.send("le",true);
                    isLEDon = false;
                }
            }
        });
        return view;
    }

    int getRed(int argb){ return (argb>>16)&0xFF; }
    int getGreen(int argb){ return (argb>>8)&0xFF; }
    int getBlue(int argb){ return argb&0xFF; }
    String threeChar(int val){
        if(val >=100)
            return val+"";
        else if(val >=10)
            return "0"+val;
        else
            return "00"+val;
    }
}
