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

import java.io.IOException;
import java.util.ArrayList;

import org.xmlpull.v1.XmlPullParserException;

import com.webkist.android.DarkroomTimer.DarkroomPreset;

import android.app.ListActivity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.content.res.XmlResourceParser;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;

public class TimerPicker extends ListActivity {
	public static final String				TAG						= "DarkroomTimer.TimerPicker";
	public static ArrayList<DarkroomPreset>	presetList				= new ArrayList<DarkroomPreset>();
	private static final int				XML_IMPORT_DONE			= 1;
	private static final int				EDIT_ID					= 2;
	private static final int				DELETE_ID				= 3;
	// public DarkroomPreset selectedPreset = null;

	Handler									threadMessageHandler	= new Handler() {
																		public void handleMessage(Message msg) {
																			switch (msg.what) {
																				case XML_IMPORT_DONE:
																					Log.v(TAG, "XML Import Finished...");
																					XMLLoaded();
																					break;
																			}
																		}
																	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		XmlResourceParser xrp = this.getResources().getXml(R.xml.presets);
		XmlParser p = new XmlParser(xrp);
		p.run();
	}

	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		menu.add(0, EDIT_ID, 0, "Edit");
		menu.add(0, DELETE_ID, 0, "Delete");
	}

	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		Uri uri = ContentUris.withAppendedId(DarkroomPreset.CONTENT_URI_PRESET, info.id);
		Log.v(TAG, "Context menu selection for: " + uri);
		switch (item.getItemId()) {
			case EDIT_ID:
				editPreset(uri);
				return true;
			case DELETE_ID:
				deletePreset(uri);
				return true;
			default:
				return super.onContextItemSelected(item);
		}
	}

	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.timerpicker, menu);
		return true;
	}

	public void editPreset(Uri uri) {
		// TODO create a new Activity to deal with this.
		Log.v(TAG, "UNIMPLEMENTED edit preset: " + uri);
		Toast.makeText(this, "Unimplemented EDIT", Toast.LENGTH_SHORT.show();
	}

	public void deletePreset(Uri uri) {
		// TODO popup a dialog to confirm.
		Log.v(TAG, "Delete preset: " + uri);
		getContentResolver().delete(uri, null, null);
	}

	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.add_preset:
				Log.v(TAG, "Add preset.");
				return true;
		}
		return false;
	}

	void XMLLoaded() {
		Toast.makeText(this, "XML Loaded!", Toast.LENGTH_LONG);

		Cursor cursor = managedQuery(DarkroomPreset.CONTENT_URI_PRESET, null, null, null, DarkroomPreset.PRESET_NAME);
		SimpleCursorAdapter adapter = new SimpleCursorAdapter(this, android.R.layout.simple_list_item_1, cursor,
				new String[] { DarkroomPreset.PRESET_NAME }, new int[] { android.R.id.text1 });
		setListAdapter(adapter);

		setListAdapter(adapter);
		registerForContextMenu(getListView());

	}

	public void onListItemClick(ListView l, View v, int position, long id) {
		Uri uri = ContentUris.withAppendedId(DarkroomPreset.CONTENT_URI_PRESET, id);
		Log.v(TAG, "List Item Clicked: preset=" + uri);
		setResult(RESULT_OK, new Intent().setData(uri));
		finish();
	}

	class XmlParser implements Runnable {
		private XmlResourceParser	xrp;

		public XmlParser(XmlResourceParser xrp) {
			this.xrp = xrp;
		}

		public void run() {
			Cursor cur = managedQuery(DarkroomPreset.CONTENT_URI_PRESET, null, null, null, DarkroomPreset.PRESET_NAME);

			if (cur.getCount() == 0) {
				fillDatabaseFromXML();
			}

			Message m = new Message();
			m.what = TimerPicker.XML_IMPORT_DONE;
			TimerPicker.this.threadMessageHandler.sendMessage(m);

		}

		private void fillDatabaseFromXML() {
			ContentResolver cr = getContentResolver();

			ArrayList<DarkroomPreset> darkroomPresets = new ArrayList<DarkroomPreset>();
			try {
				DarkroomPreset p = null;
				@SuppressWarnings("unused")
				DarkroomPreset.DarkroomStep step = null;
				while (xrp.getEventType() != XmlResourceParser.END_DOCUMENT) {
					if (xrp.getEventType() == XmlResourceParser.START_TAG) {
						String s = xrp.getName();
						if (s.equals("preset")) {
							p = new DarkroomPreset(xrp.getAttributeValue(null, "id"), xrp.getAttributeValue(null, "name"));
							darkroomPresets.add(p);
						} else if (s.equals("step")) {
							step = p.addStep(p.steps.size(), xrp.getAttributeValue(null, "name"), xrp.getAttributeIntValue(
									null, "duration", 120), xrp.getAttributeIntValue(null, "agitate", 0), xrp
									.getAttributeValue(null, "promptBefore"), xrp.getAttributeIntValue(null, "pour", 0));
						}
					}
					xrp.next();
				}
			} catch (XmlPullParserException xppe) {
				Log.e(TAG, "XML Parser Problem: " + xppe);
			} catch (IOException e) {
				Log.e(TAG, "XML Parser Problem: " + e);
			}

			for (int i = 0; i < darkroomPresets.size(); i++) {
				DarkroomPreset preset = darkroomPresets.get(i);
				ContentValues vals = new ContentValues();
				vals.put(DarkroomPreset.PRESET_NAME, preset.name);
				Uri uri = cr.insert(DarkroomPreset.CONTENT_URI_PRESET, vals);
				String presetId = uri.getPathSegments().get(1);
				for (int j = 0; j < preset.steps.size(); j++) {
					cr.insert(Uri.withAppendedPath(uri, "step"), preset.steps.get(j).toContentValues(presetId));
				}
				Log.v(TAG, "Inserted " + uri);
			}
		}
	}

}
