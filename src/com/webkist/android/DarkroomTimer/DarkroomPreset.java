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

import java.io.Serializable;
import java.util.ArrayList;

import android.app.Activity;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;
import android.util.Log;

public class DarkroomPreset implements BaseColumns, Serializable {

	private static final long serialVersionUID = 1L;

	public static final String TAG = "DarkroomPreset";

	public static final String AUTHORITY = "com.webkist.android.DarkroomTimer";
	public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY);
	public static final Uri CONTENT_URI_PRESET = Uri.parse("content://" + AUTHORITY + "/preset");

	public static final String PRESET_CONTENT_TYPE = "vnd.android.cursor.item/vnd.webkist.preset";
	public static final String PRESET_CONTENT_TYPE_LIST = "vnd.android.cursor.dir/vnd.webkist.preset";

	public static final String PRESET_NAME = "name";
	public static final String PRESET_TEMP = "temp";
	public static final String PRESET_ISO = "iso";

	public String name;
	public int iso;
	public String temp = "";
	public String id;
	public String uri;

	public ArrayList<DarkroomStep> steps = new ArrayList<DarkroomStep>();

	DarkroomPreset(String id, String name, int iso, String temp) {
		this.name = name;
		this.id = id;
		this.iso = iso;
		this.temp = temp;
	}

	DarkroomPreset(String id, String name, String iso, String temp) {
		this.name = name;
		this.id = id;
		try {		
			this.iso = Integer.parseInt(iso);
		} catch (NumberFormatException e) {
			this.iso = 0;
		}
		this.temp = temp;
	}

	DarkroomPreset() {
	}

	public DarkroomPreset(Activity ctx, Uri uri) {
		this.uri = uri.toString();
		Cursor cur = ctx.managedQuery(uri, null, null, null, DarkroomPreset.PRESET_NAME);

		int idCol = cur.getColumnIndex(DarkroomPreset._ID);
		int nameCol = cur.getColumnIndex(DarkroomPreset.PRESET_NAME);
		int tempCol = cur.getColumnIndex(DarkroomPreset.PRESET_TEMP);
		int isoCol = cur.getColumnIndex(DarkroomPreset.PRESET_ISO);

		if (cur.moveToFirst()) {
			Uri stepUri = Uri.withAppendedPath(uri, "step");
			this.name = cur.getString(nameCol);
			this.id = cur.getString(idCol);
			this.temp = cur.getString(tempCol);
			this.iso = cur.getInt(isoCol);
			Log.v(TAG, "Creating " + this.name + " preset.");
			Cursor step_cur = ctx.managedQuery(stepUri, null, null, null, null);

			int step_nameCol = step_cur.getColumnIndex(DarkroomPreset.DarkroomStep.STEP_NAME);
			int step_durationCol = step_cur.getColumnIndex(DarkroomPreset.DarkroomStep.STEP_DURATION);
			int step_agitageEveryCol = step_cur.getColumnIndex(DarkroomPreset.DarkroomStep.STEP_AGITATION);
			int step_pourForCol = step_cur.getColumnIndex(DarkroomPreset.DarkroomStep.STEP_POUR);

			if (step_cur.moveToFirst()) {
				do {
					String name = step_cur.getString(step_nameCol);
					int duration = step_cur.getInt(step_durationCol);
					int agitateEvery = step_cur.getInt(step_agitageEveryCol);
					int pourFor = step_cur.getInt(step_pourForCol);
					this.addStep(name, duration, agitateEvery, pourFor);

				} while (step_cur.moveToNext());
			}
		} else {
			throw new IllegalArgumentException("Unknown URI: " + uri); 
		}
	}

	public DarkroomStep blankStep() {
		return new DarkroomStep();
	}

	public void addStep(DarkroomStep s) {
		steps.add(s);
		s.fromBlank = false;
	}

	public DarkroomStep addStep(String name, int duration, int agitateEvery, int pourFor) {
		DarkroomStep s = new DarkroomStep(name, duration, agitateEvery, pourFor);
		steps.add(s);
		return s;
	}

	public DarkroomStep addStep(int stepNum, String name, String duration, String agitateEvery, String pourFor) {
		int duration_i = 120, agitateEvery_i = 0, pourFor_i = 0;
		try { duration_i = Integer.parseInt(duration); } catch (NumberFormatException e) { };
		try { agitateEvery_i = Integer.parseInt(agitateEvery); } catch (NumberFormatException e) { };
		try { pourFor_i = Integer.parseInt(pourFor); } catch (NumberFormatException e) { };
		
		DarkroomStep s = new DarkroomStep(name, duration_i, agitateEvery_i, pourFor_i);
		steps.add(s);
		return s;
	}

	public String toString() {
		return name;
	}

	// Doesn't include pour times!
	public int totalDuration() {
		int total = 0;
		for (int i = 0; i < steps.size(); i++) {
			total += steps.get(i).duration;
		}
		return total;
	}

	public ContentValues toContentValues() {
		ContentValues vals = new ContentValues();
		vals.put(DarkroomPreset.PRESET_NAME, name);
		vals.put(DarkroomPreset.PRESET_ISO, iso);
		vals.put(DarkroomPreset.PRESET_TEMP, temp);
		return vals;
	}

	public class DarkroomStep implements BaseColumns, Serializable {
		private static final long serialVersionUID = 1L;
		public String name;
		public int duration;
		public int agitateEvery;
		public int agitateFor = 10;
		public int pourFor;
		public boolean fromBlank;

		public static final String STEP_PRESET = "preset";
		public static final String STEP_NAME = "name";
		public static final String STEP_DURATION = "duration";
		public static final String STEP_AGITATION = "agitation";
		public static final String STEP_POUR = "pour";

		DarkroomStep(String name, int duration, int agitateEvery, int pourFor) {
			this.name = name;
			this.duration = duration;
			this.agitateEvery = agitateEvery;
			this.pourFor = pourFor;
			this.fromBlank = false;
		}

		DarkroomStep() {
			this.fromBlank = true;
		}

		public DarkroomStep clone() {
			DarkroomStep newStep = new DarkroomStep();
			newStep.fromBlank = this.fromBlank;
			newStep.name = this.name;
			newStep.duration = this.duration;
			newStep.agitateEvery = this.agitateEvery;
			newStep.pourFor = this.pourFor;
			return newStep;
		}

		public void overwrite(DarkroomStep newStep) {
			this.fromBlank = newStep.fromBlank;
			this.name = newStep.name;
			this.duration = newStep.duration;
			this.agitateEvery = newStep.agitateEvery;
			this.pourFor = newStep.pourFor;
		}

		public ContentValues toContentValues(String presetId) {
			ContentValues vals = new ContentValues();
			vals.put(STEP_PRESET, presetId);
			vals.put(STEP_NAME, name);
			vals.put(STEP_DURATION, duration);
			vals.put(STEP_AGITATION, agitateEvery);
			vals.put(STEP_POUR, pourFor);
			return vals;
		}

		public String toString() {
			return this.name;
		}
	}

}
