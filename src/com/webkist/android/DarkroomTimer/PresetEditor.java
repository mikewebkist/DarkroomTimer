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
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

public class PresetEditor extends ListActivity implements OnClickListener {
	public static final String TAG = "DarkroomTimer.TimerPicker";
	public static ArrayList<DarkroomPreset> darkroomPresets = new ArrayList<DarkroomPreset>();
	private static final int XML_IMPORT_DONE = 1;
	public DarkroomPreset selectedPreset = null;
	private LayoutInflater mInflater;
	private ViewGroup mContentView;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mInflater = getLayoutInflater();
        mContentView = (ViewGroup)mInflater.inflate(R.layout.preseteditor, null);
        setContentView(mContentView);
        
        View view = findViewById(R.id.saveButton);
        view.setOnClickListener(this);
        view = findViewById(R.id.discardButton);
        view.setOnClickListener(this);

        Intent intent = getIntent();
        String action = intent.getAction();
        if (action.equals(Intent.ACTION_EDIT)) {
        	setTitle(R.string.editPreset_title_edit);
        } else if (action.equals(Intent.ACTION_INSERT)) {
        	setTitle(R.string.editPreset_title_insert);
        }
	}

	public boolean onCreateOptionsMenu(Menu menu) {
//		MenuInflater inflater = getMenuInflater();
//		inflater.inflate(R.menu.preseteditor, menu);
		return true;
	}
	
	public void onListItemClick(ListView l, View v, int position, long id) {
		// TODO
	}

	@Override
	public void onClick(View arg0) {
		// TODO Auto-generated method stub
		
	}


}
