package com.kurt.swapi;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
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
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListView;
import android.widget.ProgressBar;

public class MainFragment extends Fragment {
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
            new GetDataTask().execute(data);
        }        
        
        return rootView;
    }
    
    private void getUIElements() {
        // Expandable list
        elv = (ExpandableListView) rootView.findViewById(R.id.elv_list);
        // Progress bar
        pb = (ProgressBar) rootView.findViewById((R.id.pb_fragment_main));
        pb.setMax(0);
    }
    
    private class GetDataTask extends AsyncTask<String, Integer, ArrayList<JSONObject>> {
        @Override
        protected void onPreExecute() {
            // Add updating spinner
            pb.setVisibility(View.VISIBLE);
        }
        
        @Override
        protected ArrayList<JSONObject> doInBackground(String... params)
        {
            // Get and parse data
            String baseurl = getActivity().getString(R.string.root);
            String url = baseurl + params[0] + "/";
            ArrayList<JSONObject> objects = new ArrayList<JSONObject>();
            // Max nubmer of entries, useful for tracking progress
            int max = 10;
            String currentID = "";
            boolean done = false;
            for (int i = 0; !done; i++) {
                DefaultHttpClient httpclient = new DefaultHttpClient();
                HttpGet httpget = new HttpGet(url + currentID);
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
                    JSONObject current = new JSONObject(sb.toString());
                    // bail if no results available
                    if (i > 9 || (current.has("detail") && current.getString("detail") == "Not found")) {
                        // only get 10 at a time
                        done = true;
                    } else if (current.has("count")) {
                        // update total count if available
                        //publishProgress(current.getInt("count"));
                        currentID = "1";
                    } else {
                        // add object to arraylist
                        objects.add(current);
                        // udpate currentID
                        currentID = Integer.toString(objects.size() + 1);
                        // update progress
                        publishProgress(objects.size());
                    }

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
            } // for

            return objects;
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
        protected void onPostExecute(ArrayList<JSONObject> objects) {
            // Remove updating spinner
            pb.setVisibility(View.INVISIBLE);
            // Update list
        }
    }
    
}
