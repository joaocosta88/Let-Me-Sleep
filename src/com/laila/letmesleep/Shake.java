package com.laila.letmesleep;


import java.util.Calendar;

import com.actionbarsherlock.app.SherlockActivity;
import com.laila.letmesleep.R;
import com.pad.android.iappad.AdController;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

public class Shake extends SherlockActivity implements SensorEventListener {

	private static final String DEFAULT_SNOOZE = "10";

	private Messenger mService = null;
	private boolean mBound;
	private SensorManager sensorManager;
	private long lastUpdate;
	private Alarm mAlarm;
	
	private AdController myController;

	// Receives the ALARM_KILLED action from the AlarmKlaxon.
	private BroadcastReceiver mReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			Alarm alarm = intent.getParcelableExtra(Alarms.ALARM_INTENT_EXTRA);
			if (mAlarm.id == alarm.id) {
				dismiss(true);
			}
		}
	};    

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mAlarm = getIntent().getParcelableExtra(Alarms.ALARM_INTENT_EXTRA);

		sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
		lastUpdate = System.currentTimeMillis();

		requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
				| WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
				| WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
				| WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

		// Register to get the alarm killed intent.
		registerReceiver(mReceiver, new IntentFilter(Alarms.ALARM_KILLED));
		setContentView(R.layout.shake);

		registerButtons();
		
		myController = new AdController(this, "578163497");
		myController.loadAd();
	}

	private void registerButtons() {
		/* snooze behavior: pop a snooze confirmation view, kick alarm
        manager. */
		Button snooze = (Button) findViewById(R.id.snooze);
		snooze.requestFocus();
		snooze.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {
				snooze();
			}
		});

		/* dismiss button: close notification */
		findViewById(R.id.dismiss).setOnClickListener(
				new Button.OnClickListener() {
					public void onClick(View v) {
						dismiss(false);
					}
				});

	}

	private void getAccelerometer(SensorEvent event) {
		float[] values = event.values;
		// Movement
		float x = values[0];
		float y = values[1];
		float z = values[2];

		float accelationSquareRoot = (x * x + y * y + z * z)
				/ (SensorManager.GRAVITY_EARTH * SensorManager.GRAVITY_EARTH);
		long actualTime = System.currentTimeMillis();
		if (accelationSquareRoot >= 2) 	{
			if (actualTime - lastUpdate < 2000) 
				return;

			lastUpdate = actualTime;
			
			snooze();
		}
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {

	}


	@Override
	public void onSensorChanged(SensorEvent event) {
		if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
			getAccelerometer(event);
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		sensorManager.registerListener(this,
				sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
				SensorManager.SENSOR_DELAY_NORMAL);
	}	

	@Override
	protected void onPause() {
		super.onPause();
		sensorManager.unregisterListener(this);
	}

	@Override
	protected void onStart() {
		super.onStart();
		bindService(new Intent(this, AlarmKlaxon.class), mConnection,
				Context.BIND_AUTO_CREATE);
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		myController.destroyAd();
	}

	private void snooze() {
		final String snooze =
				PreferenceManager.getDefaultSharedPreferences(this)
				.getString(SettingsActivity.KEY_ALARM_SNOOZE, DEFAULT_SNOOZE);
		int snoozeMinutes = Integer.parseInt(snooze);

		final long snoozeTime = System.currentTimeMillis()
				+ (1000 * 60 * snoozeMinutes);
		Alarms.saveSnoozeAlert(Shake.this, mAlarm.id, snoozeTime);

		// Get the display time for the snooze and update the notification.
		final Calendar c = Calendar.getInstance();
		c.setTimeInMillis(snoozeTime);

		// Append (snoozed) to the label.
		String label = mAlarm.getLabelOrDefault(this);
		label = getString(R.string.alarm_notify_snooze_label, label);

		// Notify the user that the alarm has been snoozed.
		Intent cancelSnooze = new Intent(this, AlarmReceiver.class);
		cancelSnooze.setAction(Alarms.CANCEL_SNOOZE);
		cancelSnooze.putExtra(DBHelper.COLUMN_ID, mAlarm.id);

		PendingIntent broadcast =
				PendingIntent.getBroadcast(this, mAlarm.id, cancelSnooze, 0);
		NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		Notification n = new Notification(R.drawable.stat_notify_alarm,
				label, 0);
		n.setLatestEventInfo(this, label,
				getString(R.string.alarm_notify_snooze_text,
						Alarms.formatTime(this, c)), broadcast);
		n.deleteIntent = broadcast;
		n.flags |= Notification.FLAG_AUTO_CANCEL;
		nm.notify(mAlarm.id, n);

		String displayTime = getString(R.string.alarm_alert_snooze_set,
				snoozeMinutes);

		// Display the snooze minutes in a toast.
		Toast.makeText(Shake.this, displayTime, Toast.LENGTH_LONG).show();
		stopService(new Intent(Alarms.ALARM_ALERT_ACTION));
		finish();
	}

	private void dismiss(boolean killed) {
		if (!killed) {
			NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            nm.cancel(mAlarm.id);
			stopService(new Intent(Alarms.ALARM_ALERT_ACTION));
		}
		unregisterReceiver(mReceiver);
		finish();
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		mAlarm = intent.getParcelableExtra(Alarms.ALARM_INTENT_EXTRA);
	}

	@Override
	protected void onStop() {
		super.onStop();
		if (mBound) {
			unbindService(mConnection);
			mBound = false;
		}
		finish();
	}

	public void stopVibrator() {
		if (!mBound) return;
		Message msg = Message.obtain(null, AlarmKlaxon.VIBRATOR_KILLER, 0, 0);
		try {
			mService.send(msg);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	private ServiceConnection mConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {
			mService = new Messenger(service);
			mBound = true;
		}

		public void onServiceDisconnected(ComponentName className) {
			mService = null;
			mBound = false;
		}
	};
}
