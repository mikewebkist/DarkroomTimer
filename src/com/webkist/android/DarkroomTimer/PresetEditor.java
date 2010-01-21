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
import java.util.List;

import com.webkist.android.DarkroomTimer.DarkroomPreset.DarkroomStep;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
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
import android.widget.SimpleAdapter;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.ViewFlipper;
import android.widget.AdapterView.OnItemClickListener;

public class PresetEditor extends Activity implements OnItemClickListener {
	public static final String TAG = "PresetEditor";
	private static final int EDIT_STEP = 0;
	private Uri uri;
	private DarkroomPreset preset;
	private ViewFlipper vf;
	private DarkroomStep selectedStep;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.preseteditor);
		ListView lv = (ListView) findViewById(R.id.list);
		lv.setEmptyView(findViewById(R.id.empty));

		Intent intent = getIntent();
		uri = intent.getData();

		if (uri == null) {
			preset = new DarkroomPreset("New Preset");
		} else {
			Log.v(TAG, "We have a uri: " + uri);
			preset = new DarkroomPreset(this, uri);
		}

		((TextView) findViewById(R.id.name)).setText(preset.name);

		MyAdapter adapter = new MyAdapter(this, preset.steps);
		TextView t = new TextView(this);
//		LinearLayout v = (LinearLayout) getLayoutInflater().inflate(android.R.layout.simple_list_item_1, lv, false);
		t.setText("Add a step...");
//		((TextView) v.findViewById(android.R.id.text1)).setText("Add a step...");
		lv.addFooterView(t);
		lv.setAdapter(adapter);
		lv.setOnItemClickListener(this);

		vf = (ViewFlipper) findViewById(R.id.details);

		// Set an animation from res/anim: I pick push left in
		vf.setAnimation(AnimationUtils.loadAnimation(this, android.R.anim.slide_in_left));

		Button saveBtn = (Button) findViewById(R.id.saveButton);
		saveBtn.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				ContentValues vals = new ContentValues();
				ContentResolver cr = getContentResolver();
				Log.v(TAG, "Deleting: " + preset.uri);
				if(uri != null) {
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

		Button cancelBtn = (Button) findViewById(R.id.discardButton);
		cancelBtn.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				finish();
			}
		});
		Button saveBtnEdit = (Button) findViewById(R.id.saveButtonEdit);
		saveBtnEdit.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Log.v(TAG, "Save edit.");
				selectedStep.name = ((EditText) findViewById(R.id.nameEdit)).getText().toString();
				selectedStep.duration = Integer.parseInt(((EditText) findViewById(R.id.durationEdit)).getText().toString());
				selectedStep.agitateEvery = Integer.parseInt(((EditText) findViewById(R.id.agitateEdit)).getText()
						.toString());
				selectedStep.pourFor = Integer.parseInt(((EditText) findViewById(R.id.pourEdit)).getText().toString());
				selectedStep.promptBefore = ((EditText) findViewById(R.id.promptBeforeEdit)).getText().toString();
				vf.showPrevious();
			}
		});
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

	public void onItemClick(AdapterView parent, View v, int position, long id) {
		selectedStep = preset.steps.get(position);
		Log.v(TAG, "List Item Clicked: preset=" + preset.name + ", step=" + selectedStep);
		// Get the ViewFlipper from the layout
		fillEditor(selectedStep);
		vf.showNext();
	}

	protected void fillEditor(DarkroomPreset.DarkroomStep step) {
		((EditText) findViewById(R.id.nameEdit)).setText(step.name);
		((EditText) findViewById(R.id.durationEdit)).setText(String.format("%d", step.duration));
		((EditText) findViewById(R.id.agitateEdit)).setText(String.format("%d", step.agitateEvery));
		((EditText) findViewById(R.id.pourEdit)).setText(String.format("%d", step.pourFor));
		((EditText) findViewById(R.id.promptBeforeEdit)).setText(step.promptBefore);
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
