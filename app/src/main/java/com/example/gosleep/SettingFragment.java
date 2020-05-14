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
    Button setButton;
    RadioGroup radioGroup;
    RadioButton r1,r2,r3,r4,r5;
    TextView lastSetTime;

    boolean badInput = false;

    EditText eHour,eMin,eDay,eMonth;
    int month,day,hour,min;
    String mMonth,mDay,mHour,mMin;
    DateFormat dateFormat;
    RadioButton.OnClickListener radioButtonClickListener;
    SharedPreferences.Editor editor;
    public final String PREFERENCE = "com.example.gosleep";
    SharedPreferences pref;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_alarm, container, false);

        pref = getActivity().getSharedPreferences(PREFERENCE,MODE_PRIVATE);
        editor = pref.edit();
        lastSetTime = (TextView)view.findViewById(R.id.lastset);
        lastSetTime.setText(pref.getString("savedAlarm","Last Set Time : "));

        eHour = (EditText)view.findViewById(R.id.hour);
        eMin = (EditText)view.findViewById(R.id.min);
        eDay = (EditText)view.findViewById(R.id.day);
        eMonth = (EditText)view.findViewById(R.id.month);

        radioButtonClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                    switch(view.getId()){
                        case R.id.radioButton:
                            ((GoSleepActivity) getActivity()).bt.send("fs0" + 51, true);
                            //eFan.setText(""+51);
                            break;
                        case R.id.radioButton2:
                            ((GoSleepActivity) getActivity()).bt.send("fs" + 102, true);
                            //eFan.setText(""+102);
                            break;
                        case R.id.radioButton3:
                            ((GoSleepActivity) getActivity()).bt.send("fs" + 153, true);
                            //eFan.setText(""+153);
                            break;
                        case R.id.radioButton4:
                            ((GoSleepActivity) getActivity()).bt.send("fs" + 204, true);
                            //eFan.setText(""+204);
                            break;
                        case R.id.radioButton5:
                            ((GoSleepActivity) getActivity()).bt.send("fs" + 255, true);
                            //eFan.setText(""+255);
                            break;
                    }
            }
        };

        r3 = (RadioButton)view.findViewById(R.id.radioButton3);
        r3.setOnClickListener(radioButtonClickListener);
        r3.setChecked(true);

        r1 = (RadioButton)view.findViewById(R.id.radioButton);
        r1.setOnClickListener(radioButtonClickListener);
        r2 = (RadioButton)view.findViewById(R.id.radioButton2);
        r2.setOnClickListener(radioButtonClickListener);
        r4 = (RadioButton)view.findViewById(R.id.radioButton4);
        r4.setOnClickListener(radioButtonClickListener);
        r5 = (RadioButton)view.findViewById(R.id.radioButton5);
        r5.setOnClickListener(radioButtonClickListener);


        long now = System.currentTimeMillis();
        dateFormat = new SimpleDateFormat("yyyyMMddHHmm");
        Date dateNow = new Date(now);

        // 현재 시각으로 설정
        eHour.setText(dateNow.getHours()+"");
        eMin.setText(dateNow.getMinutes()+"");
        eDay.setText(dateNow.getDate()+"");
        eMonth.setText(dateNow.getMonth()+1+"");
        //eFan.setText(""+0);

        setButton = (Button)view.findViewById(R.id.setbt);
        setButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                hour = Integer.parseInt(eHour.getText().toString());
                month = Integer.parseInt(eMonth.getText().toString());
                day = Integer.parseInt(eDay.getText().toString());
                min = Integer.parseInt(eMin.getText().toString());

                if(hour > 60 || month >12 || day >31 || min > 60) //윤년 및 각 달별 날짜 계산 보류
                    badInput = true;
                else
                    badInput = false;

                if(hour < 10)
                    mHour = "0"+hour;
                else
                    mHour = hour+"";
                if(month<10)
                    mMonth = "0"+month;
                else
                    mMonth = month+"";
                if(day<10)
                    mDay = "0"+day;
                else
                    mDay = day+"";
                if(min<10)
                    mMin = "0"+min;
                else
                    mMin = min +"";

                try {
                    long now = System.currentTimeMillis();
                    Date dateNow = new Date(now);
                    Date dateAlram = dateFormat.parse((dateNow.getYear()+1900)+mMonth+mDay+mHour+mMin);
                    long duration = dateAlram.getTime() - dateNow.getTime();

                    if(duration>0 && !badInput) {
                        ((GoSleepActivity) getActivity()).bt.send("t" + mMonth + mDay + mHour + mMin, true);
                        Log.d("dddd", "Sending..: " + mMonth + mDay + mHour + mMin);
                        editor.putString("savedAlarm","Last Set Time : "+mMonth+"월 "+mDay+"일 "+mHour+"시 "+mMin+"분");
                        lastSetTime.setText("Last Set Time : "+mMonth+"월 "+mDay+"일 "+mHour+"시 "+mMin+"분");
                        Toast.makeText(view.getContext(),"알람이 설정 되었습니다.",Toast.LENGTH_SHORT).show();
                        editor.commit();
                    }
                    else if(badInput) {
                        Toast.makeText(view.getContext(),"날짜와 시간을 정확히 입력해 주세요",Toast.LENGTH_SHORT).show();
                    }
                    else
                        Toast.makeText(view.getContext(),"현재 시간 보다 이후 시간을 설정하세요",Toast.LENGTH_SHORT).show();

                }catch (ParseException e) {
                    e.printStackTrace();
                }
            }
        });
        return view;
    }
}

