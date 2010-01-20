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
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
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

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.preseteditor);
		ListView lv = (ListView) findViewById(R.id.list);
		lv.setEmptyView(findViewById(R.id.empty));

		Intent intent = getIntent();
		uri = intent.getData();
		preset = new DarkroomPreset(this, uri);

		((TextView) findViewById(R.id.name)).setText(preset.name);
		Log.v(TAG, "We have a uri: " + uri);

		MyAdapter adapter = new MyAdapter(this, preset.steps);
		lv.setAdapter(adapter);
		lv.setOnItemClickListener(this);
		
		vf = (ViewFlipper) findViewById(R.id.details);

		// Set an animation from res/anim: I pick push left in
		vf.setAnimation(AnimationUtils.loadAnimation(this, android.R.anim.slide_in_left));
		
		Button saveBtn = (Button) findViewById(R.id.saveButton);
		Button cancelBtn = (Button) findViewById(R.id.discardButton);
		Button saveBtnEdit = (Button) findViewById(R.id.saveButtonEdit);
		Button cancelBtnEdit = (Button) findViewById(R.id.discardButtonEdit);
		cancelBtnEdit.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Log.v(TAG, "Cancel edit.");
				vf.showPrevious();
			}
		});

	}

	public void onItemClick(AdapterView parent, View v, int position, long id) {
		DarkroomStep selectedStep = preset.steps.get(position);
		Log.v(TAG, "List Item Clicked: preset=" + preset.name + ", step=" + selectedStep);
		// Get the ViewFlipper from the layout
		fillEditor(selectedStep);
		vf.showNext();
	}

	protected void fillEditor(DarkroomPreset.DarkroomStep step) {
		((EditText) findViewById(R.id.nameEdit)).setText(step.name);
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
