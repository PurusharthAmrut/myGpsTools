package com.example.extractreader;

import android.Manifest;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;

import com.example.extractreader.TheRealDeal;

import permissions.dispatcher.NeedsPermission;
import permissions.dispatcher.OnNeverAskAgain;
import permissions.dispatcher.OnPermissionDenied;
import permissions.dispatcher.OnShowRationale;
import permissions.dispatcher.PermissionRequest;
import permissions.dispatcher.RuntimePermissions;

@RuntimePermissions
public class MainActivity extends AppCompatActivity {

    private TheRealDeal mTheRealDeal;
    Button testButton;
    TextView infoTextView;
    TextView prnTextView;
    EditText infoCapacityEditText;
    String fileName = "Extract.txt"/*/storage/emulated/0/Extract.txt/*/;
    int[] prnMask = new int[210];
    static final String FILE_READ_ERROR = "FileReadError";
    static final String PERMISSION_ERROR = "NoPermissionGiven";
    static final String EXTERNAL_STORAGE_READ_ERROR = "ExternalStorageNotReadable";
    static final String UNPREDICTED_ERROR = "UnpredictedError";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mTheRealDeal = new TheRealDeal();

        testButton = (Button) findViewById(R.id.testButton);
        infoTextView = (TextView) findViewById(R.id.infoTextView);
        prnTextView = (TextView) findViewById(R.id.prnTextView);
        infoCapacityEditText = (EditText) findViewById(R.id.infoCapacityEditText);

        testButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MainActivityPermissionsDispatcher.extractAndDisplayWithPermissionCheck(MainActivity.this);
            }
        });
    }

    @NeedsPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    public void extractAndDisplay(){        //Conditions for the toast messages need to be improved upon
        if (isExternalStorageReadable() && checkPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)){
            mTheRealDeal.messageExtract(fileName);
            displayPrn();
        }
        else if (!isExternalStorageReadable()){
            Toast.makeText(this, "Failed to read external storage", Toast.LENGTH_SHORT).show();
        }
        else if (!checkPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)){
            Toast.makeText(this, "Permission to write external storage not obtained", Toast.LENGTH_SHORT).show();
        }
        else{
            Toast.makeText(this, "Unpredicted error", Toast.LENGTH_SHORT).show();
        }
    }

    public void displayPrn(){
        for (int j = 0; j<210; j++){
            prnTextView.append(Integer.toString(mTheRealDeal.prnMask[j]));
        }
    }

    public boolean checkPermission(String permission){
        int check = ContextCompat.checkSelfPermission(this, permission);
        return (check == PackageManager.PERMISSION_GRANTED);
    }

    public boolean isExternalStorageReadable(){
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)){
            return true;
        }
        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        MainActivityPermissionsDispatcher.onRequestPermissionsResult(this, requestCode, grantResults);
    }

    @OnShowRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    void showRationaleForExternalStorage(final PermissionRequest permissionRequest){
        new AlertDialog.Builder(this)
                .setTitle("Permission needed")
                .setMessage("This permission is required to allow this app to store files in phone's external storage")
                .setPositiveButton("Grant permission", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        permissionRequest.proceed();
                    }
                })
                .setNegativeButton("Deny permission", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        permissionRequest.cancel();
                    }
                })
                .show();
    }

    @OnPermissionDenied(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    void onExternalStorageDenied(){
        Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
    }

    @OnNeverAskAgain(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    void onNeverAskAgain(){
        Toast.makeText(this, "Never asking again", Toast.LENGTH_SHORT).show();
    }

}
