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

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.DialogInterface;
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
	public static final String TAG = "DarkroomTimer.TimerPicker";
	private static final int XML_IMPORT_DONE = 1;
	private static final int EDIT_ID = 2;
	private static final int DELETE_ID = 3;
	private static final int DIALOG_DELETE_CONFIRM = 1;
	private static final int EDIT_PRESET = 0;
	private Cursor listViewCursor;

	Handler threadMessageHandler = new Handler() {
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case XML_IMPORT_DONE:
				Log.v(TAG, "XML Import Finished...");
				listViewCursor.requery();
				break;
			}
		}
	};
	private DarkroomPreset longClickPreset;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		listViewCursor = managedQuery(DarkroomPreset.CONTENT_URI_PRESET, null, null, null, DarkroomPreset.PRESET_NAME);
		if (listViewCursor.getCount() == 0) {
			XmlResourceParser xrp = this.getResources().getXml(R.xml.presets);
			XmlParser p = new XmlParser(xrp);
			p.run();
		}

		SimpleCursorAdapter adapter = new SimpleCursorAdapter(this, android.R.layout.simple_list_item_1, listViewCursor,
				new String[] { DarkroomPreset.PRESET_NAME }, new int[] { android.R.id.text1 });
		setListAdapter(adapter);
		registerForContextMenu(getListView());
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
			longClickPreset = new DarkroomPreset(this, uri);
			showDialog(DIALOG_DELETE_CONFIRM);
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

	protected Dialog onCreateDialog(int id) {
		// LayoutInflater factory = LayoutInflater.from(this);
		if (id == DIALOG_DELETE_CONFIRM) {
			return new AlertDialog.Builder(TimerPicker.this).setTitle(R.string.app_name).setMessage(
					R.string.preset_confirm_delete).setCancelable(true).setPositiveButton(R.string.time_picker_ok,
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int whichButton) {
							if (longClickPreset != null) {
								getContentResolver().delete(longClickPreset.uri, null, null);
								Toast.makeText(TimerPicker.this, "Deleted.", Toast.LENGTH_SHORT).show();
								longClickPreset = null;
							}
						}
					}).setNegativeButton(R.string.time_picker_cancel, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					Toast.makeText(TimerPicker.this, "Delete cancelled.", Toast.LENGTH_SHORT).show();
					longClickPreset = null;
				}
			}).setOnCancelListener(new DialogInterface.OnCancelListener() {
				@Override
				public void onCancel(DialogInterface dialog) {
					Toast.makeText(TimerPicker.this, "Delete cancelled.", Toast.LENGTH_SHORT).show();
					longClickPreset = null;
				}
			}).create();
		}
		return null;
	}

	protected void onPrepareDialog(int id, Dialog dialog) {
		if (id == DIALOG_DELETE_CONFIRM) {
			String message = String.format(getResources().getString(R.string.preset_confirm_delete), longClickPreset.name);
			((AlertDialog) dialog).setMessage(message);
		}
	}

	public void editPreset(Uri uri) {
		Intent intent = new Intent(this, PresetEditor.class);
		intent.setData(uri);
//		intent.setAction(Intent.ACTION_EDIT);
		startActivityForResult(intent, EDIT_PRESET);
	}

	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == EDIT_PRESET) {
			if (resultCode == RESULT_OK) {
				listViewCursor.requery();
			}
		}
	}

	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.add_preset:
			Log.v(TAG, "Add preset.");
			Intent intent = new Intent(this, PresetEditor.class);
			intent.setData(null);
			startActivityForResult(intent, EDIT_PRESET);
			return true;
		}
		return false;
	}

	public void onListItemClick(ListView l, View v, int position, long id) {
		Uri uri = ContentUris.withAppendedId(DarkroomPreset.CONTENT_URI_PRESET, id);
		Log.v(TAG, "List Item Clicked: preset=" + uri);
		setResult(RESULT_OK, new Intent().setData(uri));
		finish();
	}

	class XmlParser implements Runnable {
		private XmlResourceParser xrp;

		public XmlParser(XmlResourceParser xrp) {
			this.xrp = xrp;
		}

		public void run() {
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

			Message m = new Message();
			m.what = TimerPicker.XML_IMPORT_DONE;
			TimerPicker.this.threadMessageHandler.sendMessage(m);

		}
	}
}
