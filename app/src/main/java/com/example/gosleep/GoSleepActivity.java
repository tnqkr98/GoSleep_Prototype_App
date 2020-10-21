package com.example.gosleep;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;
import androidx.viewpager.widget.ViewPager;
import app.akexorcist.bluetotohspp.library.BluetoothSPP;
import app.akexorcist.bluetotohspp.library.BluetoothState;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;
import com.google.gson.annotations.SerializedName;

import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Set;
import java.util.zip.CRC32;

public class GoSleepActivity extends AppCompatActivity {
    // 블루투스 자동페어링
    BluetoothSPP bt;
    BluetoothAdapter mBluetoothAdapter;
    Set<BluetoothDevice> mPairedDevices;
    String goSleepMacAddress = null;
    Boolean task_doing = false, pairingOn = false;
    Intent goSleepIntent;
    static final String GOSLEEP_DEVICE_ID = "gosleep";
    static HashMap<String,String> unBondedDeviceList;
    ProgressDialog progressDialog;
    static boolean unbindDiscovering  = false;
    private static Handler mHandler;

    private ImageView img_bluetooth;
    private LinearLayout progress_layout;
    private TextView progress_text;
    private int loading_num = 0;

    MyBroadcastReceiver receiver;

    public int current_mode = 2;
    public String tem = "0 °C", hum = "0 %", co2 = "000 ppm", dist = "00 cm", fanspeed = "000", cds="0 lux";
    public String gosleepTime = "00:00";
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

    // network
    private NetworkAPI service;
    public int past_mode = 2;
    private String product_id = "NYX-";
    private String regId;

    // DevClick
    private int clickCount = 0;
    private boolean devIsOn = false;

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
        navigation.getMenu().getItem(3).setVisible(false);

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
                    if(array[0].equals("t")) {               // 설정된 알람 시간 동기화
                        if(array[1].equals("n"))
                            editor.putString("savedAlarm","Last Set Time : None");
                        else {
                            String s = "Last Set Time : "+array[1]+"시 "+array[2]+"분";
                            Log.d("dddd","alarm receiv : "+s);
                            editor.putString("savedAlarm", s);
                        }
                        editor.commit();
                    }
                    else if(array[0].equals("r"))         // 아두이노로부터 인터넷시간 동기화 실패 재전송 요청시
                        bt.send(generatePacket(),true);
                    else {
                        hum = array[0].concat(" %");
                        tem = array[1].concat("°C");
                        fanspeed = array[2];
                        current_mode = Integer.parseInt(array[3]);
                        co2 = array[4].concat(" ppm");
                        dist = array[5].concat(" cm");
                        cds = array[6].concat(" lux");

                        if (array[7].equals("1")) fanOn = true;
                        else fanOn = false;
                        if (array[8].equals("1")) velveOn = true;
                        else velveOn = false;

                        try {
                            gosleepTime = "제품 시간 : " + array[9] + ":" + array[10];
                        }catch (Exception e){
                            gosleepTime = "제품 시간 : 오류발생";
                        }

                        // 쓰레드 없이 프레그먼트 조작
                        MoodFragment mf = (MoodFragment) getSupportFragmentManager().findFragmentByTag("android:switcher:" + R.id.viewpager + ":" + 1);
                        if (current_mode > 2) {
                            mf.lightonoff.setChecked(false);
                            moodLEDon = false;
                        } else if (current_mode == 6){
                            mf.lightonoff.setChecked(true);
                           moodLEDon = true;
                        }

                        // 수면 시작 시간 서버에 기록
                        try {
                            if (past_mode == 3 && current_mode == 4) {
                                // POST request
                                startRequest(new ReqData(product_id, "android", "Sleep Mode"));
                            }
                            past_mode = current_mode;
                        }catch (Exception e){
                            Log.d("dddd",e.getMessage()+"서버 POST 요청 오류!!");
                        }
                        past_mode = current_mode;
                        //Log.d("dddd", "분석 >> 습도 : " + hum + " 온도 :" + tem + "  팬 속도 : " + fanspeed + "  현재 고슬립 모드 :" + current_mode);
                    }
                }catch (Exception e){
                    Log.d("dddd",e.getMessage()+" 아두이노 수신메시지 오류!!");
                }
            }
        });

        bt.setBluetoothConnectionListener(new BluetoothSPP.BluetoothConnectionListener() {
            @Override
            public void onDeviceConnected(String name, String address) {
                Log.d("dddd","Activity: onDeviceConnected");
                startService(goSleepIntent);     // 포그라운드 서비스 시작.

                bt.send(generatePacket(),true);

                progressDialog.dismiss();
                task_doing = false;
                pairingOn = true;
                product_id = "NYX-"+name.substring(7,14);

                img_bluetooth.setColorFilter(Color.argb(255, 0, 0, 255));
                progress_layout.setVisibility(View.INVISIBLE);
                Toast.makeText(getApplicationContext(),"gosleep : Connected to " + product_id, Toast.LENGTH_LONG).show();
            }

            @Override
            public void onDeviceDisconnected() {
                Log.d("dddd","Activity: onDeviceDisconnected");
                pairingOn = false;

                stopService(goSleepIntent); // 포그라운드 서비스 중단
                if(!pairingOn && !task_doing) {
                    img_bluetooth.setColorFilter(Color.argb(255, 68, 68, 68));
                    progress_layout.setVisibility(View.VISIBLE);
                    newConnectGoSleep();
                }
            }

            @Override
            public void onDeviceConnectionFailed() {
                Log.d("dddd","Activity: onDeviceConnectionFailed");
                pairingOn = false;
                stopService(goSleepIntent); // 포그라운드 서비스 중단
                if(!pairingOn && !task_doing) {
                    img_bluetooth.setColorFilter(Color.argb(255, 68, 68, 68));
                    progress_layout.setVisibility(View.VISIBLE);
                    newConnectGoSleep();
                }
            }
        });

        img_bluetooth = (ImageView)findViewById(R.id.bluetooth);
        progress_layout = (LinearLayout)findViewById(R.id.progress_layout);
        progress_text = (TextView)findViewById(R.id.progress_text);
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {       // 비정상 종료 핸들러( 사용자가 블루투스 강종시 동작)
            @Override
            public void uncaughtException(@NonNull Thread t, @NonNull Throwable e) {
                Toast.makeText(getApplicationContext(),"블루투스를 종료하시면 앱을 이용할 수 없습니다",Toast.LENGTH_SHORT).show();
                e.printStackTrace();
                android.os.Process.killProcess(android.os.Process.myPid());
                System.exit(10);
            }
        });

        mHandler = new Handler(){
            @Override
            public void handleMessage(@NonNull Message msg) {       // MainThread View control
                if (msg.what == 0) {
                    if (loading_num == 0)
                        progress_text.setText("");
                    else if (loading_num == 1)
                        progress_text.setText(" .");
                    else if (loading_num == 2)
                        progress_text.setText(" . .");
                    else if (loading_num == 3)
                        progress_text.setText(" . . .");
                    if (loading_num++ == 2) loading_num = 0;
                }
            }
        };

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        service = NetworkClient.getClient().create(NetworkAPI.class);   // REST API

        // FCM
        FirebaseInstanceId.getInstance().getInstanceId()
                .addOnCompleteListener(new OnCompleteListener<InstanceIdResult>() {
                    @Override
                    public void onComplete(@NonNull Task<InstanceIdResult> task) {
                        if(!task.isSuccessful()){
                            Log.w("dddd","getInstanceId failed",task.getException());
                            return;
                        }
                        String token = task.getResult().getToken();
                        Log.d("FCM Log","FCM 토큰 : "+token);
                    }
                });
        regId = FirebaseInstanceId.getInstance().getToken();
        Log.d("dddd","regId : "+regId);
        /* 비페어링된 기기 탐색을 위한 브로드캐스트리시버 */
        // 등록되지 않은 기기 탐색을 위해
        /*unBondedDeviceList = new HashMap<>();

        receiver = new MyBroadcastReceiver();

        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);             // 왜  android 9.0 은  이것을 수신하지 못할까?
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        this.registerReceiver(receiver, filter);*/
    }

    public static final class MyBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {     // 이거 다음 기기 연동때, 완전 자동 연결 실험해볼것.
            String action = intent.getAction(); //may need to chain this to a recognizing function
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                Log.d("dddd","BroadcastReceiver : ACTION_FOUND");
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String derp = device.getName() + " - " + device.getAddress();
                Log.d("dddd", derp);
                unBondedDeviceList.put(device.getName(),device.getAddress());
            }

            if(BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                unBondedDeviceList.clear();
                unbindDiscovering = true;
                Log.d("dddd", "BroadcastReceiver : ACTION_DISCOVERY_STARTED");
            }

            if(BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                Log.d("dddd", "BroadcastReceiver : ACTION_DISCOVERY_FINISHED");
                unbindDiscovering = false;
            }
        }
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
        //unregisterReceiver(receiver);   // 리시버 등록해제
        finish();
        super.onDestroy();
    }

    @Override
    protected void onStart() {   // 호출시점 : 뒤로가기 앱 종료 후 다시 실행시 onCreate 가 아닌 이것이 호출. (아두이노와의 연결 다시 수행해야함)
        super.onStart();
        Log.d("dddd","Activity: onStart");
        if(!pairingOn && !task_doing)
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
        builder.setTitle("").setMessage("앱을 종료하시겠습니까?");
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
        Log.d("dddd","newConnectGosleep");
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
        Log.d("dddd","connectThread()");
        //if(mBluetoothAdapter.isDiscovering())
        //    mBluetoothAdapter.cancelDiscovery();
        //mBluetoothAdapter.startDiscovery();

        progressDialog = new ProgressDialog(GoSleepActivity.this);
        progressDialog.setMessage("Find gosleep....");
        progressDialog.setCancelable(true);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.show();

        Thread thread = new Thread(){
            @Override
            public void run() {
                for(int i=0;i<50000;i++){
                    if(!pairingOn) {
                        mHandler.sendEmptyMessage(0);
                        Log.d("dddd", "connectThread() run() i:" + i);
                        task_doing = true;
                        // 페어링된(Bonded) 기기집합에서 먼저 찾기
                        mPairedDevices = mBluetoothAdapter.getBondedDevices();
                        for (BluetoothDevice device : mPairedDevices) {
                            if(device.getName().length() < 7) continue;
                            Log.d("dddd", "device name : " + device.getName().substring(0, 7));
                            if (device.getName().substring(0, 7).equals(GOSLEEP_DEVICE_ID)) {
                                goSleepMacAddress = device.getAddress();
                                //Log.d("dddd","device address : "+device.getAddress());
                                if (goSleepMacAddress != null) {
                                    bt.connect(goSleepMacAddress);
                                    Log.d("dddd", "디바이스 발견 " + device.getName() + "," + device.getAddress());
                                    try {
                                        Thread.sleep(2000);
                                    } catch (Exception e) { }
                                    bt.send(generatePacket(),true);
                                    break;
                                }
                            }
                        }
                        // 페어링되지 않은 집합에서 찾기(될까? 된다. Android 8.0 이하만;)
                        /*if(!unbindDiscovering){
                            mBluetoothAdapter.cancelDiscovery();
                            mBluetoothAdapter.startDiscovery();
                        }*/

                        /*for(String device : unBondedDeviceList.keySet()){
                            if(device!=null && device.substring(0,6).equals(GOSLEEP_DEVICE_ID)){
                                bt.connect(unBondedDeviceList.get(GOSLEEP_DEVICE_ID));
                                try { Thread.sleep(1000); } catch (Exception e) { }
                            }
                        }*/

                        /*if(pairingOn) {
                            startService(goSleepIntent);     // 포그라운드 서비스 시작.
                            bt.send("r",true);   // 재연결 시 아두이노 상황 동기화 요청
                            progressDialog.dismiss();
                            //mBluetoothAdapter.cancelDiscovery(); // 계속 시작되어있으면 대역폭감소유발 (BroadCast 관련)
                            task_doing = false;
                            break;
                        }*/
                        try {
                            Thread.sleep(1000);
                        } catch (Exception e) {
                        }
                    }
                }
            }
        };
        thread.start();
    }

    public void onQuestionClick(View v){
        View dlgView = View.inflate(this, R.layout.dialog_question,null);
        final Dialog dlg = new Dialog(this);
        dlg.setContentView(dlgView);
        dlg.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        Button before, after;
        before = (Button)dlgView.findViewById(R.id.bt_before);
        after = (Button)dlgView.findViewById(R.id.bt_after);

        before.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dlg.cancel();
                Intent intent = new Intent(getApplicationContext(),QuestionBeforeSleepActivity.class);
                startActivity(intent);
            }
        });

        dlg.show();
    }

    public void onDevClick(View view){
        clickCount++;
        if(clickCount == 5 && !devIsOn) {
            devIsOn = true;
            Toast.makeText(getApplicationContext(), "Dev Mode Activated", Toast.LENGTH_LONG);
            navigation.getMenu().getItem(3).setVisible(true);
            clickCount = 0;
        }else if(clickCount == 5 && devIsOn){
            devIsOn = false;
            Toast.makeText(getApplicationContext(), "Dev Mode Deactivated", Toast.LENGTH_LONG);
            navigation.getMenu().getItem(3).setVisible(false);
            clickCount = 0;
        }
    }

    public byte[] generatePacket(){
        long now = System.currentTimeMillis();
        Date dateNow = new Date(now);

        // checksum 생성
        byte checksum = 0;
        checksum += (dateNow.getYear()-100)/10;
        checksum += (dateNow.getYear()-100)%10;
        checksum += (dateNow.getMonth()+1)/10;
        checksum += (dateNow.getMonth()+1)%10;
        checksum += dateNow.getDate()/10;
        checksum += dateNow.getDate()%10;
        checksum += dateNow.getHours()/10;
        checksum += dateNow.getHours()%10;
        checksum += dateNow.getMinutes()/10;
        checksum += dateNow.getMinutes()%10;
        checksum += dateNow.getSeconds()/10;
        checksum += dateNow.getSeconds()%10;

        // byte packet 길이제한 11? 까지인듯
        byte[] packet = {'r', (byte)(dateNow.getYear()-100), (byte)(dateNow.getMonth()+1), (byte)dateNow.getDate(),
                (byte)dateNow.getHours(), (byte)dateNow.getMinutes(), (byte)dateNow.getSeconds(),checksum};
        return packet;
    }

    // Network
    private void startRequest(ReqData data){
        service.callData(data).enqueue(new Callback<ResData>() {
            @Override
            public void onResponse(Call<ResData> call, Response<ResData> response) {
                ResData result = response.body();
                //textView.setText(result.getBottle_id());
                //Toast.makeText(GoSleepActivity.this,"Server Msg : "+result.getCode(), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(Call<ResData> call, Throwable t) {
                //Toast.makeText(GoSleepActivity.this, "Server Msg : Client Error", Toast.LENGTH_SHORT).show();
                t.printStackTrace();
            }
        });
    }

    public class ReqData{
        @SerializedName("Product_id")
        String Product_id;
        @SerializedName("User_id")
        String User_id;
        @SerializedName("Product_mode")
        String Product_mode;

        public ReqData(String product_id, String user_id, String product_mode) {
            Product_id = product_id;
            User_id = user_id;
            Product_mode = product_mode;
        }
    }

    public class ResData{
        @SerializedName("code")
        String code;

        public ResData(String code) { this.code = code; }
        public String getCode() { return code;}
    }
}
