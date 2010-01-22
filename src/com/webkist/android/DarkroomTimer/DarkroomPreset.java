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

import android.app.Activity;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;
import android.util.Log;

public class DarkroomPreset implements BaseColumns {
	public static final String AUTHORITY = "com.webkist.android.DarkroomTimer";
	public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY);
	public static final Uri CONTENT_URI_PRESET = Uri.parse("content://" + AUTHORITY + "/preset");

	public static final String PRESET_CONTENT_TYPE = "vnd.android.cursor.item/vnd.webkist.preset";
	public static final String PRESET_CONTENT_TYPE_LIST = "vnd.android.cursor.dir/vnd.webkist.preset";

	public static final String PRESET_NAME = "name";

	public String name;
	public String id;
	public Uri uri;
	private int currentStep = 0;
	public ArrayList<DarkroomStep> steps = new ArrayList<DarkroomStep>();

	public static final String TAG = "DarkroomPreset";

	DarkroomPreset(String id, String name) {
		this.name = name;
		this.id = id;
	}

	DarkroomPreset(String name) {
		this.name = name;
	}

	public DarkroomPreset(Activity ctx, Uri uri) {
		this.uri = uri;
		Cursor cur = ctx.managedQuery(uri, null, null, null, DarkroomPreset.PRESET_NAME);

		int idCol = cur.getColumnIndex(DarkroomPreset._ID);
		int nameCol = cur.getColumnIndex(DarkroomPreset.PRESET_NAME);

		if (cur.moveToFirst()) {
			Uri stepUri = Uri.withAppendedPath(uri, "step");
			this.name = cur.getString(nameCol);
			this.id = cur.getString(idCol);
			Log.v(TAG, "Creating " + this.name + " preset.");
			Cursor step_cur = ctx.managedQuery(stepUri, null, null, null, DarkroomPreset.DarkroomStep.STEP_STEP);

			int step_nameCol = step_cur.getColumnIndex(DarkroomPreset.DarkroomStep.STEP_NAME);
			int step_stepNumCol = step_cur.getColumnIndex(DarkroomPreset.DarkroomStep.STEP_STEP);
			int step_durationCol = step_cur.getColumnIndex(DarkroomPreset.DarkroomStep.STEP_DURATION);
			int step_agitageEveryCol = step_cur.getColumnIndex(DarkroomPreset.DarkroomStep.STEP_AGITATION);
			int step_clickPromptCol = step_cur.getColumnIndex(DarkroomPreset.DarkroomStep.STEP_PROMPT_BEFORE);
			int step_pourForCol = step_cur.getColumnIndex(DarkroomPreset.DarkroomStep.STEP_POUR);

			Log.v(TAG, "Steps found: " + step_cur.getCount());
			if (step_cur.moveToFirst()) {
				do {
					int stepNum = step_cur.getInt(step_stepNumCol);
					String name = step_cur.getString(step_nameCol);
					int duration = step_cur.getInt(step_durationCol);
					int agitateEvery = step_cur.getInt(step_agitageEveryCol);
					String clickPrompt = step_cur.getString(step_clickPromptCol);
					int pourFor = step_cur.getInt(step_pourForCol);

					this.addStep(stepNum, name, duration, agitateEvery, clickPrompt, pourFor);

				} while (step_cur.moveToNext());
			}
		}
	}

	public DarkroomStep addStep() {
		DarkroomStep s = new DarkroomStep();
		steps.add(s);
		return s;
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
		public String name;
		public int stepNum = 0;
		public int duration = 0;
		public int agitateEvery = 0;
		public int agitateFor = 10;
		public String promptBefore = null;
		public String promptAfter = null;
		public int pourFor = 0;

		public static final String STEP_PRESET = "preset";
		public static final String STEP_STEP = "step";
		public static final String STEP_NAME = "name";
		public static final String STEP_DURATION = "duration";
		public static final String STEP_AGITATION = "agitation";
		public static final String STEP_POUR = "pour";
		public static final String STEP_PROMPT_BEFORE = "prompt_before";
		public static final String STEP_PROMPT_AFTER = "prompt_after";

		DarkroomStep(int stepNum, String name, int duration, int agitateEvery, String clickPrompt, int pourFor) {
			this.stepNum = stepNum;
			this.name = name;
			this.duration = duration;
			this.promptBefore = clickPrompt;
			this.agitateEvery = agitateEvery;
			this.pourFor = pourFor;
		}

		DarkroomStep() {
			this.name = "New Step";
			this.duration = 300;
			this.agitateEvery = 50;
			this.pourFor = 15;
			this.promptBefore = "Click to continue...";
		}
		
		public ContentValues toContentValues(String presetId) {
			ContentValues vals = new ContentValues();
			vals.put(STEP_PRESET, presetId);
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
