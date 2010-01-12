package com.webkist.android.DarkroomTimer;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.util.Log;

public class DarkroomPresetProvider extends ContentProvider {
	public static final String AUTHORITY = "com.webkist.android.DarkroomTimer";
	public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY);
	private static final String TAG = "DarkroomPresetProvider";
	
	private static final String DATABASE_NAME = "presets.db";
	private static final int DATABASE_VERSION = 1;

	public static final String PRESET_CONTENT_TYPE = "vnd.android.cursor.item/vnd.webkist.preset";
	public static final String PRESET_CONTENT_TYPE_LIST = "vnd.android.cursor.dir/vnd.webkist.preset";
	private static final int PRESETS = 1;
	private static final int PRESET_ID = 2;
	private static final UriMatcher sUriMatcher;

	private static final String PRESET_TABLE_NAME = "presets";
	public static final String PRESET_NAME = "name";
	
	private static final String STEPS_TABLE_NAME = "steps";
	public static final String STEP_NAME = "name";
	public static final String STEP_DURATION = "duration";
	public static final String STEP_AGITATION = "agitation";
	public static final String STEP_POUR = "pour";
	public static final String STEP_PROMPT_BEFORE = "prompt_before";
	public static final String STEP_PROMPT_AFTER = "prompt_after";

	private static class DatabaseHelper extends SQLiteOpenHelper {
		private Resources res;

		DatabaseHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
			res = context.getResources();
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL(res.getString(R.string.sql_presets));
			db.execSQL(res.getString(R.string.sql_steps));			
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			Log.w(TAG, "Upgrading DB from version " + oldVersion + " to " + newVersion + ", which will destroy all data");
			db.execSQL("DROP TABLE IF EXISTS steps");
			db.execSQL("DROP TABLE IF EXISTS presets");
			onCreate(db);
		}
	}
	
	private DatabaseHelper mOpenHelper;

	@Override
	public boolean onCreate() {
		mOpenHelper = new DatabaseHelper(getContext());
		return false;
	}
	
//	public DarkroomPresetProvider() {
//		// TODO Auto-generated constructor stub
//	}

	@Override
	public int delete(Uri arg0, String arg1, String[] arg2) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String getType(Uri uri) {
		switch(sUriMatcher.match(uri)) {
		case PRESETS:
			return PRESET_CONTENT_TYPE_LIST;
		case PRESET_ID:
			return PRESET_CONTENT_TYPE;
		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}
	}

	@Override
	public Uri insert(Uri arg0, ContentValues arg1) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Cursor query(Uri arg0, String[] arg1, String arg2, String[] arg3,
			String arg4) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int update(Uri arg0, ContentValues arg1, String arg2, String[] arg3) {
		// TODO Auto-generated method stub
		return 0;
	}
	
	static {
		sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
		sUriMatcher.addURI(AUTHORITY, "presets", PRESETS);
		sUriMatcher.addURI(AUTHORITY, "preset/#", PRESET_ID);
	}
}
