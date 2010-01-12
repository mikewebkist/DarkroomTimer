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
import android.content.Intent;
import android.content.res.XmlResourceParser;
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
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;

public class TimerPicker extends ListActivity {
	public static final String TAG = "DarkroomTimer.TimerPicker";
	public static ArrayList<DarkroomPreset> darkroomPresets = new ArrayList<DarkroomPreset>();
	private static final int XML_IMPORT_DONE = 1;
	private static final int EDIT_ID = 2;
	private static final int DELETE_ID = 3;
	public DarkroomPreset selectedPreset = null;

	Handler threadMessageHandler = new Handler() {
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case XML_IMPORT_DONE:
				Log.v(TAG, "XML Import Finished...");
				XMLLoaded();
				break;
			}
		}
	};
	private ArrayAdapter<DarkroomPreset> myAdapter;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if(darkroomPresets.size() == 0) {
			XmlResourceParser xrp = this.getResources().getXml(R.xml.presets);
			XmlParser p = new XmlParser(xrp);
			p.run();
		} else {
			XMLLoaded();
		}
	}

	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		menu.add(0, EDIT_ID, 0, "Edit");
		menu.add(0, DELETE_ID, 0,  "Delete");
	}

	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		DarkroomPreset preset = (DarkroomPreset) getListView().getItemAtPosition(info.position);
		switch (item.getItemId()) {
			case EDIT_ID:
				editPreset(preset);
				return true;
			case DELETE_ID:
				deletePreset(preset);
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
	
	public void editPreset(DarkroomPreset preset) {
		// TODO
		Log.v(TAG, "Edit preset: " + preset.name);
	}
	
	public void deletePreset(DarkroomPreset preset) {
		// TODO popup a dialog to confirm.
		Log.v(TAG, "Delete preset: " + preset.name);
		myAdapter.remove(preset);
	}
	
//	Intent intent = new Intent(this, TimerPicker.class);
//	startActivityForResult(intent, GET_PRESET);
	
	public boolean onOptionsItemSelected(MenuItem item) {
	    switch (item.getItemId()) {
	    case R.id.add_preset:
	    	Log.v(TAG, "Add preset.");
	    	return true;
	    }
	    return false;
	}
	
	public static DarkroomPreset getPreset(int index) {
		return darkroomPresets.get(index);
	}
	
	void XMLLoaded() {
		Toast.makeText(this, "XML Loaded!", Toast.LENGTH_LONG);

		myAdapter = new ArrayAdapter<DarkroomPreset>(this, android.R.layout.simple_list_item_1, darkroomPresets);
		setListAdapter(myAdapter);
		registerForContextMenu(getListView());

	}

	public DarkroomPreset getById(int id) {
		return darkroomPresets.get(id);
	}

	public void onListItemClick(ListView l, View v, int position, long id) {
		selectedPreset = (DarkroomPreset) getListView().getItemAtPosition(position);
		if(selectedPreset.id == "addNew") {
			Toast.makeText(this, "You picked + Add New", Toast.LENGTH_SHORT).show();
			// TODO start new Activity here to create a new preset.
		} else {
			Log.v(TAG, "List Item Clicked: preset=" + selectedPreset);
			Intent intent = new Intent(TimerPicker.this, DarkroomTimer.class);
			intent.putExtra("com.webkist.android.DarkroomTimer.DarkroomPreset", darkroomPresets.indexOf(selectedPreset));
			setResult(RESULT_OK, intent);
			finish();
		}
	}

	class XmlParser implements Runnable {
		private XmlResourceParser xrp;

		public XmlParser(XmlResourceParser xrp) {
			this.xrp = xrp;
		}

		public void run() {
			try {
				DarkroomPreset p = null;
				DarkroomPreset.DarkroomStep step = null;
				while (xrp.getEventType() != XmlResourceParser.END_DOCUMENT) {
					if (xrp.getEventType() == XmlResourceParser.START_TAG) {
						String s = xrp.getName();
						if (s.equals("preset")) {
							p = new DarkroomPreset(
									xrp.getAttributeValue(null, "id"), 
									xrp.getAttributeValue(null, "name"));
							darkroomPresets.add(p);
						} else if (s.equals("step")) {
							step = p.addStep(
									xrp.getAttributeValue(null, "name"),
									xrp.getAttributeIntValue(null, "duration", 120),
									xrp.getAttributeIntValue(null, "agitate", 0),
									xrp.getAttributeValue(null, "promptBefore"),
									xrp.getAttributeIntValue(null, "pour", 0));
						}
					}
					xrp.next();
				}
				Message m = new Message();
				m.what = TimerPicker.XML_IMPORT_DONE;
				TimerPicker.this.threadMessageHandler.sendMessage(m);
			} catch (XmlPullParserException xppe) {
				Log.e(TAG, "XML Parser Problem: " + xppe);
			} catch (IOException e) {
				Log.e(TAG, "XML Parser Problem: " + e);
			}

		}
	}

}
