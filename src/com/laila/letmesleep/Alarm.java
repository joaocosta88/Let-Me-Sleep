package com.laila.letmesleep;


import java.text.DateFormatSymbols;
import java.util.Calendar;

import com.laila.letmesleep.R;

import android.content.Context;
import android.database.Cursor;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

public class Alarm implements Parcelable {

	public int id;
	public String label;
	public int hour;
	public long time;
	public int minutes;
	public boolean vibrate;
	public boolean enabled;
	public DaysOfWeek daysOfWeek;
	public boolean silent;
	public Uri alert;

	public static final String ALARM_ALERT_SILENT = "silent";

	Alarm(Cursor c) {		
		if (c.getCount()>0) {
			this.id = c.getInt(0);
			this.label=c.getString(1);
			this.enabled = c.getInt(2)==1;
			this.vibrate=c.getInt(3)==1;
			this.hour=c.getInt(4);
			this.minutes=c.getInt(5);
			this.daysOfWeek = new DaysOfWeek(c.getInt(6));
			this.time = c.getLong(7);
			String alertString = c.getString(8);
			if (ALARM_ALERT_SILENT.equals(alertString)) 
				silent = true;
			else {
				if (alertString != null && alertString.length() != 0) 
					alert = Uri.parse(alertString);

				// If the database alert is null or it failed to parse, use the
				// default alert.
				if (alert == null) {
					alert = RingtoneManager.getDefaultUri(
							RingtoneManager.TYPE_ALARM);
				}
			}
		}
		else {
			this.label="";
			this.hour = Alarms.getCurrentHour();
			this.minutes = Alarms.getCurrentMinute();
			this.vibrate = true;
			this.enabled = true;
			this.daysOfWeek = null;
			alert = RingtoneManager.getDefaultUri(
					RingtoneManager.TYPE_ALARM);
		}
	}

	public Alarm(Parcel p) {
		id = p.readInt();
		enabled = p.readInt() == 1;
		hour = p.readInt();
		minutes = p.readInt();
		daysOfWeek = new DaysOfWeek(p.readInt());
		time = p.readLong();
		vibrate = p.readInt() == 1;
		label = p.readString();
		alert = (Uri) p.readParcelable(null);
		silent = p.readInt() == 1;
	}

	public void writeToParcel(Parcel p, int flags) {
		p.writeInt(id);
		p.writeInt(enabled ? 1 : 0);
		p.writeInt(hour);
		p.writeInt(minutes);
		p.writeInt(daysOfWeek.getCoded());
		p.writeLong(time);
		p.writeInt(vibrate ? 1 : 0);
		p.writeString(label);
		p.writeParcelable(alert, flags);
		p.writeInt(silent ? 1 : 0);
	}

	public static final Parcelable.Creator<Alarm> CREATOR
	= new Parcelable.Creator<Alarm>() {
		public Alarm createFromParcel(Parcel p) {
			return new Alarm(p);
		}

		public Alarm[] newArray(int size) {
			return new Alarm[size];
		}
	};

	public String getLabelOrDefault(Context context) {
		if (label == null || label.length() == 0) {
			return context.getString(R.string.default_label);
		}
		return label;
	}

	/*
	 * Days of week code as a single int.
	 * 0x00: no day
	 * 0x01: Monday
	 * 0x02: Tuesday
	 * 0x04: Wednesday
	 * 0x08: Thursday
	 * 0x10: Friday
	 * 0x20: Saturday
	 * 0x40: Sunday
	 */
	static final class DaysOfWeek {

		private static int[] DAY_MAP = new int[] {
			Calendar.MONDAY,
			Calendar.TUESDAY,
			Calendar.WEDNESDAY,
			Calendar.THURSDAY,
			Calendar.FRIDAY,
			Calendar.SATURDAY,
			Calendar.SUNDAY,
		};

		// Bitmask of all repeating days
		private int mDays;

		DaysOfWeek(int days) {
			mDays = days;
		}

		public String toString(Context context, boolean showNever) {
			StringBuilder ret = new StringBuilder();

			// no days
			if (mDays == 0) {
				return showNever ?
						context.getText(R.string.never).toString() : "";
			}

			// every day
			if (mDays == 0x7f) {
				return context.getText(R.string.every_day).toString();
			}

			// count selected days
			int dayCount = 0, days = mDays;
			while (days > 0) {
				if ((days & 1) == 1) dayCount++;
				days >>= 1;
			}

			// short or long form?
			DateFormatSymbols dfs = new DateFormatSymbols();
			String[] dayList = (dayCount > 1) ?
					dfs.getShortWeekdays() :
						dfs.getWeekdays();

					// selected days
					for (int i = 0; i < 7; i++) {
						if ((mDays & (1 << i)) != 0) {
							ret.append(dayList[DAY_MAP[i]]);
							dayCount -= 1;
							if (dayCount > 0) ret.append(
									context.getText(R.string.day_concat));
						}
					}
					return ret.toString();
		}

		private boolean isSet(int day) {
			return ((mDays & (1 << day)) > 0);
		}

		public void set(int day, boolean set) {
			if (set) {
				mDays |= (1 << day);
			} else {
				mDays &= ~(1 << day);
			}
		}

		public void set(DaysOfWeek dow) {
			mDays = dow.mDays;
		}

		public int getCoded() {
			return mDays;
		}

		// Returns days of week encoded in an array of booleans.
		public boolean[] getBooleanArray() {
			boolean[] ret = new boolean[7];
			for (int i = 0; i < 7; i++) {
				ret[i] = isSet(i);
			}
			return ret;
		}

		public boolean isRepeatSet() {
			return mDays != 0;
		}

		/**
		 * returns number of days from today until next alarm
		 * @param c must be set to today
		 */
		public int getNextAlarm(Calendar c) {
			if (mDays == 0) {
				return -1;
			}

			int today = (c.get(Calendar.DAY_OF_WEEK) + 5) % 7;

			int day = 0;
			int dayCount = 0;
			for (; dayCount < 7; dayCount++) {
				day = (today + dayCount) % 7;
				if (isSet(day)) {
					break;
				}
			}
			return dayCount;
		}
	}

	@Override
	public int describeContents() {
		return 0;
	}

}