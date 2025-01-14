package com.grobo.messscanner;

import android.Manifest;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.PointF;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.dlazaro66.qrcodereaderview.QRCodeReaderView;
import com.grobo.messscanner.database.AppDatabase;
import com.grobo.messscanner.database.UserDao;
import com.grobo.messscanner.database.UserModel;
import com.grobo.messscanner.database.Utils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class ScanActivity extends AppCompatActivity implements QRCodeReaderView.OnQRCodeReadListener {

    private QRCodeReaderView qrCodeReaderView;
    private UserDao userDao;
    private int currentMess;
    private String currentDate;
    private String mealNo;
    AlertDialog alertDialog;
    private Spinner spinner;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);

        qrCodeReaderView = findViewById(R.id.qr_reader_view);

        userDao = AppDatabase.getDatabase(this).userDao();

        currentMess = PreferenceManager.getDefaultSharedPreferences(this).getInt("mess", 1);

        checkPermission();

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (dialog != null) dialog.dismiss();
            }
        });
        alertDialogBuilder.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                qrCodeReaderView.startCamera();
            }
        });
        alertDialog = alertDialogBuilder.create();

//        spinner = findViewById(R.id.meal_spinner);
//        ArrayAdapter<CharSequence> spinnerAdapter = ArrayAdapter.createFromResource(this, R.array.spinner_items, android.R.layout.simple_spinner_item);
//        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
//        spinner.setAdapter(spinnerAdapter);
//        spinner.performClick();

    }

    private void checkPermission() {

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    10101);

        } else {
            initializeCamera();
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        checkPermission();
    }

    private void initializeCamera() {

        qrCodeReaderView.setOnQRCodeReadListener(this);

        // Use this function to enable/disable decoding
        qrCodeReaderView.setQRDecodingEnabled(true);

        // Use this function to change the auto-focus interval (default is 5 secs)
        qrCodeReaderView.setAutofocusInterval(1000L);

//        // Use this function to enable/disable Torch
//        qrCodeReaderView.setTorchEnabled(true);
//
//        // Use this function to set front camera preview
//        qrCodeReaderView.setFrontCamera();
//
//        // Use this function to set back camera preview
        qrCodeReaderView.setBackCamera();

    }

    @Override
    public void onQRCodeRead(String text, PointF[] points) {

        parseQRData(text);

        qrCodeReaderView.stopCamera();
    }

    private void parseQRData(String instituteId) {

        UserModel currentUser = null;
//        mealNo = spinner.getSelectedItem().toString();

        Utils.LoadUserByMongoId task = new Utils.LoadUserByMongoId(userDao);

        try {
            currentUser = task.execute(instituteId).get();
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (currentUser == null) {
            showDialog(instituteId, "User not found");
        } else if (currentUser.getMess() != currentMess) {
            showDialog(instituteId, "Mess not supported");
        } else {

            List<String> foodData = currentUser.getFoodData();

            String currentCancelledFoodData = currentDate + "_" + mealNo + "_-1";
            String currentTakenFoodData = currentDate + "_" + mealNo + "_1";

            if (foodData.contains(currentCancelledFoodData)) {
                showDialog(currentUser.getName(), currentDate + " Meal: " + mealNo + "\n\nFood cancelled");
            } else if (foodData.contains(currentTakenFoodData)) {
                showDialog(currentUser.getName(), currentDate + " Meal: " + mealNo + "\n\nFood already taken");
            } else {
                showDialog(currentUser.getName(), currentDate + " Meal: " + mealNo + "\n\nWelcome");

                Utils.InsertUser newTask = new Utils.InsertUser(userDao);
                currentUser.getFoodData().add(currentTakenFoodData);
                newTask.execute(currentUser);
            }

        }

    }

    private void showDialog(String title, String message) {

        if (alertDialog != null && !alertDialog.isShowing()) {
            alertDialog.setTitle(title);
            alertDialog.setMessage(message);
            alertDialog.show();
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        qrCodeReaderView.startCamera();

        DateFormat dateFormat = new SimpleDateFormat("dd_MM_yyyy", Locale.getDefault());
        Calendar calendar = Calendar.getInstance();
        currentDate = dateFormat.format(calendar.getTime());

        int hour = calendar.getTime().getHours();

        if (hour >= 7 && hour <= 10) {
            mealNo = "1";
        } else if (hour >= 12 && hour <= 15) {
            mealNo = "2";
        } else if (hour >= 16 && hour <= 18) {
            mealNo = "3";
        } else if (hour >= 19 && hour <= 23) {
            mealNo = "4";
        } else {
            mealNo = "other";
        }

    }

    @Override
    protected void onPause() {
        super.onPause();
        qrCodeReaderView.stopCamera();
    }
}
