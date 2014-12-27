package com.kurt.swapi;

import java.io.File;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;

import android.app.Activity;

import android.app.ActionBar;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.support.v4.widget.DrawerLayout;
import android.widget.ArrayAdapter;
import android.widget.TextView;


public class MainActivity extends Activity
        implements NavigationDrawerFragment.NavigationDrawerCallbacks {

    // cache variables
    public static long cacheLimit = 150000;
    public static int BLASTCACHE = 0;
    public static int FITCACHE = 1;
    
    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */
    private NavigationDrawerFragment mNavigationDrawerFragment;

    /**
     * Used to store the last screen title. For use in {@link #restoreActionBar()}.
     */
    private CharSequence mTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mNavigationDrawerFragment = (NavigationDrawerFragment)
                getFragmentManager().findFragmentById(R.id.navigation_drawer);
        mTitle = getTitle();

        // Set up the drawer.
        mNavigationDrawerFragment.setUp(
                R.id.navigation_drawer,
                (DrawerLayout) findViewById(R.id.drawer_layout));
    }

    @Override
    public void onNavigationDrawerItemSelected(int position) {
        // update the main content by replacing fragments
        FragmentManager fragmentManager = getFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.container, MainFragment.newInstance(position + 1))
                .commit();
    }

    public void onSectionAttached(int number) {
        Log.d("SWAPI", "Page number " + Integer.toString(number));
        SharedPreferences settings = getSharedPreferences(getString(R.string.prefs), 0);
        mTitle = settings.getString("page" + Integer.toString(number), "null");
    }

    public void restoreActionBar() {
        ActionBar actionBar = getActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(getTitle());
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!mNavigationDrawerFragment.isDrawerOpen()) {
            // Only show items in the action bar relevant to this screen
            // if the drawer is not showing. Otherwise, let the drawer
            // decide what to show in the action bar.
            getMenuInflater().inflate(R.menu.main, menu);
            restoreActionBar();
            return true;
        }
        return super.onCreateOptionsMenu(menu);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    /**
     * Clears out the cache.
     * Full clear: 0
     * Restore limit: 1
     * @author kurt
     *
     */
    public class BlastCache extends AsyncTask<Integer, Void, Void> {

        @Override
        protected Void doInBackground(Integer... params)
        {
            File[] cacheFiles;
            switch (params[0]) {
                case 0:
                    // Clear it all
                    cacheFiles = getCacheDir().listFiles();
                    Log.d("SWAPI", "Blast my cache!");
                    for (File name : cacheFiles) {
                        name.delete();
                    }
                    break;
                case 1:
                    // Clear to size
                    long currentSize = getCacheSize();
                    Log.d("SWAPI", "Current cache size: " + currentSize);
                    while (getCacheSize() > MainActivity.cacheLimit) {
                        // find oldest file
                        cacheFiles = getCacheDir().listFiles();
                        File oldest = cacheFiles[0];
                        for (File f : cacheFiles) {
                            if (oldest.lastModified() < f.lastModified()) oldest = f;
                        }
                        // delete oldest file
                        oldest.delete();
                    }
                    break;
            }
            return null;
        }
        
        /**
         * Count the current cache size.
         * @return size the current cache size
         */
        private long getCacheSize() {
            long size = 0;
            for (File f: getCacheDir().listFiles())
            {
                size += f.length();
            }
            return size;
        }
        
    }


}
