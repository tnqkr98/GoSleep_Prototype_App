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
                if(((GoSleepActivity)getActivity()).moodLEDon) {   // 한번에 많은 터치 인식 이슈 해결 필요
                    ((GoSleepActivity) getActivity()).bt.send("lp" + threeChar(getRed(argb)) + threeChar(getGreen(argb)) + threeChar(getBlue(argb)), true);
                    try {
                        Thread.sleep(30); //동작이 너무 빠르면 아두이노 통신 상 큐에 너무쌓여서 출력 딜레이발생.
                    }catch (Exception e){}
                }
            }
        });

        lightonoff = (Switch)view.findViewById(R.id.switch1);
        brightness = (SeekBar)view.findViewById(R.id.seekBar);
        brightness.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {}

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) { }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if(((GoSleepActivity)getActivity()).moodLEDon && ((GoSleepActivity) getActivity()).current_mode <= 3) {
                    int data = seekBar.getProgress();
                    if(data >=100)
                        ((GoSleepActivity) getActivity()).bt.send("lb"+data, true);
                    else if(data>=10)
                        ((GoSleepActivity) getActivity()).bt.send("lb0"+data, true);
                    else
                        ((GoSleepActivity) getActivity()).bt.send("lb00"+data, true);
                }
            }
        });
        lightonoff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int argb = picker.getColor();
                if(lightonoff.isChecked()) {
                    Log.d("dddd","ledset :"+threeChar(getRed(argb))+threeChar(getGreen(argb))+threeChar(getBlue(argb)));
                    if(((GoSleepActivity) getActivity()).current_mode < 3) {
                        ((GoSleepActivity)getActivity()).bt.send("lp" + threeChar(getRed(argb)) + threeChar(getGreen(argb)) + threeChar(getBlue(argb)), true);
                        ((GoSleepActivity)getActivity()).moodLEDon = true;
                    }
                    else
                        Toast.makeText(v.getContext(),"대기모드에서만 동작합니다.",Toast.LENGTH_SHORT);
                }
                else {
                    if(((GoSleepActivity) getActivity()).current_mode <3) {
                        ((GoSleepActivity)getActivity()).bt.send("le", true);
                        ((GoSleepActivity)getActivity()).moodLEDon = false;
                    }
                    else
                        Toast.makeText(v.getContext(),"대기모드에서만 동작합니다.",Toast.LENGTH_SHORT);
                }
            }
        });

        return view;
    }

    int getRed(int argb){ return (argb>>16)&0xFF; }
    int getGreen(int argb){ return (argb>>8)&0xFF; }
    int getBlue(int argb){ return argb&0xFF; }
    String threeChar(int val){
        if(val >=100) return val+"";
        else if(val >=10) return "0"+val;
        else return "00"+val;
    }
}
