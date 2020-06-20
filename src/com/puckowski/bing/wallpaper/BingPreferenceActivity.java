package com.puckowski.bing.wallpaper;

import android.os.Bundle;
import android.preference.PreferenceActivity;

public class BingPreferenceActivity extends PreferenceActivity 
{
	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		
		addPreferencesFromResource(R.xml.wallpaperpreferences);
	}
}
