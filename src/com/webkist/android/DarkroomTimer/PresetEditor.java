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

import com.webkist.android.DarkroomTimer.DarkroomPreset.DarkroomStep;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
import android.widget.AdapterView.OnItemClickListener;

public class PresetEditor extends Activity implements OnItemClickListener {
	public static final String TAG = "PresetEditor";
	private static final int EDIT_STEP = 1;

	private Uri uri;
	private DarkroomPreset preset;
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
			preset = new DarkroomPreset(this, uri);
		}

		((TextView) findViewById(R.id.name)).setText(preset.name);

		Spinner spinner = (Spinner) findViewById(R.id.isoSpinner);
		ArrayAdapter<CharSequence> spinnerAdapter = ArrayAdapter.createFromResource(this, R.array.iso_values,
				android.R.layout.simple_spinner_item);
		spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner.setAdapter(spinnerAdapter);

		if (preset.iso > 0) {
			spinner.setSelection(spinnerAdapter.getPosition(String.format("%d", preset.iso)));
		}

		EditText tempEdit = (EditText) findViewById(R.id.tempEdit);
		if (preset.temp != null && preset.temp.length() > 2) {
			tempEdit.setText(preset.temp.substring(0, preset.temp.length() - 1));
			ToggleButton tempToggle = (ToggleButton) findViewById(R.id.tempToggle);
			tempToggle.setChecked(preset.temp.endsWith("F"));
		} else {
			tempEdit.setText("");
		}

		adapter = new MyAdapter(this, preset.steps);
		LinearLayout v = (LinearLayout) getLayoutInflater().inflate(android.R.layout.two_line_list_item, lv, false);
		TextView t = (TextView) v.findViewById(android.R.id.text1);
		t.setText("Add a step...");
		lv.addFooterView(v);
		lv.setFooterDividersEnabled(true);
		lv.setAdapter(adapter);
		lv.setOnItemClickListener(this);

		// Save entire preset.
		Button saveBtn = (Button) findViewById(R.id.saveButton);
		saveBtn.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if (preset.steps.size() == 0) {
					Toast.makeText(getBaseContext(), "You can't create an empty preset!", Toast.LENGTH_LONG).show();
				} else {
					EditText tempEdit = (EditText) findViewById(R.id.tempEdit);
					String temp = tempEdit.getText().toString();

					if (temp.length() > 0) {
						ToggleButton tempToggle = (ToggleButton) findViewById(R.id.tempToggle);
						if (tempToggle.isChecked()) {
							temp += "F";
						} else {
							temp += "C";
						}
						preset.temp = temp;
					} else {
						preset.temp = "";
					}

					Spinner spinner = (Spinner) findViewById(R.id.isoSpinner);
					if (spinner.getSelectedItemPosition() > 0) {
						preset.iso = Integer.parseInt(spinner.getSelectedItem().toString());
					} else {
						preset.iso = 0;
					}

					ContentResolver cr = getContentResolver();
					preset.name = ((TextView) findViewById(R.id.name)).getText().toString();
					
					// TODO: We need to actually use cr.update() here. Otherwise the 
					// _ID values change every time a preset is edited, making shortcuts
					// stop working.
					if (uri != null) {
						cr.delete(uri, null, null);
					}
					Uri newUri = cr.insert(DarkroomPreset.CONTENT_URI_PRESET, preset.toContentValues());
					
					// TODO: This will still have to delete/insert to keep the steps
					// in the right order, etc.
					String presetId = newUri.getPathSegments().get(1);
					for (int j = 0; j < preset.steps.size(); j++) {
						cr.insert(Uri.withAppendedPath(newUri, "step"), preset.steps.get(j).toContentValues(presetId));
					}
					finish();
				}
			}
		});

		// Cancel entire preset.
		Button cancelBtn = (Button) findViewById(R.id.discardButton);
		cancelBtn.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				finish();
			}
		});

	}

	protected Dialog onCreateDialog(int id) {
		Dialog dialog = null;
		LayoutInflater factory = LayoutInflater.from(this);
		switch (id) {
			case EDIT_STEP:
				final View v = factory.inflate(R.layout.editstep, null);

				dialog = new AlertDialog.Builder(PresetEditor.this).setTitle("Edit Step").setView(v).setPositiveButton(
						R.string.time_picker_ok, new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int whichButton) {
								modifiedStep.name = ((EditText) v.findViewById(R.id.nameEdit)).getText().toString();
								
								String newDuration = ((EditText) v.findViewById(R.id.durationEdit)).getText().toString();
								int colon = newDuration.indexOf(":");
								int minutes = 0;
								int seconds = 0;
								try {
									if (colon == -1) {
										// MM
										minutes = Integer.parseInt(newDuration);
									} else if (colon == 0) {
										// :SS
										seconds = Integer.parseInt(newDuration.substring(1));
									} else {
										// MM:SS
										minutes = Integer.parseInt(newDuration.substring(0, colon));
										seconds = Integer.parseInt(newDuration.substring(colon + 1));
									}
								} catch (NumberFormatException e) {
									Log.w(TAG, "Problem with duration \"" + newDuration + "\": " + e);
								}
								modifiedStep.duration = minutes * 60 + seconds;
								
								try {
									modifiedStep.agitateEvery = Integer.parseInt(((EditText) v
											.findViewById(R.id.agitateEdit)).getText().toString());
								} catch (NumberFormatException e) {
									modifiedStep.agitateEvery = 0;
								}
								
								CheckBox cb = (CheckBox) v.findViewById(R.id.pourCheck);
								if (cb.isChecked()) {
									modifiedStep.pourFor = 10;
								}
								
								selectedStep.overwrite(modifiedStep);
								adapter.notifyDataSetChanged();
							}
						}).setNegativeButton(R.string.time_picker_cancel, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						// Do nothing on cancel.
					}
				}).setNeutralButton("Delete", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						preset.steps.remove(selectedStep);
						adapter.notifyDataSetChanged();
					}
				}).create();
				break;

			default:
				Log.e(TAG, "Asked to create an unexpected dialog: " + id);
		}
		return dialog;
	}

	public void onPrepareDialog(int id, Dialog dialog) {
		switch (id) {
			case EDIT_STEP:
				((EditText) dialog.findViewById(R.id.nameEdit)).setText(modifiedStep.name);
				((EditText) dialog.findViewById(R.id.nameEdit)).clearFocus();

				((EditText) dialog.findViewById(R.id.durationEdit)).setText(String.format("%d:%02d",
						modifiedStep.duration / 60, modifiedStep.duration % 60));
				((EditText) dialog.findViewById(R.id.durationEdit)).clearFocus();

				((EditText) dialog.findViewById(R.id.agitateEdit)).setText(String.format("%d", modifiedStep.agitateEvery));
				((EditText) dialog.findViewById(R.id.agitateEdit)).clearFocus();

				CheckBox cb = (CheckBox) dialog.findViewById(R.id.pourCheck);
				if (modifiedStep.pourFor > 0) {
					cb.setChecked(true);
				} else {
					cb.setChecked(false);
				}
				break;
		}
	}

	@SuppressWarnings("unchecked")
	public void onItemClick(AdapterView parent, View v, int position, long id) {
		if (id == -1) {
			selectedStep = preset.blankStep();
			preset.addStep(selectedStep);
		} else {
			selectedStep = (DarkroomPreset.DarkroomStep) parent.getItemAtPosition(position);
		}
		modifiedStep = selectedStep.clone();
		showDialog(EDIT_STEP);
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
			if (step.pourFor > 0) {
				name = String.format("%s + %d sec pour", name, step.pourFor);
			}
			((TextView) convertView.findViewById(android.R.id.text1)).setText(name);

			String details = step.agitateEvery > 0 ? String.format("agitate: %ds", step.agitateEvery) : "";

			TextView tv = (TextView) convertView.findViewById(android.R.id.text2);
			tv.setGravity(Gravity.RIGHT);
			tv.setText(details);

			return convertView;
		}
	}
}
