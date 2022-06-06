package com.dgsensor.lotwmc_01;

import android.Manifest;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

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

import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity {

    RadioButton rdbRead, rdbWrite;
    TextView tvResetCount, tvTimestart, tvTimeStop, tvErrorCode, tvRssi,  tvRtc, tvDgsID;
    CheckBox ckbSetVol, ckbSetNbOn, ckbSendLive, ckbClearWarning, ckbSetID;
    EditText edtVol, edtWmid;
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
    String action = "read";
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

    public static byte[] stringToBytesASCII(String str) {
        str= str.trim();
        if (str.length()==0)
            return null;
        if (str.length()>12){
            str= str.substring(0,12);
        }
        byte[] b = str.getBytes();
        if (b.length<12){
            byte[] copiedArray = Arrays.copyOf(b, 12);
            return  copiedArray;
        }
        return b;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkPermission()) {
                //do your work
            } else {
                requestPermission();
            }
        }
        ckbSetID = findViewById(R.id.ckb_set_ID);
        edtVol = findViewById(R.id.edtVolume);
        rdbRead = findViewById(R.id.rdbRead);
        rdbWrite = findViewById(R.id.rdbWrite);
        mAdapter = NfcAdapter.getDefaultAdapter(this);
        mPendingIntent = PendingIntent.getActivity(this, 0, new Intent(this,
                getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
        IntentFilter ndef = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
        mFilters = new IntentFilter[]{ndef,};
        mTechLists = new String[][]{new String[]{android.nfc.tech.NfcV.class
                .getName()}};

        alertDialog = new AlertDialog.Builder(MainActivity.this).create();
        alertDialog.setTitle("To Read data");
        alertDialog.setMessage("Place your phone close to the screen of WaterMeter!");
        alertDialog.setCanceledOnTouchOutside(false);
        //alertDialog.show();

        rdbRead.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    action = "read";
                    /*alertDialog.setTitle("To Read data");
                    alertDialog.show();*/
                }
            }
        });
        rdbWrite.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    action = "write";
                    /*alertDialog.setTitle("To Turn On");
                    alertDialog.show();*/
                }
            }
        });
        tvErrorCode = findViewById(R.id.tvErrorCode);
        tvResetCount = findViewById(R.id.tvResetCount);
        tvRssi = findViewById(R.id.tvRssi);
        tvTimestart = findViewById(R.id.tvTimeStart);
        tvTimeStop = findViewById(R.id.tvTimeStop);
        edtWmid = findViewById(R.id.tvWmid);
        ckbSetVol = findViewById(R.id.ckb_set_volume);
        ckbSetNbOn = findViewById(R.id.ckb_nbon);
        ckbSendLive = findViewById(R.id.ckb_sendlive);
        ckbClearWarning = findViewById(R.id.ckb_clear_warning);
        tvRtc = findViewById(R.id.tvRtc);
        tvDgsID = findViewById(R.id.tvDgsID);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {

            return true;
        }

        return super.onOptionsItemSelected(item);
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

    protected boolean checkPermission() {
        int result = ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (result == PackageManager.PERMISSION_GRANTED) {
            return true;
        } else {
            return false;
        }
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

    private class StartReadTask extends AsyncTask<Void, Void, Void> {
        private final ProgressDialog dialog = new ProgressDialog(MainActivity.this);
        //block 06
        char[] byte1bit = new char[]{'0', '1', '0', '0', '1', '0', '0', '0'};
        char[] byte3bit = new char[]{'0', '0', '0', '0', '0', '0', '0', '0'};
        char[] byte2bit = new char[]{'0', '0', '0', '0', '0', '0', '0', '0'};
        int vol = 0;
        String WMID = "";

        @Override
        protected void onPreExecute() {
            DataDevice dataDevice = (DataDevice) getApplication();
            GetSystemInfoAnswer = NFCCommand.SendGetSystemInfoCommandCustom(
                    dataDevice.getCurrentTag(), dataDevice);
            if (DecodeGetSystemInfoResponse(GetSystemInfoAnswer)) {
                startAddressString = From;
                startAddressString = Helper.castHexKeyboard(startAddressString);
                startAddressString = Helper.FormatStringAddressStart(
                        startAddressString, dataDevice);
                addressStart = Helper
                        .ConvertStringToHexBytes(startAddressString);
                sNbOfBlock = To;
                sNbOfBlock = Helper.FormatStringNbBlockInteger(sNbOfBlock,
                        startAddressString, dataDevice);
                /*numberOfBlockToRead = Helper
                        .ConvertIntTo2bytesHexaFormat(Integer
                                .parseInt(sNbOfBlock));*/
                sNbOfBlock = "0003";//doc 3 byte
                numberOfBlockToRead = new byte[]{0, 3};
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
                //for set VOL
                if (ckbSetVol.isChecked())
                    byte3bit[6] = '1';
                else
                    byte3bit[6] = '0';
                //bit clear warning=
                if (ckbClearWarning.isChecked())
                    byte3bit[5] = '1';
                else
                    byte3bit[5] = '0';
                //bit set WMID

                if (ckbSetID.isChecked())
                    byte2bit[5] = '1';
                else
                    byte2bit[5] = '0';

                if (ckbSetVol.isChecked()) {
                    if (edtVol.length() > 0)
                        vol = Integer.parseInt(edtVol.getText().toString());
                }


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
            BlockVol = null;
            Block1234 = null;
            BlockID = null;
            /*block012 =null;
            block3536=null;
            blockVol=null;*/
            cpt = 0;

            if (DecodeGetSystemInfoResponse(GetSystemInfoAnswer)) {
                try {
                    Thread.sleep(2500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (action == "read") {
                    if (ma.isMultipleReadSupported() == false
                            || Helper.Convert2bytesHexaFormatToInt(numberOfBlockToRead) <= 1) {
                        while ((ReadMultipleBlockAnswer == null || ReadMultipleBlockAnswer[0] == 1)
                                && cpt <= 10) {
                            ReadMultipleBlockAnswer = NFCCommand.Send_several_ReadSingleBlockCommands_NbBlocks(dataDevice.getCurrentTag(), addressStart, numberOfBlockToRead, dataDevice);
                            cpt++;
                        }
                        cpt = 0;
                        addressStart[1] = 8;
                        numberOfBlockToRead[1] = 1;
                        while ((BlockVol == null || BlockVol[0] == 1)
                                && cpt <= 10) {
                            BlockVol = NFCCommand.Send_several_ReadSingleBlockCommands_NbBlocks(dataDevice.getCurrentTag(), addressStart, numberOfBlockToRead, dataDevice);
                            cpt++;
                        }
                        //Read WMID
                        cpt = 0;
                        addressStart[1] = 9;
                        numberOfBlockToRead[1] = 3;
                        while ((BlockID == null || BlockID[0] == 1)
                                && cpt <= 10) {
                            BlockID = NFCCommand.Send_several_ReadSingleBlockCommands_NbBlocks(dataDevice.getCurrentTag(), addressStart, numberOfBlockToRead, dataDevice);
                            cpt++;
                        }
                        //read block 1,2,3,4
                        cpt = 0;
                        addressStart[1] = 1;
                        numberOfBlockToRead[1] = 4;
                        while ((Block1234 == null || Block1234[0] == 1)
                                && cpt <= 10) {
                            Block1234 = NFCCommand.Send_several_ReadSingleBlockCommands_NbBlocks(dataDevice.getCurrentTag(), addressStart, numberOfBlockToRead, dataDevice);
                            cpt++;
                        }
                    } else if (Helper.Convert2bytesHexaFormatToInt(numberOfBlockToRead) < 32) {
                        while ((ReadMultipleBlockAnswer == null || ReadMultipleBlockAnswer[0] == 1)
                                && cpt <= 10) {
                            ReadMultipleBlockAnswer = NFCCommand.SendReadMultipleBlockCommandCustom(dataDevice.getCurrentTag(), addressStart, numberOfBlockToRead[1], dataDevice);
                            cpt++;
                        }
                        cpt = 0;
                    } else {

                        while ((ReadMultipleBlockAnswer == null || ReadMultipleBlockAnswer[0] == 1)
                                && cpt <= 10) {
                            ReadMultipleBlockAnswer = NFCCommand.SendReadMultipleBlockCommandCustom2(dataDevice.getCurrentTag(), addressStart, numberOfBlockToRead, dataDevice);
                            cpt++;
                        }
                        cpt = 0;

                    }
                } else if (action == "write") {
                    short byte01 = Short.parseShort(new String(byte1bit), 2);
                    byte byte03 = (byte) Short.parseShort(new String(byte3bit), 2);
                    byte byte02 = (byte) Short.parseShort(new String(byte2bit), 2);
                    byte[] block6Data = new byte[]{byte03, byte02, (byte) byte01, 0x00};
                    byte[] blockVol = longToByte(vol);
                    startAddressString = Helper.castHexKeyboard("06");
                    startAddressString = Helper.FormatStringAddressStart(startAddressString, dataDevice);
                    addressStart = Helper.ConvertStringToHexBytes(startAddressString);
                    WriteSingleBlockAnswer = NFCCommand.SendWriteSingleBlockCommand(dataDevice.getCurrentTag(), addressStart, block6Data, ma);
                    addressStart[0] = 0;
                    addressStart[1] = 8;
                    WriteSingleBlockAnswer = NFCCommand.SendWriteSingleBlockCommand(dataDevice.getCurrentTag(), addressStart, blockVol, ma);
                    if (ckbSetID.isChecked() ) {
                        byte[] blockWMID = stringToBytesASCII(edtWmid.getText().toString());
                        if (blockWMID!=null){
                            addressStart[0] = 0;
                            addressStart[1] = 9;
                            WriteSingleBlockAnswer = NFCCommand.SendWriteMultipleBlockCommand(dataDevice.getCurrentTag(), addressStart, blockWMID, ma);
                        }
                    }
                } else if (action == "lcdOff") {
                    byte[] blockData = new byte[]{0x03, 0x00, 0x32, 0x18};
                    startAddressString = Helper.castHexKeyboard("0F");
                    startAddressString = Helper.FormatStringAddressStart(startAddressString, dataDevice);
                    addressStart = Helper.ConvertStringToHexBytes(startAddressString);
                    WriteSingleBlockAnswer = NFCCommand.SendWriteSingleBlockCommand(dataDevice.getCurrentTag(), addressStart, blockData, ma);
                }

            }

            return null;
        }

        @Override
        protected void onPostExecute(final Void unused) {

            Log.i("ScanRead", "Button Read CLICKED **** On Post Execute ");
            if (this.dialog.isShowing())
                this.dialog.dismiss();
            if (DecodeGetSystemInfoResponse(GetSystemInfoAnswer)) {
                if (action == "read") {
                    nbblocks = Integer.parseInt(sNbOfBlock);
                    if (ReadMultipleBlockAnswer != null && ReadMultipleBlockAnswer.length - 1 > 0) {
                        if (ReadMultipleBlockAnswer[0] == 0x00) {
                            //catBlocks = Helper.buildArrayBlocks(addressStart, nbblocks);
                            catValueBlocks = Helper.buildArrayValueBlocks(ReadMultipleBlockAnswer, nbblocks);
                            tvResetCount.setText(ReadMultipleBlockAnswer[3] + "");
                            tvTimestart.setText(ReadMultipleBlockAnswer[5] + "- " + ReadMultipleBlockAnswer[6] + ":" +
                                    ReadMultipleBlockAnswer[7] + ":" + ReadMultipleBlockAnswer[8]);
                            tvErrorCode.setText(ReadMultipleBlockAnswer[9] + "");
                            tvRssi.setText(ReadMultipleBlockAnswer[10] + "");
                            tvTimeStop.setText(ReadMultipleBlockAnswer[11] + ":" + ReadMultipleBlockAnswer[12]);
                            //long length= ByteBuffer.wrap(BlockVol).order(ByteOrder.LITTLE_ENDIAN).getInt() & 0xFFFFFFFFL;
                            int volume = ByteBuffer.wrap(BlockVol, 1, 4).getInt();
                            long l = ((BlockVol[4] & 0xFFL) << 24) |
                                    ((BlockVol[3] & 0xFFL) << 16) |
                                    ((BlockVol[2] & 0xFFL) << 8) |
                                    ((BlockVol[1] & 0xFFL) << 0);
                            edtVol.setText(String.valueOf(l));
                            String s = new String(BlockID);
                            edtWmid.setText(s);
                            long rtc = ((Block1234[4] & 0xFFL) << 24) |
                                    ((Block1234[3] & 0xFFL) << 16) |
                                    ((Block1234[2] & 0xFFL) << 8) |
                                    ((Block1234[1] & 0xFFL) << 0);
                            int year = (int) (rtc & 0x7f);
                            int month = (int) ((rtc >> 7) & 0x0f);
                            int day = (int) ((rtc >> 11) & 0x1f);
                            rtc = ((Block1234[8] & 0xFFL) << 24) |
                                    ((Block1234[7] & 0xFFL) << 16) |
                                    ((Block1234[6] & 0xFFL) << 8) |
                                    ((Block1234[5] & 0xFFL) << 0);
                            int hour = (int) ((rtc >> 3) & 0x1f);
                            int minute = (int) ((rtc >> 8) & 0x3f);
                            int second = (int) ((rtc >> 14) & 0x3f);
                            tvRtc.setText(String.valueOf(day) + "/" + month + "/" + year + " " + hour + ":" +
                                    minute + ":" + second);
                            rtc = ((Block1234[12] & 0xFFL) << 24) |
                                    ((Block1234[11] & 0xFFL) << 16) |
                                    ((Block1234[10] & 0xFFL) << 8) |
                                    ((Block1234[9] & 0xFFL) << 0);
                            long dgsId = rtc & 0xffffff;
                            int dgsMonth = (int) ((rtc >> 24) & 0xff);

                            int dgsYear = Block1234[13];

                            tvDgsID.setText(String.format("%02d", dgsMonth) + String.format("%02d", dgsYear) + String.format("%05d", dgsId));

                        } else {
                            Toast.makeText(getApplicationContext(), "ERROR Read ",
                                    Toast.LENGTH_LONG).show();
                        }
                    } else {
                        Toast.makeText(getApplicationContext(), "ERROR Read (no Tag answer) ", Toast.LENGTH_LONG).show();
                    }
                } else if (action == "write") {
                    if (writeMessage()) {
                        Toast.makeText(getApplicationContext(), "Write successfully ", Toast.LENGTH_SHORT).show();
                    }

                } else if (action == "lcdOff") {
                    if (writeMessage())
                        Toast.makeText(getApplicationContext(), "LCD OFF", Toast.LENGTH_SHORT).show();
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

    private String ConvertBlockToLong(String block) {

        String[] b = block.split(" ");
        String[] cData = new String[4];
        byte count = 0;
        for (int i = 0; i < b.length; i++) {
            if (b[i].equalsIgnoreCase("") || b[i].equalsIgnoreCase(" ")) {
            } else {
                cData[count] = b[i];
                count++;
            }
        }

        long ID = 0;
        for (int i = cData.length - 1; i >= 0; i--) {
            String[] tmp = cData[i].split("");
            String[] tmp1 = new String[2];
            byte tmpCount = 0;
            for (int j = 0; j < tmp.length; j++) {
                if (tmp[j].equalsIgnoreCase("")) {
                } else {
                    tmp1[tmpCount] = tmp[j];
                    tmpCount++;
                }
            }

            for (int k = 0; k < tmp1.length; k++) {
                int sum = 0;
                if (tmp1[k].toUpperCase().equalsIgnoreCase("0")) {
                    sum = 0;
                } else if (tmp1[k].toUpperCase().equalsIgnoreCase("1")) {
                    sum = 1;
                } else if (tmp1[k].toUpperCase().equalsIgnoreCase("2")) {
                    sum = 2;
                } else if (tmp1[k].toUpperCase().equalsIgnoreCase("3")) {
                    sum = 3;
                } else if (tmp1[k].toUpperCase().equalsIgnoreCase("4")) {
                    sum = 4;
                } else if (tmp1[k].toUpperCase().equalsIgnoreCase("5")) {
                    sum = 5;
                } else if (tmp1[k].toUpperCase().equalsIgnoreCase("6")) {
                    sum = 6;
                } else if (tmp1[k].toUpperCase().equalsIgnoreCase("7")) {
                    sum = 7;
                } else if (tmp1[k].toUpperCase().equalsIgnoreCase("8")) {
                    sum = 8;
                } else if (tmp1[k].toUpperCase().equalsIgnoreCase("9")) {
                    sum = 9;
                } else if (tmp1[k].toUpperCase().equalsIgnoreCase("A")) {
                    sum = 10;
                } else if (tmp1[k].toUpperCase().equalsIgnoreCase("B")) {
                    sum = 11;
                } else if (tmp1[k].toUpperCase().equalsIgnoreCase("C")) {
                    sum = 12;
                } else if (tmp1[k].toUpperCase().equalsIgnoreCase("D")) {
                    sum = 13;
                } else if (tmp1[k].toUpperCase().equalsIgnoreCase("E")) {
                    sum = 14;
                } else if (tmp1[k].toUpperCase().equalsIgnoreCase("F")) {
                    sum = 15;
                } else {
                    sum = 0;
                }

                if (k == 0) {
                    ID += sum * Math.pow(16, (i + 1) * 2 - 1);
                } else {
                    ID += sum * Math.pow(16, (i + 1) * 2 - 2);
                }

            }
        }
        return String.valueOf(ID);
    }


}
