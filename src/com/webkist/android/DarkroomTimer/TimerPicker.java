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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;

import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

import com.webkist.android.DarkroomTimer.DarkroomPreset;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.XmlResourceParser;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.Xml;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;

public class TimerPicker extends ListActivity {
	public static final String TAG = "DarkroomTimer.TimerPicker";
	private static final int XML_IMPORT_DONE = 1;
	private static final int EDIT_ID = 2;
	private static final int DELETE_ID = 3;
	private static final int DUPLICATE_ID = 4;
	private static final int DIALOG_DELETE_CONFIRM = 1;
	private static final int EDIT_PRESET = 0;
	private Cursor listViewCursor;

	Handler threadMessageHandler = new Handler() {
		public void handleMessage(Message msg) {
			switch (msg.what) {
				case XML_IMPORT_DONE:
					listViewCursor.requery();
					break;
			}
		}
	};
	private DarkroomPreset longClickPreset;
	private SharedPreferences settings;
	private CharSequence[] fileList;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		listViewCursor = managedQuery(DarkroomPreset.CONTENT_URI_PRESET, null, null, null, DarkroomPreset.PRESET_NAME);
		if (listViewCursor.getCount() == 0) {
			XmlResourceParser xrp = this.getResources().getXml(R.xml.presets);
			XmlParser p = new XmlParser(xrp);
			p.run();
		}

		MyOtherAdapter adapter = new MyOtherAdapter(this, listViewCursor);
		setListAdapter(adapter);
		registerForContextMenu(getListView());
	}

	@Override
	public void onResume() {
		super.onResume();
		settings = PreferenceManager.getDefaultSharedPreferences(this);
		boolean alreadyRan = settings.getBoolean("AlreadyRanFlag", false);
		if (!alreadyRan) {
			showDialog(R.id.info);
		}
	}

	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		menu.setHeaderTitle("Modify Preset");
		menu.add(0, DUPLICATE_ID, 0, "Duplicate");
		menu.add(0, EDIT_ID, 0, "Edit");
		menu.add(0, DELETE_ID, 0, "Delete");
	}

	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		Uri uri = ContentUris.withAppendedId(DarkroomPreset.CONTENT_URI_PRESET, info.id);
		switch (item.getItemId()) {
			case EDIT_ID:
				editPreset(uri);
				return true;
			case DELETE_ID:
				longClickPreset = new DarkroomPreset(this, uri);
				showDialog(DIALOG_DELETE_CONFIRM);
				return true;
			case DUPLICATE_ID:
				DarkroomPreset preset = new DarkroomPreset(this, uri);
				preset.name = preset.name + " copy";

				ContentResolver cr = getContentResolver();

				Uri newUri = cr.insert(DarkroomPreset.CONTENT_URI_PRESET, preset.toContentValues());
				String presetId = newUri.getPathSegments().get(1);
				for (int j = 0; j < preset.steps.size(); j++) {
					cr.insert(Uri.withAppendedPath(newUri, "step"), preset.steps.get(j).toContentValues(presetId));
				}
				editPreset(newUri);
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
		Dialog dialog;
		switch (id) {
			case DIALOG_DELETE_CONFIRM:
				dialog = new AlertDialog.Builder(TimerPicker.this).setTitle(R.string.app_name).setMessage(
						R.string.preset_confirm_delete).setCancelable(true).setPositiveButton(R.string.time_picker_ok,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int whichButton) {
								if (longClickPreset != null) {
									getContentResolver().delete(Uri.parse(longClickPreset.uri), null, null);
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
				break;
			case R.id.info:
				dialog = new AlertDialog.Builder(TimerPicker.this).setTitle(R.string.app_name).setMessage(
						R.string.first_run_message).setCancelable(true).setPositiveButton(R.string.time_picker_ok,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int whichButton) {
								Editor editor = settings.edit();
								editor.putBoolean("AlreadyRanFlag", true);
								editor.commit();
							}
						}).create();
				break;
			case R.id.export:
				dialog = new AlertDialog.Builder(TimerPicker.this).setTitle(R.string.app_name).setMessage(
						R.string.exportMessage).setCancelable(true).setPositiveButton("Export",
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int whichButton) {
								XmlWriter p = new XmlWriter();
								p.run();
							}
						}).setNegativeButton("Cancel", null).create();
				break;
			case R.id.load:
				AlertDialog.Builder builder = new AlertDialog.Builder(TimerPicker.this);
				builder.setTitle("Load from which file?");
				builder.setItems(fileList, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int item) {
						Toast.makeText(getApplicationContext(), fileList[item], Toast.LENGTH_SHORT).show();
					}
				});
				dialog = builder.create();
				break;
			default:
				dialog = null;
		}
		return dialog;
	}

	protected void onPrepareDialog(int id, Dialog dialog) {
		switch (id) {
			case DIALOG_DELETE_CONFIRM:
				String message = String.format(getResources().getString(R.string.preset_confirm_delete), longClickPreset.name);
				((AlertDialog) dialog).setMessage(message);
				break;
			case R.id.load:
				break;
		}
	}

	public void editPreset(Uri uri) {
		Intent intent = new Intent(this, PresetEditor.class);
		intent.setData(uri);
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
				Intent intent = new Intent(this, PresetEditor.class);
				intent.setData(null);
				startActivityForResult(intent, EDIT_PRESET);
				break;
				
			case R.id.load:
				String state = Environment.getExternalStorageState();
				if (!(Environment.MEDIA_MOUNTED.equals(state) || Environment.MEDIA_MOUNTED_READ_ONLY.equals(state))) {
					Toast.makeText(getBaseContext(), "Storage is unavailable for reading!", Toast.LENGTH_LONG).show();
					return false;
				}

				File path = Environment.getExternalStorageDirectory();
			    File dir = new File(path, getPackageName());
			    fileList = dir.list(null);
				showDialog(R.id.load);
			    break;

			default:
				showDialog(item.getItemId());
		}
		return true;
	}

	public void onListItemClick(ListView l, View v, int position, long id) {
		Uri uri = ContentUris.withAppendedId(DarkroomPreset.CONTENT_URI_PRESET, id);
		Intent i = new Intent(this, DarkroomTimer.class);
		i.setAction(Intent.ACTION_VIEW);
		i.setData(uri);

		startActivity(i);
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

	class XmlParser implements Runnable {
		private XmlResourceParser xrp;

		public XmlParser(XmlResourceParser xrp) {
			this.xrp = xrp;
		}

		public void run() {
			ContentResolver cr = getContentResolver();

			Log.w(TAG, "Initializing DB from XML resources.");
			ArrayList<DarkroomPreset> darkroomPresets = new ArrayList<DarkroomPreset>();
			try {
				DarkroomPreset p = null;
				while (xrp.getEventType() != XmlResourceParser.END_DOCUMENT) {
					if (xrp.getEventType() == XmlResourceParser.START_TAG) {
						String s = xrp.getName();
						if (s.equals("preset")) {
							p = new DarkroomPreset(xrp.getAttributeValue(null, "id"), xrp.getAttributeValue(null, "name"),
									xrp.getAttributeIntValue(null, "iso", 0), xrp.getAttributeValue(null, "temp"));
							darkroomPresets.add(p);
						} else if (s.equals("step")) {
							p.addStep(p.steps.size(), xrp.getAttributeValue(null, "name"), xrp.getAttributeIntValue(null,
									"duration", 120), xrp.getAttributeIntValue(null, "agitate", 0), xrp
									.getAttributeIntValue(null, "pour", 0));
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
				Uri uri = cr.insert(DarkroomPreset.CONTENT_URI_PRESET, preset.toContentValues());
				String presetId = uri.getPathSegments().get(1);
				for (int j = 0; j < preset.steps.size(); j++) {
					cr.insert(Uri.withAppendedPath(uri, "step"), preset.steps.get(j).toContentValues(presetId));
				}
			}

			Message m = new Message();
			m.what = TimerPicker.XML_IMPORT_DONE;
			TimerPicker.this.threadMessageHandler.sendMessage(m);

		}
	}

	class XmlWriter implements Runnable {

		public void run() {

			String state = Environment.getExternalStorageState();
			if (!Environment.MEDIA_MOUNTED.equals(state)) {
				Toast.makeText(getBaseContext(), "Storage is unavailable for writing!", Toast.LENGTH_LONG).show();
				return;
			}

			File path = Environment.getExternalStorageDirectory();
		    File dir = new File(path, getPackageName());
		    dir.mkdirs();
		    File file = new File(dir, "preset_backup.xml");
		    
			Log.w(TAG, "Writing DB to XML:" + file.toString());
			XmlSerializer s = Xml.newSerializer();
			FileWriter writer;
			try {
				writer = new FileWriter(file);
			} catch (IOException e1) {
				Toast.makeText(getBaseContext(), "Couldn't open backup file for writing!", Toast.LENGTH_LONG).show();
				return;
			}
			
			try {
				s.setOutput(writer);
				s.startDocument("UTF-8", true);
				s.text("\n");
				s.startTag("", "preset-array");
				s.text("\n");
				ContentResolver cr = getContentResolver();
				Cursor cur = cr.query(DarkroomPreset.CONTENT_URI_PRESET, null, null, null, null);
				if(cur.moveToFirst()) {
					do {
						Uri uri = ContentUris.withAppendedId(DarkroomPreset.CONTENT_URI_PRESET, cur.getInt(cur.getColumnIndex(DarkroomPreset._ID)));
						DarkroomPreset preset = new DarkroomPreset(TimerPicker.this, uri);
						s.startTag("", "preset");
						s.attribute("", "name", preset.name);
						if(preset.temp != null) { 
							s.attribute("", "temp", preset.temp);
						}
						if(preset.iso > 0) {
							s.attribute("", "iso", String.valueOf(preset.iso));
						}
						for(int i=0; i< preset.steps.size(); i++) {
							DarkroomPreset.DarkroomStep step = preset.steps.get(i);
							s.text("\n	");
							s.startTag("", "step");
							s.attribute("", "name", step.name);
							if(step.duration > 0) {
								s.attribute("", "duration", String.valueOf(step.duration));
							}
							if(step.agitateEvery != 0) {
								s.attribute("", "agitate", String.valueOf(step.agitateEvery));
							}
							if(step.pourFor > 0) {
								s.attribute("", "pour", String.valueOf(step.pourFor));
							}
							s.endTag("", "step");
						}
						s.text("\n");
						s.endTag("", "preset");
						s.text("\n");
					} while(cur.moveToNext());
				}
				s.text("\n");
				s.endTag("", "preset-array");
				s.flush();
				Toast.makeText(getBaseContext(), "Backup saved to " + file.toString(), Toast.LENGTH_LONG).show();

			} catch (Exception e) {
				throw new RuntimeException(e);
			}

		}
	}
	
}