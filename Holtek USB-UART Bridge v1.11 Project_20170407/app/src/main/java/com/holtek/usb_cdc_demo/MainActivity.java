package com.holtek.usb_cdc_demo;

//参考网址:http://www.osedu.net/article/linux/2014-04-16/678.html

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.holtek.usb_cdc.CDCConstants;
import com.holtek.usb_cdc.CDCUSBDeviceManager;
import com.holtek.usb_cdc.DataReceiveThread;
import com.holtek.usb_cdc.SerialSettings;
import com.holtek.usb_cdc.USBTerminalException;
import com.holtek.util.SocketLogger;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    public static final int RESEND_DATA_HANDLENUM = 4;
    CDCUSBDeviceManager usbDeviceManager = null;
    static final String USB_REQUEST_PERMISSION_ACTION = "com.holtek.usb_cdc.USB_PERMISSION";
    static final int SETCOM_REQUEST = 1;
    private UsbManager usbManager = null;
    private DataReceiveThread dataReceiveThread = null;
    private UsbDevice cdcDevice;
    private SerialSettings serialSettings = new SerialSettings();
    private PendingIntent permissionIntent = null;
    private Timer reSendTimer = null;
    private TimerTask reSendTimerTask = null;
    private ArrayList historyList = new ArrayList<String>();
    //private String[] historyList;
    private int mHeight;
    private int middleHeight;
    private int maxHeight;

    Button sendButton;
    Button setComBtn;
    Button clearBtn;
    TextView dataTextView;
    EditText sendEditText;
    EditText timerEditText;
    CheckBox hexCheck;
    CheckBox reSendCheck;
    ListView historyListView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        dataTextView = (TextView)findViewById(R.id.dataTextView);
        //内容太多时，需要让其滚动
        dataTextView.setMovementMethod(ScrollingMovementMethod.getInstance());

        sendEditText = (EditText)findViewById(R.id.sendEditText);
        sendEditText.setFocusable(true);
        sendEditText.setFocusableInTouchMode(true);
        //取得控件高度
        //sendEditHeight = sendEditText.getHeight();
        ViewTreeObserver vto2 = sendEditText.getViewTreeObserver();
        vto2.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @SuppressWarnings("deprecation")
            @Override
            public void onGlobalLayout() {
                sendEditText.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                mHeight = sendEditText.getHeight();
                middleHeight = 8 * mHeight / 5;
                maxHeight = 21 * mHeight / 10;
            }
        });

        //添加监听，动态改变高度
        //sendEditText.addTextChangedListener(mTextWatcher);

        timerEditText = (EditText)findViewById(R.id.timeEditText);
        historyListView = (ListView)findViewById(R.id.historyListView);
       // historyListView.addHeaderView(dataTextView,"历史数据",false);
        //添加历史的list view
        historyListView.setAdapter(new ArrayAdapter<String>(this,
                    android.R.layout.simple_list_item_1, historyList));
        historyListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                //获取要发送的字符串
                if(position < historyList.size()) {
                    sendData((String)historyList.get(position), false);
                }
            }
        });


        //添加重发checkbox监听
        reSendCheck = (CheckBox)findViewById(R.id.reSendCheck);
        reSendCheck.setOnCheckedChangeListener(new ResendCheckedListener());

        //十六进制与字符切换
        hexCheck = (CheckBox)findViewById(R.id.hexCheckBox);
        hexCheck.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener(){
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked){
                //点击了HEX Checkbox
                if(isChecked){
                    //更改输入框提示文字，请输入十六进制数以空格隔开
                    sendEditText.setHint("Please enter hex numbers.");
                    Toast.makeText(MainActivity.this, "Please enter hex numbers separated by spaces！", Toast.LENGTH_LONG).show();
                    //获取焦点
                    sendEditText.requestFocus();
                }else{
                    //请输入字符串
                    sendEditText.setHint("Please enter a string.");
                    Toast.makeText(MainActivity.this, "Please enter a string.", Toast.LENGTH_LONG).show();
                    //获取焦点
                    sendEditText.requestFocus();
                }
            }
        });

        //添加发送按钮监听
        sendButton = (Button)findViewById(R.id.sendBtn);
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //获取文本
                String sendText = sendEditText.getText().toString();
                sendData(sendText,false);
                //添加到历史列表
                historyList.add(sendText);
                //刷新列表
                ArrayAdapter listAdapter = (ArrayAdapter)historyListView.getAdapter();
                listAdapter.notifyDataSetChanged();
            }
        });

        //添加设置com口参数按钮监听
        setComBtn = (Button)findViewById(R.id.setCom);
        setComBtn.setOnClickListener(nextActivityClickListener);

        clearBtn = (Button)findViewById(R.id.clearBtn);
        clearBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //清除显示的数据
                dataTextView.setText("Data Received/Transmitted:");
            }
        });

        usbManager = ((UsbManager)getSystemService(Context.USB_SERVICE));
        usbDeviceManager = new CDCUSBDeviceManager(this, handler, usbManager);

        //getBroadcast(Context context, int requestCode, Intent intent, int flags)
        //从系统取得一个用于向BroadcastReceiver的Intent广播的PendingIntent对象
        permissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(USB_REQUEST_PERMISSION_ACTION), PendingIntent.FLAG_UPDATE_CURRENT);
        //IntentFilter告诉系统，app可以接收的action，收到后再通过广播发出
        IntentFilter filter = new IntentFilter(USB_REQUEST_PERMISSION_ACTION);
        registerReceiver(mUsbReceiver, filter);

        //USB设备接入时，发出通知
        IntentFilter filter1 = new IntentFilter(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        registerReceiver(mUsbReceiver, filter1);

        //USB设备拔出时，发出通知
        IntentFilter filter2 = new IntentFilter(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(mUsbReceiver, filter2);

        //轮询设备
        EnumerateDevice();

        //加载保存的设置
        loadPreferences();
    }


    /**
     * edittext输入监听
     */
    TextWatcher mTextWatcher = new TextWatcher() {
        private CharSequence temp;

        // private int editStart;
        // private int editEnd;
        @Override
        public void onTextChanged(CharSequence s, int start, int before,
                                  int count) {
            // TODO Auto-generated method stub
            temp = s.toString().trim();
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count,
                                      int after) {
            // TODO Auto-generated method stub
        }

        @Override
        public void afterTextChanged(Editable s) {
            // TODO Auto-generated method stub
            // editStart = mMsg.getSelectionStart();
            // editEnd = mMsg.getSelectionEnd();
            int len = temp.length();//取得内容长度
            int lineCount = sendEditText.getLineCount();//取得内容的行数
            /**
             * 根据行数动态计算输入框的高度
             */
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) sendEditText.getLayoutParams();
            if (lineCount <= 1) {
                params.height = mHeight;
                sendEditText.setLayoutParams(params);
            } else if (lineCount >= 2) {
                params.height = mHeight*4;
                sendEditText.setLayoutParams(params);
            }
            //else {
            //    params.height = maxHeight;
            //    sendEditText.setLayoutParams(params);
            //}
        }
    };


    //发送数据
    //fTimer: 发送事件是否来自timer
    private boolean sendData(String sendText, boolean fTimer){
        //发送数据
        //默认波特率：38400
        //获取时间
        SimpleDateFormat dateFormat = new SimpleDateFormat("hh:mm:ss");  //MM-dd
        String date = dateFormat.format(new java.util.Date());

        if(sendText.equals("")){
            //输入为空
            if(!fTimer) {
                Toast.makeText(MainActivity.this, "Empty content！", Toast.LENGTH_SHORT).show();
            }
            return false;
        }
        if(!hexCheck.isChecked()){
            //以字符形式发送
            byte[] dataBuff = sendText.getBytes();
            usbDeviceManager.send(dataBuff);
            //显示到显示框
            dataTextView.append("\r\n"+date+"  [TX] "+sendText);
        }else {
            //以十六进制发送，根据空格分割字符串
            String[] sourceStrArray = sendText.split(" ");
            try {
                byte[] dataBuff = new byte[sourceStrArray.length];
                String sendStr = "";
                for (int i = 0; i < sourceStrArray.length; i++) {
                    //将字符串转成各个字符的ASCII码数组
                    dataBuff[i] = hexString2Bytes(sourceStrArray[i])[0];
                    sendStr += String.format("%X ",dataBuff[i]);
                }
                //显示到显示框
                dataTextView.append("\r\n"+date+"  [TX] "+sendStr);
                //线程等待
                //dataReceiveThread.wait();
                usbDeviceManager.send(dataBuff);
                //线程唤醒
                //dataReceiveThread.notify();
            } catch (Exception e) {
                if (!fTimer) {
                    Toast.makeText(MainActivity.this, "String to data error!", Toast.LENGTH_SHORT).show();
                }
                return false;
            }
        }
        return true;
    }

    //跳转到下一个Activity
    View.OnClickListener nextActivityClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            //读取串口参数
            if(!usbDeviceManager.GetFeatureReport()){
                Toast.makeText(MainActivity.this, "Get COM parameter fail!", Toast.LENGTH_SHORT).show();
                return ;
            }

            //跳到下一个Activity
            Intent setIntent = new Intent(MainActivity.this,SetComActivity.class);
            //startActivity(setIntent);
            byte[] portSetting = new byte[8];
            for(int i=1; i<usbDeviceManager.portSetting.length; i++){
                //去掉命令号
                portSetting[i-1] = usbDeviceManager.portSetting[i];
            }
            //获取
            setIntent.putExtra("SerialSets",portSetting);
            startActivityForResult(setIntent,SETCOM_REQUEST);
        }
    };

    //是否重发事件
    private class ResendCheckedListener implements CompoundButton.OnCheckedChangeListener{
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked){
            //点击了定时发送选择框
            if(isChecked){
                //选中了，则读取时间值
                if(timerEditText.getText().toString().length() <= 0){
                    return ;
                }
                int time = Integer.valueOf(timerEditText.getText().toString());
                if(time>0) {
                    if(reSendTimerTask == null){
                        reSendTimerTask = new TimerTask() {
                            @Override
                            public void run() {
                                //发送消息
                                Message message = handler.obtainMessage();
                                message.what = RESEND_DATA_HANDLENUM;
                                handler.sendMessage(message);
                            }
                        };
                    }
                    if(reSendTimer == null) {
                        reSendTimer = new Timer();
                        reSendTimer.schedule(reSendTimerTask, 0, time);
                    }
                }
            }else {

                //停止重发
                if(reSendTimerTask != null){
                    reSendTimerTask.cancel();
                    reSendTimerTask = null;
                }
                //调用cancel后不能再马上执行schedule，否则提示出错
                if(reSendTimer != null) {
                    reSendTimer.cancel();
                    reSendTimer = null;
                }

            }
        }
    }

    /*
     * 16进制字符串转字节数组
     */
    public static byte[] hexString2Bytes(String hex) {

        if ((hex == null) || (hex.equals(""))){
            return null;
        }
        else if (hex.length()%2 != 0){
            //如果字符为奇数个，在前面补0
            hex = "0"+hex;
        }
        hex = hex.toUpperCase();
        int len = hex.length()/2;
        byte[] b = new byte[len];
        char[] hc = hex.toCharArray();
        for (int i=0; i<len; i++){
            int p=2*i;
            b[i] = (byte) (charToByte(hc[p]) << 4 | charToByte(hc[p+1]));
        }
        return b;
    }
    /*
 * 字符转换为字节
 */
    private static byte charToByte(char c) {
        return (byte) "0123456789ABCDEF".indexOf(c);
    }



    //设置com口参数返回结果，重载保存在本地的参数
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == SETCOM_REQUEST){
            if(resultCode == RESULT_OK){
                byte[] serialSets = data.getByteArrayExtra("GetSerialSets");
                try {
                    usbDeviceManager.MakeLineControl(serialSets);
                    usbDeviceManager.SetFeatureReport();
                }catch (Exception e){

                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }


    public final Handler handler = new Handler() {
        public void handleMessage(Message message) {
            switch (message.what){
                case CDCConstants.CDC_READ_DATA_HANDLENUM:{
                    //读到数据，显示出来
                    byte[] data = (byte[])message.obj;
                    //Toast.makeText(MainActivity.this, "Received data length : " + data.length, Toast.LENGTH_SHORT).show();
                    try {
                        //获取时间
                        SimpleDateFormat dateFormat = new SimpleDateFormat("hh:mm:ss");  //MM-dd
                        String date = dateFormat.format(new java.util.Date());
                        dataTextView.append("\r\n"+date);
                        String getText = "";

                        //for(int i=1; i<data[1]+1; i++){
                        int len = data[0];
                        for(int i=1; i<len; i++){
                            if(hexCheck.isChecked()) {
                                getText += String.format("%X ", data[i]);
                            }else{
                                getText += String.format("%c", data[i]);
                            }
                        }
                        dataTextView.append("  [RX] "+getText);

                        // alvin test
                        //Log.d("USBDeviceManager",String.format("333:len:%d", data[0]));
                    }catch(Exception e){
                        Toast.makeText(MainActivity.this, "Data to string error!", Toast.LENGTH_SHORT).show();
                    };
                }break;
                //处理timer重发的消息
                case RESEND_DATA_HANDLENUM:{
                    if(!sendData(sendEditText.getText().toString(),true)){
                        //返回false，结束定时发送
                        reSendTimer.cancel();
                    }
                };
                default:{

                }break;
            }
        }
    };

    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver()
    {
        public void onReceive(Context context, Intent intent) {
            String str = intent.getAction();
            //点击了访问权限按钮
            if (USB_REQUEST_PERMISSION_ACTION.equals(str)) {
                synchronized (this) {
                    UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if (device != null) {
                            //在这里增加通信的代码
                            prepareDevice(device);
                        }
                    }else{
                        //拒绝访问USB
                    }
                }
            }
            //设备插上
            else if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(str)) {
                synchronized (this) {
                    UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if (device != null && device.getVendorId() == CDCConstants.HT_VENDOR_ID
                            && device.getProductId() == CDCConstants.HT_PRODUCT_ID) {
                        if(usbDeviceManager.findHIDInterface(device) != null) {
                            //在这里增加通信的代码
                            Toast.makeText(MainActivity.this, String.format("DEVICE CONNECTED: VID=0x%X PID=0x%X",
                                    device.getVendorId(),device.getProductId()), Toast.LENGTH_SHORT).show();
                            prepareDevice(device);
                        }
                    }
                }
            }
            //设备拔出
            else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(str)) {
                synchronized (this) {
                    UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if (device != null && device.getVendorId() == CDCConstants.HT_VENDOR_ID
                            && device.getProductId() == CDCConstants.HT_PRODUCT_ID) {
                        //在这里增加通信的代码
                        Toast.makeText(MainActivity.this,String.format("DEVICE DISCONNECTED: VID=0x%X PID=0x%X",
                                device.getVendorId(),device.getProductId()), Toast.LENGTH_SHORT).show();
                        if (MainActivity.this.dataReceiveThread != null)
                        {
                            SocketLogger.debug("Interrupt thread");
                            MainActivity.this.dataReceiveThread.interrupt();
                            MainActivity.this.dataReceiveThread = null;
                            MainActivity.this.usbDeviceManager.close();
                        }
                        cdcDevice = null;
                    }
                }
            }
        }
    };

    //枚举设备，查询CDC
    private UsbDevice EnumerateDevice() {
        Iterator localIterator = usbManager.getDeviceList().values().iterator();
        UsbDevice localUsbDevice;
        int index = 0;
        //Toast.makeText(MainActivity.this, usbManager.toString(), Toast.LENGTH_SHORT).show();
        Toast.makeText(MainActivity.this, String.format("Device count:%d",
                usbManager.getDeviceList().size()), Toast.LENGTH_LONG).show();

        while (localIterator.hasNext()) {
            //Toast.makeText(MainActivity.this, index++, Toast.LENGTH_SHORT).show();
            localUsbDevice = (UsbDevice) localIterator.next();
           // Toast.makeText(MainActivity.this, String.format("USB VID:0x%04x",localUsbDevice.getVendorId()), Toast.LENGTH_SHORT).show();
            if (usbDeviceManager.findHIDInterface(localUsbDevice) != null
                    && localUsbDevice.getVendorId() == CDCConstants.HT_VENDOR_ID) {
                // 判断你的应用程序是否有接入此USB设备的权限，如果有则返回真，否则返回false.
                // hasPermission(UsbDevice device)
                //显示询问对话框
                usbManager.requestPermission(localUsbDevice, permissionIntent);
              //  Toast.makeText(MainActivity.this, "FIND USB-CDC", Toast.LENGTH_SHORT).show();
                return localUsbDevice;
            }
        }
        //Toast.makeText(MainActivity.this, "NO FIND USB-CDC", Toast.LENGTH_LONG).show();
        return null;
    }

    private void loadPreferences()
    {
        SharedPreferences localSharedPreferences1 = getSharedPreferences(SerialSettings.SETTING_PREFERENCE_NAME, 0);
        serialSettings.loadPreferences(localSharedPreferences1);
     //   SharedPreferences localSharedPreferences2 = getSharedPreferences("macros", 0);
     //   this.macros.loadPreferences(localSharedPreferences2);
    }

    private void savePreference()
    {
        SharedPreferences localSharedPreferences = getSharedPreferences(SerialSettings.SETTING_PREFERENCE_NAME, 0);
        serialSettings.savePreference(localSharedPreferences);
    }

    private boolean prepareDevice(UsbDevice paramUsbDevice)
    {
        //查询是否有权限访问
        if(!usbManager.hasPermission(paramUsbDevice)){
            //显示权限询问对话框
            usbManager.requestPermission(paramUsbDevice, permissionIntent);
            return false;
        }

        SocketLogger.debug("prepareDevice: " + serialSettings);
        cdcDevice = paramUsbDevice;
        try {
            if (!usbDeviceManager.initDevice(this, usbManager, paramUsbDevice, this.serialSettings))
            {
                Toast.makeText(this, "OPEN DEVICE FAIL", Toast.LENGTH_SHORT).show();
                return false;
            }else{
                Toast.makeText(this, "OPEN DEVICE OK", Toast.LENGTH_SHORT).show();
            }
        }
        catch (USBTerminalException localUSBTerminalException)
        {
            localUSBTerminalException.printStackTrace();
            return false;
        }

        // Default settings, 19200, n, 8, 1
        byte[] serialSets = { 0, 75, 0, 0, 0, 0, 8, 0 };
        try {
            usbDeviceManager.MakeLineControl(serialSets);
            usbDeviceManager.SetFeatureReport();
        }catch (Exception e){

        }

        //读数据线程
        dataReceiveThread = new DataReceiveThread(this, usbDeviceManager, serialSettings.enableFileLogging);
        dataReceiveThread.start();
        return true;
    }
}
