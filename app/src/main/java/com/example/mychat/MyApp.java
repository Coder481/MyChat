package com.example.mychat;

import android.app.AlertDialog;
import android.app.Application;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.google.firebase.firestore.FirebaseFirestore;

public class MyApp extends Application {

    public FirebaseFirestore db;
    private ConnectivityManager connectivityManager;
    private AlertDialog dialog;

    @Override
    public void onCreate() {
        super.onCreate();
        setup();
    }

    private void setup() {
        db = FirebaseFirestore.getInstance();
        connectivityManager = (ConnectivityManager)getSystemService(CONNECTIVITY_SERVICE);
    }

    public void showLoadingDialog(Context context){
        dialog = new AlertDialog.Builder(context)
                .setTitle("Loading...")
                .setMessage("Please Wait!")
                .show();
    }

    public void hideLoadingDialog(){
        if (dialog != null){
            dialog.dismiss();
        }
    }


    public boolean isOffline(){
        NetworkInfo wifiNetworkInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        NetworkInfo dataNetworkInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
        return !(wifiNetworkInfo.isConnected() || dataNetworkInfo.isConnected());
    }
}
