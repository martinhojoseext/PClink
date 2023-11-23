package com.holtek.usb_cdc_demo;

import android.content.Context;
import android.graphics.Color;
import android.support.annotation.LayoutRes;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by cheetah on 2016/8/13.
 */
public class CTArrayAdapter extends  ArrayAdapter {

    private @LayoutRes int mResource;

    public CTArrayAdapter(Context context, @LayoutRes int resource, ArrayList<String> objectList) {
        super(context, resource, objectList);
        mResource = resource;
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        LinearLayout listViewLayout;
        String itemText = (String)getItem(position);
        // 修改Spinner选择后结果的字体颜色
        if (convertView == null) {
            listViewLayout = new LinearLayout(getContext());
            LayoutInflater inflater = (LayoutInflater)getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            inflater.inflate(mResource, listViewLayout, true);
        }else{
            listViewLayout = (LinearLayout)convertView;
        }
        //此处text1是Spinner默认的用来显示文字的TextView
        TextView tv = (TextView) listViewLayout.findViewById(R.id.itemText);
        tv.setText(itemText);
        tv.setTextSize(22f);
        tv.setBackgroundColor(Color.WHITE);
        tv.setTextColor(Color.BLACK);
        return listViewLayout;

    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LinearLayout listViewLayout;
        String itemText = (String)getItem(position);
        // 修改Spinner选择后结果的字体颜色
        if (convertView == null) {
            listViewLayout = new LinearLayout(getContext());
            LayoutInflater inflater = (LayoutInflater)getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            inflater.inflate(mResource, listViewLayout, true);
        }else{
            listViewLayout = (LinearLayout)convertView;
        }
        //此处text1是Spinner默认的用来显示文字的TextView
        TextView tv = (TextView) listViewLayout.findViewById(R.id.itemText);
        tv.setText(itemText);
        tv.setTextSize(35f);
        tv.setBackgroundColor(Color.GRAY);
        tv.setTextColor(Color.BLACK);
        return listViewLayout;
    }

}
