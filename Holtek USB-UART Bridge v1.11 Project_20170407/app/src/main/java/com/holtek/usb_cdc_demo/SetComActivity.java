package com.holtek.usb_cdc_demo;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

import java.util.ArrayList;
import java.util.List;

public class SetComActivity extends AppCompatActivity {

    byte[] serialSets;

    Button saveBtn ;
    Button cancelBtn;
    Spinner spinnerRate;
    Spinner spinnerStop;
    Spinner spinnerData;
    Spinner spinnerParity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setcom);

        //取得传入的物件
        Intent intent = getIntent();
        serialSets = intent.getByteArrayExtra("SerialSets");

        //波特率
        //2400/4800/9600/19200/38400/57600/115200/230400/460800/1700000/2300000/3400000
        String[] rateStr = {"9600","19200","38400","57600","115200"};
        spinnerRate = (Spinner)findViewById(R.id.spinnerRate);
        setSpinner(spinnerRate,rateStr);
        SetSpinnerDefault(spinnerRate,0xFF & serialSets[0] | (0xFF & serialSets[1]) << 8 | (0xFF & serialSets[2]) << 16 | (0xFF & serialSets[3]) << 24);
        SetSpinnerListener(spinnerRate);

        //停止位
        String[] stopStr = {"1","2"};
        spinnerStop = (Spinner)findViewById(R.id.spinnerStop);
        setSpinner(spinnerStop,stopStr);
        SetSpinnerDefault(spinnerStop,serialSets[4]);
        SetSpinnerListener(spinnerStop);

        //数据位
        String[] dataStr = {"8"};
        spinnerData = (Spinner)findViewById(R.id.spinnerData);
        setSpinner(spinnerData,dataStr);
        SetSpinnerDefault(spinnerData,serialSets[6]);
        SetSpinnerListener(spinnerData);

        //校验位
        //None / Odd / Even
        String[] prityStr = {"None","Odd","Even"};
        spinnerParity = (Spinner)findViewById(R.id.spinnerParity);
        setSpinner(spinnerParity,prityStr);
        SetSpinnerDefault(spinnerParity,serialSets[5]);
        SetSpinnerListener(spinnerParity);


        //保存
        saveBtn = (Button)findViewById(R.id.SaveBtn);
        saveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.putExtra("GetSerialSets",serialSets);
                setResult(RESULT_OK, intent);
                finish();
            }
        });

        //返回
        cancelBtn = (Button)findViewById(R.id.CancelBtn);
        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

    }

    //设置默认加载值
    private void SetSpinnerDefault(Spinner spinner, int defaultValue){

        int index = 0;
        if(spinner == spinnerRate){
            //波特率
            int[] rate = {9600,19200,38400,57600,115200};
            for(int i=0; i<rate.length; i++){
                if(defaultValue == rate[i]){
                    index = i;
                    break;
                }
            }
        }else if(spinner == spinnerData){
            //数据
            int[] data = {8};
            for(int i=0; i<data.length; i++){
                if(defaultValue == data[i]){
                    index = i;
                    break;
                }
            }
        }else if(spinner == spinnerStop){
            //停止位
            index = defaultValue;
        }else if(spinner == spinnerParity){
            //校验
            index = defaultValue;
        }
        if(index < 0 || index >= spinner.getCount()){
            return ;
        }
        spinner.setSelection(index);
    }

    //设置监听
    private void SetSpinnerListener(Spinner spinner){
        spinner.setOnItemSelectedListener(new Spinner.OnItemSelectedListener(){
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id){
                //String itemStr = (String)parent.getAdapter().getItem(position);
                if(parent == spinnerRate){
                    //波特率
                    int[] rate = {9600,19200,38400,57600,115200};
                    if(position>=0 && position<rate.length) {
                        serialSets[0] = ((byte) (rate[position] & 0xFF));
                        serialSets[1] = ((byte) (rate[position] >> 8));
                        serialSets[2] = ((byte) (rate[position] >> 16));
                        serialSets[3] = ((byte) (rate[position] >> 24));
                    }
                }else if(parent == spinnerData){
                    //数据
                    byte[] data = {8};
                    if(position>=0 && position<data.length) {
                        serialSets[6] = data[position];
                    }
                }else if(parent == spinnerStop){
                    //停止位
                    serialSets[4] = (byte)position;
                }else if(parent == spinnerParity){
                    //校验
                    serialSets[5] = (byte)position;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent){

            }
        });
    }


    //设置下拉控件
    private void setSpinner(Spinner spinner, String[] strList){
        ArrayList spinnerlist = new ArrayList();
        for(String itemStr : strList ){
            spinnerlist.add(itemStr);
        }
        //第二步：为下拉列表定义一个适配器，这里就用到里前面定义的list。
        CTArrayAdapter adapter = new CTArrayAdapter(this,R.layout.spinner_item, spinnerlist);
        //第三步：为适配器设置下拉列表下拉时的菜单样式。
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        //第四步：将适配器添加到下拉列表上
        spinner.setAdapter(adapter);
        //第五步：为下拉列表设置各种事件的响应，这个事响应菜单被选中
        spinner.setOnItemSelectedListener(new Spinner.OnItemSelectedListener(){
            public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                // TODO Auto-generated method stub

                /* 将mySpinner 显示*/
                arg0.setVisibility(View.VISIBLE);
            }
            public void onNothingSelected(AdapterView<?> arg0) {
                // TODO Auto-generated method stub
                arg0.setVisibility(View.VISIBLE);
            }
        });
    }

}
