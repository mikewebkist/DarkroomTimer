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

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

public class PresetEditor extends Activity implements OnClickListener {
	public static final String TAG = "PresetEditor";
	private Uri uri;
	// private DarkroomPreset preset;
	private Cursor listViewCursor;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.preseteditor);

		Intent intent = getIntent();
		uri = intent.getData();
		// preset = new DarkroomPreset(this, uri);
		Log.v(TAG, "We have a uri: " + uri);
		ListView listView = (ListView) findViewById(R.id.list);
		listViewCursor = managedQuery(Uri.withAppendedPath(uri, "step"), null, null, null,
				DarkroomPreset.DarkroomStep.STEP_NAME);
		SimpleCursorAdapter adapter = new SimpleCursorAdapter(this, android.R.layout.simple_list_item_1, listViewCursor,
				new String[] { DarkroomPreset.DarkroomStep.STEP_NAME }, new int[] { android.R.id.text1 });
		listView.setAdapter(adapter);
	}

	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub

	}

}
