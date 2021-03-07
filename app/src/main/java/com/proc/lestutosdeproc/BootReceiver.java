package com.proc.lestutosdeproc;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;

public class BootReceiver extends BroadcastReceiver {
    private final String TAG = "Proc BootReceiver";

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(TAG, "Boot completed");
        ProcService.scheduleJob(context);
    }

}
