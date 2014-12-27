package com.kurt.swapi;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

public class MainFragment extends Fragment {
    
    private String next, prev;
    // Don't lookup new info if current info is less than 5 minutes old
    private long timeLimit = 5 * 60 * 1000;
    
    /**
     * The fragment argument representing the section number for this
     * fragment.
     */
    private static final String ARG_SECTION_NUMBER = "section_number";

    // instance fields
    Context context;
    View rootView;
    static int sectionNumber = 0;
    String data = "";
    
    // UI
    ExpandableListView elv;
    ProgressBar pb;
    
    /**
     * Returns a new instance of this fragment for the given section
     * number.
     */
    public static MainFragment newInstance(int aSectionNumber) {
        MainFragment fragment = new MainFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_SECTION_NUMBER, aSectionNumber);
        fragment.setArguments(args);
        sectionNumber = aSectionNumber;
        return fragment;
    }

    public MainFragment() {
        // to inflate the view inside the activity
    }
    
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        ((MainActivity) activity).onSectionAttached(
                getArguments().getInt(ARG_SECTION_NUMBER));
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = getActivity();
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
        Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);
        this.rootView = rootView;
        getUIElements();
        
        // Get data type from settings
        SharedPreferences settings = getActivity().getSharedPreferences(getString(R.string.prefs), 0);
        data = settings.getString("page" + Integer.toString(sectionNumber), "");
        if (data == "")
            Log.d("SWAPI", "Cannot locate data type: " + data);
        else {
            // move on
            Log.d("SWAPI", "Finding data: " + data);
            String baseurl = getActivity().getString(R.string.root);
            new GetDataTask().execute(baseurl + data + getString(R.string.firstPage));
        }
        
        return rootView;
    }
        
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        switch (id) {
        case R.id.action_backward:
            if (prev == "null") {
                Toast.makeText(getActivity(), "No previous data", Toast.LENGTH_SHORT).show();
                return true;
            }
            else {
                new GetDataTask().execute(prev);
                return true;
            }
        case R.id.action_forward:
            if (next == "null") {
                Toast.makeText(getActivity(), "No next data", Toast.LENGTH_SHORT).show();
                return true;
            }
            else {
                new GetDataTask().execute(next);
                return true;
            }
        }
        
        return super.onOptionsItemSelected(item);
    }
    
    private void getUIElements() {
        // Expandable list
        elv = (ExpandableListView) rootView.findViewById(R.id.elv_list);
        // Progress bar
        pb = (ProgressBar) rootView.findViewById((R.id.pb_fragment_main));
        pb.setMax(0);
    }
    
    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        //MenuInflater inflater = getActivity().getMenuInflater();
        //inflater.inflate(R.menu.main, menu);
        if (prev == "null") {
            MenuItem item = menu.findItem(R.id.action_backward);
            if (item != null) {
                item.setIcon(R.drawable.ic_action_cancel);
                item.setEnabled(false);
            }
        }
        else {
            MenuItem item = menu.findItem(R.id.action_backward);
            if (item != null) {
                item.setIcon(R.drawable.ic_action_back);
                item.setEnabled(true);
            }
        }
        if (next == "null") {
            MenuItem item = menu.findItem(R.id.action_forward);
            if (item != null) {
                item.setIcon(R.drawable.ic_action_cancel);
                item.setEnabled(false);
            }
        }
        else {
            MenuItem item = menu.findItem(R.id.action_forward);
            if (item != null) {
                item.setIcon(R.drawable.ic_action_forward);
                item.setEnabled(true);
            }
        }
    }
    
    private class GetDataTask extends AsyncTask<String, Integer, JSONObject> {
        @Override
        protected void onPreExecute() {
            // Add updating spinner
            pb.setVisibility(View.VISIBLE);
            elv.setVisibility(View.INVISIBLE);
        }
        
        @Override
        protected JSONObject doInBackground(String... params)
        {
            // Get and parse data
            String url = params[0];
            
            // Check for cached data that is reasonably young
            String[] cacheList = getActivity().getCacheDir().list();
            String search = url.replace(getString(R.string.root), "").replace("/?page=", "");
            for (String s : cacheList) {
                if (search.equals(s.split("-")[0])) {
                    long now = new Date().getTime();
                    long old = Long.parseLong(s.split("-")[1]);
                    if (now - old < timeLimit) {
                        // read cache file into JSONObject and return
                        try
                        {
                            BufferedReader in = new BufferedReader(new FileReader(new File(getActivity().getCacheDir(), s)));
                            StringBuffer sb = new StringBuffer("");
                            String line;
                            while ((line = in.readLine()) != null) sb.append(line);
                            in.close();
                            return new JSONObject(sb.toString());
                        } catch (FileNotFoundException e)
                        {
                            //e.printStackTrace();
                            Log.d("SWAPI", "File not found error: MainFragment.doInBackground");
                        } catch (IOException e)
                        {
                            //e.printStackTrace();
                            Log.d("SWAPI", "File read error: MainFragment.doInBackground");
                        } catch (JSONException e)
                        {
                            //e.printStackTrace();
                            Log.d("SWAPI", "Error converting JSON error: MainFragment.doInBackground");
                        }
                    }
                    // remove outdated cache file and continue
                    File file = new File(getActivity().getCacheDir().getPath() + "/" + s);
                    file.delete();
                    break;
                }
            }
            
            JSONObject current = new JSONObject();
            DefaultHttpClient httpclient = new DefaultHttpClient();
            HttpGet httpget = new HttpGet(url);
            HttpResponse response;
            StringBuffer sb = new StringBuffer("");
            try
            {
                response = httpclient.execute(httpget);
                BufferedReader in = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
                String line = "";
                while ((line = in.readLine()) != null) {                    
                    sb.append(line);
                }
                in.close();
                
                // handle JSON
                current = new JSONObject(sb.toString());
            } catch (ClientProtocolException e)
            {
                //e.printStackTrace();
                Log.d("SWAPI", "ClientProtocolException");
            } catch (IOException e)
            {
                //e.printStackTrace();
                Log.d("SWAPI", "IOException");
            } catch (JSONException e)
            {
                //e.printStackTrace();
                Log.d("SWAPI", "JSON load failure");
            }
            
            // trim url for cache file name
            url = url.replace(getString(R.string.root), "").replace("/?page=", "");
            url += "-" + new Date().getTime();
            // cache it cache it cache it up
            try
            {
                // cache naming convention: <data><page>-<current time in milliseconds>
                File file = new File(getActivity().getCacheDir(), url);
                OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(file));
                outputStream.write(sb.toString().getBytes());
                outputStream.close();
                // check cache size and clean if necessary
                new BlastCache().execute(MainActivity.FITCACHE);
            } catch (IOException e)
            {
                //e.printStackTrace();
                Log.d("SWAPI", "File creation failure: MainFragment.doInBackground");
            }

            return current;
        }
        
        @Override
        protected void onProgressUpdate(Integer... progress) {
            Log.d("SWAPI", "Updating progress");
            // Update progress bar
            if (pb.getMax() == 0) {
                // set max if not set
                pb.setMax(progress[0]);
            }
            else {
                // set progress
                pb.setProgress(progress[0]);
            }
        }

        
        @Override
        protected void onPostExecute(JSONObject current) {
            // variables needed with JSON manipulation
            JSONArray results = new JSONArray();
            ArrayList<String> data = new ArrayList<String>();
            HashMap<String, ArrayList<String[]>> map = new HashMap<String, ArrayList<String[]>>();
            // werk it JSON
            try
            {
                // set next and prev
                next = current.getString("next");
                if (next == "null") {
                    
                }
                prev = current.getString("previous");
                if (prev == "null") {
                    
                }
                // Get results array
                results = current.getJSONArray("results");
                for (int i = 0; i < results.length(); i++) {
                    JSONObject temp = results.getJSONObject(i);
                    Iterator<String> iter = temp.keys();
                    ArrayList<String[]> props = new ArrayList<String[]>();
                    String master = "";
                    while (iter.hasNext()) {
                        String key = iter.next();
                        if (key.equals("name")) {
                            data.add(temp.getString("name"));
                            master = temp.getString("name");
                        }
                        else if (key.equals("title")) {
                            data.add(temp.getString("title"));
                            master = temp.getString("title");
                        }
                        else {
                            props.add(new String[]{key, temp.getString(key)});
                        }
                    }
                    map.put(master, props);
                }
            } catch (JSONException e)
            {
                e.printStackTrace();
                Log.d("SWAPI", "Fail to manipulate JSON: MainFragment.onPostExecute");
            }
            // Remove updating spinner and show listview
            pb.setVisibility(View.INVISIBLE);
            elv.setVisibility(View.VISIBLE);
            // set list adapter
            elv.setAdapter(new ExpandableListAdapter(context, data, map));
            
            // update menu to reflect available navigation options
            getActivity().invalidateOptionsMenu();
        }
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
                    cacheFiles = getActivity().getCacheDir().listFiles();
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
                        cacheFiles = getActivity().getCacheDir().listFiles();
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
            for (File f: getActivity().getCacheDir().listFiles())
            {
                size += f.length();
            }
            return size;
        }
        
    }
    
}
