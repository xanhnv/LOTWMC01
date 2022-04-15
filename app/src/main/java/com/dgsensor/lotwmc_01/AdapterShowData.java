package com.dgsensor.lotwmc_01;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;

public class AdapterShowData extends BaseAdapter
{
    private Activity activity;
//    private String[] items;
    private List<dataRow> items;
    public AdapterShowData(Activity activity, List<dataRow> items) {
        this.activity = activity;
        this.items = items;
    }

    @Override
    public int getCount() {
        return items.size();
    }

    @Override
    public Object getItem(int position) {
        return items.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Gọi layoutInflater ra để bắt đầu ánh xạ view và data.
        LayoutInflater inflater = activity.getLayoutInflater();

        // Đổ dữ liệu vào biến View, view này chính là những gì nằm trong item_name.xml
        convertView = inflater.inflate(R.layout.tableviewrow, null);

        // Đặt chữ cho từng view trong danh sách.
        TextView tvID = convertView.findViewById(R.id.tvIndex);
        tvID.setText(items.get(position).id);
        TextView tvVol= convertView.findViewById(R.id.tvVol);
        tvVol.setText(items.get(position).vol);
        TextView tvTime= convertView.findViewById(R.id.tvTime);
        tvTime.setText(items.get(position).time);
        // Trả về view kết quả.
        return convertView;
    }
}
