/*
Copyright 2009 Michael Cramer

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */

package com.webkist.android.DarkroomTimer;

import com.webkist.android.DarkroomTimer.DarkroomPreset.DarkroomStep;

import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnClickListener;

public class DarkroomTimer extends Activity implements OnClickListener {
	private static final String TAG = "DarkroomTimer";

	// TODO Organize these.
	private static final int TICK = 1;
	private static final int DONE = 2;
	private static final int ADJUST_STOPPED_CLOCK = 3;
	private static final int GET_PRESET = 4;
	private static final int FAILED_PRESET_PICK = 5;
	private static final int ADJUST_RUNNING_CLOCK = 6;
	private static final int NEXT = 7;

	private static final String SELECTED_PRESET = "selectedPreset";
	private static final String RUNNING_START_TIME = "runningStartTime";

	private TextView timerText;
	private TextView upcomingText;
	private TextView userActionText;
	private TextView stepHead;

	private long startTime = 0;

	private DarkroomPreset preset = null;
	private Ringtone ping;
	private boolean done = false;

	private Thread timer = null;
	private boolean timerRunning = false;

	Handler threadMessageHandler = new Handler() {

		public void handleMessage(Message msg) {
			switch (msg.what) {
			case TICK:
				long remaining = stepTimeRemaining();

				if(remaining <= 0) {
					// DONE
					stopThread();
					startTime = 0;

					upcomingText.setText("");
					
					if(preset.nextStep()) {
						long dur = stepTimeRemaining() / 1000;
						stepHead.setText(preset.currentStep().name);
						timerText.setText(String.format("%02d:%02d", (int) dur / 60, (int) dur % 60));
						userActionText.setText("Click to start...");
					} else {
						stepHead.setText(R.string.prompt_done);
						timerText.setText("DONE");
						timerRunning=false;
						userActionText.setText("");
						done=true;
					} 

				} else {
					int minutes = (int) remaining / 60000;
					int seconds = (int) ((remaining % 60000) / 1000);

					timerText.setText(String.format("%02d:%02d", minutes, seconds));
					double elapsedSecs = (System.currentTimeMillis() - startTime) / 1000;
					DarkroomStep step = preset.currentStep();

					// If we're close to the end, play the notification sound as often as possible.
					if(remaining <= 5000 && !ping.isPlaying()) {
						ping.play();
					}

					// Figure out what the user should be doing now.
					if (step.pourFor > 0 && elapsedSecs < step.pourFor) {
						// Pour.
						userActionText.setText("Pour...");
					} else if (step.agitateEvery > 0) {
						if (elapsedSecs < (step.pourFor + step.agitateFor)) {
							// Agitate after pour.
							userActionText.setText(R.string.prompt_agitate);
						} else if (((elapsedSecs - step.pourFor) % step.agitateEvery) < step.agitateFor) {
							// Agitate. 
							userActionText.setText(R.string.prompt_agitate);
						} else {
							// Nothing.
							userActionText.setText("");
						}
					} else { // This clears the "Click to start..." prompt.
						userActionText.setText("");
					}

					// What's coming up.
					if(step.agitateEvery > 0) {
						double elapsedRemainder = (elapsedSecs - step.pourFor) % step.agitateEvery;
						Log.v(TAG, String.format("%.0f > (%d - 10) && %.1f >= (%d + 10)", elapsedRemainder, step.agitateEvery, ((double) remaining) / 1000, step.agitateEvery));

						if (elapsedRemainder > (step.agitateEvery - 10) // Not the first iteration.
								&& (remaining / 1000) >= (step.agitateFor + 10)) { // And we have enough time left.
							// Coming up on agitation Ð but not after we're done.
							double agitateIn = step.agitateEvery - elapsedRemainder;
							Resources res = getResources();
							upcomingText.setText(String.format("%s in %02d:%02d", res.getString(R.string.prompt_agitate),
									(int) agitateIn / 60, (int) agitateIn % 60));
						} else {
							upcomingText.setText("");
						}
					} else { // This clears probably nothing.
						upcomingText.setText("");
					}
				}
				
				break;
			}
		}
	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Log.v(TAG, "in onCreate");
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		setContentView(R.layout.main);
		LinearLayout mainView = (LinearLayout) findViewById(R.id.mainLayout);
		mainView.setOnClickListener(this);

		timerText = (TextView) findViewById(R.id.stepClock);
		timerText.setOnClickListener(this);
		upcomingText = (TextView) findViewById(R.id.upcoming);
		userActionText = (TextView) findViewById(R.id.userAction);
		stepHead = (TextView) findViewById(R.id.stepLabel);
		
		ping = RingtoneManager.getRingtone(this, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));

		if(savedInstanceState != null) {
			preset = (DarkroomPreset) savedInstanceState.getSerializable(SELECTED_PRESET);
			startTime = savedInstanceState.getLong(RUNNING_START_TIME);
			long dur = stepTimeRemaining() / 1000;
			stepHead.setText(preset.currentStep().name);
			timerText.setText(String.format("%02d:%02d", (int) dur / 60, (int) dur % 60));
		} else {
			Intent intent = new Intent(this, TimerPicker.class);
			startActivityForResult(intent, GET_PRESET);
		}

	}
	
	@Override
	public void onResume() {
		super.onResume();
		if (preset != null) {
			TextView header = (TextView) findViewById(R.id.presetName);
			header.setText(preset.name);
			
			long dur = stepTimeRemaining() / 1000;
			stepHead.setText(preset.currentStep().name);
			timerText.setText(String.format("%02d:%02d", (int) dur / 60, (int) dur % 60));

			Log.v(TAG, "in onResume(): " + String.format("%02d:%02d", (int) dur / 60, (int) dur % 60));
			if (preset.running()) {
				startThread();
			} else {
				// Wait for a click.
			}
		}
	}


	@Override
	public void onSaveInstanceState(Bundle outState) {
		outState.putSerializable(SELECTED_PRESET, preset);
		outState.putLong(RUNNING_START_TIME, startTime);
	}

	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
			Intent intent = new Intent(this, TimerPicker.class);
			startActivityForResult(intent, GET_PRESET);
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}
	
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.darkroomtimer, menu);
		return true;
	}

	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.stop_timer:
			stopThread();
			return true;
		case R.id.select_preset:
			Intent intent = new Intent(this, TimerPicker.class);
			startActivityForResult(intent, GET_PRESET);
			return true;
		}
		return false;
	}

	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == GET_PRESET) {
			if (resultCode == RESULT_OK) {
				timerRunning = false;
				startTime = 0;
				stopThread();

				Uri uri = data.getData();
				Log.v(TAG, "onActivityResult - URI: " + uri);
				preset = new DarkroomPreset(this, uri);
			}
		}
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		switch (keyCode) {

		case KeyEvent.KEYCODE_DPAD_CENTER:
			Log.v(TAG, "Key: " + event);
			handleClick();
			break;

		default:
			return false;

		}
		return true;
	}

	protected Dialog onCreateDialog(int id) {
		LayoutInflater factory = LayoutInflater.from(this);
		if (id == FAILED_PRESET_PICK) {
			return new AlertDialog.Builder(DarkroomTimer.this).setTitle(R.string.app_name).setMessage(
					R.string.preset_pick_failed).setCancelable(false).setPositiveButton(R.string.time_picker_ok,
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int whichButton) {
							finish();
						}
					}).create();
		} else if (id == ADJUST_RUNNING_CLOCK) {
			final View timePickerView = factory.inflate(R.layout.addseconds, null);

			NumberPicker addClock = (NumberPicker) timePickerView.findViewById(R.id.addSeconds);
			addClock.setFormatter(NumberPicker.TWO_DIGIT_FORMATTER);
			addClock.setRange(-360, 360);
			addClock.setSpeed(100);
			addClock.setIncBy(5);
			addClock.setEnabled(true);

			return new AlertDialog.Builder(DarkroomTimer.this).setTitle(R.string.time_picker_title).setView(timePickerView)
					.setPositiveButton(R.string.time_picker_ok, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int whichButton) {
							NumberPicker addClock = (NumberPicker) timePickerView.findViewById(R.id.addSeconds);

							int addseconds = addClock.getCurrent();

							preset.currentStep().duration += addseconds;
							Message m = new Message();
							m.what = DarkroomTimer.TICK;
							DarkroomTimer.this.threadMessageHandler.sendMessage(m);

						}
					}).setNegativeButton(R.string.time_picker_cancel, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int whichButton) {
							Log.v(TAG, "in onCreateDialog, clicked Cancel");
						}
					}).create();
		} else if (id == ADJUST_STOPPED_CLOCK) {

			final View timePickerView = factory.inflate(R.layout.time_picker, null);

			NumberPicker minClock = (NumberPicker) timePickerView.findViewById(R.id.minuteClock);
			minClock.setFormatter(NumberPicker.TWO_DIGIT_FORMATTER);
			minClock.setRange(0, 60);
			minClock.setSpeed(100);
			minClock.setEnabled(true);

			NumberPicker secClock = (NumberPicker) timePickerView.findViewById(R.id.secondClock);
			secClock.setFormatter(NumberPicker.TWO_DIGIT_FORMATTER);
			secClock.setRange(0, 59);
			secClock.setSpeed(100);
			secClock.setEnabled(true);

			return new AlertDialog.Builder(DarkroomTimer.this).setTitle(R.string.time_picker_title).setView(timePickerView)
					.setPositiveButton(R.string.time_picker_ok, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int whichButton) {
							NumberPicker minClock = (NumberPicker) timePickerView.findViewById(R.id.minuteClock);
							NumberPicker secClock = (NumberPicker) timePickerView.findViewById(R.id.secondClock);

							int minutes = minClock.getCurrent();
							int seconds = secClock.getCurrent();

							preset.currentStep().duration = minutes * 60 + seconds;
							Message m = new Message();
							m.what = DarkroomTimer.TICK;
							DarkroomTimer.this.threadMessageHandler.sendMessage(m);
						}
					}).setNegativeButton(R.string.time_picker_cancel, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int whichButton) {
							Log.v(TAG, "in onCreateDialog, clicked Cancel");
						}
					}).create();
		}

		return null;
	}

	public void onPrepareDialog(int id, Dialog dialog) {
		if (id == ADJUST_RUNNING_CLOCK) {
			NumberPicker addClock = (NumberPicker) dialog.findViewById(R.id.addSeconds);
			addClock.changeCurrent(0);
		} else if (id == ADJUST_STOPPED_CLOCK) {
			int minutes = (int) preset.currentStep().duration / 60;
			int seconds = (int) preset.currentStep().duration % 60;

			NumberPicker minClock = (NumberPicker) dialog.findViewById(R.id.minuteClock);
			minClock.changeCurrent(minutes);

			NumberPicker secClock = (NumberPicker) dialog.findViewById(R.id.secondClock);
			secClock.changeCurrent(seconds);
		}
	}

	@Override
	public void onClick(View v) {
		if(!done) {
			if (v.getId() == R.id.stepClock) {
				Log.v(TAG, "We have a clock click.");
				if (timerRunning) {
					showDialog(ADJUST_RUNNING_CLOCK);
				} else {
					showDialog(ADJUST_STOPPED_CLOCK);
				}
			} else {
				Log.v(TAG, "In onClick: " + v.getId());
				handleClick();
			}
		}
	}

	private void handleClick() {
		if (timerRunning) {
			Log.v(TAG, "Timer already running!");
		} else {
			startTime = System.currentTimeMillis();
			preset.start();
			startThread();
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		Log.v(TAG, "in onPause()");
		stopThread();
	}

	@Override
	public void onStop() {
		super.onStop();
		Log.v(TAG, "in onStop()");
	}

	private void startThread() {
		timerRunning = true;
		timer = new Thread(new TimerThread());
		timer.start();
	}

	private void stopThread() {
		if (timerRunning) {
			timer.interrupt();
			timerRunning = false;
		}
	}

	private long stepTimeRemaining() {
		if (startTime == 0) {
			return (preset.currentStep().pourFor + preset.currentStep().duration) * 1000;
		} else {
			return startTime + (preset.currentStep().pourFor + preset.currentStep().duration) * 1000 - System.currentTimeMillis();
		}
	}

	class TimerThread implements Runnable {
		public void run() {
			while (!Thread.currentThread().isInterrupted()) {
				Message m = new Message();
				m.what = DarkroomTimer.TICK;
				DarkroomTimer.this.threadMessageHandler.sendMessage(m);

				try {
					Thread.sleep(500); // Tick every half-second.
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}

			}
		}
	}

}
