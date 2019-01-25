package com.laila.letmesleep;


import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import com.laila.letmesleep.R;


public class AlarmTable {

	private static SQLiteDatabase database;
	private static DBHelper dbHelper;
	private static String[] allColumns = 
		{DBHelper.COLUMN_ID, DBHelper.COLUMN_LABEL, DBHelper.COLUMN_ENABLED, DBHelper.COLUMN_VIBRATE,
			DBHelper.COLUMN_HOUR,DBHelper.COLUMN_MINUTES,DBHelper.COLUMN_DAYSOFWEEK,DBHelper.COLUMN_ALARMTIME,
			DBHelper.COLUMN_ALERT };

	public AlarmTable(Context context) {
		dbHelper = new DBHelper(context);
	}

	public void open() throws SQLException {
		database = dbHelper.getWritableDatabase();
	}

	public void close() {
		dbHelper.close();
	}

	public static void updateAlarm(int id, ContentValues values) {
		database.update(DBHelper.TABLE_ALARM, values, DBHelper.COLUMN_ID+"="+id, null);
	}

	public static long createAlarm(ContentValues initialValues) {
		ContentValues values;
		if (initialValues == null)
			values = new ContentValues();
		else
			values = initialValues;

		if (!values.containsKey(DBHelper.COLUMN_ALARMTIME))
			values.put(DBHelper.COLUMN_ALARMTIME, 0);
		if (!values.containsKey(DBHelper.COLUMN_ALERT))
			values.put(DBHelper.COLUMN_ALERT, "");
		if (!values.containsKey(DBHelper.COLUMN_DAYSOFWEEK))
			values.put(DBHelper.COLUMN_DAYSOFWEEK, 0);
		if (!values.containsKey(DBHelper.COLUMN_ENABLED))
			values.put(DBHelper.COLUMN_ENABLED, 0);
		if (!values.containsKey(DBHelper.COLUMN_HOUR))
			values.put(DBHelper.COLUMN_HOUR, 0);
		if (!values.containsKey(DBHelper.COLUMN_LABEL))
			values.put(DBHelper.COLUMN_LABEL, "");
		if (!values.containsKey(DBHelper.COLUMN_MINUTES))
			values.put(DBHelper.COLUMN_MINUTES, 0);
		if (!values.containsKey(DBHelper.COLUMN_VIBRATE))
			values.put(DBHelper.COLUMN_VIBRATE, 1);

		long insertId = database.insert(DBHelper.TABLE_ALARM, null, values);

		Cursor cursor = database.query(DBHelper.TABLE_ALARM, allColumns, DBHelper.COLUMN_ID + " = " + insertId, null,null, null, null);
		cursor.moveToFirst();
		return insertId;
	}

	public static void deleteAlarm(long alarmId) {
		database.delete(DBHelper.TABLE_ALARM, DBHelper.COLUMN_ID + " = " + alarmId, null);
	}

	public static Cursor getAlarms(Context context) {
		
		if (dbHelper == null) 
			dbHelper = new DBHelper(context);
		
		
		if (database == null) 
			database = dbHelper.getWritableDatabase();		
		
		return database.query(DBHelper.TABLE_ALARM, allColumns, null, null, null, null, null);		
	}

	public static Cursor getAlarm(int id) {
		Cursor cursor = database.query(DBHelper.TABLE_ALARM, allColumns, DBHelper.COLUMN_ID +" = "+ id, null, null, null, null);
		Log.v("AlarmTable", "found  "+cursor.getCount()+" alarms with id "+id);
		return cursor;
	}
}
