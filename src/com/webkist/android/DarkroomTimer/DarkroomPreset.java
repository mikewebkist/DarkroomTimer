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

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;
import android.util.Log;

public class DarkroomPreset implements BaseColumns {
	public static final String		AUTHORITY					= "com.webkist.android.DarkroomTimer";
	public static final Uri			CONTENT_URI					= Uri.parse("content://" + AUTHORITY);
	public static final Uri			CONTENT_URI_PRESET			= Uri.parse("content://" + AUTHORITY + "/preset");

	public static final String		PRESET_CONTENT_TYPE			= "vnd.android.cursor.item/vnd.webkist.preset";
	public static final String		PRESET_CONTENT_TYPE_LIST	= "vnd.android.cursor.dir/vnd.webkist.preset";

	public static final String		PRESET_NAME					= "name";

	public String					name;
	public String					id;
	private int						currentStep					= 0;
	public ArrayList<DarkroomStep>	steps						= new ArrayList<DarkroomStep>();

	public static final String		TAG							= "DarkroomPreset";

	DarkroomPreset(String id, String name) {
		this.name = name;
		this.id = id;
	}

	public DarkroomPreset(DarkroomTimer ctx, Uri uri) {
		Cursor cur = ctx.managedQuery(uri, null, null, null, DarkroomPreset.PRESET_NAME);

		int idCol = cur.getColumnIndex(DarkroomPreset._ID);
		int nameCol = cur.getColumnIndex(DarkroomPreset.PRESET_NAME);
		int step_nameCol = cur.getColumnIndex(DarkroomPreset.DarkroomStep.STEP_NAME);

		if (cur.moveToFirst()) {
			String id = cur.getString(idCol);
			Uri stepUri = Uri.withAppendedPath(uri, "step");
			Log.v(TAG, "Step uri for " + cur.getString(nameCol) + " is: " + stepUri);
			Cursor step_cur = ctx.managedQuery(stepUri, null, null, null, DarkroomPreset.DarkroomStep.STEP_STEP);
			Log.v(TAG, "Steps found: " + step_cur.getCount());
			if (step_cur.moveToFirst()) {
				do {
					String name = step_cur.getString(step_nameCol);
					Log.v(TAG, "Step: " + name);
				} while (step_cur.moveToNext());
			}
		}
	}

	public DarkroomStep addStep(int stepNum, String name, int duration, int agitateEvery, String clickPrompt, int pourFor) {
		DarkroomStep s = new DarkroomStep(stepNum, name, duration, agitateEvery, clickPrompt, pourFor);
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
		return vals;
	}

	public class DarkroomStep implements BaseColumns {
		public String				name;
		public int					stepNum				= 0;
		public int					duration;
		public int					agitateEvery		= 0;
		public int					agitateFor			= 10;
		public String				promptBefore		= null;
		public String				promptAfter			= null;
		public int					pourFor				= 0;

		public static final String	STEP_PRESET			= "preset";
		public static final String	STEP_STEP			= "step";
		public static final String	STEP_NAME			= "name";
		public static final String	STEP_DURATION		= "duration";
		public static final String	STEP_AGITATION		= "agitation";
		public static final String	STEP_POUR			= "pour";
		public static final String	STEP_PROMPT_BEFORE	= "prompt_before";
		public static final String	STEP_PROMPT_AFTER	= "prompt_after";

		DarkroomStep(int stepNum, String name, int duration, int agitateEvery, String clickPrompt, int pourFor) {
			this.stepNum = stepNum;
			this.name = name;
			this.duration = duration;
			this.promptBefore = clickPrompt;
			this.agitateEvery = agitateEvery;
			this.pourFor = pourFor;
		}

		public ContentValues toContentValues() {
			ContentValues vals = new ContentValues();
			vals.put(STEP_NAME, name);
			vals.put(STEP_STEP, stepNum);
			vals.put(STEP_DURATION, duration);
			vals.put(STEP_AGITATION, agitateEvery);
			vals.put(STEP_POUR, pourFor);
			vals.put(STEP_PROMPT_BEFORE, promptBefore);
			vals.put(STEP_PROMPT_AFTER, promptAfter);
			return vals;
		}

		public String toString() {
			return this.name;
		}
	}

}
