package com.dipcore.radio;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

public class NotifyData {

    public static final int NOTIFY_ID = 0;
    public static final String NOTIFICATION_TITLE = "Radio App";
    public static final int NOTIFICATION_FLAGS = Notification.FLAG_ONGOING_EVENT | Notification.FLAG_NO_CLEAR;

    public int id;
    public int smallIcon;
    public String Title;
    public String Text;
    private Context context;
    private NotificationManager notificationManager;
    Notification.Builder builder;
    RadioSharedPreferences mRadioSharedPreferences;

    public NotifyData(Context context) {
        id = NOTIFY_ID;
        smallIcon = R.mipmap.ic_launcher;
        Title = NOTIFICATION_TITLE;
        Text = "";
        this.context = context;
        mRadioSharedPreferences = new RadioSharedPreferences(context);
    }

    public void setTitle(String title){
        this.Title = title;
        builder.setContentTitle(title);
        notificationManager.notify(this.id, builder.build());
    }

    public void setText(String text){
        this.Text = text;
        builder.setContentText(text);
        notificationManager.notify(this.id, builder.build());
    }

    public Notification show() {

        notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(this.id);
        builder = new Notification.Builder(context);
        builder.setContentTitle(this.Title);
        builder.setContentText(this.Text);
        builder.setAutoCancel(false);
        builder.setContentIntent(PendingIntent.getActivity(context, 0, new Intent(context, (Class) RadioActivity.class), PendingIntent.FLAG_UPDATE_CURRENT));

        if (mRadioSharedPreferences.isTopBarNotificationsEnabled()) {
            builder.setSmallIcon(this.smallIcon);
        }

        Notification notification = builder.build();
        notification.flags |= NOTIFICATION_FLAGS;

        notificationManager.notify(this.id, notification);
        return notification;
    }

    public void hide(){
        notificationManager.cancel(this.id);
    }

}