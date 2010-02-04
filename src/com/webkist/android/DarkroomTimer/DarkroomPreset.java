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
	public float temp;
	public String id;
	public Uri uri;
	private int currentStep = 0;
	public ArrayList<DarkroomStep> steps = new ArrayList<DarkroomStep>();

	public static final String TAG = "DarkroomPreset";

	DarkroomPreset(String id, String name, int iso, float temp) {
		this.name = name;
		this.id = id;
		this.iso = iso;
		this.temp = temp;
	}
	
//	DarkroomPreset(String id, String name) {
//		this.name = name;
//		this.id = id;
//	}
	
	DarkroomPreset() {
	}

//	DarkroomPreset(String name) {
////		this.name = name;
//	}

	public DarkroomPreset(Activity ctx, Uri uri) {
		this.uri = uri;
		Cursor cur = ctx.managedQuery(uri, null, null, null, DarkroomPreset.PRESET_NAME);

		int idCol = cur.getColumnIndex(DarkroomPreset._ID);
		int nameCol = cur.getColumnIndex(DarkroomPreset.PRESET_NAME);
		int tempCol = cur.getColumnIndex(DarkroomPreset.PRESET_TEMP);
		int isoCol = cur.getColumnIndex(DarkroomPreset.PRESET_ISO);

		if (cur.moveToFirst()) {
			Uri stepUri = Uri.withAppendedPath(uri, "step");
			this.name = cur.getString(nameCol);
			this.id = cur.getString(idCol);
			this.temp = cur.getFloat(tempCol);
			this.iso = cur.getInt(isoCol);
			Log.v(TAG, "Creating " + this.name + " preset.");
			Cursor step_cur = ctx.managedQuery(stepUri, null, null, null, DarkroomPreset.DarkroomStep.STEP_STEP);

			int step_nameCol = step_cur.getColumnIndex(DarkroomPreset.DarkroomStep.STEP_NAME);
			int step_stepNumCol = step_cur.getColumnIndex(DarkroomPreset.DarkroomStep.STEP_STEP);
			int step_durationCol = step_cur.getColumnIndex(DarkroomPreset.DarkroomStep.STEP_DURATION);
			int step_agitageEveryCol = step_cur.getColumnIndex(DarkroomPreset.DarkroomStep.STEP_AGITATION);
			int step_pourForCol = step_cur.getColumnIndex(DarkroomPreset.DarkroomStep.STEP_POUR);

			Log.v(TAG, "Steps found: " + step_cur.getCount());
			if (step_cur.moveToFirst()) {
				do {
					int stepNum = step_cur.getInt(step_stepNumCol);
					String name = step_cur.getString(step_nameCol);
					int duration = step_cur.getInt(step_durationCol);
					int agitateEvery = step_cur.getInt(step_agitageEveryCol);
					int pourFor = step_cur.getInt(step_pourForCol);
					this.addStep(stepNum, name, duration, agitateEvery, pourFor);

				} while (step_cur.moveToNext());
			}
		}
	}

	public DarkroomStep blankStep() {
		return new DarkroomStep();
	}

	public void addStep(DarkroomStep s) {
		steps.add(s);
		s.fromBlank = false;
	}
	
	public DarkroomStep addStep(int stepNum, String name, int duration, int agitateEvery, int pourFor) {
		DarkroomStep s = new DarkroomStep(stepNum, name, duration, agitateEvery, pourFor);
		steps.add(s);
		return s;
	}

	public String toString() {
		return name;
	}

	public void reset() {
		currentStep = 0;
	}

	public DarkroomStep nextStep() {
		if (currentStep < steps.size()) {
			return steps.get(currentStep++);
		} else {
			return null;
		}
	}

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

	public class DarkroomStep implements BaseColumns {
		public String name;
		public int stepNum;
		public int duration;
		public int agitateEvery;
		public int agitateFor = 10;
		public int pourFor;
		public boolean fromBlank;

		public static final String STEP_PRESET = "preset";
		public static final String STEP_STEP = "step";
		public static final String STEP_NAME = "name";
		public static final String STEP_DURATION = "duration";
		public static final String STEP_AGITATION = "agitation";
		public static final String STEP_POUR = "pour";

		DarkroomStep(int stepNum, String name, int duration, int agitateEvery, int pourFor) {
			this.stepNum = stepNum;
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
			vals.put(STEP_STEP, stepNum);
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
