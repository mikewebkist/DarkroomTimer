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
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

public class TimerPicker extends ListActivity {
	public static final String TAG = "DarkroomTimer.TimerPicker";
	public static ArrayList<DarkroomPreset> darkroomPresets = new ArrayList<DarkroomPreset>();
	private static final int XML_IMPORT_DONE = 1;
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

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Log.v(TAG, "DarkroomPresets Loaded: " + darkroomPresets.size());
		if(darkroomPresets.size() == 0) {
			XmlResourceParser xrp = this.getResources().getXml(R.xml.presets);
			XmlParser p = new XmlParser(xrp);
			p.run();
		} else {
			XMLLoaded();
		}
	}

	public static DarkroomPreset getPreset(int index) {
		return darkroomPresets.get(index);
	}
	
	void XMLLoaded() {
		Toast.makeText(this, "XML Loaded!", Toast.LENGTH_LONG);

		setListAdapter(new ArrayAdapter<DarkroomPreset>(this,
				android.R.layout.simple_list_item_1, darkroomPresets));

	}

	public DarkroomPreset getById(int id) {
		return darkroomPresets.get(id);
	}

	public void onListItemClick(ListView l, View v, int position, long id) {
		selectedPreset = (DarkroomPreset) getListView().getItemAtPosition(position);
		Log.v(TAG, "List Item Clicked: preset=" + selectedPreset);
		Intent intent = new Intent(TimerPicker.this, DarkroomTimer.class);
		intent.putExtra("com.webkist.android.DarkroomTimer.DarkroomPreset", darkroomPresets.indexOf(selectedPreset));
		setResult(RESULT_OK, intent);
//		startActivity(intent);
		finish();
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
				// Log.v(TAG, "Starting parser.");
				while (xrp.getEventType() != XmlResourceParser.END_DOCUMENT) {
					// Log.v(TAG, "event type: " + xrp.getEventType());
					if (xrp.getEventType() == XmlResourceParser.START_TAG) {
						String s = xrp.getName();
						// Log.v(TAG, "Found a tag: " + s);
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
