package com.webkist.android.DarkroomTimer;

import java.util.HashMap;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.provider.LiveFolders;
import android.util.Log;

public class DarkroomPresetProvider extends ContentProvider {
	private static final String TAG = "DarkroomPresetProvider";

	private static final String DATABASE_NAME = "presets.db";
	private static final int DATABASE_VERSION = 4;

	private static final int URI_PRESETS = 1;
	private static final int URI_PRESET_ID = 2;
	private static final int URI_PRESET_STEPS = 3;
	private static final int URI_LIVE_FOLDER = 4;
	private static final UriMatcher sUriMatcher;

	private static final String PRESET_TABLE_NAME = "presets";
	private static final String STEP_TABLE_NAME = "steps";

	private static final HashMap<String, String> LIVE_FOLDER_PROJECTION_MAP;
	static {
		LIVE_FOLDER_PROJECTION_MAP = new HashMap<String, String>();
		LIVE_FOLDER_PROJECTION_MAP.put(LiveFolders._ID, DarkroomPreset._ID + " AS " + LiveFolders._ID);
		LIVE_FOLDER_PROJECTION_MAP.put(LiveFolders.NAME, DarkroomPreset.PRESET_NAME + " AS " + LiveFolders.NAME);
		LIVE_FOLDER_PROJECTION_MAP
				.put(LiveFolders.DESCRIPTION, DarkroomPreset.PRESET_ISO + " AS " + LiveFolders.DESCRIPTION);
	}

	static {
		sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
		sUriMatcher.addURI(DarkroomPreset.AUTHORITY, "preset", URI_PRESETS);
		sUriMatcher.addURI(DarkroomPreset.AUTHORITY, "preset/#", URI_PRESET_ID);
		sUriMatcher.addURI(DarkroomPreset.AUTHORITY, "preset/#/step", URI_PRESET_STEPS);
		sUriMatcher.addURI(DarkroomPreset.AUTHORITY, "live_folder/presets", URI_LIVE_FOLDER);
	}

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

	@Override
	public int delete(Uri uri, String where, String[] whereArgs) {
		SQLiteDatabase db = mOpenHelper.getWritableDatabase();
		int count;
		String presetId;
		switch (sUriMatcher.match(uri)) {
			case URI_PRESET_ID:
				presetId = uri.getPathSegments().get(1);
				count = db.delete(PRESET_TABLE_NAME, DarkroomPreset._ID + "=" + presetId, null);
				count += this.delete(Uri.withAppendedPath(uri, "step"), null, null);
				break;

			case URI_PRESET_STEPS:
				presetId = uri.getPathSegments().get(1);
				count = db.delete(STEP_TABLE_NAME, DarkroomPreset.DarkroomStep.STEP_PRESET + "=" + presetId, null);
				break;
				
			default:
				throw new IllegalArgumentException("Unknown URI " + uri);
		}
		getContext().getContentResolver().notifyChange(uri, null);
		return count;
	}

	@Override
	public String getType(Uri uri) {
		switch (sUriMatcher.match(uri)) {
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
		if (initialValues != null) {
			values = new ContentValues(initialValues);
		} else {
			values = new ContentValues();
		}

		long rowId;

		switch (sUriMatcher.match(uri)) {
			case URI_PRESETS:
				rowId = db.insert(PRESET_TABLE_NAME, DarkroomPreset.PRESET_NAME, values);
				if (rowId > 0) {
					Uri presetUri = ContentUris.withAppendedId(DarkroomPreset.CONTENT_URI_PRESET, rowId);
					getContext().getContentResolver().notifyChange(presetUri, null);
					getContext().getContentResolver().notifyChange(DarkroomPreset.CONTENT_URI_PRESET, null);

					return presetUri;
				}
				break;
			case URI_PRESET_STEPS:
				// Deal with default values in the DarkroomPreset class.
				String presetId = uri.getPathSegments().get(1);
				rowId = db.insert(STEP_TABLE_NAME, DarkroomPreset.DarkroomStep.STEP_NAME, values);
				if (rowId > 0) {
					Uri presetUri = ContentUris
							.withAppendedId(DarkroomPreset.CONTENT_URI_PRESET, Integer.parseInt(presetId));
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

		String orderBy = DarkroomPreset.PRESET_NAME;

		switch (sUriMatcher.match(uri)) {
			case URI_PRESETS: // Get all presets
				qb.setTables(PRESET_TABLE_NAME);
				break;

			case URI_PRESET_ID:
				qb.setTables(PRESET_TABLE_NAME);
				qb.appendWhere(DarkroomPreset._ID + "=" + uri.getPathSegments().get(1));
				break;

			case URI_PRESET_STEPS: // Get steps associated with a preset
				qb.setTables(STEP_TABLE_NAME);
				qb.appendWhere(DarkroomPreset.DarkroomStep.STEP_PRESET + "=" + uri.getPathSegments().get(1));
				orderBy = DarkroomPreset.DarkroomStep.STEP_STEP;
				break;

			case URI_LIVE_FOLDER:
				qb.setTables(PRESET_TABLE_NAME);
				// qb.setProjectionMap(LIVE_FOLDER_PROJECTION_MAP);
				break;

			default:
				throw new IllegalArgumentException("Unknown URI: " + uri);
		}

		SQLiteDatabase db = mOpenHelper.getReadableDatabase();
		Cursor c = qb.query(db, projection, selection, selectionArgs, null, null, orderBy);
		c.setNotificationUri(getContext().getContentResolver(), uri);

		if (sUriMatcher.match(uri) == URI_LIVE_FOLDER) {
			MatrixCursor mc = new MatrixCursor(new String[] { LiveFolders._ID, LiveFolders.NAME, LiveFolders.DESCRIPTION });

			try {
				while (c.moveToNext()) {
					int iso = c.getInt(c.getColumnIndex(DarkroomPreset.PRESET_ISO));
					String temp = c.getString(c.getColumnIndex(DarkroomPreset.PRESET_TEMP));
					String description = "";
					if (iso > 0) {
						description += "ISO " + iso;
						if (temp != null && temp.length() > 0) {
							description += ", ";
						}
					}
					if (temp != null && temp.length() > 0) {
						description += temp;
					}
					Log.v(TAG, "In query, description=" + description + " for " + uri);
					mc.addRow(new Object[] { c.getLong(c.getColumnIndex(DarkroomPreset._ID)),
							c.getString(c.getColumnIndex(DarkroomPreset.PRESET_NAME)), description });
				}
				return mc;
			} catch (Exception e) {
				return null;
			} finally {
				c.close();
			}
		} else {
			return c;
		}
	}

	@Override
	public int update(Uri uri, ContentValues values, String where, String[] whereArgs) {
		Log.w(TAG, "Updating <" + uri + ">");
		SQLiteDatabase db = mOpenHelper.getWritableDatabase();
		int count;
		switch (sUriMatcher.match(uri)) {
			case URI_PRESET_ID:
				String presetId = uri.getPathSegments().get(1);
				Log.v(TAG, "Updating a URI_PRESET_ID: " + presetId);
				count = db.update(PRESET_TABLE_NAME, values, DarkroomPreset._ID + "=" + presetId, whereArgs);
				break;
			case URI_PRESETS:
				throw new IllegalArgumentException("Can't update URI_PRESETS: " + uri);
			case URI_PRESET_STEPS:
				throw new IllegalArgumentException("Can't update URI_PRESET_STEPS: " + uri);
			case URI_LIVE_FOLDER:
				throw new IllegalArgumentException("Can't update URI_LIVE_FOLDER: " + uri);
			default:
				throw new IllegalArgumentException("Unknown URI " + uri);
		}
		
		getContext().getContentResolver().notifyChange(uri, null);

		return count;
	}
}
