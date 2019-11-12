package com.ecnu.zhuo.gearcheck;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.nfc.tech.MifareUltralight;
import android.nfc.tech.Ndef;
import android.nfc.tech.NfcA;
import android.nfc.tech.NfcF;
import android.nfc.tech.NfcV;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Locale;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.ViewPager;


public class LoadActivity extends BaseNfcActivity {
    String mTagText="null";
    private ViewPager viewPager;
    private static final String TAG = "MainActivity";
    /**写入内容的文本框*/
    private EditText mEditText;
    /**显示NFC卡中的内容*/
    private TextView mTextView;

    /**NFC相关*/
    /**NFC适配器*/
    private NfcAdapter mNfcAdapter;
    /**延时意图*/
    private PendingIntent mPendingIntent;
    /**检测的卡类型*/
    private static String[][] sTechArr = null;
    /**意图过滤器*/
    private static IntentFilter[] sFilters = null;
    static {
        try {
            sTechArr = new String[][]{
                    {IsoDep.class.getName()}, {NfcV.class.getName()}, {NfcF.class.getName()},
                    {MifareUltralight.class.getName()}, {NfcA.class.getName()}
            };
            // 如果这里不设置过滤器，就必须要在AndroidManifest.xml中为MainActivity设置过滤器
            // 这里的ACTION在NfcAdapter中有四个选中，个人认为是在设置响应优先级

            sFilters = new IntentFilter[]{
                    new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED, "*/*")
            };
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**蓝牙的变量**/
    private static final int ACCESS_LOCATION =11 ;
    /** Called when the activity is first created. */
    private Button autopairbtn=null;
    private BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getPermission();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_load);
        Log.d("tishi on create","in create");
        initView();
        initNfc();
        // 判断当前手机是否支持NFC
        if (null == mNfcAdapter) {
            // 如果为null,则为不支持。
            Toast.makeText(this, "当前设备不支持NFC功能!", Toast.LENGTH_SHORT).show();
            return;
        }
        //PendingIntent intent=PendingIntent.getActivity(this, 0, new Intent(this,getClass()), 0);
        Log.d("tishi on create","ready to in resolveIntent");
        resolveIntent(getIntent());
        // Toast.makeText(this, String.valueOf(intent), Toast.LENGTH_SHORT).show();
        Log.d("tishi on create",mTagText);
       if(mTagText!=null){
           String str = mTagText;
           if(mTagText.length()>=3){
               String str1=str.substring(0,3);
               Log.d("tishi str1",str1);
               if(str.contains("#")){
                   String btname=str.substring(3,str.indexOf("#"));
                   Log.d("tishi btname",btname);
                   String pin=str.substring(str.indexOf("#")+1,str.length());
                   Log.d("tishi pin",pin);
                   BluetoothReceiver mybluetoothreceiver =new BluetoothReceiver();//这里明天来加参数
                   if (!bluetoothAdapter.isEnabled())
                   {
                       bluetoothAdapter.enable();//异步的，不会等待结果，直接返回。
                       Toast.makeText(LoadActivity.this,"请打开蓝牙再试一次",Toast.LENGTH_SHORT).show();
                   }else{
                       String name = bluetoothAdapter.getName();
//获取本机蓝牙地址
                       @SuppressLint("HardwareIds") String address = bluetoothAdapter.getAddress();
                       Log.d("tishi","bluetooth name ="+name+" address ="+address);
                       IntentFilter filter = new IntentFilter();
//发现设备
                       filter.addAction(BluetoothDevice.ACTION_FOUND);
//设备连接状态改变
                       filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
//蓝牙设备状态改变
                       filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);

                       filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
                       registerReceiver(mybluetoothreceiver, filter);
                       if (bluetoothAdapter.isEnabled()) {
                           if (bluetoothAdapter.getScanMode() !=
                                   BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
                               Intent discoverableIntent = new Intent(
                                       BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                               discoverableIntent.putExtra(
                                       BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 3600);
                               startActivity(discoverableIntent);
                           }
                       }
                       bluetoothAdapter.startDiscovery();
                       Log.e("tishi","配对成功");
                   }

               }
               Log.d("tishi","跳转");
           }

       }

        mTextView.setText(mTagText+"??");
        // startActivity(intent);

    }

    /**
     * 初始化NFC
     */
    private void initNfc() {
        // 1. 初始化Nfc适配器
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
        // 2. 初始化延时意图
        mPendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
    }

    @Override
    public void onResume() {
        super.onResume();
        // 3. 允许后台唤醒--此处是指应用处于不可见的状态时，将卡片贴近手机时，唤醒当前App。
        // 如果sFilters为null,则为不过滤任何意图；如果sTechArr为null时，则为接收所有类型的卡片。
        mNfcAdapter.enableForegroundDispatch(this, mPendingIntent, sFilters, sTechArr);

    }

    @Override
    public void onPause() {
        super.onPause();
        // 4. 禁止后台唤醒
        mNfcAdapter.disableForegroundDispatch(this);
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        // 5. 重写OnIntent方法, 读取到的数据存储在intent中
        // 获取到意图中的Tag数据
        Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
//        TAG: Tech [android.nfc.tech.NfcV, android.nfc.tech.Ndef]
        Log.e(TAG, "onNewIntent mTagText: " + tag);
        // 此处判断输入框mEditText的内容是否为空，如果为空，则为读取数据；如果不为空，则为写入数据
        if (TextUtils.isEmpty(mEditText.getText().toString())) {
            // 数据为空，读取数据
            readData(tag);
        } else {
            // 数据不为空，写入数据
            writeData(tag);
        }
        Toast.makeText(this, String.valueOf(tag), Toast.LENGTH_SHORT).show();
    }

    /**
     * 读取数据
     * @param tag Tag
     */
    private void readData(Tag tag) {
        // 从Tag中获取Ndef信息
        Ndef ndef = Ndef.get(tag);
        try {
            // 连接
            ndef.connect();
//            readData: type: android.ndef.unknown, size: 250, message: NdefMessage [NdefRecord tnf=1 type=54 payload=027A68E4BDA0E5A5BD]
            Log.e(TAG, "readData: type: " + ndef.getType() + ", size: " + ndef.getMaxSize() + ", message: " + ndef.getNdefMessage().toString());
            // 获取NdefMessage消息
            NdefMessage ndefMessage = ndef.getNdefMessage();
            // 获取NdefRecord记录
            //ndefMessage.getRecords();
            // 遍历数组
            StringBuilder stringBuilder = new StringBuilder();
            // 文本
            String text;
            for (NdefRecord ndefRecord : ndefMessage.getRecords()) {
                text = parseNdefRecord(ndefRecord);
                if (!TextUtils.isEmpty(text)) {
                    stringBuilder.append(text);
                }
            }
            // 设置文本
            mTextView.setText(stringBuilder.toString());
        } catch (IOException | FormatException e) {
            e.printStackTrace();
        }
    }

    /**
     * 解析NdefRecord数据
     * @param ndefRecord NdefRecord记录
     */
    private String parseNdefRecord(NdefRecord ndefRecord) {
        // 判断是否为文本格式数据
        if (NdefRecord.TNF_WELL_KNOWN != ndefRecord.getTnf()) {
            return "";
        }
        if (!Arrays.equals(ndefRecord.getType(), NdefRecord.RTD_TEXT)) {
            return "";
        }

        try {
            // 获得字节流
            byte[] payload = ndefRecord.getPayload();
            // 获得编码格式
            String textEncoding = ((payload[0] & 0x80) == 0) ? "utf-8" : "utf-16";
            // 获得语言编码长度
            int languageCodeLength = payload[0] & 0x3f;
            // 语言编码
            String languageCode = new String(payload, 1, languageCodeLength, "US-ASCII");
            // 获取文本
            return new String(payload, languageCodeLength + 1, payload.length - languageCodeLength -1, textEncoding);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return "";
    }

    /**
     * 写数据
     * @param tag Tag
     */
    //这段可能要删除
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    private void writeData(Tag tag) {
        // 创建一个NdefMessage消息
        NdefMessage ndefMessage = new NdefMessage(createTextRecord(mEditText.getText().toString()));
        // 写入Tag
        if (writeTag(ndefMessage, tag)) {
            mEditText.setText("");
            Toast.makeText(this, "写入成功", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "写入失败", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 将NdefMessage消息写入Tag
     * @param ndefMessage Ndef消息
     * @param tag Tag
     */
    private boolean writeTag(NdefMessage ndefMessage, Tag tag) {
        try {
            // 获取Ndef
            Ndef ndef = Ndef.get(tag);
            // 连接
            ndef.connect();
            // 写入数据
            ndef.writeNdefMessage(ndefMessage);
            return true;
        } catch (IOException | FormatException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 创建文本信息
     * @param text 要写入的文本信息
     */
    private NdefRecord createTextRecord(String text) {
        // 设置语言
        byte[] langBytes = Locale.CHINA.getLanguage().getBytes(Charset.forName("US-ASCII"));
        // 设置编码
        Charset utfEncoding = Charset.forName("UTF-8");
        // 要写入的二进制数据
        byte[] textBytes = text.getBytes(utfEncoding);
        int utfBit = 0;
        // 将语言长度转化对应的字符
        char status = (char)(utfBit + langBytes.length);
        // 创建一个大小为 1(即语言长度转化对应的字符) + 语言字节长度 + 文本字节长度的字节数组
        byte[] data = new byte[1 + langBytes.length + textBytes.length];
        // 写入第一位数据
        data[0] = (byte) status;
        // 写入语言字节
        System.arraycopy(langBytes, 0, data, 1, langBytes.length);
        // 写入文本数据
        System.arraycopy(textBytes, 0, data, langBytes.length + 1, textBytes.length);
        // 创建一个NdefRecord记录
        return new NdefRecord(NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_TEXT, new byte[0], data);
    }

    /**
     * 初始化布局
     */
    private void initView() {
        mEditText = (EditText) this.findViewById(R.id.editText);
        mTextView = (TextView) this.findViewById(R.id.textView);
    }


    private void resolveIntent(final Intent intent){
        Log.i("touchez", "resolve intent");

        String action=intent.getAction();
        if(NfcAdapter.ACTION_TAG_DISCOVERED.equals(action)
                || NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)
                || NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)){

            //1.获取Tag对象
            Tag detectedTag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            //2.获取Ndef的实例
            Ndef ndef = Ndef.get(detectedTag);
            //mTagText = ndef.getType() + "\nmaxsize:" + ndef.getMaxSize() + "bytes\n\n";
            readNfcTag(intent);
            if(mTagText!=null)
                Log.d("mTagText",mTagText);
            else
            {
                mTagText="This is a null tag!!!";
                Log.d("mTagText","msg is null");
            }



        }

        final Bundle extras = intent.getExtras();

    }

    /**
     * 读取NFC标签文本数据
     */
    private void readNfcTag(Intent intent) {
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(intent.getAction())) {
            Parcelable[] rawMsgs = intent.getParcelableArrayExtra(
                    NfcAdapter.EXTRA_NDEF_MESSAGES);
            NdefMessage msgs[] = null;
            int contentSize = 0;
            if (rawMsgs != null) {
                msgs = new NdefMessage[rawMsgs.length];
                for (int i = 0; i < rawMsgs.length; i++) {
                    msgs[i] = (NdefMessage) rawMsgs[i];
                    contentSize += msgs[i].toByteArray().length;
                }
            }
            try {
                if (msgs != null) {
                    NdefRecord record = msgs[0].getRecords()[0];
                    String textRecord = parseTextRecord(record);
                    mTagText = textRecord ;
                }
            } catch (Exception e) {
            }
        }
    }
    /**
     * 解析NDEF文本数据，从第三个字节开始，后面的文本数据
     *
     * @param ndefRecord
     * @return
     */
    public static String parseTextRecord(NdefRecord ndefRecord) {
        /**
         * 判断数据是否为NDEF格式
         */
        //判断TNF
        if (ndefRecord.getTnf() != NdefRecord.TNF_WELL_KNOWN) {
            return null;
        }
        //判断可变的长度的类型
        if (!Arrays.equals(ndefRecord.getType(), NdefRecord.RTD_TEXT)) {
            return null;
        }
        try {
            //获得字节数组，然后进行分析
            byte[] payload = ndefRecord.getPayload();
            //下面开始NDEF文本数据第一个字节，状态字节
            //判断文本是基于UTF-8还是UTF-16的，取第一个字节"位与"上16进制的80，16进制的80也就是最高位是1，
            //其他位都是0，所以进行"位与"运算后就会保留最高位
            String textEncoding = ((payload[0] & 0x80) == 0) ? "UTF-8" : "UTF-16";
            //3f最高两位是0，第六位是1，所以进行"位与"运算后获得第六位
            int languageCodeLength = payload[0] & 0x3f;
            //下面开始NDEF文本数据第二个字节，语言编码
            //获得语言编码
            String languageCode = new String(payload, 1, languageCodeLength, "US-ASCII");
            //下面开始NDEF文本数据后面的字节，解析出文本
            String textRecord = new String(payload, languageCodeLength + 1,
                    payload.length - languageCodeLength - 1, textEncoding);
            return textRecord;
        } catch (Exception e) {
            throw new IllegalArgumentException();
        }
    }













    /**蓝牙的函数**/
    @SuppressLint("WrongConstant")
    private void getPermission() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
            int permissionCheck = 0;
            permissionCheck = this.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION);
            permissionCheck += this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION);
            if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                this.requestPermissions( // 请求授权
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION},
                        ACCESS_LOCATION);// 自定义常量,任意整型
            } else {
                // 已经获得权限
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case ACCESS_LOCATION:
                if (hasAllPermissionGranted(grantResults)) {
                    Log.d("perimission tishi", "onRequestPermissionsResult: OK");
                } else {
                    Log.d("perimission tishi", "onRequestPermissionsResult: NOT OK");
                }
                break;
        }
    }

    private boolean hasAllPermissionGranted(int[] grantResults) {
        for (int grantResult : grantResults) {
            if (grantResult == PackageManager.PERMISSION_DENIED) {
                return false;
            }
        }
        return true;
    }



}