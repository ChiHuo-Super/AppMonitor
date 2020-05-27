package com.demo.appmonitor.utils;

import android.app.usage.UsageEvents;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.SystemClock;
import android.text.TextUtils;

import com.demo.appmonitor.bean.AppInforBean;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

public class SystemInforUtils {

    public final static int DAY = 0;//天
    public final static int WEEK = 1;//周
    public final static int MONTH = 2;//月
    public final static int YEAR = 3;//年

    private Context context;
    private int type;//统计周期类型
    private ArrayList<AppInforBean> appInforList;
    private ArrayList<AppInforBean> showDataList;//最终显示数据列表

    private long allPlayTime;//运行时间
    private int allUsedNumber;//本次开机操作次数

    public SystemInforUtils(Context context, int type) {
        try {
            this.context = context;
            this.type = type;
            statUsageList();
            setShowList();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 将次数和时间为0的应用信息过滤掉
     */
    private void setShowList() {
        showDataList = new ArrayList<>();
        allPlayTime = 0;

        for (int i = 0; i < appInforList.size(); i++) {
            if (appInforList.get(i).getUsedTimes() > 0) {//&& !isSystemApp(context, AppInfoList.get(i).getPackageName())) {
                showDataList.add(appInforList.get(i));
                allPlayTime += appInforList.get(i).getUsedTimes();
                allUsedNumber += appInforList.get(i).getUsedNumbers();
            }
        }

        //将显示列表中的应用按显示顺序排序
        for (int i = 0; i < showDataList.size() - 1; i++) {
            for (int j = 0; j < showDataList.size() - i - 1; j++) {
                if (showDataList.get(j).getUsedTimes() < showDataList.get(j + 1).getUsedTimes()) {
                    AppInforBean bean = showDataList.get(j);
                    showDataList.set(j, showDataList.get(j + 1));
                    showDataList.set(j + 1, bean);
                }
            }
        }
    }

    /**
     * 统计指定周期内应用使用时间
     */
    private void statUsageList() {
        UsageStatsManager manager = (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);
        appInforList = new ArrayList<>();
        if (manager != null) {
            long nowTime = Calendar.getInstance().getTimeInMillis();
            long begintime = getBeginTime();
            List<UsageStats> result;
            if (type == DAY) {
                result = manager.queryUsageStats(UsageStatsManager.INTERVAL_BEST, begintime, nowTime);
                appInforList = getAccurateDailyStatsList(context, result, manager, begintime, nowTime);
            } else {
                if (type == WEEK)
                    result = manager.queryUsageStats(UsageStatsManager.INTERVAL_WEEKLY, begintime, nowTime);
                else if (type == MONTH)
                    result = manager.queryUsageStats(UsageStatsManager.INTERVAL_MONTHLY, begintime, nowTime);
                else if (type == YEAR)
                    result = manager.queryUsageStats(UsageStatsManager.INTERVAL_YEARLY, begintime, nowTime);
                else {
                    result = manager.queryUsageStats(UsageStatsManager.INTERVAL_BEST, begintime, nowTime);
                }

                List<UsageStats> Mergeresult = MergeList(result);
                for (UsageStats usageStats : Mergeresult) {
                    appInforList.add(new AppInforBean(usageStats, context));
                }
                calculateLaunchTimesAfterBootOn(context, appInforList);
            }
        }
    }

    /**
     * 根据UsageEvents 精确计算APP开机的启动(activity打开的)次数
     */
    private void calculateLaunchTimesAfterBootOn(Context context, List<AppInforBean> AppInfoList) {

        UsageStatsManager manager = (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);
        if (manager == null || AppInfoList == null || AppInfoList.size() < 1) {
            return;
        }
        //针对每个packageName建立一个  使用信息
        HashMap<String, AppInforBean> mapData = new HashMap<>();

        UsageEvents events = manager.queryEvents(bootTime(), System.currentTimeMillis());
        for (AppInforBean appInforBean : AppInfoList) {
            mapData.put(appInforBean.getPackageName(), appInforBean);
            appInforBean.setUsedNumbers(0);
        }

        UsageEvents.Event e = new UsageEvents.Event();
        while (events.hasNextEvent()) {
            events.getNextEvent(e);
            String packageName = e.getPackageName();
            AppInforBean appInforBean = mapData.get(packageName);
            if (appInforBean == null) {
                continue;
            }

            if (e.getEventType() == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                appInforBean.timesPlusPlus();
            }
        }
    }

    /**
     * 进行数据列表合并
     *
     * @param result
     * @return
     */
    private List<UsageStats> MergeList(List<UsageStats> result) {
        List<UsageStats> Mergeresult = new ArrayList<>();
        long begintime = getBeginTime();
        for (int i = 0; i < result.size(); i++) {
            if (result.get(i).getLastTimeUsed() > begintime) {
                int number = FoundUsageStats(Mergeresult, result.get(i));
                if (number >= 0) {
                    UsageStats stats = Mergeresult.get(number);
                    stats.add(result.get(i));
                    Mergeresult.set(number, stats);
                } else Mergeresult.add(result.get(i));
            }
        }
        return Mergeresult;
    }

    /**
     * 统计使用情况
     *
     * @param Mergeresult
     * @param usageStats
     * @return
     */
    private int FoundUsageStats(List<UsageStats> Mergeresult, UsageStats usageStats) {
        for (int i = 0; i < Mergeresult.size(); i++) {
            if (Mergeresult.get(i).getPackageName().equals(usageStats.getPackageName())) {
                return i;
            }
        }
        return -1;
    }

    /**
     * 根据UsageEvents来对当天的操作次数和开机后运行时间来进行精确计算
     *
     * @param context
     * @param result
     * @param manager
     * @param begintime
     * @param nowTime
     * @return
     */
    private ArrayList<AppInforBean> getAccurateDailyStatsList(Context context, List<UsageStats> result,
                                                              UsageStatsManager manager, long begintime, long nowTime) {
        //针对每个packageName建立一个  使用信息
        HashMap<String, AppInforBean> mapData = new HashMap<>();
        //得到包名
        for (UsageStats stats : result) {
            if (stats.getLastTimeUsed() > begintime && stats.getTotalTimeInForeground() > 0) {
                if (mapData.get(stats.getPackageName()) == null) {
                    AppInforBean information = new AppInforBean(stats, context);
                    //重置总运行时间  开机操作次数
                    information.setUsedNumbers(0);
                    information.setUsedTimes(0);
                    mapData.put(stats.getPackageName(), information);
                }
            }
        }

        //这个是相对比较精确的
        long bootTime = bootTime();
        UsageEvents events = manager.queryEvents(bootTime, nowTime);

        UsageEvents.Event e = new UsageEvents.Event();
        while (events.hasNextEvent()) {
            events.getNextEvent(e);
            String packageName = e.getPackageName();

            AppInforBean appInforBean = mapData.get(packageName);
            if (appInforBean == null) {
                continue;
            }

            //这里在同时计算开机后的操作次数和运行时间，所以如果获取到的时间戳是昨天的话就得过滤掉 continue

            if (e.getEventType() == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                appInforBean.timesPlusPlus();
                if (e.getTimeStamp() < begintime) {
                    continue;
                }
                appInforBean.setTimeStampMoveToForeground(e.getTimeStamp());
            } else if (e.getEventType() == UsageEvents.Event.MOVE_TO_BACKGROUND) {
                if (e.getTimeStamp() < begintime) {
                    continue;
                }
                appInforBean.setTimeStampMoveToBackGround(e.getTimeStamp());
                //当前应用是在昨天进入的前台，0点后转入了后台，所以会先得到MOVE_TO_BACKGROUND 的timeStamp
                if (appInforBean.getTimeStampMoveToForeground() < 0) {
                    //从今天开始计算即可
                    appInforBean.setTimeStampMoveToForeground(begintime);
                }
            }
            appInforBean.calculateRunningTime();
        }

        //再计算一次当前应用的运行时间，因为当前应用，最后得不到MOVE_TO_BACKGROUND 的timeStamp
        AppInforBean appInforBean = mapData.get(context.getPackageName());
        appInforBean.setTimeStampMoveToBackGround(nowTime);
        appInforBean.calculateRunningTime();

        return new ArrayList<>(mapData.values());
    }

    /**
     * 返回开机时间，单位微妙
     *
     * @return
     */
    private long bootTime() {
        return System.currentTimeMillis() - SystemClock.elapsedRealtime();
    }

    /**
     * 获取在指定范围内的启动时间点
     *
     * @return
     */
    private long getBeginTime() {
        Calendar calendar = Calendar.getInstance();
        long begintime;
        if (type == WEEK) {
            calendar.add(Calendar.DATE, -7);
            begintime = calendar.getTimeInMillis();
        } else if (type == MONTH) {
            calendar.add(Calendar.DATE, -30);
            begintime = calendar.getTimeInMillis();
        } else if (type == YEAR) {
            calendar.add(Calendar.YEAR, -1);
            begintime = calendar.getTimeInMillis();
        } else {
            //剩下的输入均显示当天的数据
            int hour = calendar.get(Calendar.HOUR_OF_DAY);
            int minute = calendar.get(Calendar.MINUTE);
            int second = calendar.get(Calendar.SECOND);

            calendar.add(Calendar.SECOND, -1 * second);
            calendar.add(Calendar.MINUTE, -1 * minute);
            calendar.add(Calendar.HOUR, -1 * hour);
            begintime = calendar.getTimeInMillis();
        }
        return begintime;
    }

    /**
     * 判断app是否为系统qpp
     *
     * @param packageName
     * @return
     */
    public boolean isSystemApp(String packageName) {
        try {
            if (TextUtils.isEmpty(packageName)) {
                return false;
            }
            PackageManager pManager = context.getPackageManager();
            ApplicationInfo aInfo = pManager.getApplicationInfo(packageName, 0);
            return aInfo != null && (aInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return false;
        }
    }

    public long getAllPlayTime() {
        return allPlayTime;
    }

    public int getAllUsedNumber() {
        return allUsedNumber;
    }

    public ArrayList<AppInforBean> getShowDataList() {
        return showDataList;
    }
}
