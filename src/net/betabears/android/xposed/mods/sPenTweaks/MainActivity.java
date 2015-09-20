package net.betabears.android.xposed.mods.sPenTweaks;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends PreferenceActivity {
    private final Context ctx = this;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getPreferenceManager().setSharedPreferencesMode(Context.MODE_WORLD_READABLE);
		addPreferencesFromResource(R.xml.preferences);
	}
}
