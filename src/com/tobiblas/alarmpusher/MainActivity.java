package com.tobiblas.alarmpusher;

import java.util.HashMap;
import java.util.Map;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.SoundPool;
import android.media.SoundPool.OnLoadCompleteListener;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.widget.Toast;

public class MainActivity extends Activity {

	// adb shell am start -a android.intent.action.VIEW -n
	// com.tobiblas.alarmpusher/.MainActivity -e sound false -e phonenumbers
	// '+46760732005' -e message "Hej tobias du är söt"

	// adb shell am force-stop com.tobiblas.alarmpusher
	private Map<String, BroadcastReceiver> receivers = new HashMap<String, BroadcastReceiver>();
	
	private AudioManager audioManager;
	private int soundID;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.main);

		String phoneNumbersToPushTo = getIntent()
				.getStringExtra("phonenumbers");
		System.out.println("phonenumbers = " + phoneNumbersToPushTo);

		boolean playAlarmSound = false;
		String sound = getIntent().getStringExtra("sound");
		if (sound != null && sound.equals("true")) {
			playAlarmSound = true;
		}
		System.out.println("playAlarmSound = " + playAlarmSound);

		boolean sendSMS = false;
		String sms = getIntent().getStringExtra("sms");
		if (sms != null && sms.equals("true")) {
			sendSMS = true;
		}
		System.out.println("sendSMS = " + sendSMS);

		String message = getIntent().getStringExtra("message");
		if (message == null) {
			message = "ALARM!! The alarm went off!!";
		}
		System.out.println("message = " + message);

		final String mess = message;

		if (phoneNumbersToPushTo != null && sendSMS) {
			final String[] numbers = phoneNumbersToPushTo.split("#");
			Thread t = new Thread() {
				public void run() {
					sendSMS(numbers, mess);
				};
			};
			t.start();
		}
		playAlarmsound(playAlarmSound);
	}


	private void playAlarmsound(boolean playAlarmSound) {

		audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
		float actVolume = (float) audioManager
				.getStreamVolume(AudioManager.STREAM_MUSIC);
		float maxVolume = (float) audioManager
				.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
		final float volume = actVolume / maxVolume;

		this.setVolumeControlStream(AudioManager.STREAM_MUSIC);
		
		SoundPool soundPool = new SoundPool(10, AudioManager.STREAM_MUSIC, 0);
		soundPool.setOnLoadCompleteListener(new OnLoadCompleteListener() {
			@Override
			public void onLoadComplete(SoundPool soundPool, int sampleId,
					int status) {
				soundPool.play(soundID, volume, volume, 1, -1, 1f);
			}

		});

		soundID = soundPool.load(this, R.raw.burglar_alarm_going_off, 1);
	}

	private void sendSMS(String[] numbers, String message) {
		SmsManager smsManager = SmsManager.getDefault();

		int i = 0;
		for (String number : numbers) {
			String SENT_INTENT = i + "_" + numbers[i];
			PendingIntent sentPI = PendingIntent.getBroadcast(this, i,
					new Intent(SENT_INTENT), Intent.FILL_IN_CATEGORIES);

			// ---when the SMS has been sent---
			BroadcastReceiver r = getReciever(number);
			receivers.put(number, r);
			registerReceiver(r, new IntentFilter(SENT_INTENT));
			smsManager.sendTextMessage(number, null, message, sentPI, null);
			++i;
		}
	}

	private BroadcastReceiver getReciever(final String number) {
		final BroadcastReceiver r = new BroadcastReceiver() {
			@Override
			public void onReceive(Context arg0, Intent arg1) {
				switch (getResultCode()) {
				case Activity.RESULT_OK:
					Toast.makeText(getBaseContext(), "SMS sent",
							Toast.LENGTH_SHORT).show();
					break;
				case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
					Toast.makeText(getBaseContext(), "Generic failure",
							Toast.LENGTH_SHORT).show();
					break;
				case SmsManager.RESULT_ERROR_NO_SERVICE:
					Toast.makeText(getBaseContext(), "No service",
							Toast.LENGTH_SHORT).show();
					break;
				case SmsManager.RESULT_ERROR_NULL_PDU:
					Toast.makeText(getBaseContext(), "Null PDU",
							Toast.LENGTH_SHORT).show();
					break;
				case SmsManager.RESULT_ERROR_RADIO_OFF:
					Toast.makeText(getBaseContext(), "Radio off",
							Toast.LENGTH_SHORT).show();
					break;
				}
				unregisterReciever(number);
			}

		};
		return r;
	}

	private void unregisterReciever(String number) {
		BroadcastReceiver r = receivers.remove(number);
		if (r != null) {
			unregisterReceiver(r);
		}
	}
}
