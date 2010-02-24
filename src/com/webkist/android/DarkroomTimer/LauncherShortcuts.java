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

import android.app.ListActivity;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class LauncherShortcuts extends ListActivity {
	
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);

		// Resolve the intent

		final Intent intent = getIntent();
		final String action = intent.getAction();

		// Inflate our UI from its XML layout description.
		Cursor listViewCursor = managedQuery(DarkroomPreset.CONTENT_URI_PRESET, null, null, null, DarkroomPreset.PRESET_NAME);
		MyOtherAdapter adapter = new MyOtherAdapter(this, listViewCursor);
		setListAdapter(adapter);
	}
	
	public void onListItemClick(ListView l, View v, int position, long id) {
		Uri uri = ContentUris.withAppendedId(DarkroomPreset.CONTENT_URI_PRESET, id);
		Intent shortcutIntent = new Intent(this, DarkroomTimer.class);
		shortcutIntent.setData(uri);
		
		Intent intent = new Intent();
		intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
		DarkroomPreset preset = new DarkroomPreset(this, uri);
		intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, preset.name);
		Parcelable iconResource = Intent.ShortcutIconResource.fromContext(this, R.drawable.darkroomtimer);
		intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, iconResource);

		// Now, return the result to the launcher

		setResult(RESULT_OK, intent);
		finish();
	}
	
	private class MyOtherAdapter extends CursorAdapter {

		public MyOtherAdapter(Context context, Cursor c) {
			super(context, c);
		}

		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent) {
			final LayoutInflater inflater = getLayoutInflater();
			View view = inflater.inflate(R.layout.presetlist, parent, false);
			return view;
		}

		@Override
		public void bindView(View view, Context context, Cursor cursor) {
			((TextView) view.findViewById(R.id.name)).setText(cursor.getString(cursor
					.getColumnIndex(DarkroomPreset.PRESET_NAME)));
			int iso = cursor.getInt(cursor.getColumnIndex(DarkroomPreset.PRESET_ISO));
			((TextView) view.findViewById(R.id.iso)).setText(iso > 0 ? String.format("ISO %d", iso) : "");
			String temp = cursor.getString(cursor.getColumnIndex(DarkroomPreset.PRESET_TEMP));
			TextView tempView = (TextView) view.findViewById(R.id.temp);
			if (temp != null && temp.length() > 0) {
				tempView.setText(String.format(" @ %s", temp));
			} else {
				tempView.setText("");
			}
		}

	}

}
