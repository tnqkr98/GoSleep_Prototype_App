package com.example.gosleep;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import static android.content.Context.MODE_PRIVATE;

public class SettingFragment extends Fragment {
    View view;
    Button setButton, resetButton;
    RadioButton r2,r3,r4;   // fan
    RadioButton r5,r6,r7;   // co2
    TextView lastSetTime;

    boolean badInput = false;

    EditText eHour,eMin,eDay,eMonth;
    int month,day,hour,min;
    String mMonth,mDay,mHour,mMin;
    DateFormat dateFormat;
    RadioButton.OnClickListener radioButtonClickListener;

    SharedPreferences.Editor editor;
    SharedPreferences pref;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_alarm, container, false);

        pref = getActivity().getSharedPreferences("com.example.gosleep",MODE_PRIVATE);
        editor = pref.edit();
        lastSetTime = (TextView)view.findViewById(R.id.lastset);
        lastSetTime.setText(pref.getString("savedAlarm","Last Set Time : None"));

        eHour = (EditText)view.findViewById(R.id.hour);        eMin = (EditText)view.findViewById(R.id.min);
        //eDay = (EditText)view.findViewById(R.id.day);
        //eMonth = (EditText)view.findViewById(R.id.month);

        radioButtonClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                    switch(view.getId()){
                        case R.id.radioButton1:
                            ((GoSleepActivity) getActivity()).bt.send("fs" + 80, true);
                            break;
                        case R.id.radioButton2:
                            ((GoSleepActivity) getActivity()).bt.send("fs" + 90, true);
                            break;
                        case R.id.radioButton3:
                            ((GoSleepActivity) getActivity()).bt.send("fs" + 100, true);
                            break;
                        case R.id.radioButton4:
                            ((GoSleepActivity) getActivity()).bt.send("fs" + 110, true);
                            break;
                        case R.id.radioButton8:
                            ((GoSleepActivity) getActivity()).bt.send("fs" + 120, true);  // 4.5v  약
                            break;
                    }
            }
        };

        r3 = (RadioButton)view.findViewById(R.id.radioButton3);
        r3.setOnClickListener(radioButtonClickListener);
        r3.setChecked(true);

        r2 = (RadioButton)view.findViewById(R.id.radioButton2);
        r2.setOnClickListener(radioButtonClickListener);
        r4 = (RadioButton)view.findViewById(R.id.radioButton4);
        r4.setOnClickListener(radioButtonClickListener);

        r5= (RadioButton)view.findViewById(R.id.radioButton5);
        r5.setOnClickListener(radioButtonClickListener);

        r6= (RadioButton)view.findViewById(R.id.radioButton6);
        r6.setOnClickListener(radioButtonClickListener);
        r6.setChecked(true);

        r7= (RadioButton)view.findViewById(R.id.radioButton7);
        r7.setOnClickListener(radioButtonClickListener);

        long now = System.currentTimeMillis();
        dateFormat = new SimpleDateFormat("yyyyMMddHHmm");
        Date dateNow = new Date(now);

        // 현재 시각으로 설정
        eHour.setText(dateNow.getHours()+"");
        eMin.setText(dateNow.getMinutes()+"");
        //eDay.setText(dateNow.getDate()+"");
        //eMonth.setText(dateNow.getMonth()+1+"");
        //eFan.setText(""+0);

        setButton = (Button)view.findViewById(R.id.setbt);
        setButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String h,m;
                h = eHour.getText().toString();
                m = eMin.getText().toString();
                if(!h.isEmpty() && !m.isEmpty()) {
                    hour = Integer.parseInt(h);
                    min = Integer.parseInt(m);

                    if (hour > 60 || min > 60)
                        badInput = true;
                    else
                        badInput = false;

                    if (hour < 10)
                        mHour = "0" + hour;
                    else
                        mHour = hour + "";
                    if (min < 10)
                        mMin = "0" + min;
                    else
                        mMin = min + "";

                    if (!badInput) {
                        ((GoSleepActivity) getActivity()).bt.send("t" + mHour + mMin, true);
                        Log.d("dddd", "Sending..: " + mHour + mMin);
                        editor.putString("savedAlarm", "Last Set Time : " + mHour + "시 " + mMin + "분");
                        lastSetTime.setText("Last Set Time : " + mHour + "시 " + mMin + "분");
                        Toast.makeText(view.getContext(), "알람이 설정 되었습니다.", Toast.LENGTH_SHORT).show();
                        editor.commit();
                    } else
                        Toast.makeText(view.getContext(), "시간을 정확히 입력해 주세요", Toast.LENGTH_SHORT).show();
                }
                else
                    Toast.makeText(view.getContext(),"값을 입력하시오",Toast.LENGTH_SHORT);
            }
        });
        resetButton = (Button)view.findViewById(R.id.resetbt);
        resetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((GoSleepActivity) getActivity()).bt.send("tr",true);
                editor.putString("savedAlarm","Last Set Time : None");
                lastSetTime.setText("Last Set Time : None");
                Toast.makeText(view.getContext(),"알람이 해제 되었습니다.",Toast.LENGTH_SHORT).show();
                editor.commit();
            }
        });

        return view;
    }
}

