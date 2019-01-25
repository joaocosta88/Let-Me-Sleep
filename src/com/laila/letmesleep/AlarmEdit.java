package com.laila.letmesleep;



import com.actionbarsherlock.app.SherlockPreferenceActivity;
import com.laila.letmesleep.R;
import com.laila.letmesleep.Alarm.DaysOfWeek;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TimePicker;
import android.widget.Toast;
import android.app.TimePickerDialog;

public class AlarmEdit extends SherlockPreferenceActivity implements TimePickerDialog.OnTimeSetListener {

	private EditTextPreference mLabel;
	private Preference mTimePref;
	private AlarmPreference mAlarmPref;
	private CheckBoxPreference mVibratePref;
	private RepeatPreference mRepeatPref;

	public int id;
	private boolean mEnabled;
	private int     mHour;
	private int     mMinutes;
	private AlarmTable table;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.alarm_preferences);

		// Get each preference so we can retrieve the value later.
		mLabel = (EditTextPreference) findPreference("label");
		mLabel.setOnPreferenceChangeListener(
				new Preference.OnPreferenceChangeListener() {
					public boolean onPreferenceChange(Preference p,
							Object newValue) {
						// Set the summary based on the new label.
						p.setSummary((String) newValue);
						return true;
					}
				});
		mTimePref = findPreference("time");
		mAlarmPref = (AlarmPreference) findPreference("alarm");
		mVibratePref = (CheckBoxPreference) findPreference("vibrate");
		mRepeatPref = (RepeatPreference) findPreference("setRepeat");


		id = getIntent().getIntExtra("id", -1);
		Log.i("Alarm edit", "searching for alarm with id "+id);
		table = new AlarmTable(getApplicationContext());
		table.open();
		Cursor c = AlarmTable.getAlarm(id);
		c.moveToFirst();
		Alarm alarm = new Alarm(c);

		mEnabled = alarm.enabled;
		mLabel.setText(alarm.label);
		mLabel.setSummary(alarm.label);
		mHour = alarm.hour;
		mMinutes = alarm.minutes;
		if (alarm.daysOfWeek != null)
			mRepeatPref.setDaysOfWeek(alarm.daysOfWeek);

		mVibratePref.setChecked(alarm.vibrate);
		// Give the alert uri to the preference.
		mAlarmPref.setAlert(alarm.alert);
		updateTime();


		if (id > -1) {
			setContentView(R.layout.save_cancel_delete_alarm);
			Button b = (Button) findViewById(R.id.alarm_delete);
			b.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					AlarmTable.deleteAlarm(id);
					finish();
				}
			});
		}
		else 
			setContentView(R.layout.save_cancel_alarm);


		// Attach actions to each button.
		Button b = (Button) findViewById(R.id.alarm_save);
		b.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				saveAlarm();
				finish();
			}
		});

		b = (Button) findViewById(R.id.alarm_cancel);
		b.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				finish();
			}
		});

	}
	public void updateTime() {
		mTimePref.setSummary(Alarms.formatTime(this, mHour, mMinutes,
				mRepeatPref.getDaysOfWeek()));
	}

	/**
	 * Display a toast that tells the user how long until the alarm
	 * goes off.  This helps prevent "am/pm" mistakes.
	 */
	static void popAlarmSetToast(Context context, int hour, int minute,
			Alarm.DaysOfWeek daysOfWeek) {

		String toastText = formatToast(context, hour, minute, daysOfWeek);
		Toast toast = Toast.makeText(context, toastText, Toast.LENGTH_LONG);
		toast.show();
	}

	/**
	 * format "Alarm set for 2 days 7 hours and 53 minutes from
	 * now"
	 */
	static String formatToast(Context context, int hour, int minute,
			Alarm.DaysOfWeek daysOfWeek) {
		long alarm = Alarms.calculateAlarm(hour, minute,
				daysOfWeek).getTimeInMillis();
		long delta = alarm - System.currentTimeMillis();;
		long hours = delta / (1000 * 60 * 60);
		long minutes = delta / (1000 * 60) % 60;
		long days = hours / 24;
		hours = hours % 24;

		String daySeq = (days == 0) ? "" :
			(days == 1) ? context.getString(R.string.day) :
				context.getString(R.string.days, Long.toString(days));

			String minSeq = (minutes == 0) ? "" :
				(minutes == 1) ? context.getString(R.string.minute) :
					context.getString(R.string.minutes, Long.toString(minutes));

				String hourSeq = (hours == 0) ? "" :
					(hours == 1) ? context.getString(R.string.hour) :
						context.getString(R.string.hours, Long.toString(hours));

					boolean dispDays = days > 0;
					boolean dispHour = hours > 0;
					boolean dispMinute = minutes > 0;

					int index = (dispDays ? 1 : 0) |
							(dispHour ? 2 : 0) |
							(dispMinute ? 4 : 0);

					String[] formats = context.getResources().getStringArray(R.array.alarm_set);
					return String.format(formats[index], daySeq, hourSeq, minSeq);
	}


	@Override
	public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
			Preference preference) {
		if (preference == mTimePref) {
			new TimePickerDialog(this, this, mHour, mMinutes,
					DateFormat.is24HourFormat(this)).show();
		}

		return super.onPreferenceTreeClick(preferenceScreen, preference);
	}

	@Override
	public void onBackPressed() {
		if (id > -1)
			saveAlarm();
		finish();
	}

	private void saveAlarm() {
		final String alert = mAlarmPref.getAlertString();
		setAlarm(this, mEnabled, mHour, mMinutes,
				mRepeatPref.getDaysOfWeek(), mVibratePref.isChecked(),
				mLabel.getText(), alert);

		if (mEnabled) {
			popAlarmSetToast(this, mHour, mMinutes,
					mRepeatPref.getDaysOfWeek());
		}
	}

	private void setAlarm(AlarmEdit alarmEdit, boolean enabled, int hour,
			int minutes, DaysOfWeek daysOfWeek, boolean vibrate, String label,
			String alert) {
		ContentValues values = new ContentValues(8);

		long time = 0;
		if (!daysOfWeek.isRepeatSet()) 
			time = Alarms.calculateAlarm(hour, minutes, daysOfWeek).getTimeInMillis();

		values.put(DBHelper.COLUMN_ENABLED, enabled ? 1 : 0);
		values.put(DBHelper.COLUMN_HOUR, hour);
		values.put(DBHelper.COLUMN_MINUTES, minutes);
		values.put(DBHelper.COLUMN_VIBRATE, vibrate);
		values.put(DBHelper.COLUMN_LABEL, label);
		values.put(DBHelper.COLUMN_DAYSOFWEEK, daysOfWeek.getCoded());
		values.put(DBHelper.COLUMN_ALARMTIME, time);
		values.put(DBHelper.COLUMN_ALERT, alert);

		if (id > 0)
			AlarmTable.updateAlarm(id, values);
		else
			AlarmTable.createAlarm(values);

		Alarms.setNextAlert(getApplicationContext());
	}	

	@Override
	public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
		mHour = hourOfDay;
		mMinutes = minute;
		updateTime();
		// If the time has been changed, enable the alarm.
		mEnabled = true;
	}
}
