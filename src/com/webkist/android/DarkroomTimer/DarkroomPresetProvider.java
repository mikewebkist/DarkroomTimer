package com.webkist.android.DarkroomTimer;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import java.lang.*;

public class DarkroomPresetProvider extends ContentProvider {
	public static final String AUTHORITY = "com.webkist.android.DarkroomTimer";
	public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY);
	public static final Uri CONTENT_URI_PRESET = Uri.parse("content://" + AUTHORITY + "/preset");
	private static final String TAG = "DarkroomPresetProvider";
	
	private static final String DATABASE_NAME = "presets.db";
	private static final int DATABASE_VERSION = 1;

	public static final String PRESET_CONTENT_TYPE = "vnd.android.cursor.item/vnd.webkist.preset";
	public static final String PRESET_CONTENT_TYPE_LIST = "vnd.android.cursor.dir/vnd.webkist.preset";
	private static final int URI_PRESETS = 1;
	private static final int URI_PRESET_ID = 2;
	private static final int URI_STEP_ID = 3;
	private static final UriMatcher sUriMatcher;

	private static final String PRESET_TABLE_NAME = "presets";
	public static final String PRESET_NAME = "name";
	
	private static final String STEP_TABLE_NAME = "steps";
	public static final String STEP_PRESET = "preset";
	public static final String STEP_STEP = "step";
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
	public int delete(Uri uri, String where, String[] whereArgs) {
		SQLiteDatabase db = mOpenHelper.getWritableDatabase();
		int count;
		switch(sUriMatcher.match(uri)) {
		case URI_PRESETS:
			Log.w(TAG, "Deleting all presets is not allowed!");
			break;
		case URI_PRESET_ID:
			Log.w(TAG, "Deleting preset id=" + uri.getPathSegments().get(1));
			break;
		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}
		return 0;
	}

	@Override
	public String getType(Uri uri) {
		switch(sUriMatcher.match(uri)) {
		case URI_PRESETS:
			return PRESET_CONTENT_TYPE_LIST;
		case URI_PRESET_ID:
			return PRESET_CONTENT_TYPE;
		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}
	}

	@Override
	public Uri insert(Uri uri, ContentValues initialValues) {
		SQLiteDatabase db = mOpenHelper.getWritableDatabase();

		ContentValues values;
		if(initialValues != null) {
			values = new ContentValues(initialValues);
		} else {
			values = new ContentValues();
		}
		
		long rowId;
				
		switch(sUriMatcher.match(uri)) {
		case URI_PRESET_ID:
			rowId = db.insert(PRESET_TABLE_NAME, PRESET_NAME, values);
			if(rowId > 0) {
				Uri presetUri = ContentUris.withAppendedId(CONTENT_URI_PRESET, rowId);
				getContext().getContentResolver().notifyChange(presetUri, null);
				return presetUri;
			}
			break;
		case URI_STEP_ID:
			// Deal with default values in the DarkroomPreset class.
			rowId = db.insert(STEP_TABLE_NAME, STEP_NAME, values);
			if(rowId > 0) {
				Uri presetUri = ContentUris.withAppendedId(CONTENT_URI_PRESET, Long.decode(values.get(STEP_PRESET).toString()));
				getContext().getContentResolver().notifyChange(presetUri, null);
				return presetUri;
			}
			break;
		default:
			throw new IllegalArgumentException("Can't insert: " + uri);
		}

		throw new SQLException("Failed to insert row into " + uri);
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
		
		String orderBy;
		
		switch(sUriMatcher.match(uri)) {
		case URI_PRESETS: // Get all presets
			qb.setTables(PRESET_TABLE_NAME);
			orderBy = PRESET_NAME;
			break;
		
		case URI_PRESET_ID: // Get steps associated with a preset
			qb.setTables(STEP_TABLE_NAME);
			qb.appendWhere(STEP_PRESET + "=" + uri.getPathSegments().get(1));
			orderBy = STEP_STEP;
			break;
			
		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}
		
		SQLiteDatabase db = mOpenHelper.getReadableDatabase();
		Cursor c = qb.query(db, projection, selection, selectionArgs, null, null, orderBy);
		c.setNotificationUri(getContext().getContentResolver(), uri);
		return c;		
	}

	@Override
	public int update(Uri arg0, ContentValues arg1, String arg2, String[] arg3) {
		// TODO Auto-generated method stub
		return 0;
	}
	
	static {
		sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
		sUriMatcher.addURI(AUTHORITY, "presets", URI_PRESETS);
		sUriMatcher.addURI(AUTHORITY, "preset/#", URI_PRESET_ID);
		sUriMatcher.addURI(AUTHORITY, "step/#", URI_STEP_ID);
	}
}
