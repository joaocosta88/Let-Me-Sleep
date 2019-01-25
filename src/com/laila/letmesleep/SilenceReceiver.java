package com.laila.letmesleep;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.util.Log;
import com.laila.letmesleep.R;

public class SilenceReceiver extends BroadcastReceiver  {

	@Override
	public void onReceive(Context context, Intent intent) {
		AudioManager audio = (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
		audio.setRingerMode(AudioManager.RINGER_MODE_SILENT);
	}
}
