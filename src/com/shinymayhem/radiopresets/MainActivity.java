/*
 * Copyright (C) 2013 Reese Wilson | Shiny Mayhem

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
package com.shinymayhem.radiopresets;

import android.app.Activity;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;

import com.shinymayhem.radiopresets.AddDialogFragment.AddDialogListener;
import com.shinymayhem.radiopresets.RadioDbContract.StationsDbHelper;
import com.shinymayhem.radiopresets.RadioPlayer.LocalBinder;
import com.shinymayhem.radiopresets.StationsFragment.PlayerListener;

public class MainActivity extends Activity implements AddDialogListener, PlayerListener {

	//string-extra key for intent
	public final static String URL = "com.shinymayhem.radiopresets.URL";

	public static final int BUTTON_LIMIT = 25;
	public static final int LOADER_STATIONS = 0;
	public final static String LOG_FILENAME = "log.txt";
	
	protected boolean mBound = false;
	//protected StationsDbHelper mDbHelper;
	protected RadioPlayer mService;
	protected StationsDbHelper mDbHelper;
	protected Logger mLogger = new Logger();
	
	protected Context getContext()
	{
		return this;
	}
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		log("creating main activity", "d");

		//set content view first so findViewById works
		setContentView(R.layout.activity_main);
		
		if (findViewById(R.id.fragment_container) != null) {
			if (savedInstanceState != null) { //don't create overlapping fragments
                return;
            }
			
			StationsFragment stationsFragment = new StationsFragment();
			stationsFragment.setArguments(getIntent().getExtras());
			
			FragmentManager fragmentManager = getFragmentManager();
			fragmentManager.beginTransaction()
					.add(R.id.fragment_container, stationsFragment)
					.commit();
		}
		
	}
	
	@Override
    public void onDialogPositiveClick(DialogFragment dialog) {
        // User touched the dialog's positive button
		log("add station confirmed", "i");
		EditText titleView = (EditText)dialog.getDialog().findViewById(R.id.station_title);
		EditText urlView = (EditText)dialog.getDialog().findViewById(R.id.station_url);
		
		int preset = 1;
		String title = titleView.getText().toString();
		String url = urlView.getText().toString();
		ContentValues values = new ContentValues();
		values.put(RadioDbContract.StationEntry.COLUMN_NAME_PRESET_NUMBER, preset);
        values.put(RadioDbContract.StationEntry.COLUMN_NAME_TITLE, title);
        values.put(RadioDbContract.StationEntry.COLUMN_NAME_URL, url);
		//CursorLoader var = getLoaderManager().getLoader(MainActivity.LOADER_STATIONS);
		Uri uri = getContentResolver().insert(RadioContentProvider.CONTENT_URI_STATIONS, values);
		log("uri of addition" + uri, "v");
		
    }

    @Override
    public void onDialogNegativeClick(DialogFragment dialog) {
        // User touched the dialog's negative button
    	log("add station cancelled", "i");
    }
	
	

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}
	
	public void play(String url)
	{
		log("Play button received, sending play intent", "d");
		Intent intent = new Intent(this, RadioPlayer.class);
		intent.setAction(RadioPlayer.ACTION_PLAY);
		intent.putExtra(URL, url);
		startService(intent);
		//mService.play(url);
	}
	

	public void stop(View view)
	{
		//Intent intent = new Intent(this, RadioPlayer.class);
		//stopService(intent);
		/*
		log("Stop button received, sending stop intent", "d");
		Intent intent = new Intent(this, RadioPlayer.class);
		intent.setAction(RadioPlayer.ACTION_STOP);
		startService(intent);
		*/
		mService.stop();
	}
	
	//tell service to copy logs to sd card
	public boolean copy(MenuItem item)
	{
		//Intent intent = new Intent(this, RadioPlayer.class);
		//stopService(intent);
		mService.copyLog();
		return true;
	}
	
	//tell service to clear local logs
	public boolean clear(MenuItem item)
	{
		//Intent intent = new Intent(this, RadioPlayer.class);
		//stopService(intent);
		mService.clearLog();
		return true;
	}
	

	private ServiceConnection mConnection = new ServiceConnection()
	{
		@Override
		public void onServiceConnected(ComponentName className, IBinder service)
		{
			log("service connected", "d");
			LocalBinder binder = (LocalBinder) service;
			mService = binder.getService();
			mBound = true;
		}
		@Override
		public void onServiceDisconnected(ComponentName arg0)
		{
			log("service disconnected", "d");
			mBound = false;
		}
	};
	

	private void log(String text, String level)
	{
		mLogger.log(this, text, level);
	}
	
	@Override
	protected void onStart()
	{
		super.onStart();
		log("starting main activity", "d");
		bindRadioPlayer();
	}
	
	protected void bindRadioPlayer()
	{
		log("binding radio player", "d");
		Intent intent = new Intent(this, RadioPlayer.class);
		startService(intent);
		bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
	}
	
	protected void onRestart()
	{
		super.onRestart();
		log("restarting main activity", "d");
	}
	
	protected void onResume()
	{
		super.onResume();
		log("resuming main activity", "d");
	}
	
	@Override
	protected void onStop()
	{
		log("stopping main activity", "d");
		if (mService != null && mService.isPlaying() == false)
		{
			mService.stop();
		}
		if (mBound)
		{
			unbindService(mConnection);
			mBound = false;
		}
		super.onStop();
	}
	
	
	
	@Override
	public void onPause()
	{
		log("pausing main activity", "d");
		super.onPause();
		
	}
	
	public void onDestroy()
	{
		log("destroying main activity", "d");
		log("another experimental fix, sometimes 'end' isn't called on service unbound", "d");
		if (!mService.isPlaying())
		{
			log("mservice.end()", "d");
			mService.end();
		}
		super.onDestroy();
	}	

}