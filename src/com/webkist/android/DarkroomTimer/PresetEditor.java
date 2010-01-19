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

import android.app.Activity;
import android.app.ListActivity;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

public class PresetEditor extends ListActivity {
	public static final String TAG = "PresetEditor";
	private Uri uri;
	private DarkroomPreset preset;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.preseteditor);
//		getListView().setEmptyView(findViewById(R.id.empty));
		
		Intent intent = getIntent();
		uri = intent.getData();
		preset = new DarkroomPreset(this, uri);
		
		((TextView) findViewById(R.id.name)).setText(preset.name);
		Log.v(TAG, "We have a uri: " + uri);
		
		ArrayAdapter<DarkroomPreset.DarkroomStep> adapter = new MyAdapter(this, preset.steps);
		getListView().setAdapter(adapter);
	}

	public void onListItemClick(ListView l, View v, int position, long id) {
		Log.v(TAG, "List Item Clicked: preset=" + preset.name + ", step=" + id);
	}

	private class MyAdapter extends ArrayAdapter<DarkroomPreset.DarkroomStep> {

		public MyAdapter(Context context, ArrayList<DarkroomPreset.DarkroomStep> steps) {
			super(context, 0, steps);
		}
		
		public View getView(int position, View convertView, ViewGroup parent) {		
			DarkroomPreset.DarkroomStep step = preset.steps.get(position);
			
//			if(step.pourFor == 0 && step.agitateEvery == 0) {
//				
//			}
			
			if(convertView == null) {
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
