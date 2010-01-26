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
				selectedStep.name = ((EditText) findViewById(R.id.nameEdit)).getText().toString();
				// Resources r = getResources();
				// int dur = Integer.parseInt(((EditText)
				// findViewById(R.id.durationEdit)).getText().toString());
				// selectedStep.duration =
				// String.format(r.getString(R.id.durationEdit),)
				selectedStep.duration = Integer.parseInt(((EditText) findViewById(R.id.durationEdit)).getText().toString());
				selectedStep.agitateEvery = Integer.parseInt(((EditText) findViewById(R.id.agitateEdit)).getText()
						.toString());
				selectedStep.pourFor = Integer.parseInt(((EditText) findViewById(R.id.pourEdit)).getText().toString());
				selectedStep.promptBefore = ((EditText) findViewById(R.id.promptBeforeEdit)).getText().toString();
				if (selectedStep.fromBlank) {
					preset.addStep(selectedStep);
					adapter.notifyDataSetChanged();
				}
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
			case R.id.durationEdit:
				dialog = new AlertDialog.Builder(PresetEditor.this).setTitle(R.string.app_name).setMessage("Testing")
						.setCancelable(false).setPositiveButton(R.string.time_picker_ok,
								new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog, int whichButton) {
										// finish();
									}
								}).create();
				break;
			default:
				dialog = new AlertDialog.Builder(PresetEditor.this).setTitle(R.string.app_name).setMessage("UNDEFINED DIALOG")
				.setCancelable(false).setPositiveButton(R.string.time_picker_ok,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int whichButton) {
								// finish();
							}
						}).create();
		}
		return dialog;
	}

	static final List<Integer> dialogFields = Arrays.asList(new Integer[] { R.id.durationEdit, R.id.agitateEdit,
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
		Resources r = getResources();

		setField(R.id.nameEdit, selectedStep.name);
		setField(R.id.durationEdit, String.format(r.getString(R.string.durationEdit), selectedStep.duration / 60,
				selectedStep.duration % 60));
		setField(R.id.agitateEdit, String.format(r.getString(R.string.agitateEdit), selectedStep.agitateEvery));
		setField(R.id.pourEdit, String.format(r.getString(R.string.pourEdit), selectedStep.pourFor));
		setField(R.id.promptBeforeEdit, String.format(r.getString(R.string.promptBeforeEdit), selectedStep.promptBefore));
		vf.showNext();
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
