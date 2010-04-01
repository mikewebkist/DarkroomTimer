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
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ToggleButton;
import android.widget.ViewAnimator;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnClickListener;

public class DarkroomTimer extends Activity implements OnClickListener, OnCheckedChangeListener {
	private static final String TAG = "DarkroomTimer";

	private static final int TICK = 1;
	private static final int ADJUST_STOPPED_CLOCK = 3;
	private static final int GET_PRESET = 4;
	private static final int FAILED_PRESET_PICK = 5;
	private static final int ADJUST_RUNNING_CLOCK = 6;

	private static final int PROMPT_NONE = 0;
	private static final int PROMPT_POUR = 1;
	private static final int PROMPT_AGITATE = 2;

	private static final String SELECTED_PRESET = "selectedPreset";
	private static final String RUNNING_START_TIME = "runningStartTime";
	private static final String RUNNING_PAUSE_TIME = "runningPauseTime";
	private static final String RUNNING_CURRENT_STEP_RUNNING = "runningCurrentStepRunning";
	private static final String RUNNING_CURRENT_STEP = "runningCurrentStep";

	private TextView timerText;
	private TextView upcomingText;
	private TextView stepLabel;

	private long startTime = 0;
	private long pauseTime = 0;
	private int currentStep = 0;
	private boolean currentStepRunning = false;

	private DarkroomPreset preset = null;
	private Ringtone ping = null;

	private Thread timer = null;
	private boolean timerRunning = false;

	private ViewAnimator actionFlipper;

	private Ringtone agitatePing = null;
	private boolean agitatePingPlayed = false;

	Handler threadMessageHandler = new Handler() {

		public void handleMessage(Message msg) {
			switch (msg.what) {
				case TICK:
					long remaining = stepTimeRemaining();

					if (remaining <= 0) {
						// DONE
						stopThread();
						startTime = 0;

						upcomingText.setText("");

						if(preset.steps.size() == (currentStep + 1)) {
							stepLabel.setText(R.string.prompt_done);
							timerText.setText(R.string.prompt_done);
							actionFlipper.setDisplayedChild(PROMPT_NONE);
							ToggleButton startButton = (ToggleButton) findViewById(R.id.toggleButton);
							startButton.setEnabled(false);
							// On orientation change, this will restart everything.
							currentStep = 0;
							startTime = 0;
							pauseTime = 0;
							currentStepRunning = false;
						} else {
							currentStep++;
							long dur = stepTimeRemaining() / 1000;
							stepLabel.setText(preset.steps.get(currentStep).name);
							timerText.setText(String.format("%02d:%02d", (int) dur / 60, (int) dur % 60));
							actionFlipper.setDisplayedChild(PROMPT_NONE);
							((ToggleButton) findViewById(R.id.toggleButton)).setChecked(false);
						}
						
					} else {
						int minutes = (int) remaining / 60000;
						int seconds = (int) ((remaining % 60000) / 1000);

						timerText.setText(String.format("%02d:%02d", minutes, seconds));
						double elapsedSecs = (System.currentTimeMillis() - startTime) / 1000;
						DarkroomStep step = preset.steps.get(currentStep);

						// If we're close to the end, play the notification
						// sound as often as possible.
						if (ping != null && remaining <= 5000 && !ping.isPlaying()) {
							ping.play();
						}

						// Figure out what the user should be doing now.
						if (step.pourFor > 0 && elapsedSecs < step.pourFor) {
							// Pour.
							actionFlipper.setDisplayedChild(PROMPT_POUR);
						} else if (step.agitateEvery > 0) {
							if (elapsedSecs < (step.pourFor + step.agitateFor)) {
								// Agitate after pour.
								actionFlipper.setDisplayedChild(PROMPT_AGITATE);
							} else if (((elapsedSecs - step.pourFor) % step.agitateEvery) < step.agitateFor) {
								// Agitate.
								actionFlipper.setDisplayedChild(PROMPT_AGITATE);
								agitatePingPlayed = false;
							} else {
								// Nothing.
								actionFlipper.setDisplayedChild(PROMPT_NONE);
							}
						} else { // This clears the "Click to start..." prompt.
							actionFlipper.setDisplayedChild(PROMPT_NONE);
						}

						// What's coming up.
						if (step.agitateEvery > 0) {
							double elapsedRemainder = (elapsedSecs - step.pourFor) % step.agitateEvery;

							if (elapsedRemainder > (step.agitateEvery - 10) && (remaining / 1000) >= (step.agitateFor + 10)) {
								double agitateIn = step.agitateEvery - elapsedRemainder;
								Resources res = getResources();
								if ((elapsedRemainder > (step.agitateEvery - 3)) && !agitatePingPlayed
										&& agitatePing != null && !agitatePing.isPlaying()) {
									agitatePing.play();
									agitatePingPlayed = true;
								}
								upcomingText.setText(String.format("%s in %02d:%02d",
										res.getString(R.string.prompt_agitate), (int) agitateIn / 60, (int) agitateIn % 60));
							} else {
								upcomingText.setText("");
							}
						} else {
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

		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON|WindowManager.LayoutParams.FLAG_FULLSCREEN);

		setContentView(R.layout.main);
		ToggleButton startButton = (ToggleButton) findViewById(R.id.toggleButton);
		startButton.setOnCheckedChangeListener(this);

		timerText = (TextView) findViewById(R.id.stepClock);
		Typeface face = Typeface.createFromAsset(getAssets(), "digital-7-mono.ttf");
		timerText.setTypeface(face);
		TextView timerTextBG = (TextView) findViewById(R.id.stepClockBlack);
		timerTextBG.setTypeface(face);
		timerText.setOnClickListener(this);
		upcomingText = (TextView) findViewById(R.id.upcoming);
		actionFlipper = (ViewAnimator) findViewById(R.id.actionFlipper);
		stepLabel = (TextView) findViewById(R.id.stepLabel);

		if (savedInstanceState != null) {
			preset = (DarkroomPreset) savedInstanceState.getSerializable(SELECTED_PRESET);
			startTime = savedInstanceState.getLong(RUNNING_START_TIME);
			pauseTime = savedInstanceState.getLong(RUNNING_PAUSE_TIME);
			currentStep = savedInstanceState.getInt(RUNNING_CURRENT_STEP);
			currentStepRunning = savedInstanceState.getBoolean(RUNNING_CURRENT_STEP_RUNNING);
		}
	}

	public void onStart() {
		super.onStart();

		final Intent intent = getIntent();

		if (intent != null && preset == null) {
			final String action = intent.getAction();
			if (Intent.ACTION_VIEW.equals(action)) {
				try {
					preset = new DarkroomPreset(this, intent.getData());
				} catch (IllegalArgumentException e) {
					Log.e(TAG, e.toString());
					preset = null;
				}
				// After we setup the preset, clear the Intent.
				setIntent(null);
			} else {
				Log.e(TAG, "Don't know what to do with intent.getAction() == " + action + " for content type: "
						+ getContentResolver().getType(intent.getData()));
			}
		}

		if (preset == null) {
			startActivity(new Intent(this, TimerPicker.class));
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		// We should only get here if we already have a preset, from onStart().

		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
		String ring = settings.getString("alertTone", null);
		String agitateRing = settings.getString("agitateTone", null);
		boolean normalMode = settings.getBoolean("normalmode", true);

		ping = null;
		agitatePing = null;
		
		if (ring == null) {
			// if null, preference is unset so use the system default.
			ping = RingtoneManager.getRingtone(this, Settings.System.DEFAULT_NOTIFICATION_URI);
		} else if (ring != "") {
			// if not empty string, preference is set to a real ringtone.
			ping = RingtoneManager.getRingtone(this, Uri.parse(ring));
		}

		if (agitateRing == null) {
			// if null, preference is unset so use the system default.
			agitatePing = RingtoneManager.getRingtone(this, Settings.System.DEFAULT_NOTIFICATION_URI);
		} else if (agitateRing != "") {
			// if not empty string, preference is set to a real ringtone.
			agitatePing = RingtoneManager.getRingtone(this, Uri.parse(agitateRing));
		}

		// In normal mode, use the color from preferences. In redlight mode, force to red, natch.
		int ledColor;
		int normalColor;
		if(normalMode) {
			ledColor = Color.parseColor(settings.getString("ledColor", "#ffff0000"));
			normalColor = Color.parseColor("#ffffffff");
		} else {
			ledColor = Color.parseColor("#ffff0000");
			normalColor = ledColor;
		}

		((LinearLayout) findViewById(R.id.stepBlock)).setBackgroundColor(normalColor);
		((TextView) findViewById(R.id.stepLabel)).setTextColor(normalColor);
		
		timerText.setTextColor(ledColor);
		TextView timerTextBG = (TextView) findViewById(R.id.stepClockBlack);
		timerTextBG.setBackgroundColor(ledColor & 0x22ffffff);
		timerTextBG.setTextColor(ledColor & 0x44ffffff);

		String title = preset.name;

		if (preset.temp != null && preset.temp.length() > 0) {
			title += String.format(" @ %s", preset.temp);
		}
		if (preset.iso > 0) {
			title += String.format(", ISO %d", preset.iso);
		}
		this.setTitle(title);
		ToggleButton toggleButton = (ToggleButton) findViewById(R.id.toggleButton);

		if (preset.steps.size() == (currentStep + 1) && startTime > 0 && !currentStepRunning) {
			stepLabel.setText(R.string.prompt_done);
			timerText.setText(R.string.prompt_done);
			actionFlipper.setDisplayedChild(PROMPT_NONE);
			toggleButton.setEnabled(false);
		} else {
			long dur = stepTimeRemaining() / 1000;
			stepLabel.setText(preset.steps.get(currentStep).name);
			timerText.setText(String.format("%02d:%02d", (int) dur / 60, (int) dur % 60));
			toggleButton.setEnabled(true);
		}

		if (currentStepRunning && startTime > 0) {
			if (pauseTime > 0) {
				toggleButton.setChecked(false);
				pauseTimer();
			} else {
				toggleButton.setChecked(true);
				startTimer();
			}
		} else if (currentStepRunning || (!currentStepRunning && preset.steps.size() <= (currentStep + 1))) {
			toggleButton.setChecked(false);
			actionFlipper.setDisplayedChild(PROMPT_NONE);
		}

	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		outState.putSerializable(SELECTED_PRESET, preset);
		outState.putLong(RUNNING_START_TIME, startTime);
		outState.putLong(RUNNING_PAUSE_TIME, pauseTime);
		outState.putInt(RUNNING_CURRENT_STEP, currentStep);
		outState.putBoolean(RUNNING_CURRENT_STEP_RUNNING, currentStepRunning);
	}

	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.darkroomtimer, menu);
		return true;
	}

	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.preferences:
				startActivity(new Intent(this, EditPreferences.class));
				return true;

			case R.id.select_preset:
				Intent intent = new Intent(this, TimerPicker.class);
				startActivity(intent);
				return true;
		}
		return false;
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

							preset.steps.get(currentStep).duration += addseconds;
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

							preset.steps.get(currentStep).duration = minutes * 60 + seconds;

							Message m = new Message();
							m.what = DarkroomTimer.TICK;
							DarkroomTimer.this.threadMessageHandler.sendMessage(m);
						}
					}).setNegativeButton(R.string.time_picker_cancel, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int whichButton) {
							// Just close. Don't need to do anything.
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
			int duration = preset.steps.get(currentStep).duration;

			NumberPicker minClock = (NumberPicker) dialog.findViewById(R.id.minuteClock);
			minClock.changeCurrent((int) duration / 60);

			NumberPicker secClock = (NumberPicker) dialog.findViewById(R.id.secondClock);
			secClock.changeCurrent((int) duration % 60);
		}
	}

	@Override
	public void onClick(View v) {
		if (preset.steps.size() >= (currentStep + 1)) {
			if (v.getId() == R.id.stepClock) {
				if (timerRunning() && pauseTime == 0) {
					showDialog(ADJUST_RUNNING_CLOCK);
				} else {
					showDialog(ADJUST_STOPPED_CLOCK);
				}
			}
		}
	}

	private void startTimer() {
		if (timerRunning()) {
			// Nothing happens if the timer is already running.
			// Also if preset == null (which is the case after the timer
			// finishes) noop.
		} else {
			if (pauseTime > 0 && startTime > 0) {
				startTime += System.currentTimeMillis() - pauseTime;
			} else if (startTime == 0) {
				startTime = System.currentTimeMillis();
			}
			pauseTime = 0;
			currentStepRunning = true;
			timerRunning(true);
			timer = new Thread(new TimerThread());
			timer.start();

		}
	}

	private void pauseTimer() {
		if (timerRunning()) {
			pauseTime = System.currentTimeMillis();
			stopThread();
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		stopThread();
	}

	private void stopThread() {
		if (timerRunning()) {
			timer.interrupt();
			timerRunning(false);
		}
	}

	private long stepTimeRemaining() {
		if (startTime == 0) {
			return (preset.steps.get(currentStep).pourFor + preset.steps.get(currentStep).duration) * 1000;
		} else {
			if (pauseTime > 0) {
				return startTime + (preset.steps.get(currentStep).pourFor + preset.steps.get(currentStep).duration) * 1000
						- pauseTime;
			} else {
				return startTime + (preset.steps.get(currentStep).pourFor + preset.steps.get(currentStep).duration) * 1000
						- System.currentTimeMillis();
			}
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

	private boolean timerRunning() {
		return timerRunning;
	}

	private void timerRunning(boolean flag) {
		timerRunning = flag;
	}

	@Override
	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
		if (isChecked) {
			startTimer();
		} else {
			pauseTimer();
		}
	}

}
