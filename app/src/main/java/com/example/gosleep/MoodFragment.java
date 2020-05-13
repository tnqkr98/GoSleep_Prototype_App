package com.example.gosleep;

import android.os.Bundle;
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


        lightonoff = (Switch)view.findViewById(R.id.switch1);
        brightness = (SeekBar)view.findViewById(R.id.seekBar);
        brightness.setScaleX(1);

        //Toast.makeText(view.getContext(),picker.getColor()+"",Toast.LENGTH_SHORT).show();

        lightonoff.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if(b)
                    Toast.makeText(view.getContext(),"on : "+picker.getColor(),Toast.LENGTH_SHORT).show();
                else
                    Toast.makeText(view.getContext(),"off",Toast.LENGTH_SHORT).show();
            }
        });

        return view;
    }
}
