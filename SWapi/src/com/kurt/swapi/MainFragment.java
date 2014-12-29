package com.kurt.swapi;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;

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
import android.widget.ExpandableListView;
import android.widget.ProgressBar;
import android.widget.Toast;

public class MainFragment extends Fragment {
    
    private String next, prev;
    // Don't lookup new info if current info is less than 5 minutes old
    private JSONArray results;
    
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
            Date start = new Date();
            Log.d("SWAPI", "Starting data pull: " + start.getTime());
            // Get and parse data
            String url = params[0];
            
            // Check for cached data that is reasonably young
            CacheManager cm = new CacheManager(getActivity());
            File cacheFile = cm.findByUrl(url);
            if (cacheFile != null) {
                try
                {
                    BufferedReader in = new BufferedReader(new FileReader(cacheFile));
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
                        
            // Request JSON data and parse to string
            JSONObject current = new JSONObject();
            DefaultHttpClient httpclient = new DefaultHttpClient();
            HttpGet httpget = new HttpGet(url);
            HttpResponse response;
            StringBuffer sb = new StringBuffer("");
            try
            {
                Log.d("SWAPI", "Requesting " + url);
                response = httpclient.execute(httpget);
                BufferedReader in = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
                String line = "";
                while ((line = in.readLine()) != null) {                    
                    sb.append(line);
                }
                in.close();
                
                // handle JSON
                current = new JSONObject(sb.toString());
            
                // normalize JSON to remove URLs and replace them with name/title
                // need to do this before they get cached
                // remove results array from response
                JSONArray arrayJSON = (JSONArray) current.remove("results");
                JSONArray normalizedJSON = new JSONArray();
                for (int i = 0; i < arrayJSON.length(); i++) {
                    // remove from old array
                    JSONObject temp = (JSONObject) arrayJSON.get(i);
                    // normalize
                    temp = normalizeJSONObject(temp);
                    // put into new array
                    normalizedJSON.put(temp);
                }
                // add refilled array back to results
                current.put("results", normalizedJSON);
            
            } catch (ClientProtocolException e)
            {
                //e.printStackTrace();
                Log.e("SWAPI", "ClientProtocolException");
            } catch (IOException e)
            {
                //e.printStackTrace();
                Log.e("SWAPI", "IOException");
            } catch (JSONException e)
            {
                //e.printStackTrace();
                Log.e("SWAPI", "JSON load failure");
            }

            
            // cache it cache it cache it up
            cm.cacheFileByUrl(url, sb.toString());
            
            
            Date end = new Date();
            Log.d("SWAPI", "End data pull and processing: " + end.getTime());
            long duration = (end.getTime() - start.getTime()) / 1000;
            Log.d("SWAPI", "Action took " + duration + " seconds");

            
            return current;
        }
        
        /**
         * Normalizes and returns JSONObject by removing links and replacing them
         * with their associated name or title.
         * @param obj the JSONObject to normalize
         * @return a normalized JSONObject
         */
        protected JSONObject normalizeJSONObject(JSONObject obj) {
            JSONObject normalized = new JSONObject();
            try
            {
                Iterator<String> iter = obj.keys();
                while (iter.hasNext()) {
                    String key = iter.next();
                    String normalizedKey = "";
                    StringBuilder value = new StringBuilder();
                    normalizedKey = key.replace("_", " ");
                    
                    // SINGLE LINK
                    // homeworld
                    if (key.equals("homeworld")) {
                            value.append(getNameFromUrl(obj.get("homeworld")));
                    }
                    // url - do nothing
                    
                    // ARRAYS OF LINKS
                    // films
                    // species
                    // vehicles
                    // starships
                    else if (key.equals("films") || key.equals("species") || key.equals("vehicles") || key.equals("starships") || key.equals("characters") || key.equals("planets")) {
                        JSONArray arr = obj.getJSONArray(key);
                        ArrayList<String> values = new ArrayList<String>();
                        for (int i = 0; i < arr.length(); i++) {
                            values.add(getNameFromUrl(arr.get(i)));
                        }
                        Collections.sort(values);
                        value.append(values.toString().replace("[", "").replace("]", ""));
                    }
                    // DATE OBJECTS
                    // created
                    // edited
                    else if (key.equals("created") || key.equals("edited")) {
                        String date = obj.getString(key);
                        date = date.replace("T", " ").replace("Z", " UTC");
                        value.append(date);
                    }
                    // Special non-string value THAT ONLY EXISTS IN FILMS
                    else if (key.equals("episode_id")) {
                        value.append(Integer.toString(obj.getInt("episode_id")));
                    }
                    // String values are the easiest
                    else {
                        value.append(obj.getString(key));
                    }
                    
                    // remove the key/value and reinsert new
                    iter.remove();
                    normalized.putOpt(normalizedKey, value.toString());
                    
                }
            } catch (JSONException e)
            {
                //e.printStackTrace();
                Log.e("SWAPI", "Fail to manipulate JSON: MainFragment.NormalizeJSONObject");
            }
            
            return normalized;
        }
        
        /**
         * Helper function to get a name or title from a URL
         * @param url the URL to get
         * @return the name or title from the JSONObject at the url
         */
        protected String getNameFromUrl(Object url) {
            StringBuilder sb = new StringBuilder();
            DefaultHttpClient httpclient = new DefaultHttpClient();
            HttpGet httpget = new HttpGet(url.toString());
            HttpResponse response;
            String toReturn = "";
            CacheManager cm = new CacheManager(getActivity());

            try
            {
                File cacheFile = cm.findByUrl(url.toString());
                // check cache for existence of matching file
                if (cacheFile != null) {
                    Log.d("SWAPI", "Cache file found: " + cacheFile.toString());
                    BufferedReader in = new BufferedReader(new FileReader(cacheFile));
                    String line;
                    while ((line = in.readLine()) != null) sb.append(line);
                    in.close();
                }
                else {
                    // if not load resource from web
                    Log.d("SWAPI", "Requesting " + url.toString());
                    response = httpclient.execute(httpget);
                    BufferedReader in = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
                    String line = "";
                    while ((line = in.readLine()) != null) {                    
                        sb.append(line);
                    }
                    in.close();
                    // cache file for later use
                    cm.cacheFileByUrl(url.toString(), sb.toString());
                }
                
                // handle JSON
                JSONObject current = new JSONObject(sb.toString());
                sb = new StringBuilder();
                if (current.has("title")) toReturn = current.getString("title");
                if (current.has("name")) toReturn = current.getString("name");
            } catch (ClientProtocolException e)
            {
                //e.printStackTrace();
                Log.d("SWAPI", "Parse URL for name/title fail: MainFragment.getNameFromUrl");
            } catch (IOException e)
            {
                //e.printStackTrace();
                Log.d("SWAPI", "IO write fail: MainFragment.getNameFromUrl");
            } catch (JSONException e)
            {
                //e.printStackTrace();
                Log.d("SWAPI", "Load JSON fail: MainFragment.doInBackground.getNameFromUrl");
            }

            // if nothing works return the url
            return (toReturn.equals("")) ? url.toString(): toReturn;
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
                    //new NormalizeJSONObject().execute(temp);
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