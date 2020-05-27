package com.demo.appmonitor.bean;

import android.app.usage.UsageStats;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.SystemClock;
import android.util.Log;

/**
 * 应用信息实体类
 */
public class AppInforBean {
    private UsageStats usageStats;//应用信息存储包对象
    private String packageName = "";//包名称
    private Drawable icon;//应用图标
    private String appName = "";//应用名称
    private long beginPlayTime;//最近一次使用时间
    private long usedTimes = 0;//运行时长
    private int usedNumbers = 0;//本次开机操作次数

    private long timeStampMoveToForeground = -1;//开始时间戳
    private long timeStampMoveToBackGround = -1;//结束时间戳

    private Context context;

    public AppInforBean(UsageStats usageStats, Context context) {
        this.usageStats = usageStats;
        this.context = context;
        generateInfor();
    }

    /**
     * 设置应用相关信息
     *
     * @return
     */
    private void generateInfor() {
        try {
            PackageManager packageManager = context.getPackageManager();
            this.packageName = usageStats.getPackageName();
            if (this.packageName != null && !this.packageName.equals("")) {
                ApplicationInfo applicationInfo = packageManager.getApplicationInfo(this.packageName, 0);
                this.appName = (String) packageManager.getApplicationLabel(applicationInfo);
                this.usedTimes = usageStats.getTotalTimeInForeground();
                this.usedNumbers = (Integer) usageStats.getClass().getDeclaredField("mLaunchCount").get(usageStats);
                this.beginPlayTime = usageStats.getLastTimeUsed();
                if (this.usedTimes > 0) {
                    this.icon = applicationInfo.loadIcon(packageManager);
                }
            }
        } catch (PackageManager.NameNotFoundException | IllegalAccessException | NoSuchFieldException e) {
            e.printStackTrace();
        }
    }

    public void timesPlusPlus() {
        usedNumbers++;
    }

    /**
     * 计算运行时长
     */
    public void calculateRunningTime() {
        if (timeStampMoveToForeground < 0 || timeStampMoveToBackGround < 0) {
            return;
        }

        if (timeStampMoveToBackGround > timeStampMoveToForeground) {
            usedTimes += (timeStampMoveToBackGround - timeStampMoveToForeground);
            timeStampMoveToForeground = -1;
            timeStampMoveToBackGround = -1;
        }
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public Drawable getIcon() {
        return icon;
    }

    public void setIcon(Drawable icon) {
        this.icon = icon;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public long getBeginPlayTime() {
        return beginPlayTime;
    }

    public void setBeginPlayTime(long beginPlayTime) {
        this.beginPlayTime = beginPlayTime;
    }

    public long getUsedTimes() {
        return usedTimes;
    }

    public void setUsedTimes(long usedTimes) {
        this.usedTimes = usedTimes;
    }

    public int getUsedNumbers() {
        return usedNumbers;
    }

    public void setUsedNumbers(int usedNumbers) {
        this.usedNumbers = usedNumbers;
    }

    public long getTimeStampMoveToForeground() {
        return timeStampMoveToForeground;
    }

    public void setTimeStampMoveToForeground(long timeStampMoveToForeground) {
        this.timeStampMoveToForeground = timeStampMoveToForeground;
    }

    public long getTimeStampMoveToBackGround() {
        return timeStampMoveToBackGround;
    }

    public void setTimeStampMoveToBackGround(long timeStampMoveToBackGround) {
        this.timeStampMoveToBackGround = timeStampMoveToBackGround;
    }
}
