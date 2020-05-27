package com.demo.appmonitor.activity;

import android.annotation.SuppressLint;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.demo.appmonitor.R;
import com.demo.appmonitor.bean.AppInforBean;
import com.demo.appmonitor.utils.SystemInforUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InfoListActivity extends AppCompatActivity {
    private ListView list;
    private TextView txt_all_duration, txt_all_numbers;
    private RadioGroup rg_type;

    private int type;//日期条件

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_info_list);
        type = SystemInforUtils.DAY;
        initView();
    }

    @Override
    protected void onResume() {
        super.onResume();

        SystemInforUtils systemInfor = new SystemInforUtils(this, type);
        txt_all_duration.setText("总运行时长:" +  DateUtils.formatElapsedTime(systemInfor.getAllPlayTime() / 1000));
        txt_all_numbers.setText("本次开机操作总次数:" + systemInfor.getAllUsedNumber());

        List<Map<String, Object>> datalist = getDataList(systemInfor.getShowDataList());

        SimpleAdapter adapter = new SimpleAdapter(this, datalist, R.layout.item_info_list,
                new String[]{"icon", "name", "usedTime", "lastTime", "playNumber"},
                new int[]{R.id.img_icon, R.id.txt_name, R.id.txt_duration, R.id.txt_use_time, R.id.txt_numbers});
        list.setAdapter(adapter);

        adapter.setViewBinder(new SimpleAdapter.ViewBinder() {
            @Override
            public boolean setViewValue(View view, Object o, String s) {
                if (view instanceof ImageView && o instanceof Drawable) {
                    ImageView iv = (ImageView) view;
                    iv.setImageDrawable((Drawable) o);
                    return true;
                } else {
                    return false;
                }
            }
        });
    }

    private void initView() {
        list = findViewById(R.id.list);
        txt_all_duration = findViewById(R.id.txt_all_duration);
        txt_all_numbers = findViewById(R.id.txt_all_numbers);
        list = findViewById(R.id.list);
        rg_type = findViewById(R.id.rg_type);
        rg_type.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                switch (checkedId) {
                    case R.id.rb_day:
                        if (type != SystemInforUtils.DAY) {
                            type = SystemInforUtils.DAY;
                            onResume();
                        }
                        break;
                    case R.id.rb_week:
                        if (type != SystemInforUtils.WEEK) {
                            type = SystemInforUtils.WEEK;
                            onResume();
                        }
                        break;
                    case R.id.rb_month:
                        if (type != SystemInforUtils.MONTH) {
                            type = SystemInforUtils.MONTH;
                            onResume();
                        }
                        break;
                    case R.id.rb_year:
                        if (type != SystemInforUtils.YEAR) {
                            type = SystemInforUtils.YEAR;
                            onResume();
                        }
                        break;
                }
            }
        });
    }


    private List<Map<String, Object>> getDataList(ArrayList<AppInforBean> showDataList) {
        List<Map<String, Object>> dataList = new ArrayList<>();

        for (AppInforBean bean : showDataList) {
            Map<String, Object> map = new HashMap<>();
            map.put("icon", bean.getIcon());
            map.put("name", bean.getAppName());
            map.put("usedTime", "运行时长: " + DateUtils.formatElapsedTime(bean.getUsedTimes() / 1000));
            map.put("lastTime", "最近使用时间点: " + getTimeStrings(bean.getBeginPlayTime()));
            map.put("playNumber", "本次开机操作次数: " + bean.getUsedNumbers());
            dataList.add(map);
        }
        return dataList;
    }

    @SuppressLint("SimpleDateFormat")
    private String getTimeStrings(long times) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return simpleDateFormat.format(times);
    }
}
