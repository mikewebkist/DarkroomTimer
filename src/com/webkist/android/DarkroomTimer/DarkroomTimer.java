package com.webkist.android.DarkroomTimer;

import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
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
		
	private TextView timerText;
	private TextView stepActionText;
	private TextView clickText;
	private TextView stepHead;

	private long startTime = 0;
	
	private DarkroomPreset preset = null;
	private DarkroomPreset.DarkroomStep step;

	private Thread timer = null;
	protected PowerManager.WakeLock mWakeLock = null;
	private boolean timerRunning = false;

	Handler threadMessageHandler = new Handler() {
		// TODO Move all this to an onDraw() method?
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case TICK:
				long remaining = stepTimeRemaining();
				
				int minutes = (int) remaining / 60000;
				int seconds = (int) ((remaining % 60000) / 1000);

				timerText.setText(String.format("%02d:%02d", minutes, seconds));
				double elapsedSecs = (System.currentTimeMillis() - startTime) / 1000;

				if(step.pourFor > 0 && elapsedSecs < step.pourFor) {
					clickText.setText(R.string.prompt_pour);
					stepActionText.setText("");
				} else if(step.agitateEvery > 0) {
					if(elapsedSecs < (step.pourFor + 5 + step.agitateFor)) {
						clickText.setText(R.string.prompt_agitate);
						stepActionText.setText("");
					} else {
						double elapsedRemainder = elapsedSecs % step.agitateEvery;
						
						if(elapsedRemainder < step.agitateFor) {
							// Currently agitating
							stepActionText.setText("");
							clickText.setText(R.string.prompt_agitate);
						} else if(elapsedRemainder > step.agitateEvery - 10) {
							// Coming up on agitation
							double agitateIn = step.agitateEvery - elapsedRemainder;
							stepActionText.setText(String.format("%s in %02d:%02d", 
									R.string.prompt_agitate, 
									(int) agitateIn / 60, 
									(int) agitateIn % 60));
						} else {
							stepActionText.setText("");
							clickText.setText("");
						}
					}
				} else {
					clickText.setText("");
					stepActionText.setText("");
				}
		
				break;
			case NEXT:
				step = preset.nextStep();
				if(step == null) {
					stepHead.setText(R.string.prompt_done);
					timerText.setText("--:--");
				} else {
					stepHead.setText(step.name);
					timerText.setText(String.format("%02d:%02d", (int) step.duration / 60, (int) step.duration % 60));

					if(step.promptBefore == null) {
						handleClick();
					} else {
						clickText.setText(step.promptBefore);

					}
				}

				break;
				
			case DONE:
				timerRunning = false;
				startTime = 0;
				clickText.setText("");
				stepActionText.setText("");

				// TODO promptAfter
								
				Message m = new Message();
				m.what = DarkroomTimer.NEXT;
				DarkroomTimer.this.threadMessageHandler.sendMessage(m);
				
				break;
			}
		}
	};
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		LinearLayout mainView = (LinearLayout) findViewById(R.id.mainLayout);
		mainView.setOnClickListener(this);
		
		if(preset == null) {
			Intent intent = new Intent(this, TimerPicker.class);
			startActivityForResult(intent, GET_PRESET);
		}
		
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
			if(this.mWakeLock != null) { 
				this.mWakeLock.release();
				this.mWakeLock = null;
			}
	        return true;
	    case R.id.select_preset:
	    	Intent intent = new Intent(this, TimerPicker.class);
			startActivityForResult(intent, GET_PRESET);
	        return true;
	    }
	    return false;
	}
	
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if(requestCode == GET_PRESET) {
			if(resultCode == RESULT_OK) {
				timerRunning = false;
				startTime = 0;
		    	stopThread();
		    	step=null;

				int index = data.getExtras().getInt("com.webkist.android.DarkroomTimer.DarkroomPreset");
				preset = TimerPicker.getPreset(index);
				preset.reset();

				TextView header = (TextView) findViewById(R.id.presetName);
				header.setText(preset.name);

				timerText = (TextView) findViewById(R.id.stepClock);
				timerText.setOnClickListener(this);
				stepActionText = (TextView) findViewById(R.id.stepActionText);
				clickText = (TextView) findViewById(R.id.clickText);
				stepHead = (TextView) findViewById(R.id.stepLabel);
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
		if(id == FAILED_PRESET_PICK) {
			return new AlertDialog.Builder(DarkroomTimer.this)
			.setTitle(R.string.app_name)
			.setMessage(R.string.preset_pick_failed)
			.setCancelable(false)
			.setPositiveButton(R.string.time_picker_ok, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					finish();
				}
			})
			.create();
		} else if (id == ADJUST_RUNNING_CLOCK) {
			final View timePickerView = factory.inflate(R.layout.addseconds, null);

			NumberPicker addClock = (NumberPicker) timePickerView.findViewById(R.id.addSeconds);
			addClock.setFormatter(NumberPicker.TWO_DIGIT_FORMATTER);
			addClock.setRange(-360, 360);
			addClock.setSpeed(100);
			addClock.setIncBy(5);
			addClock.setEnabled(true);

			return new AlertDialog.Builder(DarkroomTimer.this)
			.setTitle(R.string.time_picker_title)
			.setView(timePickerView)
			.setPositiveButton(R.string.time_picker_ok, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					NumberPicker addClock = (NumberPicker) timePickerView.findViewById(R.id.addSeconds);

					int addseconds = addClock.getCurrent();

					step.duration += addseconds;
					Message m = new Message();
					m.what = DarkroomTimer.TICK;
					DarkroomTimer.this.threadMessageHandler.sendMessage(m);

				}
			})
			.setNegativeButton(R.string.time_picker_cancel, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					Log.v(TAG, "in onCreateDialog, clicked Cancel");
				}
			})
			.create();
		} else if(id == ADJUST_STOPPED_CLOCK) {

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

			return new AlertDialog.Builder(DarkroomTimer.this)
			.setTitle(R.string.time_picker_title)
			.setView(timePickerView)
			.setPositiveButton(R.string.time_picker_ok, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					NumberPicker minClock = (NumberPicker) timePickerView.findViewById(R.id.minuteClock);
					NumberPicker secClock = (NumberPicker) timePickerView.findViewById(R.id.secondClock);

					int minutes = minClock.getCurrent();
					int seconds = secClock.getCurrent();

					step.duration = minutes * 60 + seconds;
					Message m = new Message();
					m.what = DarkroomTimer.TICK;
					DarkroomTimer.this.threadMessageHandler.sendMessage(m);
				}
			})
			.setNegativeButton(R.string.time_picker_cancel, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					Log.v(TAG, "in onCreateDialog, clicked Cancel");
				}
			})
			.create();
		}

		return null;
	}

	public void onPrepareDialog(int id, Dialog dialog) {
		if(id == ADJUST_RUNNING_CLOCK) {
			NumberPicker addClock = (NumberPicker) dialog.findViewById(R.id.addSeconds);
			addClock.changeCurrent(0);
		} else if(id == ADJUST_STOPPED_CLOCK) {
			int minutes = (int) step.duration / 60;
			int seconds = (int) step.duration % 60;

			NumberPicker minClock = (NumberPicker) dialog.findViewById(R.id.minuteClock);
			minClock.changeCurrent(minutes);

			NumberPicker secClock = (NumberPicker) dialog.findViewById(R.id.secondClock);
			secClock.changeCurrent(seconds);
		}
	}

	@Override
	public void onClick(View v) {
		if(v.getId() == R.id.stepClock) {
			Log.v(TAG, "We have a clock click.");
			if(timerRunning) {
				showDialog(ADJUST_RUNNING_CLOCK);
			} else {
				showDialog(ADJUST_STOPPED_CLOCK);
			}
		} else {
			Log.v(TAG, "In onClick: " + v.getId());
			handleClick();
		}
	}
	
	private void handleClick() {
		if(timerRunning) {
			Log.v(TAG, "Timer already running!");
		} else {
			startTime = System.currentTimeMillis();
			startThread();
		}
	}
	
	@Override
	public void onResume() {
		super.onResume();
		if(preset != null) {
			// TODO: make this only happen while timer is running, perhaps?
			final PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
			this.mWakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "My Tag");
			this.mWakeLock.acquire();
			Log.v(TAG, "in onResume()");
			if(stepTimeRemaining() > 0) {
				startThread();
			} else {
				Message m = new Message();					
				m.what = DarkroomTimer.DONE;
				DarkroomTimer.this.threadMessageHandler.sendMessage(m);
			}
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		Log.v(TAG, "in onPause()");
		stopThread();
		// TODO We don't actually run in the background at this point, so if
		// someone starts another Activity we won't alert them when the timer
		// expires. 
		if(this.mWakeLock != null) { 
			this.mWakeLock.release();
			this.mWakeLock = null;
		}
	}	

	@Override
	public void onStop() {
		super.onStop();
		Log.v(TAG, "in onStop()");
	}
	

	private void startThread() {
		clickText.setText("");
		timerRunning = true;
		timer = new Thread(new TimerThread());
		timer.start();
	}
	
	private void stopThread() {
		if(timerRunning) {
			timer.interrupt();
			timerRunning = false;
		}
	}

	private long stepTimeRemaining() {
		if(step == null) {
			return 0;
		} else if(startTime == 0) {
			return step.duration * 1000;
		} else {
			return startTime + step.duration * 1000 - System.currentTimeMillis();
		}
	}
	
	class TimerThread implements Runnable {
		public void run() {
			while(!Thread.currentThread().isInterrupted()) {
				if(DarkroomTimer.this.stepTimeRemaining() <= 0) {
					Message m = new Message();					
					m.what = DarkroomTimer.DONE;
					DarkroomTimer.this.threadMessageHandler.sendMessage(m);
					
					break;
				} else {
					Message m = new Message();
					m.what = DarkroomTimer.TICK;
					DarkroomTimer.this.threadMessageHandler.sendMessage(m);

					try {
						Thread.sleep(500);
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
					}
				}
			}
		}
	}

}
