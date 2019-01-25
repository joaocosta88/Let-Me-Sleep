package com.laila.letmesleep;
/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */



import java.util.Calendar;

import com.actionbarsherlock.app.SherlockPreferenceActivity;
import com.laila.letmesleep.R;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.text.format.DateFormat;
import android.widget.TimePicker;

/**
 * Settings for the Alarm Clock.
 */
public class SettingsActivity extends SherlockPreferenceActivity
implements Preference.OnPreferenceChangeListener, TimePickerDialog.OnTimeSetListener {

	private Preference mTimePrefStart;
	private Preference mTimePrefEnd;

	private SharedPreferences preferences;

	private int mHourStart;
	private int mMinuteStart;
	private int mHourEnd;
	private int mMinuteEnd;

	//start = 0
	//end = 1
	private int openPicker; 

	private static final int ALARM_STREAM_TYPE_BIT =
			1 << AudioManager.STREAM_ALARM;

	private static final String KEY_ALARM_IN_SILENT_MODE =
			"alarm_in_silent_mode";
	static final String KEY_ALARM_SNOOZE =
			"snooze_duration";
	static final String KEY_VOLUME_BEHAVIOR =
			"volume_button_setting";
	static final String DND_ENABLER = 
			"dnd_enabler";

	static final int START_ID = 1010101;
	static final int END_ID = 0101010;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);
		mTimePrefStart = findPreference("dnd_start");
		mTimePrefEnd = findPreference("dnd_end");

		openPicker = -1;

		preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

		getTime();
		updateTime();
	}

	@Override
	protected void onResume() {
		super.onResume();
		refresh();
	}

	@Override
	public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
			Preference preference) {

		getTime();
		if (preference == mTimePrefStart) {
			openPicker = 0;
			new TimePickerDialog(this, this, mHourStart, mMinuteStart,
					DateFormat.is24HourFormat(this)).show();
		}

		else if (preference == mTimePrefEnd) {
			openPicker = 1;
			new TimePickerDialog(this, this, mHourEnd, mMinuteEnd,
					DateFormat.is24HourFormat(this)).show();
		}

		if (DND_ENABLER.equals(preference.getKey())) {
			if (preferences.getBoolean("dnd_enabler", false)) {
				setDND();
			}

			else {
				unsetDND();
			}				
		}

		if (KEY_ALARM_IN_SILENT_MODE.equals(preference.getKey())) {
			CheckBoxPreference pref = (CheckBoxPreference) preference;
			int ringerModeStreamTypes = Settings.System.getInt(
					getContentResolver(),
					Settings.System.MODE_RINGER_STREAMS_AFFECTED, 0);

			if (pref.isChecked()) {
				ringerModeStreamTypes &= ~ALARM_STREAM_TYPE_BIT;
			} else {
				ringerModeStreamTypes |= ALARM_STREAM_TYPE_BIT;
			}

			Settings.System.putInt(getContentResolver(),
					Settings.System.MODE_RINGER_STREAMS_AFFECTED,
					ringerModeStreamTypes);

			return true;
		}

		return super.onPreferenceTreeClick(preferenceScreen, preference);
	}

	public boolean onPreferenceChange(Preference pref, Object newValue) {
		final ListPreference listPref = (ListPreference) pref;
		final int idx = listPref.findIndexOfValue((String) newValue);
		listPref.setSummary(listPref.getEntries()[idx]);
		return true;
	}

	private void refresh() {
		final CheckBoxPreference alarmInSilentModePref =
				(CheckBoxPreference) findPreference(KEY_ALARM_IN_SILENT_MODE);
		final int silentModeStreams =
				Settings.System.getInt(getContentResolver(),
						Settings.System.MODE_RINGER_STREAMS_AFFECTED, 0);
		alarmInSilentModePref.setChecked(	
				(silentModeStreams & ALARM_STREAM_TYPE_BIT) == 0);

		final ListPreference snooze =
				(ListPreference) findPreference(KEY_ALARM_SNOOZE);
		snooze.setSummary(snooze.getEntry());
		snooze.setOnPreferenceChangeListener(this);
	}

	private void getTime() {
		mHourStart = preferences.getInt("mHoursStart", -1);
		mMinuteStart = preferences.getInt("mMinutesStart", -1);
		mHourEnd =	preferences.getInt("mHoursEnd", -1);
		mMinuteEnd = preferences.getInt("mMinutesEnd", -1);
	}

	private void updateTime() {
		if (mHourStart != -1) {
			CharSequence start = mHourStart+":"+mMinuteStart;
			mTimePrefStart.setSummary(start);
		}

		if (mHourEnd != -1) {
			CharSequence end = mHourEnd+":"+mMinuteEnd;
			mTimePrefEnd.setSummary(end);
		}
	}

	@Override
	public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
		SharedPreferences.Editor editor = preferences.edit();
		if (openPicker == 0) {
			editor.putInt("mHoursStart", hourOfDay);
			editor.putInt("mMinutesStart", minute);
		}
		else if (openPicker == 1) {
			editor.putInt("mHoursEnd", hourOfDay);
			editor.putInt("mMinutesEnd", minute);
		}
		editor.commit();
		getTime();
		updateTime();

		setDND();

	}

	protected void setDND() {

		Calendar start = Calendar.getInstance();
		start.setTimeInMillis(System.currentTimeMillis());
		start.set(Calendar.HOUR_OF_DAY, mHourStart);
		start.set(Calendar.MINUTE, mMinuteStart);
		
		Calendar end = Calendar.getInstance();
		end.setTimeInMillis(System.currentTimeMillis());
		end.set(Calendar.HOUR_OF_DAY, mHourEnd);
		end.set(Calendar.MINUTE, mMinuteEnd);

		Intent iStart = new Intent(this, SilenceReceiver.class);
		Intent iEnd = new Intent(this, UnsilenceReceiver.class);

		PendingIntent startPI = PendingIntent.getBroadcast(getApplicationContext(), START_ID, 
				iStart, PendingIntent.FLAG_CANCEL_CURRENT);
		PendingIntent endPI = PendingIntent.getBroadcast(getApplicationContext(), END_ID, 
				iEnd, PendingIntent.FLAG_CANCEL_CURRENT);		

		AlarmManager am = (AlarmManager) getApplicationContext().getSystemService(Context.ALARM_SERVICE);

		am.setRepeating(AlarmManager.RTC, start.getTimeInMillis(), 
				AlarmManager.INTERVAL_DAY, startPI);

		am.setRepeating(AlarmManager.RTC, System.currentTimeMillis()+60000, 
				AlarmManager.INTERVAL_DAY, endPI);
	}

	protected void unsetDND() {
		AlarmManager am = (AlarmManager) getApplicationContext().getSystemService(ALARM_SERVICE);
		PendingIntent startPI = PendingIntent.getBroadcast(getApplicationContext(), START_ID, 
				new Intent("SilenceReceiver"), PendingIntent.FLAG_UPDATE_CURRENT);

		am.cancel(startPI);

		PendingIntent endPI = PendingIntent.getBroadcast(getApplicationContext(), END_ID, 
				new Intent("UnsilenceReceiver"), PendingIntent.FLAG_UPDATE_CURRENT);

		am.cancel(endPI);
	}

}