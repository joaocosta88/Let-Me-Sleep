package com.laila.letmesleep;


import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.media.RingtoneManager;
import android.util.Log;

public class DBHelper extends SQLiteOpenHelper {

	private static final String DATABASE_NAME = "alarmtable.db";
	private static final int DATABASE_VERSION = 1;

	public static final String TABLE_ALARM = "alarm";

	public static final String COLUMN_ID = "_id";
	public static final String COLUMN_LABEL = "label";
	public static final String COLUMN_HOUR = "hour";
	public static final String COLUMN_MINUTES = "minutes";
	public static final String COLUMN_DAYSOFWEEK = "daysofweek";
	public static final String COLUMN_ALARMTIME = "alarmtime";
	public static final String COLUMN_ENABLED = "enabled";
	public static final String COLUMN_VIBRATE = "vibrate";
	public static final String COLUMN_ALERT = "alert";

	private static final String DATABASE_CREATE = "create table " 
			+ TABLE_ALARM
			+ "(" 
			+ COLUMN_ID + " integer primary key autoincrement, " 
			+ COLUMN_LABEL + " text, " 
			+ COLUMN_ENABLED + " integer, " 
			+ COLUMN_VIBRATE  + " integer, " 
			+ COLUMN_HOUR + " integer, "
			+ COLUMN_MINUTES + " integer, "
			+ COLUMN_DAYSOFWEEK + " integer, "
			+ COLUMN_ALARMTIME + " integer, "
			+ COLUMN_ALERT + " text "
			+ ");";

	public DBHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase database) {
		database.execSQL(DATABASE_CREATE);
		
		ContentValues values = new ContentValues(8);
				
		values.put(DBHelper.COLUMN_ENABLED, false);
		values.put(DBHelper.COLUMN_HOUR, 17);
		values.put(DBHelper.COLUMN_MINUTES, 00);
		values.put(DBHelper.COLUMN_VIBRATE, false);
		values.put(DBHelper.COLUMN_LABEL, "");
		values.put(DBHelper.COLUMN_DAYSOFWEEK, 0);
		values.put(DBHelper.COLUMN_ALARMTIME, 0);
		values.put(DBHelper.COLUMN_ALERT, RingtoneManager.getDefaultUri(
				RingtoneManager.TYPE_ALARM).toString());
		
		database.insert(DBHelper.TABLE_ALARM, null, values);
	}

	@Override
	public void onUpgrade(SQLiteDatabase database, int oldVersion,
			int newVersion) {
		Log.i(AlarmTable.class.getName(), "Upgrading database from version "
				+ oldVersion + " to " + newVersion
				+ ", which will destroy all old data");
		database.execSQL("DROP TABLE IF EXISTS " + TABLE_ALARM);
		onCreate(database);	
	}

	
	
}
