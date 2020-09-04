package com.example.gosleep;

import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;

public class FCMBroadCast extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        //hrow new UnsupportedOperationException("Not yet implemented");

        Log.d("dddd","BroadCastDataBroadCastDataBroadCastDataBroadCastDataBroadCastDataBroadCastDataBroadCastDataBroadCastDataBroadCastDataBroadCastDataBroadCastDataBroadCastDataBroadCastData");


        /*Intent newAct = new Intent(context,yourCallClassName.class);
        newAct.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(newAct);*/

        final Dialog dialog = new Dialog(context);
        dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(false);
        dialog.setContentView(R.layout.dialog_layout);
        // dialog.getWindow().setBackgroundDrawable(null);  //for making dialog to fill window completely

        Button ok = (Button) dialog.findViewById(R.id.ok);
        Button cancel = (Button) dialog.findViewById(R.id.can);

        ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        dialog.show();
        Window window = dialog.getWindow();

        window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
    }
}
