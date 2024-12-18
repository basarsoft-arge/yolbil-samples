package com.basarsoft.yolbilmap;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import com.basarsoft.yolbil.DirectionCommand;
import com.basarsoft.yolbil.FileControl;
import com.basarsoft.yolbil.IEventHandler;
import com.basarsoft.yolbil.LicenseFailCode;
import com.basarsoft.yolbil.LicenseIdType;
import com.basarsoft.yolbil.Map;
import com.basarsoft.yolbil.Navigation;
import com.basarsoft.yolbil.NavigationLayer;
import com.basarsoft.yolbil.Point;
import com.basarsoft.yolbil.Yolbil;
import com.basarsoft.yolbil.YolbilEventController;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

public class NavigationActivity extends AppCompatActivity implements IEventHandler, FileControl.FileControlListener {

    private Point startPoint;
    private Point endPoint;

    public boolean fileControlCompleted = false;

    public boolean isNavigationStared = false;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_navigation);

        YolbilEventController.INSTANCE.registerListener(this);
        FileControl.setup(this);
        FileControl.checkMapFiles(this,this);
        View map = Yolbil.initialize(this, YolbilEventController.INSTANCE, Const.PROJECT_ID);
        if (map == null) {
            Toast.makeText(this, "Harita yüklenemedi.", Toast.LENGTH_LONG).show();
            return;
        }

        ConstraintLayout mainLayout = findViewById(R.id.mainLayout);
        mainLayout.addView(map);
        String endLon = "32.9376695998867";
        String endLat = "39.900709717986594";
        endPoint = new Point(Double.parseDouble(endLon), Double.parseDouble(endLat));
    }

    private void checkForNavigation() {
        if (startPoint != null && endPoint != null) {
            startNavigation(startPoint, endPoint);
        }
    }

    private void startNavigation(Point startPoint, Point endPoint) {
        Navigation.start(
                startPoint,
                endPoint,
                true,
                true,
                true
        );
        //NavigationLayer.addToMarker(endPoint);

        Map.zoom(800);
        Map.move(startPoint);
    }

    @Override
    public void mapLoaded(Context context) {
        Yolbil.ForceToDraw();
        checkForNavigation();
    }

    @Override
    public void mapResumed() {
        Yolbil.ForceToDraw();
        checkForNavigation();
    }

    @Override
    public void featurePickedByTapping(ArrayList<Object> arrayList, Point point, Point point1) {

    }

    @Override
    public void featurePickedByLongPressing(ArrayList<Object> arrayList, Point point, Point point1) {

    }

    @Override
    public void mapTapped(Point point, Point point1) {

    }

    @Override
    public void mapLongPressed(Point point, Point point1) {

    }

    @Override
    public void mapRendered() {

    }

    @Override
    public void locationChanged(Point point) {
        startPoint = point;
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(!isNavigationStared && fileControlCompleted){
                    startNavigation(startPoint, endPoint);
                    isNavigationStared = true;
                    NavigationLayer.addToMarker(endPoint);
                }
                NavigationLayer.addGpsMarker(point);
                Yolbil.ForceToDraw();            }
        });
    }

    @Override
    public void licenseIsNotValid(LicenseFailCode licenseFailCode) {
        if (licenseFailCode == LicenseFailCode.DEVICE_IS_NOT_LICENSED) {
            licenseRequestValidationDialog();
        } else {
            Toast.makeText(this, "Lisans kontrolü yapılamadı. Lütfen internet bağlantınızı ve uygulamanın yetkilerini kontrol edin.", Toast.LENGTH_LONG).show();
        }
    }

    public void licenseRequestValidationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(false);
        builder.setTitle("Lisans talebi gönderilsin mi ?");
        builder.setMessage("Cihazınız lisanslı değil. Lisanslanması için yöneticiye talebiniz iletilsin mi ?");
        builder.setPositiveButton("Evet", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                HashMap<String, String> infoList = new HashMap<>();
                infoList.put("firstName", "yolbil");
                Yolbil.sendLicenseRequest(LicenseIdType.GSFID, infoList);
                Toast.makeText(NavigationActivity.this, "Lisans isteği gönderildi. Lütfen yönetici ile iletişime geçin.", Toast.LENGTH_LONG).show();

                dialogInterface.dismiss();
                NavigationActivity.this.finish();
            }
        });
        builder.setNegativeButton("Hayır", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
                NavigationActivity.this.finish();
            }
        });
        builder.create().show();
    }

    @Override
    public void mapTouched(MotionEvent motionEvent) {

    }

    @Override
    public void navigationCalculationEnded() {

    }

    @Override
    public void navigationWillRecalculate() {

    }

    @Override
    public void navigationStopped() {

    }

    @Override
    public void navigationSwitchToOfflineMode() {

    }

    @Override
    public void navigationDirectionCommandReady(DirectionCommand directionCommand) {

    }

    @Override
    public void onProgressUpdate(FileControl.ControlProgress controlProgress) {
        Log.e("file","onProgressUpdate" + controlProgress.fileName + " = " + controlProgress.progressType.toString());

    }

    @Override
    public void onControlComplete() {
        Log.e("file","onControlComplete");
        fileControlCompleted = true;

    }

    @Override
    public void onControlFail() {
        Log.e("file","fail");
    }
}
