package com.laila.letmesleep;



import java.util.Calendar;
import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.laila.letmesleep.R;
import com.pad.android.iappad.AdController;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.widget.CursorAdapter;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.view.View.OnClickListener;
import android.widget.AbsListView.MultiChoiceModeListener;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ToggleButton;

public class AlarmClock extends SherlockActivity implements OnItemClickListener {

	private AdController myController;
	private AdController mySecondController;

	private final static int EDIT_ID = Menu.FIRST;
	private final static int DELETE_ID = Menu.FIRST+1;

	private static final int ACTIVITY_CREATE = 0;
	private static final int ACTIVITY_EDIT = 1;

	final static String PREFERENCES = "AlarmClock";

	private ListView lv;
	private AlarmTable table;
	private Cursor mCursor;

	private LayoutInflater mFactory;

	@SuppressLint("NewApi")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mFactory = LayoutInflater.from(this);

		table = new AlarmTable(getApplicationContext());
		table.open();
		mCursor = AlarmTable.getAlarms(getApplicationContext());

		setContentView(R.layout.alarm_clock);
		lv = (ListView) findViewById(R.id.alarms_list);
		AlarmClock.AlarmTimeAdapter adapter = this.new AlarmTimeAdapter(this, mCursor);
		lv.setAdapter(adapter);
		lv.setVerticalScrollBarEnabled(true);
		lv.setOnItemClickListener(this);
		lv.setOnCreateContextMenuListener(this);

		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) 
			registerForContextMenu(lv);
		else {
			lv.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
			lv.setMultiChoiceModeListener(new MultiChoiceModeListener() {

				@Override
				public void onItemCheckedStateChanged(android.view.ActionMode mode, int position,
						long id, boolean checked) {
					final int checkedCount = lv.getCheckedItemCount();
					switch (checkedCount)
					{
					case 0:
						mode.setSubtitle(null);
						break;
					case 1:
						mode.setSubtitle("One item selected");
						break;
					default:
						mode.setSubtitle("" + checkedCount + " items selected");
						break;
					}
				}

				@Override
				public boolean onActionItemClicked(android.view.ActionMode mode, android.view.MenuItem item) {
					switch (item.getItemId()) {
					case R.id.delete:
						for (long l: lv.getCheckedItemIds())
							deleteAlarm(l);
						mode.finish();
						return true;
					default:
						return false;
					}
				}

				@Override
				public boolean onCreateActionMode(android.view.ActionMode mode, android.view.Menu menu) {
					android.view.MenuInflater inflater = mode.getMenuInflater();
					inflater.inflate(R.menu.list_select_menu, menu);
					mode.setTitle("Select Items");
					return true;
				}

				@Override
				public void onDestroyActionMode(android.view.ActionMode mode) {
				}

				@Override
				public boolean onPrepareActionMode(android.view.ActionMode mode, android.view.Menu menu) {
					return true;
				}
			});
		}	
		myController = new AdController(this, "739222565");
		mySecondController = new AdController(this, "409359255");
		myController.loadAd();
		mySecondController.loadAd();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getSupportMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		table.close();
		myController.destroyAd();
		mySecondController.destroyAd();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_new:
			addAlarm();
			return true;
		case R.id.menu_settings:
			//startActivity(new Intent(this, SettingsActivity.class));
			startActivity(new Intent(this, SettingsActivity.class));
			return true;

		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode,
			Intent intent) {
		super.onActivityResult(requestCode, resultCode, intent);
		updateLayout();
	}

	private void updateLayout() {
		mCursor = AlarmTable.getAlarms(getApplicationContext());
		lv.setAdapter(new AlarmTimeAdapter(this, mCursor));
	}


	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);

		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
			//menu.setHeaderTitle(title);
			menu.add(0, EDIT_ID, 0, "Edit");
			menu.add(0, DELETE_ID, 1, "Delete");
		}
	}

	@Override
	public boolean onContextItemSelected(android.view.MenuItem item) {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
			AdapterContextMenuInfo info = (AdapterContextMenuInfo) item
					.getMenuInfo();
			switch(item.getItemId()) {
			case EDIT_ID:
				editAlarm(info.id);
				return true;
			case DELETE_ID:
				deleteAlarm(info.id);
				return true;
			}
		}
		return super.onContextItemSelected(item);
	}

	public void deleteAlarm(long itemId) {
		AlarmTable.deleteAlarm(itemId);
		Alarms.setNextAlert(getApplicationContext());
		updateLayout();
	}

	public void editAlarm(long itemId) {
		Intent i = new Intent(this, AlarmEdit.class);
		i.putExtra("id", (int) itemId);
		startActivityForResult(i, ACTIVITY_EDIT);
	}

	public void addAlarm() {
		Intent i = new Intent(this, AlarmEdit.class);
		startActivityForResult(i, ACTIVITY_CREATE);
	}

	private void makeToast(String text) {
		Toast.makeText(getApplicationContext(), text, Toast.LENGTH_SHORT).show();
	}

	public class AlarmTimeAdapter extends CursorAdapter {
		public AlarmTimeAdapter(Context context, Cursor cursor) {
			super(context, cursor);
		}

		public View newView(Context context, Cursor cursor, ViewGroup parent) {
			View ret = mFactory.inflate(R.layout.alarm_time, null, false);
			((TextView) ret.findViewById(R.id.am)).setText("mAm");
			((TextView) ret.findViewById(R.id.pm)).setText("mPm");
			return ret;
		}

		public void bindView(View view, Context context, Cursor cursor) {
			final Alarm alarm = new Alarm(cursor);

			ToggleButton onButton = (ToggleButton)view.findViewById(R.id.alarmButton);
			onButton.setChecked(alarm.enabled);
			onButton.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					boolean isChecked = ((ToggleButton) v).isChecked();
					Alarms.enableAlarm(getApplicationContext(), alarm.id,
							isChecked);
					if (isChecked) {
						AlarmEdit.popAlarmSetToast(AlarmClock.this,
								alarm.hour, alarm.minutes, alarm.daysOfWeek);
					}
				}
			});

			DigitalClock digitalClock =
					(DigitalClock) view.findViewById(R.id.digitalClock);

			// set the alarm text
			final Calendar c = Calendar.getInstance();
			c.set(Calendar.HOUR_OF_DAY, alarm.hour);
			c.set(Calendar.MINUTE, alarm.minutes);
			digitalClock.updateTime(c);

			// Set the repeat text or leave it blank if it does not repeat.
			TextView daysOfWeekView =
					(TextView) digitalClock.findViewById(R.id.daysOfWeek);
			final String daysOfWeekStr = 
					alarm.daysOfWeek.toString(AlarmClock.this, false);
			if (daysOfWeekStr != null && daysOfWeekStr.length() != 0) {
				daysOfWeekView.setText(daysOfWeekStr);
				daysOfWeekView.setVisibility(View.VISIBLE);
			} else {
				//daysOfWeekView.setVisibility(View.GONE);
			}

			// Display the label
			TextView labelView =
					(TextView) digitalClock.findViewById(R.id.label);
			if (alarm.label != null && alarm.label.length() != 0) {
				labelView.setText(alarm.label);
				labelView.setVisibility(View.VISIBLE);
			} else {
				labelView.setVisibility(View.GONE);
			}
		}
	}

	@Override
	public void onItemClick(AdapterView parent, View v, int pos, long id) {
		Intent intent = new Intent(this, AlarmEdit.class);
		intent.putExtra("id", (int) id);
		startActivityForResult(intent, ACTIVITY_EDIT);
	}
}

