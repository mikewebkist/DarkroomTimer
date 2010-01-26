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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import com.webkist.android.DarkroomTimer.DarkroomPreset.DarkroomStep;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.ViewFlipper;
import android.widget.AdapterView.OnItemClickListener;

public class PresetEditor extends Activity implements OnItemClickListener {
	public static final String TAG = "PresetEditor";
	private Uri uri;
	private DarkroomPreset preset;
	private ViewFlipper vf;
	private DarkroomStep selectedStep;
	private MyAdapter adapter;
	private DarkroomStep modifiedStep;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.preseteditor);
		ListView lv = (ListView) findViewById(R.id.list);

		Intent intent = getIntent();
		uri = intent.getData();

		if (uri == null) {
			preset = new DarkroomPreset();
		} else {
			Log.v(TAG, "We have a uri: " + uri);
			preset = new DarkroomPreset(this, uri);
		}

		((TextView) findViewById(R.id.name)).setText(preset.name);

		adapter = new MyAdapter(this, preset.steps);
		LinearLayout v = (LinearLayout) getLayoutInflater().inflate(android.R.layout.two_line_list_item, lv, false);
		TextView t = (TextView) v.findViewById(android.R.id.text1);
		t.setText("Add a step...");
		lv.addFooterView(v);
		lv.setFooterDividersEnabled(true);
		lv.setAdapter(adapter);
		lv.setOnItemClickListener(this);
		vf = (ViewFlipper) findViewById(R.id.details);
		vf.setAnimation(AnimationUtils.loadAnimation(this, android.R.anim.slide_in_left));

		// Save entire preset.
		Button saveBtn = (Button) findViewById(R.id.saveButton);
		saveBtn.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				ContentValues vals = new ContentValues();
				ContentResolver cr = getContentResolver();
				Log.v(TAG, "Deleting: " + preset.uri);
				if (uri != null) {
					cr.delete(uri, null, null);
				}
				preset.name = ((TextView) findViewById(R.id.name)).getText().toString();
				vals.put(DarkroomPreset.PRESET_NAME, preset.name);
				Uri newUri = cr.insert(DarkroomPreset.CONTENT_URI_PRESET, vals);
				String presetId = newUri.getPathSegments().get(1);
				for (int j = 0; j < preset.steps.size(); j++) {
					cr.insert(Uri.withAppendedPath(newUri, "step"), preset.steps.get(j).toContentValues(presetId));
				}
				Log.v(TAG, "Inserted " + newUri);
				finish();
			}
		});

		// Cancel entire preset.
		Button cancelBtn = (Button) findViewById(R.id.discardButton);
		cancelBtn.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				finish();
			}
		});

		// Remember step (but don't write it out until we save the preset).
		Button saveBtnEdit = (Button) findViewById(R.id.saveButtonEdit);
		saveBtnEdit.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Log.v(TAG, "Save edit.");
				selectedStep.overwrite(modifiedStep);

				if (selectedStep.fromBlank) {
					preset.addStep(selectedStep);
				}

				adapter.notifyDataSetChanged();
				vf.showPrevious();
			}
		});

		// Cancel step.
		Button cancelBtnEdit = (Button) findViewById(R.id.discardButtonEdit);
		cancelBtnEdit.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Log.v(TAG, "Cancel edit.");
				vf.showPrevious();
			}
		});

	}

	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0 && vf.getDisplayedChild() == 1) {
			// Hijack BACK only if we're on the second view.
			vf.showPrevious();
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	protected Dialog onCreateDialog(int id) {
		Dialog dialog;
		LayoutInflater factory = LayoutInflater.from(this);
		switch (id) {
			case R.id.nameEdit:
				final View nameEditView = factory.inflate(R.layout.text_edit_dialog, null);

				dialog = new AlertDialog.Builder(PresetEditor.this).setTitle("Step name").setView(
						nameEditView).setPositiveButton(R.string.time_picker_ok, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						EditText nameText = (EditText) nameEditView.findViewById(R.id.edit);
						modifiedStep.name = nameText.getText().toString();
						updateFields();
					}
				}).setNegativeButton(R.string.time_picker_cancel, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						Log.v(TAG, "in onCreateDialog, clicked Cancel");
					}
				}).create();
				break;
				
			case R.id.durationEdit:
				final View durationEditView = factory.inflate(R.layout.time_picker, null);

				NumberPicker minClock = (NumberPicker) durationEditView.findViewById(R.id.minuteClock);
				minClock.setFormatter(NumberPicker.TWO_DIGIT_FORMATTER);
				minClock.setRange(0, 60);
				minClock.setSpeed(100);
				minClock.setEnabled(true);

				NumberPicker secClock = (NumberPicker) durationEditView.findViewById(R.id.secondClock);
				secClock.setFormatter(NumberPicker.TWO_DIGIT_FORMATTER);
				secClock.setRange(0, 59);
				secClock.setSpeed(100);
				secClock.setEnabled(true);

				dialog = new AlertDialog.Builder(PresetEditor.this).setTitle("Step duration (MM:SS)").setView(
						durationEditView).setPositiveButton(R.string.time_picker_ok, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						NumberPicker minClock = (NumberPicker) durationEditView.findViewById(R.id.minuteClock);
						NumberPicker secClock = (NumberPicker) durationEditView.findViewById(R.id.secondClock);

						int minutes = minClock.getCurrent();
						int seconds = secClock.getCurrent();
						modifiedStep.duration = minutes * 60 + seconds;
						updateFields();
					}
				}).setNegativeButton(R.string.time_picker_cancel, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						Log.v(TAG, "in onCreateDialog, clicked Cancel");
					}
				}).create();
				break;

			case R.id.agitateEdit:
				final View agitateEditView = factory.inflate(R.layout.addseconds, null);

				NumberPicker addClock = (NumberPicker) agitateEditView.findViewById(R.id.addSeconds);
				addClock.setFormatter(NumberPicker.TWO_DIGIT_FORMATTER);
				addClock.setRange(0, 360);
				addClock.setSpeed(100);
				addClock.setIncBy(5);
				addClock.setEnabled(true);

				dialog = new AlertDialog.Builder(PresetEditor.this).setTitle("Agitate every (seconds)").setView(
						agitateEditView).setPositiveButton(R.string.time_picker_ok, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						NumberPicker addClock = (NumberPicker) agitateEditView.findViewById(R.id.addSeconds);
						modifiedStep.agitateEvery = addClock.getCurrent();
						updateFields();
					}
				}).setNegativeButton(R.string.time_picker_cancel, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						Log.v(TAG, "in onCreateDialog, clicked Cancel");
					}
				}).create();
				break;
				
			case R.id.pourEdit:
				final View pourEditView = factory.inflate(R.layout.addseconds, null);

				NumberPicker pourClock = (NumberPicker) pourEditView.findViewById(R.id.addSeconds);
				pourClock.setFormatter(NumberPicker.TWO_DIGIT_FORMATTER);
				pourClock.setRange(0, 360);
				pourClock.setSpeed(100);
				pourClock.setIncBy(5);
				pourClock.setEnabled(true);

				dialog = new AlertDialog.Builder(PresetEditor.this).setTitle("Pour for (seconds)").setView(
						pourEditView).setPositiveButton(R.string.time_picker_ok, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						NumberPicker pourClock = (NumberPicker) pourEditView.findViewById(R.id.addSeconds);
						modifiedStep.pourFor = pourClock.getCurrent();
						updateFields();
					}
				}).setNegativeButton(R.string.time_picker_cancel, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						Log.v(TAG, "in onCreateDialog, clicked Cancel");
					}
				}).create();
				break;

			case R.id.promptBeforeEdit:
				final View promptEditView = factory.inflate(R.layout.text_edit_dialog, null);

				dialog = new AlertDialog.Builder(PresetEditor.this).setTitle("Pour for (seconds)").setView(
						promptEditView).setPositiveButton(R.string.time_picker_ok, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						EditText promptText = (EditText) promptEditView.findViewById(R.id.edit);
						modifiedStep.promptBefore = promptText.getText().toString();
						updateFields();
					}
				}).setNegativeButton(R.string.time_picker_cancel, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						Log.v(TAG, "in onCreateDialog, clicked Cancel");
					}
				}).create();
				break;
				
			default:
				dialog = new AlertDialog.Builder(PresetEditor.this).setTitle(R.string.app_name).setMessage(
						"UNDEFINED DIALOG").setCancelable(false).setPositiveButton(R.string.time_picker_ok,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int whichButton) {
								// finish();
							}
						}).create();
		}
		return dialog;
	}

	public void onPrepareDialog(int id, Dialog dialog) {
		switch (id) {
			case R.id.nameEdit:
				EditText nameText = (EditText) dialog.findViewById(R.id.edit);
				nameText.setText(modifiedStep.name);
				break;
			case R.id.durationEdit:
				NumberPicker minClock = (NumberPicker) dialog.findViewById(R.id.minuteClock);
				minClock.changeCurrent(modifiedStep.duration / 60);

				NumberPicker secClock = (NumberPicker) dialog.findViewById(R.id.secondClock);
				secClock.changeCurrent(modifiedStep.duration % 60);
				break;
			case R.id.agitateEdit:
				NumberPicker addClock = (NumberPicker) dialog.findViewById(R.id.addSeconds);
				addClock.changeCurrent(modifiedStep.agitateEvery);
				break;
			case R.id.pourEdit:
				NumberPicker pourClock = (NumberPicker) dialog.findViewById(R.id.addSeconds);
				pourClock.changeCurrent(modifiedStep.pourFor);
				break;
			case R.id.promptBeforeEdit:
				EditText promptText = (EditText) dialog.findViewById(R.id.edit);
				promptText.setText(modifiedStep.promptBefore);
				break;
		}
	}

	static final List<Integer> dialogFields = Arrays.asList(new Integer[] { R.id.nameEdit, R.id.durationEdit, R.id.agitateEdit,
			R.id.pourEdit, R.id.promptBeforeEdit });

	private void setField(int id, String val) {
		TextView v = (TextView) findViewById(id);
		v.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (dialogFields.contains(v.getId())) {
					showDialog(v.getId());
				}
			}

		});
		v.setText(val);
	}

	public void onItemClick(AdapterView parent, View v, int position, long id) {
		if (id == -1) {
			selectedStep = preset.blankStep();
			Log.v(TAG, "Add step clicked: id=" + id + ", position=" + position);
		} else {
			selectedStep = (DarkroomPreset.DarkroomStep) parent.getItemAtPosition(position);
			Log.v(TAG, "List Item Clicked: preset=" + preset.name + ", step=" + selectedStep);
		}
		modifiedStep = selectedStep.clone();
		updateFields();
		vf.showNext();
	}

	private void updateFields() {
		Resources r = getResources();
		setField(R.id.nameEdit, String.format(r.getString(R.string.nameEdit), modifiedStep.name));
		setField(R.id.durationEdit, String.format(r.getString(R.string.durationEdit), modifiedStep.duration / 60,
				modifiedStep.duration % 60));
		setField(R.id.agitateEdit, String.format(r.getString(R.string.agitateEdit), modifiedStep.agitateEvery));
		setField(R.id.pourEdit, String.format(r.getString(R.string.pourEdit), modifiedStep.pourFor));
		setField(R.id.promptBeforeEdit, String.format(r.getString(R.string.promptBeforeEdit), modifiedStep.promptBefore));
	}

	private class MyAdapter extends ArrayAdapter<DarkroomPreset.DarkroomStep> {

		public MyAdapter(Context context, ArrayList<DarkroomPreset.DarkroomStep> steps) {
			super(context, 0, steps);
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			DarkroomPreset.DarkroomStep step = preset.steps.get(position);

			if (convertView == null) {
				convertView = getLayoutInflater().inflate(android.R.layout.two_line_list_item, parent, false);
			}

			String name = String.format("%s for %d:%02d", step.name, step.duration / 60, step.duration % 60);
			((TextView) convertView.findViewById(android.R.id.text1)).setText(name);

			String details = "";
			if (step.pourFor > 0) {
				details += String.format("pour: %ds", step.pourFor);
			}
			if (step.agitateEvery > 0) {
				details += step.pourFor > 0 ? ", " : "";
				details += String.format("agitate: %ds", step.agitateEvery);
			}
			TextView tv = (TextView) convertView.findViewById(android.R.id.text2);
			tv.setGravity(Gravity.RIGHT);
			tv.setText(details);

			return convertView;
		}
	}

}
