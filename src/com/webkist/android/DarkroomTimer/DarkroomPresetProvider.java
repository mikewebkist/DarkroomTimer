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
	private static final String TAG = "DarkroomPresetProvider";
	
	private static final String DATABASE_NAME = "presets.db";
	private static final int DATABASE_VERSION = 1;

	private static final int URI_PRESETS = 1;
	private static final int URI_PRESET_ID = 2;
	private static final int URI_STEP_ID = 3;
	private static final UriMatcher sUriMatcher;

	private static final String PRESET_TABLE_NAME = "presets";
	private static final String STEP_TABLE_NAME = "steps";

	private static class DatabaseHelper extends SQLiteOpenHelper {
		private Resources res;

		DatabaseHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
			res = context.getResources();
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			Log.v(TAG, "Creating db tables.");
			
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
//		case URI_PRESETS:
//			Log.w(TAG, "Deleting all presets is not allowed! Do it one at a time! " + uri);
//			break;
		case URI_PRESET_ID:
			Log.w(TAG, "Deleting preset id=" + uri.getPathSegments().get(1));
			String presetId = uri.getPathSegments().get(1);
            count = db.delete(PRESET_TABLE_NAME, DarkroomPreset._ID + "=" + presetId
                    + (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""), whereArgs);

			break;
		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}
		getContext().getContentResolver().notifyChange(uri, null);
        return count;
	}

	@Override
	public String getType(Uri uri) {
		switch(sUriMatcher.match(uri)) {
		case URI_PRESETS:
			return DarkroomPreset.PRESET_CONTENT_TYPE_LIST;
		case URI_PRESET_ID:
			return DarkroomPreset.PRESET_CONTENT_TYPE;
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
		case URI_PRESETS:
			rowId = db.insert(PRESET_TABLE_NAME, DarkroomPreset.PRESET_NAME, values);
			if(rowId > 0) {
				Uri presetUri = ContentUris.withAppendedId(DarkroomPreset.CONTENT_URI_PRESET, rowId);
				getContext().getContentResolver().notifyChange(presetUri, null);
				return presetUri;
			}
			break;
		case URI_STEP_ID:
			// Deal with default values in the DarkroomPreset class.
			rowId = db.insert(STEP_TABLE_NAME, DarkroomPreset.STEP_NAME, values);
			if(rowId > 0) {
				Uri presetUri = ContentUris.withAppendedId(DarkroomPreset.CONTENT_URI_PRESET, Integer.parseInt(values.get(DarkroomPreset.STEP_PRESET).toString()));
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
			orderBy = DarkroomPreset.PRESET_NAME;
			break;
		
		case URI_PRESET_ID: // Get steps associated with a preset
			qb.setTables(STEP_TABLE_NAME);
			qb.appendWhere(DarkroomPreset.STEP_PRESET + "=" + uri.getPathSegments().get(1));
			orderBy = DarkroomPreset.STEP_STEP;
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
	public int update(Uri uri, ContentValues values, String where, String[] whereArgs) {
		// TODO Auto-generated method stub
		Log.w(TAG, "Updating unimplemented: " + uri);
		return 0;
	}
	
	static {
		sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
		sUriMatcher.addURI(DarkroomPreset.AUTHORITY, "preset", URI_PRESETS);
		sUriMatcher.addURI(DarkroomPreset.AUTHORITY, "preset/#", URI_PRESET_ID);
		sUriMatcher.addURI(DarkroomPreset.AUTHORITY, "preset/#/step", URI_STEP_ID);
	}
}
