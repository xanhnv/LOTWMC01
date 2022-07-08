package com.dgsensor.lotwmc_01;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    TextView tvID, tvVol, tvName, txtTampering, txtReverse, txtLastTam, txtLastRev;
    ListView listDev;
    Button btnRead, btnOffLcd, btnOnLcd;
    private NfcAdapter mAdapter;
    private PendingIntent mPendingIntent;
    private IntentFilter[] mFilters;
    private String[][] mTechLists;
    private DataDevice ma = (DataDevice) getApplication();
    private long cpt = 0;
    String[] catBlocks = null;
    String[] catValueBlocks = null;

    byte[] GetSystemInfoAnswer = null;
    byte[] ReadMultipleBlockAnswer = null;
    byte[] block012 =null;
    byte[] block3536=null;
    byte[] blockVol=null;
    int nbblocks = 0;

    String sNbOfBlock = null;
    // o byte numberOfBlockToRead;
    byte[] numberOfBlockToRead = null;

    String startAddressString = null;
    byte[] addressStart = null;

    String ID = "";
    double H2O = 0;
    String From = "0000";
    String To = "0055";
    List<dataRow> listOfData = new ArrayList<dataRow>();
    AdapterShowData adapterShowData;
    private AlertDialog alertDialog;
    //action= "lcdOn", "lcdOff", "read"
    /**
     * action= "lcdOn", "lcdOff", "read"
     */
    String action="read";
    private byte[] WriteSingleBlockAnswer=null;

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
        FloatingActionButton fab = findViewById(R.id.fab3);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
        tvID = findViewById(R.id.tvID);
        tvVol = findViewById(R.id.tvVol);
        listDev= findViewById(R.id.listview1);
        btnRead= findViewById(R.id.btnRead);
        btnOffLcd= findViewById(R.id.btnOffLcd);
        btnOnLcd= findViewById(R.id.btnOnLcd);
        mAdapter = NfcAdapter.getDefaultAdapter(this);
        mPendingIntent = PendingIntent.getActivity(this, 0, new Intent(this,
                getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
        IntentFilter ndef = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
        mFilters = new IntentFilter[]{ndef,};
        mTechLists = new String[][]{new String[]{android.nfc.tech.NfcV.class
                .getName()}};
        listOfData.add(new dataRow("ID", "VOL", "TIME"));
        /*for (int i=0; i<100;i++){
            listOfData.add(new dataRow("123456", "0000.0000", "12/3/2020 21:36:33"));
        }*/

        adapterShowData= new AdapterShowData(this, listOfData);

        listDev.setAdapter(adapterShowData);
        alertDialog = new AlertDialog.Builder(MainActivity.this).create();
        alertDialog.setTitle("To Read data");
        alertDialog.setMessage("Place your phone close to the screen of WaterMeter!");
        alertDialog.setCanceledOnTouchOutside(false);
        btnRead.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                action="read";
                alertDialog.show();
            }
        });
        btnOnLcd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                action="lcdOn";
                alertDialog.setTitle("To Turn On");
                alertDialog.show();
            }
        });
        btnOffLcd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                action="lcdOff";
                alertDialog.setTitle("To Turn Off");
                alertDialog.show();
            }
        });
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
            ExportCSV();
            return  true;
        }

        return super.onOptionsItemSelected(item);
    }
    void  ExportCSV()  {
       try {
           if (listOfData.size()>1) {
               File root = new File(Environment.getExternalStorageDirectory() + "/EWM02/");
               if (!root.exists()) root.mkdirs();
               Calendar cal = Calendar.getInstance();
               SimpleDateFormat dateFormat = new SimpleDateFormat("dd_MM_yyyy HH_mm_ss");
               String sFileName = "EWM02_" + dateFormat.format( cal.getTime()) + ".csv";
               File gpxfile = new File(root, sFileName);
               String filePath = gpxfile.getAbsolutePath();
               FileWriter writer = new FileWriter(gpxfile);
               for (int i = 0; i < listOfData.size(); i++) {
                   writer.append(listOfData.get(i).getId());
                   writer.append(',');
                   writer.append(listOfData.get(i).getVol());
                   writer.append(',');
                   writer.append(listOfData.get(i).getTime());
                   writer.append('\n');
               }
               writer.flush();
               writer.close();
               Toast.makeText(getApplicationContext(), "File is exported", Toast.LENGTH_SHORT).show();
           }
           else {
               Toast.makeText(getApplicationContext(), "Error. The list is empty!", Toast.LENGTH_SHORT).show();
           }
       }
       catch (IOException io){

       }
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
        if (alertDialog!=null){
            if (alertDialog.isShowing()){
                alertDialog.dismiss();
            }
        }
    }

    public void checkNFC()
    {
        AlertDialog.Builder builder;
        if (mAdapter != null)
        {
            if (mAdapter.isEnabled() == false) {
                builder = new AlertDialog.Builder(this);
                builder.setMessage("Go to Settings ?")
                        .setCancelable(false)
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                //finish();
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                                    startActivity(new Intent(Settings.ACTION_NFC_SETTINGS));
                                }
                                else {
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
        }
        else {
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
    private boolean writeMessage()
    {
        if (WriteSingleBlockAnswer==null)
        {
            Toast.makeText(getApplicationContext(), "ERROR Write (No tag answer) ", Toast.LENGTH_SHORT).show();
            return false;
        }
        else if(WriteSingleBlockAnswer[0]==(byte)0x01)
        {
            Toast.makeText(getApplicationContext(), "ERROR Write. Please, place your phone near the tag.", Toast.LENGTH_SHORT).show();
            return false;
        }
        else if(WriteSingleBlockAnswer[0]==(byte)0xFF)
        {
            Toast.makeText(getApplicationContext(), "ERROR Write. Please, place your phone near the tag.", Toast.LENGTH_SHORT).show();
            return false;
        }
        else if(WriteSingleBlockAnswer[0]==(byte)0x00)
        {
            //Toast.makeText(getApplicationContext(), "Write Sucessfull ", Toast.LENGTH_SHORT).show();
            //finish();
            return true;
        }
        else
        {
            Toast.makeText(getApplicationContext(), "Write ERROR. Please, place your phone near the tag.", Toast.LENGTH_SHORT).show();
            return false;
        }
    }
    private class StartReadTask extends AsyncTask<Void, Void, Void> {
        private final ProgressDialog dialog = new ProgressDialog(MainActivity.this);
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
                numberOfBlockToRead = Helper
                        .ConvertIntTo2bytesHexaFormat(Integer
                                .parseInt(sNbOfBlock));
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
            /*block012 =null;
            block3536=null;
            blockVol=null;*/
            cpt = 0;

            if (DecodeGetSystemInfoResponse(GetSystemInfoAnswer)) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if(action=="read") {
                    if (ma.isMultipleReadSupported() == false
                            || Helper.Convert2bytesHexaFormatToInt(numberOfBlockToRead) <= 1) {
                        while ((ReadMultipleBlockAnswer == null || ReadMultipleBlockAnswer[0] == 1)
                                && cpt <= 10) {
                            ReadMultipleBlockAnswer = NFCCommand.Send_several_ReadSingleBlockCommands_NbBlocks(dataDevice.getCurrentTag(), addressStart, numberOfBlockToRead, dataDevice);
                            cpt++;
                        }
                        cpt = 0;
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
                    /*numberOfBlockToRead= new byte[2];
                    addressStart= new byte[2];
                    numberOfBlockToRead[0]=0;
                    numberOfBlockToRead[1]=3;

                    addressStart[0]=0;
                    addressStart[1]=0;
                    //block 0102
                    while ((block012 == null || block012[0] == 1) && cpt <= 10) {
                        block012 = NFCCommand.SendReadMultipleBlockCommandCustom2(dataDevice.getCurrentTag(), addressStart, numberOfBlockToRead, dataDevice);
                        cpt++;
                    }
                    //block 0x35,0x36 ID
                    addressStart[1]=53;
                    numberOfBlockToRead[1]=2;
                    cpt = 0;
                    while ((block3536 == null || block3536[0] == 1) && cpt <= 10) {
                        block3536 = NFCCommand.SendReadMultipleBlockCommandCustom2(dataDevice.getCurrentTag(), addressStart, numberOfBlockToRead, dataDevice);
                        cpt++;
                    }
                    //block VOL
                    cpt = 0;
                    // dem tu 0 den 22 ->
                    if (block012 != null && block012.length - 1 > 0) {
                        if (block012[0] == 0x00) {
                            catValueBlocks = Helper.buildArrayValueBlocks(block012, 3);
                            String currentHour = ConvertBlockToLong(catValueBlocks[2]);
                            long tempHour = Long.parseLong(currentHour) & 0x3ff;
                            long ampm = tempHour >> 9;
                            tempHour = Long.parseLong(currentHour) & 0x1ff;
                            long hourMode = tempHour >> 8;
                            int cHour = (int) ((tempHour & 255) >> 3);

                            int temp = 0;
                            if (hourMode == 1) { //24h
                                temp = cHour;
                            } else {
                                if (ampm == 1) { //pm
                                    if (cHour == 12) {
                                        temp = cHour;
                                    } else {
                                        temp = 12 + cHour;
                                    }
                                } else {
                                    if (cHour == 12) {
                                        temp = 0;
                                    } else {
                                        temp = cHour;
                                    }
                                }
                            }
                            addressStart[1]= (byte) (temp+16);
                            numberOfBlockToRead[1]=1;
                            cpt = 0;
                            while ((blockVol == null || blockVol[0] == 1) && cpt <= 10) {
                                blockVol = NFCCommand.SendReadMultipleBlockCommandCustom2(dataDevice.getCurrentTag(), addressStart, numberOfBlockToRead, dataDevice);
                                cpt++;
                            }
                        }
                    }*/
                    }
                }
                else if(action=="lcdOn") {
                    byte[]blockData= new byte[]{0x04,0x00,0x32,0x18};
                    startAddressString = Helper.castHexKeyboard("0F");
                    startAddressString = Helper.FormatStringAddressStart(startAddressString, dataDevice);
                    addressStart = Helper.ConvertStringToHexBytes(startAddressString);
                    WriteSingleBlockAnswer = NFCCommand.SendWriteSingleBlockCommand(dataDevice.getCurrentTag(), addressStart, blockData, ma);
                }else if(action=="lcdOff"){
                    byte[]blockData= new byte[]{0x03,0x00,0x32,0x18};
                    startAddressString = Helper.castHexKeyboard("0F");
                    startAddressString = Helper.FormatStringAddressStart(startAddressString, dataDevice);
                    addressStart = Helper.ConvertStringToHexBytes(startAddressString);
                    WriteSingleBlockAnswer = NFCCommand.SendWriteSingleBlockCommand(dataDevice.getCurrentTag(), addressStart, blockData, ma);
                }

            }/**/

            return null;
        }

        @Override
        protected void onPostExecute(final Void unused) {

            Log.i("ScanRead", "Button Read CLICKED **** On Post Execute ");
            if (this.dialog.isShowing())
                this.dialog.dismiss();
            if (DecodeGetSystemInfoResponse(GetSystemInfoAnswer)) {
                if (action=="read") {
                    nbblocks = Integer.parseInt(sNbOfBlock);
                    if (ReadMultipleBlockAnswer != null && ReadMultipleBlockAnswer.length - 1 > 0) {
                        if (ReadMultipleBlockAnswer[0] == 0x00) {
                            //catBlocks = Helper.buildArrayBlocks(addressStart, nbblocks);
                            catValueBlocks = Helper.buildArrayValueBlocks(ReadMultipleBlockAnswer, nbblocks);
                            //catValueBlocks= Helper.buildArrayValueBlocks(block3536,2);
                            //String[] block54 = catValueBlocks[54].split("  ");
                            //String yearID = String.valueOf(Integer.parseInt(block54[0].trim(), 16));

                            String[] block53 = catValueBlocks[53].split("  ");
                            String trueID = new DecimalFormat("00000000").format(Integer.parseInt(block53[2].trim() + block53[1].trim() + block53[0].trim(), 16));
                            //System.out.println("trueID: " + trueID + "_" + block53[2] + "." + block53[1] + "." + block53[0]);
                            ID = String.valueOf(new DecimalFormat("00").format(Integer.parseInt(block53[3].trim(), 16))) + trueID;
                            //System.out.println("trueID: " + trueID + "_" + block36[2] + "." + block36[1] + "." + block36[0]);

                        /*if (listOfData.size() > 0) {
                            for (int i = 0; i < listOfData.size(); i++) {
                                if (listOfData.get(i).getId().equalsIgnoreCase(ID)) {
                                    return;
                                }
                            }
                        }*/

                            // xac dinh so nuoc
                            String currentHour = ConvertBlockToLong(catValueBlocks[2]);
                            long tempHour = Long.parseLong(currentHour) & 0x3ff;
                            long ampm = tempHour >> 9;
                            tempHour = Long.parseLong(currentHour) & 0x1ff;
                            long hourMode = tempHour >> 8;
                            int cHour = (int) ((tempHour & 255) >> 3);

                            int temp = 0;
                            if (hourMode == 1) { //24h
                                temp = cHour;
                            } else {
                                if (ampm == 1) { //pm
                                    if (cHour == 12) {
                                        temp = cHour;
                                    } else {
                                        temp = 12 + cHour;
                                    }
                                } else {
                                    if (cHour == 12) {
                                        temp = 0;
                                    } else {
                                        temp = cHour;
                                    }
                                }
                            }
                            H2O = Double.valueOf(ConvertBlockToLong(catValueBlocks[temp + 16])) / 10000.0000;
                            tvID.setText(ID);
                            String vol = new DecimalFormat("0000.0000").format(H2O);
                            tvVol.setText(vol + " m3");
                            Log.d("TESTTTTTTTTt VOL", vol);
                            //Datetime.now
                            Date currentTime = Calendar.getInstance().getTime();
                            //add or update to listview
                            SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
                            for (int i = 0; i < listOfData.size(); i++) {
                                if (listOfData.get(i).getId().equalsIgnoreCase(ID)) {
                                    listOfData.get(i).setVol(vol);
                                    listOfData.get(i).setTime(dateFormat.format(currentTime));
                                    adapterShowData.notifyDataSetChanged();
                                    listDev.setSelection(i);
                                    return;
                                }
                            }
                            listOfData.add(new dataRow(ID, vol, dateFormat.format(currentTime)));
                            adapterShowData.notifyDataSetChanged();
                            listDev.setSelection(listOfData.size() - 1);
                        } else {
                            Toast.makeText(getApplicationContext(), "ERROR Read ",
                                    Toast.LENGTH_LONG).show();
                        }
                    } else {
                        Toast.makeText(getApplicationContext(), "ERROR Read (no Tag answer) ", Toast.LENGTH_LONG).show();
                    }
                }else if(action=="lcdOn"){
                    if (writeMessage()){
                        Toast.makeText(getApplicationContext(), "LCD ON ", Toast.LENGTH_SHORT).show();
                    }

                }else if(action=="lcdOff"){
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
            if (ma.isBasedOnTwoBytesAddress())
                ma.setBlockSize(Helper
                        .ConvertHexByteToString(GetSystemInfoResponse[14]));
            else
                ma.setBlockSize(Helper
                        .ConvertHexByteToString(GetSystemInfoResponse[13]));

            // *** IC REFERENCE ***
            if (ma.isBasedOnTwoBytesAddress())
                ma.setIcReference(Helper
                        .ConvertHexByteToString(GetSystemInfoResponse[15]));
            else
                ma.setIcReference(Helper
                        .ConvertHexByteToString(GetSystemInfoResponse[14]));

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
