package com.example.gosleep;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.ViewPager;
import app.akexorcist.bluetotohspp.library.BluetoothSPP;
import app.akexorcist.bluetotohspp.library.BluetoothState;
import app.akexorcist.bluetotohspp.library.DeviceList;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.Iterator;
import java.util.Set;

public class GoSleepActivity extends AppCompatActivity {

    // 블루투스 자동페어링
    BluetoothSPP bt;
    BluetoothAdapter mBluetoothAdapter;
    Set<BluetoothDevice> mPairedDevices;
    String goSleepMacAddress;
    Boolean current_pairing_state = false, task_doing = false;
    Intent goSleepIntent;

    DashBoardFragment dashBoardFragment;
    static final int MODE_2 = 2, MODE_3 = 3, MODE_4 = 4, MODE_5 = 5, MODE_6 = 6;
    public int current_mode = 2;
    public String tem = "0 °C", hum = "0 %", illum = "000 lux", co2 = "000 ppm", dist = "00 cm", fanspeed = "000";
    public boolean arduinoDataRecievOn = false, completeSetAlram = false;  // 이게 true 여야 모드4로 이동가능.

    private BottomNavigationView navigation;
    private ViewPager viewPager;
    private GoSleepViewPagerAdapter adapter;
    private MenuItem prevMenuItem;

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavi = new BottomNavigationView.OnNavigationItemSelectedListener() {
        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
            switch (menuItem.getItemId()){
                case R.id.navigation_dash:
                    viewPager.setCurrentItem(0);
                    return true;
                case R.id.navigation_mood:
                    viewPager.setCurrentItem(1);
                    return true;
                case R.id.navigation_alarm:
                    viewPager.setCurrentItem(2);
                    return true;
                case R.id.navigation_developer:
                    viewPager.setCurrentItem(3);
                    return true;
            }
            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gosleep);

        //navigation view
        viewPager = (ViewPager)findViewById(R.id.viewpager);
        navigation = (BottomNavigationView)findViewById(R.id.bottomNavigationView);
        navigation.setOnNavigationItemSelectedListener(mOnNavi);

        dashBoardFragment = new DashBoardFragment();
        adapter = new GoSleepViewPagerAdapter(getSupportFragmentManager());
        adapter.AddFragment(dashBoardFragment,"dash");
        adapter.AddFragment(new MoodFragment(),"mood");
        adapter.AddFragment(new SettingFragment(),"setting");
        adapter.AddFragment(new DeveloperFragment(),"develop");
        viewPager.setAdapter(adapter);

        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) { }

            @Override
            public void onPageSelected(int position) {
                if(prevMenuItem != null)
                    prevMenuItem.setChecked(false);
                else
                    navigation.getMenu().getItem(0).setChecked(false);

                navigation.getMenu().getItem(position).setChecked(true);
                prevMenuItem = navigation.getMenu().getItem(position);
            }

            @Override
            public void onPageScrollStateChanged(int state) { }
        });

        bt = new BluetoothSPP(this);
        if(!bt.isBluetoothAvailable()){
            //Toast.makeText(getApplicationContext(), "Bluetooth is not available", Toast.LENGTH_SHORT).show();
            //finish();
        }
        goSleepIntent = new Intent(getApplicationContext(),GoSleepService.class);

        bt.setOnDataReceivedListener(new BluetoothSPP.OnDataReceivedListener() {
            @Override
            public void onDataReceived(byte[] data, String message) {
                arduinoDataRecievOn = true;
                Log.d("dddd", "Receiving Data From Arduino : "+message);
                String[] array = message.split(",");
                hum = array[0].concat(" %");
                tem = array[1].concat(" °C");
                fanspeed = array[2];
                // 조도, co2 농도 받기.

                current_mode = Integer.parseInt(array[3]);

                Log.d("dddd","분석 >> 습도 : "+hum+" 온도 :"+tem+ "  팬 속도 : "+fanspeed+"  현재 고슬립 모드 :"+current_mode);
            }
        });

        bt.setBluetoothConnectionListener(new BluetoothSPP.BluetoothConnectionListener() {
            @Override
            public void onDeviceConnected(String name, String address) {
                current_pairing_state = true;
                startService(goSleepIntent);     // 포그라운드 서비스 시작.
                Toast.makeText(getApplicationContext(),"Connected to " + name + "\n" + address, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onDeviceDisconnected() {
                current_pairing_state = false;
                stopService(goSleepIntent); // 포그라운드 서비스 중단
                if(!task_doing) {
                    try {    // 앱종료 시 처리
                        ProgressTask task = new ProgressTask();
                        task.execute();
                    }
                    catch (Exception e){}
                    //Toast.makeText(getApplicationContext(), "GoSleep Connection lost", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onDeviceConnectionFailed() {
                current_pairing_state = false;
                stopService(goSleepIntent); // 포그라운드 서비스 중단
                if(!task_doing) {
                    ProgressTask task = new ProgressTask();
                    task.execute();
                    Toast.makeText(getApplicationContext(), "Find GoSleep...", Toast.LENGTH_SHORT).show();
                }
                //Toast.makeText(getApplicationContext(), "Unable to connect", Toast.LENGTH_SHORT).show();
            }
        });

        ImageView btnConnect = (ImageView)findViewById(R.id.bluetooth);
        /*btnConnect.setOnClickListener(new View.OnClickListener(){   // 버튼으로 수동 연결 설정 관련.
            public void onClick(View v){
                if(bt.getServiceState() == BluetoothState.STATE_CONNECTED){
                    bt.send("c",true);
                    bt.disconnect();
                    arduinoDataRecievOn = false;
                }
                else{
                    Intent intent = new Intent(getApplicationContext(), DeviceList.class);
                    startActivityForResult(intent,BluetoothState.REQUEST_CONNECT_DEVICE);
                }
            }
        });*/
    }

    public void onDestroy(){
        if(bt.isServiceAvailable()) {
            bt.send("c",true);
            bt.stopService();
        }
        Log.d("dddd","onDestroy");
        arduinoDataRecievOn = false;
        Toast.makeText(getApplicationContext(),"GoSleep : 기기와의 연동을 중단합니다.",Toast.LENGTH_SHORT).show();
        stopService(goSleepIntent);
        finish();
        super.onDestroy();
    }

    @Override
    protected void onStart() {   // 호출시점 : 뒤로가기 앱 종료 후 다시 실행시 onCreate 가 아닌 이것이 호출. (아두이노와의 연결 다시 수행해야함)
        super.onStart();
        if(!bt.isBluetoothEnabled()){   // 단말기 블루투스가 작동 중이 아닌경우
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);   // 블루투스를 사용할지 묻자
            startActivityForResult(intent,BluetoothState.REQUEST_ENABLE_BT);
        }
        else{  // 단말기 블루투스가 작동중인경우
            if(!bt.isServiceAvailable()){   // 서비스가 미작동 중이라면,
                bt.setupService();
                bt.startService(BluetoothState.DEVICE_OTHER);
                connectGoSleep();
            }
        }
    }

    @Override
    protected void onResume() {
        if(!bt.isBluetoothEnabled()){   // 단말기 블루투스가 작동 중이 아닌경우
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);   // 블루투스를 사용할지 묻자
            startActivityForResult(intent,BluetoothState.REQUEST_ENABLE_BT);
        }
        else{  // 단말기 블루투스가 작동중인경우
            if(!bt.isServiceAvailable()){   // 서비스가 미작동 중이라면,
                bt.setupService();
                bt.startService(BluetoothState.DEVICE_OTHER);
                connectGoSleep();
            }
        }
        super.onResume();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == BluetoothState.REQUEST_CONNECT_DEVICE){
            if(resultCode == Activity.RESULT_OK) {
                /*bt.connect(data);    // 인텐트 분석코드
                Bundle b = data.getExtras();
                Iterator<String> iter = b.keySet().iterator();
                while(iter.hasNext()) {
                    String key = iter.next();
                    Object value = b.get(key);
                    Log.d("dddd", "key : "+key+", value : " + value.toString());
                }
                arduinoDataRecievOn = true;*/
            }
        }
        else if(requestCode == BluetoothState.REQUEST_ENABLE_BT){    //(블루투스 미작동 시) 블루투스 사용 여부를 묻는 인텐트의 응답
            if(resultCode == Activity.RESULT_OK){ // 인텐트 메시지에 OK 를 누른경우
                connectGoSleep();
                /*bt.setupService();
                bt.startService(BluetoothState.DEVICE_OTHER); // Arduino*/
                arduinoDataRecievOn = true;
            }
            else{ //(블루투스 미작동 시) 인텐트 메시지에  NO 를 누른경우
                Toast.makeText(getApplicationContext(), "GoSleep : Bluetooth was not enabled.", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    @Override
    public void onBackPressed() {  // 강제 종료 시, 앱과 기기의 연동이 중단된다는 경고
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("").setMessage("앱을 종료하시면, 기기의 작동이 중단됩니다.");
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                finish();
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

            }
        });

        AlertDialog alertDialog = builder.create();
        alertDialog.show();
        //super.onBackPressed();
    }

    private void connectGoSleep(){
        if(!bt.isBluetoothEnabled()){   // 단말기 블루투스가 현재 작동 중이 아니라면
            if(!bt.isServiceAvailable()){  // 블루투스 service가 실행가능하지 않다면
                bt.setupService();
                bt.startService(BluetoothState.DEVICE_OTHER);
            }
        }
        else{
            if(!bt.isServiceAvailable()){
                bt.setupService();
                bt.startService(BluetoothState.DEVICE_OTHER);
            }
        }
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mPairedDevices = mBluetoothAdapter.getBondedDevices();

        ProgressTask task = new ProgressTask();
        for (BluetoothDevice device : mPairedDevices)
            if(device.getName().equals("dharduino"))     // 여기서 모든 고슬립 디바이스명을 지정해야함.
                goSleepMacAddress = device.getAddress();
        task.execute();
    }

    private class ProgressTask extends AsyncTask<Void,Void,Void> {
        ProgressDialog asyncDialog = new ProgressDialog(GoSleepActivity.this);

        @Override
        protected void onPreExecute() {
            asyncDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            asyncDialog.setMessage("Find GoSleep....");
            asyncDialog.setCancelable(false);
            asyncDialog.show();
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(Void... voids) {
            try {
                for (int i = 0; i < 500; i++) {
                    if(!current_pairing_state) {
                        bt.connect(goSleepMacAddress);   // 여기서 연결.
                        task_doing = true;
                        Thread.sleep(1000);
                    }
                    else {
                        task_doing = false;
                        break;
                    }
                }
            }catch (Exception e){e.printStackTrace();}
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            asyncDialog.dismiss();
            super.onPostExecute(aVoid);
        }
    }
}
