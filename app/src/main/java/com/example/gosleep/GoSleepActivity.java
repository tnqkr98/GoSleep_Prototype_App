package com.example.gosleep;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.IntentCompat;
import androidx.fragment.app.FragmentTransaction;
import androidx.viewpager.widget.ViewPager;
import app.akexorcist.bluetotohspp.library.BluetoothSPP;
import app.akexorcist.bluetotohspp.library.BluetoothState;
import app.akexorcist.bluetotohspp.library.DeviceList;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
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
    String goSleepMacAddress = null;
    Boolean task_doing = false, pairingOn = false;
    Intent goSleepIntent;
    static final String GOSLEEP_DEVICE_ID = "gosleep";

    static final int MODE_2 = 2, MODE_3 = 3, MODE_4 = 4, MODE_5 = 5, MODE_6 = 6;
    public int current_mode = 2, moduleControlCMD =0;
    public String tem = "0 °C", hum = "0 %", illum = "000 lux", co2 = "000 ppm", dist = "00 cm", fanspeed = "000";
    public boolean arduinoDataRecievOn = false, completeSetAlram = false;  // 이게 true 여야 모드4로 이동가능.
    public boolean velveOn = false, heatOn = false, fanOn = false, moodLEDon = false;

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

    SharedPreferences.Editor editor;
    SharedPreferences pref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gosleep);
        Log.d("dddd","Activity: onCreate");

        pref = getSharedPreferences("com.example.gosleep",MODE_PRIVATE);
        editor = pref.edit();

        //navigation view
        viewPager = (ViewPager)findViewById(R.id.viewpager);
        navigation = (BottomNavigationView)findViewById(R.id.bottomNavigationView);
        navigation.setOnNavigationItemSelectedListener(mOnNavi);

        adapter = new GoSleepViewPagerAdapter(getSupportFragmentManager());
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();

        adapter.AddFragment(new DashBoardFragment(),"dash");
        adapter.AddFragment(new MoodFragment(),"mood");
        adapter.AddFragment(new SettingFragment(),"setting");
        adapter.AddFragment(new DeveloperFragment(),"develop");
        fragmentTransaction.commit();
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
                try {
                    if (array[0].equals("v")) {                     // 밸브 상황(on/off) 비동기 수신
                        if (array[1].equals("1")) velveOn = true;
                        else velveOn = false;
                        moduleControlCMD++;
                    } else if (array[0].equals("f")) {              // 팬 상황(on/off) 비동기 수신
                        if (array[1].equals("1")) fanOn = true;
                        else fanOn = false;
                        moduleControlCMD++;
                    } else if (array[0].equals("h")) {              // 열선 상황(on/off) 비동기 수신
                        if (array[1].equals("1")) heatOn = true;
                        else heatOn = false;
                        moduleControlCMD++;
                    } else if(array[0].equals("t")) {               // 설정된 알람 시간 동기화
                        if(array[1].equals("n"))
                            editor.putString("savedAlarm","Last Set Time : None");
                        else {
                            String s = "Last Set Time : "+array[1]+"월 "+array[2]+"일 "+array[3]+"시 "+array[4]+"분";
                            Log.d("dddd","alarm receiv : "+s);
                            editor.putString("savedAlarm", s);
                        }
                        editor.commit();
                    }
                    else {
                        hum = array[0].concat(" %");
                        tem = array[1].concat("°C");
                        fanspeed = array[2];
                        if (array.length > 4) {   // uno test
                            co2 = array[4].concat(" ppm");
                            dist = array[5].concat(" cm");
                        }
                        // 조도 받기.

                        current_mode = Integer.parseInt(array[3]);

                        // 쓰레드 없이 프레그먼트 조작
                        MoodFragment mf = (MoodFragment) getSupportFragmentManager().findFragmentByTag("android:switcher:" + R.id.viewpager + ":" + 1);
                        if (current_mode > 2) {
                            mf.lightonoff.setChecked(false);
                            moodLEDon = false;
                        } else if (current_mode == 6){
                            mf.lightonoff.setChecked(true);
                           moodLEDon = true;
                        }
                        //Log.d("dddd", "분석 >> 습도 : " + hum + " 온도 :" + tem + "  팬 속도 : " + fanspeed + "  현재 고슬립 모드 :" + current_mode);
                    }
                }catch (Exception e){
                    Log.d("dddd",e.getMessage()+"아두이노 수신메시지 오류!!");
                }
            }
        });

        bt.setBluetoothConnectionListener(new BluetoothSPP.BluetoothConnectionListener() {
            @Override
            public void onDeviceConnected(String name, String address) {
                Log.d("dddd","Activity: onDeviceConnected");
                pairingOn = true;
                Toast.makeText(getApplicationContext(),"Connected to " + name + "\n" + address, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onDeviceDisconnected() {
                Log.d("dddd","Activity: onDeviceDisconnected");
                pairingOn = false;

                stopService(goSleepIntent); // 포그라운드 서비스 중단
                if(!task_doing)
                    newConnectGoSleep();
                /*if(!bt.isBluetoothEnabled()){   // 단말기 블루투스가 작동 중이 아닌경우
                    Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);   // 블루투스를 사용할지 묻자
                    startActivityForResult(intent,BluetoothState.REQUEST_ENABLE_BT);
                }
                else if(!bt.isServiceAvailable()){   // // 단말기 블루투스가 작동중, 서비스가 미작동 중이라면,
                    bt.setupService();
                    bt.startService(BluetoothState.DEVICE_OTHER);
                }
                else if(!task_doing)
                    newConnectGoSleep();*/
            }

            @Override
            public void onDeviceConnectionFailed() {
                Log.d("dddd","Activity: onDeviceConnectionFailed");
                pairingOn = false;
                stopService(goSleepIntent); // 포그라운드 서비스 중단
                if(!task_doing)
                    newConnectGoSleep();
            }
        });

        ImageView btnConnect = (ImageView)findViewById(R.id.bluetooth);

        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {       // 비정상 종료 핸들러( 사용자가 블루투스 강종시 동작)
            @Override
            public void uncaughtException(@NonNull Thread t, @NonNull Throwable e) {
                Toast.makeText(getApplicationContext(),"블루투스를 종료하시면 앱을 이용할 수 없습니다",Toast.LENGTH_SHORT).show();
                e.printStackTrace();
                android.os.Process.killProcess(android.os.Process.myPid());
                System.exit(10);
            }
        });
    }

    public void onDestroy(){
        Log.d("dddd","Activity: onDestroy");
        task_doing = true;         // progressTask 작동 못하게한다.
        if(bt.isServiceAvailable()) {
            bt.send("c",true);
            bt.stopService();
        }
        arduinoDataRecievOn = false;
        Toast.makeText(getApplicationContext(),"GoSleep : 기기와의 연동을 중단합니다.",Toast.LENGTH_SHORT).show();
        stopService(goSleepIntent);
        finish();
        super.onDestroy();
    }

    @Override
    protected void onStart() {   // 호출시점 : 뒤로가기 앱 종료 후 다시 실행시 onCreate 가 아닌 이것이 호출. (아두이노와의 연결 다시 수행해야함)
        super.onStart();
        Log.d("dddd","Activity: onStart");
        if(!pairingOn)
            newConnectGoSleep();
    }

    @Override
    protected void onResume() { Log.d("dddd","Activity: onResume");super.onResume();}
    @Override
    protected void onPause() { Log.d("dddd","Activity: onPause");super.onPause(); }

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
            if(resultCode == Activity.RESULT_OK) // 인텐트 메시지에 OK 를 누른경우
                newConnectGoSleep();
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
                bt.send("c",true);
                finish();
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) { }});

        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    private void newConnectGoSleep(){
        //Log.d("dddd","newConnectGosleep");
        if(!bt.isBluetoothEnabled()){   // 단말기 블루투스가 작동 중이 아닌경우
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);   // 블루투스를 사용할지 묻자
            startActivityForResult(intent,BluetoothState.REQUEST_ENABLE_BT);
        }
        else{  // 단말기 블루투스가 작동중인경우
            if(!bt.isServiceAvailable()){   // 서비스가 미작동 중이라면,
                bt.setupService();
                bt.startService(BluetoothState.DEVICE_OTHER);
                connectThread();
            }
            else
                connectThread();
        }
    }
    void connectThread(){
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mPairedDevices = mBluetoothAdapter.getBondedDevices();

        final ProgressDialog progressDialog = new ProgressDialog(GoSleepActivity.this);
        progressDialog.setMessage("Find GoSleep....");
        progressDialog.setCancelable(false);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.show();

        Thread thread = new Thread(){
            @Override
            public void run() {
                for(int i=0;i<500;i++){
                    task_doing = true;
                    for (BluetoothDevice device : mPairedDevices) {
                        if (device.getName().equals(GOSLEEP_DEVICE_ID)) {    // 여기서 모든 고슬립 디바이스명을 지정해야함.
                            goSleepMacAddress = device.getAddress();
                            if(goSleepMacAddress != null) {
                                bt.connect(goSleepMacAddress);
                                try { Thread.sleep(1000); } catch (Exception e) { }
                            }
                        }
                    }
                    if(pairingOn) {
                        startService(goSleepIntent);     // 포그라운드 서비스 시작.
                        bt.send("r",true);   // 재연결 시 아두이노 상황 동기화 요청
                        progressDialog.dismiss();
                        task_doing = false;
                        break;
                    }
                    try { Thread.sleep(1000); } catch (Exception e) { }
                }
            }
        };
        thread.start();
    }
}
