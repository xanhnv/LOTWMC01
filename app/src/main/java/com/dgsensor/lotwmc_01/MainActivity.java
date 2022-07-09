package com.dgsensor.lotwmc_01;

import android.Manifest;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    CheckBox ckbSetNbOn, ckbSendLive, ckbClearWarning;
    private NfcAdapter mAdapter;
    private PendingIntent mPendingIntent;
    private IntentFilter[] mFilters;
    private String[][] mTechLists;
    private DataDevice ma = (DataDevice) getApplication();
    private long cpt = 0;
    String[] catValueBlocks = null;

    byte[] GetSystemInfoAnswer = null;
    byte[] ReadMultipleBlockAnswer = null;
    byte[] BlockVol = null;
    byte[] BlockID = null;
    byte[] Block1234 = null;

    int nbblocks = 0;
    String sNbOfBlock = null;
    byte[] numberOfBlockToRead = null;

    String startAddressString = null;
    byte[] addressStart = null;

    String From = "0025";
    String To = "0009";

    private AlertDialog alertDialog;
    private byte[] WriteSingleBlockAnswer = null;

    private static byte[] intToByte(int value) {
        return ByteBuffer.allocate(4).putInt(value).array();
    }

    public static byte[] longToByte(long value) {
        byte[] data = new byte[4];
        data[0] = (byte) value;
        data[1] = (byte) (value >>> 8);
        data[2] = (byte) (value >>> 16);
        data[3] = (byte) (value >>> 32);
        return data;
    }

    private boolean checkStoragePermission(boolean showNotification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                return true;
            } else {
                if (showNotification) showNotificationAlertToAllowPermission();
                return false;
            }
        } else {
            return true;
        }
    }

    private void showNotificationAlertToAllowPermission() {
        new AlertDialog.Builder(this).setMessage("Please allow Storage Read/Write permission for this app to function properly.").setPositiveButton("Open Settings", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                Uri uri = Uri.fromParts("package", getPackageName(), null);
                intent.setData(uri);
                startActivity(intent);
            }
        }).setNegativeButton("Cancel", null).show();

    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mAdapter = NfcAdapter.getDefaultAdapter(this);
        mPendingIntent = PendingIntent.getActivity(this, 0, new Intent(this,
                getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
        IntentFilter ndef = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
        mFilters = new IntentFilter[]{ndef,};
        mTechLists = new String[][]{new String[]{android.nfc.tech.NfcV.class
                .getName()}};

        alertDialog = new AlertDialog.Builder(MainActivity.this).create();
        alertDialog.setTitle("To Write data");
        alertDialog.setMessage("Place your phone close to the screen of WaterMeter!");
        alertDialog.setCanceledOnTouchOutside(false);
        //alertDialog.show();
        ckbSetNbOn = findViewById(R.id.ckb_nbon);
        ckbSendLive = findViewById(R.id.ckb_sendlive);
        ckbClearWarning = findViewById(R.id.ckb_clear_warning);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }


    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Tag tagFromIntent = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        DataDevice ma = (DataDevice) getApplication();
        ma.setCurrentTag(tagFromIntent);
        if (Helper.checkDataHexa(From) == true && Helper.checkDataHexa(To) == true) {
            new StartReadTask().execute();
        } else {
            Toast.makeText(getApplicationContext(),
                    "Invalid parameters, please modify",
                    Toast.LENGTH_LONG).show();
        }
        if (alertDialog != null) {
            if (alertDialog.isShowing()) {
                alertDialog.dismiss();
            }
        }
    }

    public void checkNFC() {
        AlertDialog.Builder builder;
        if (mAdapter != null) {
            if (mAdapter.isEnabled() == false) {
                builder = new AlertDialog.Builder(this);
                builder.setMessage("Go to Settings ?")
                        .setCancelable(false)
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                //finish();
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                                    startActivity(new Intent(Settings.ACTION_NFC_SETTINGS));
                                } else {
                                    startActivity(new Intent(Settings.ACTION_WIRELESS_SETTINGS));
                                }

                            }
                        })
                        .setNegativeButton("No", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                //  Action for 'NO' Button
                                finish();
                            }
                        });
                //Creating dialog box
                AlertDialog alert = builder.create();
                //Setting the title manually
                alert.setTitle("NFC not enabled");
                alert.show();
            }
        } else {
            AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
            alertDialog.setTitle("No NFC available");
            alertDialog.setMessage("App is going to be closed.");
            alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            finish();
                        }
                    });
            alertDialog.show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mPendingIntent = PendingIntent.getActivity(this, 0, new Intent(this,
                getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
        mAdapter.enableForegroundDispatch(this, mPendingIntent, mFilters,
                mTechLists);
        checkNFC();
        //Show dialog

    }


    protected void requestPermission() {

        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            Toast.makeText(this, "Write External Storage permission allows us to store files. Please allow this permission in App Settings.", Toast.LENGTH_LONG).show();
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 100);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 100:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //do your work
                } else {
                    Log.e("value", "Permission Denied, You cannot use local drive .");
                }
                break;
        }
    }

    private boolean writeMessage() {
        if (WriteSingleBlockAnswer == null) {
            Toast.makeText(getApplicationContext(), "ERROR Write (No tag answer) ", Toast.LENGTH_SHORT).show();
            return false;
        } else if (WriteSingleBlockAnswer[0] == (byte) 0x01) {
            Toast.makeText(getApplicationContext(), "ERROR Write. Please, place your phone near the tag.", Toast.LENGTH_SHORT).show();
            return false;
        } else if (WriteSingleBlockAnswer[0] == (byte) 0xFF) {
            Toast.makeText(getApplicationContext(), "ERROR Write. Please, place your phone near the tag.", Toast.LENGTH_SHORT).show();
            return false;
        } else if (WriteSingleBlockAnswer[0] == (byte) 0x00) {

            return true;
        } else {
            Toast.makeText(getApplicationContext(), "Write ERROR. Please, place your phone near the tag.", Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    //byte [] editbyte= new byte[12];
    private class StartReadTask extends AsyncTask<Void, Void, Void> {
        public byte[] EditextToByes(String text) throws UnsupportedEncodingException {
            //byte [] editbyte= text.getBytes();
           /* text= "Vai dan";
            Charset charset = Charset.forName("ASCII");*/
            char[] charArray = text.trim().toCharArray();
            if (charArray == null)
                return null;
            int iLen = charArray.length;
            byte[] byteArrray = new byte[iLen];
            for (int p = 0; p < iLen; p++)
                byteArrray[p] = (byte) (charArray[p]);
            //byte [] byteArrray = text.getBytes("ASCII");
            byte[] copiedArray = Arrays.copyOf(byteArrray, 12);
            return copiedArray;
        }

        private final ProgressDialog dialog = new ProgressDialog(MainActivity.this);
        //block 06
        char[] byte1bit = new char[]{'0', '0', '0', '0', '0', '0', '0', '0'};
        char[] byte3bit = new char[]{'0', '0', '0', '0', '0', '0', '0', '0'};
        char[] byte2bit = new char[]{'0', '0', '0', '0', '0', '0', '0', '0'};

        @Override
        protected void onPreExecute() {
            DataDevice dataDevice = (DataDevice) getApplication();
            GetSystemInfoAnswer = NFCCommand.SendGetSystemInfoCommandCustom(
                    dataDevice.getCurrentTag(), dataDevice);
            if (DecodeGetSystemInfoResponse(GetSystemInfoAnswer)) {
                //send live data
                if (ckbSendLive.isChecked())
                    byte1bit[2] = '1';
                else
                    byte1bit[2] = '0';
                //for NB On/off
                if (ckbSetNbOn.isChecked())
                    byte3bit[0] = '1';
                else
                    byte3bit[0] = '0';

                //bit clear warning=
                if (ckbClearWarning.isChecked())
                    byte3bit[5] = '1';
                else
                    byte3bit[5] = '0';
                this.dialog
                        .setMessage("Please, keep your phone close to the tag");
                this.dialog.show();
            } else {
                this.dialog.setMessage("Please, No tag detected");
                this.dialog.show();
            }

        }

        @Override
        protected Void doInBackground(Void... params) {
            DataDevice dataDevice = (DataDevice) getApplication();
            ma = (DataDevice) getApplication();
            ReadMultipleBlockAnswer = null;
            if (DecodeGetSystemInfoResponse(GetSystemInfoAnswer)) {
                try {
                    Thread.sleep(2500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                short byte01 = Short.parseShort(new String(byte1bit), 2);
                byte byte03 = (byte) Short.parseShort(new String(byte3bit), 2);
                byte byte02 = (byte) Short.parseShort(new String(byte2bit), 2);
                byte[] block6Data = new byte[]{byte03, byte02, (byte) byte01, 0x00};
                startAddressString = Helper.castHexKeyboard("06");
                startAddressString = Helper.FormatStringAddressStart(startAddressString, dataDevice);
                addressStart = Helper.ConvertStringToHexBytes(startAddressString);
                WriteSingleBlockAnswer = NFCCommand.SendWriteSingleBlockCommand(dataDevice.getCurrentTag(), addressStart, block6Data, ma);

            }

            return null;
        }

        @Override
        protected void onPostExecute(final Void unused) {

            Log.i("ScanRead", "Button Read CLICKED **** On Post Execute ");
            if (this.dialog.isShowing())
                this.dialog.dismiss();
            if (DecodeGetSystemInfoResponse(GetSystemInfoAnswer)) {
                if (writeMessage()) {
                    Toast.makeText(getApplicationContext(), "Write successfully ", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(getApplicationContext(), "ERROR Read (no Tag answer) ", Toast.LENGTH_LONG).show();
            }
        }

    }

    /**
     * *******************************************************************
     */

    // ***********************************************************************/
    // * the function Decode the tag answer for the GetSystemInfo command
    // * the function fills the values (dsfid / afi / memory size / icRef /..)
    // * in the myApplication class. return true if everything is ok.
    // ***********************************************************************/
    public boolean DecodeGetSystemInfoResponse(byte[] GetSystemInfoResponse) {
        if (GetSystemInfoResponse[0] == (byte) 0x00
                && GetSystemInfoResponse.length >= 12) {
            DataDevice ma = (DataDevice) getApplication();
            String uidToString = "";
            byte[] uid = new byte[8];

            for (int i = 1; i <= 8; i++) {
                uid[i - 1] = GetSystemInfoResponse[10 - i];
                uidToString += Helper.ConvertHexByteToString(uid[i - 1]);
            }

            // ***** TECHNO ******
            ma.setUid(uidToString);
            if (uid[0] == (byte) 0xE0)
                ma.setTechno("ISO 15693");
            else if (uid[0] == (byte) 0xD0)
                ma.setTechno("ISO 14443");
            else
                ma.setTechno("Unknown techno");

            // ***** MANUFACTURER ****
            if (uid[1] == (byte) 0x02)
                ma.setManufacturer("STMicroelectronics");
            else if (uid[1] == (byte) 0x04)
                ma.setManufacturer("NXP");
            else if (uid[1] == (byte) 0x07)
                ma.setManufacturer("Texas Instrument");
            else
                ma.setManufacturer("Unknown manufacturer");

            // **** PRODUCT NAME *****
            if (uid[2] >= (byte) 0x04 && uid[2] <= (byte) 0x07) {
                ma.setProductName("LRI512");
                ma.setMultipleReadSupported(false);
                ma.setMemoryExceed2048bytesSize(false);
            } else if (uid[2] >= (byte) 0x14 && uid[2] <= (byte) 0x17) {
                ma.setProductName("LRI64");
                ma.setMultipleReadSupported(false);
                ma.setMemoryExceed2048bytesSize(false);
            } else if (uid[2] >= (byte) 0x20 && uid[2] <= (byte) 0x23) {
                ma.setProductName("LRI2K");
                ma.setMultipleReadSupported(true);
                ma.setMemoryExceed2048bytesSize(false);
            } else if (uid[2] >= (byte) 0x28 && uid[2] <= (byte) 0x2B) {
                ma.setProductName("LRIS2K");
                ma.setMultipleReadSupported(false);
                ma.setMemoryExceed2048bytesSize(false);
            } else if (uid[2] >= (byte) 0x2C && uid[2] <= (byte) 0x2F) {
                ma.setProductName("M24LR64");
                ma.setMultipleReadSupported(true);
                ma.setMemoryExceed2048bytesSize(true);
            } else if (uid[2] >= (byte) 0x40 && uid[2] <= (byte) 0x43) {
                ma.setProductName("LRI1K");
                ma.setMultipleReadSupported(true);
                ma.setMemoryExceed2048bytesSize(false);
            } else if (uid[2] >= (byte) 0x44 && uid[2] <= (byte) 0x47) {
                ma.setProductName("LRIS64K");
                ma.setMultipleReadSupported(true);
                ma.setMemoryExceed2048bytesSize(true);
            } else if (uid[2] >= (byte) 0x48 && uid[2] <= (byte) 0x4B) {
                ma.setProductName("M24LR01E");
                ma.setMultipleReadSupported(true);
                ma.setMemoryExceed2048bytesSize(false);
            } else if (uid[2] >= (byte) 0x4C && uid[2] <= (byte) 0x4F) {
                ma.setProductName("M24LR16E");
                ma.setMultipleReadSupported(true);
                ma.setMemoryExceed2048bytesSize(true);
                if (ma.isBasedOnTwoBytesAddress() == false)
                    return false;
            } else if (uid[2] >= (byte) 0x50 && uid[2] <= (byte) 0x53) {
                ma.setProductName("M24LR02E");
                ma.setMultipleReadSupported(true);
                ma.setMemoryExceed2048bytesSize(false);
            } else if (uid[2] >= (byte) 0x54 && uid[2] <= (byte) 0x57) {
                ma.setProductName("M24LR32E");
                ma.setMultipleReadSupported(true);
                ma.setMemoryExceed2048bytesSize(true);
                if (ma.isBasedOnTwoBytesAddress() == false)
                    return false;
            } else if (uid[2] >= (byte) 0x58 && uid[2] <= (byte) 0x5B) {
                ma.setProductName("M24LR04E");
                ma.setMultipleReadSupported(true);
                ma.setMemoryExceed2048bytesSize(true);
            } else if (uid[2] >= (byte) 0x5C && uid[2] <= (byte) 0x5F) {
                ma.setProductName("M24LR64E");
                ma.setMultipleReadSupported(true);
                ma.setMemoryExceed2048bytesSize(true);
                if (ma.isBasedOnTwoBytesAddress() == false)
                    return false;
            } else if (uid[2] >= (byte) 0x60 && uid[2] <= (byte) 0x63) {
                ma.setProductName("M24LR08E");
                ma.setMultipleReadSupported(true);
                ma.setMemoryExceed2048bytesSize(true);
            } else if (uid[2] >= (byte) 0x64 && uid[2] <= (byte) 0x67) {
                ma.setProductName("M24LR128E");
                ma.setMultipleReadSupported(true);
                ma.setMemoryExceed2048bytesSize(true);
                if (ma.isBasedOnTwoBytesAddress() == false)
                    return false;
            } else if (uid[2] >= (byte) 0x6C && uid[2] <= (byte) 0x6F) {
                ma.setProductName("M24LR256E");
                ma.setMultipleReadSupported(true);
                ma.setMemoryExceed2048bytesSize(true);
                if (ma.isBasedOnTwoBytesAddress() == false)
                    return false;
            } else if (uid[2] >= (byte) 0xF8 && uid[2] <= (byte) 0xFB) {
                ma.setProductName("detected product");
                ma.setBasedOnTwoBytesAddress(true);
                ma.setMultipleReadSupported(true);
                ma.setMemoryExceed2048bytesSize(true);
            } else {
                ma.setProductName("Unknown product");
                ma.setBasedOnTwoBytesAddress(false);
                ma.setMultipleReadSupported(false);
                ma.setMemoryExceed2048bytesSize(false);
            }

            // *** DSFID ***
            ma.setDsfid(Helper
                    .ConvertHexByteToString(GetSystemInfoResponse[10]));

            // *** AFI ***
            ma.setAfi(Helper.ConvertHexByteToString(GetSystemInfoResponse[11]));

            // *** MEMORY SIZE ***
            if (ma.isBasedOnTwoBytesAddress()) {
                String temp = new String();
                temp += Helper
                        .ConvertHexByteToString(GetSystemInfoResponse[13]);
                temp += Helper
                        .ConvertHexByteToString(GetSystemInfoResponse[12]);
                ma.setMemorySize(temp);
                Log.i("MemorySize", temp + "----" + GetSystemInfoResponse[13]
                        + "----" + GetSystemInfoResponse[12]);
            } else
                ma.setMemorySize(Helper
                        .ConvertHexByteToString(GetSystemInfoResponse[12]));

            // *** BLOCK SIZE ***
            /*if (ma.isBasedOnTwoBytesAddress())
                ma.setBlockSize(Helper
                        .ConvertHexByteToString(GetSystemInfoResponse[14]));
            else
                ma.setBlockSize(Helper
                        .ConvertHexByteToString(GetSystemInfoResponse[12]));

            // *** IC REFERENCE ***
            if (ma.isBasedOnTwoBytesAddress())
                ma.setIcReference(Helper
                        .ConvertHexByteToString(GetSystemInfoResponse[15]));
            else
                ma.setIcReference(Helper
                        .ConvertHexByteToString(GetSystemInfoResponse[14]));*/

            return true;
        } else
            return false;
    }



}
