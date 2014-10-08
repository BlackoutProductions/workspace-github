package com.kurt.deletersa;

import java.util.List;

import eu.chainfire.libsuperuser.Shell;

import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class MainFragment extends Fragment {
	
	Context context;
	View rootView;
	
	TextView tvHeader;
	Button btDelete;
	Button btReboot;
	Button btShutdown;
	Process su;
	boolean root = false;
	
	public MainFragment() {
		// to infalte the view inside the activity
	}
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = getActivity();
        
        // start background task to get root access
        (new Startup()).setContext(context).execute();
        
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);
        this.rootView = rootView;
        getUIElements();
        
        return rootView;
    }
    
    private void getUIElements() {
    	tvHeader = (TextView) rootView.findViewById(R.id.tv_header);
    	btDelete = (Button) rootView.findViewById(R.id.bt_delete);
    	btDelete.setOnClickListener(new OnClickListener()
    	{
			@Override
			public void onClick(View view)
			{
				(new Delete()).execute();
			}
		});
    	btReboot = (Button) rootView.findViewById(R.id.bt_reboot);
    	btReboot.setOnClickListener(new OnClickListener()
    	{
    		@Override
    		public void onClick(View view)
    		{
				(new Reboot()).execute();
    		}
    	});
		btShutdown = (Button) rootView.findViewById(R.id.bt_shutdown);
		btShutdown.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View view)
			{
				(new Shutdown()).execute();
			}
		});
    }
    
    private class Startup extends AsyncTask<Void, Void, Void> {
        private ProgressDialog dialog = null;
        private Context context = null;
        private boolean suAvailable = false;
        private String suVersion = null;
        private String suVersionInternal = null;
        private List<String> suResult = null;

        public Startup setContext(Context context) {
            this.context = context;
            return this;
        }

        @Override
        protected void onPreExecute() {
            // We're creating a progress dialog here because we want the user to wait.
            // If in your app your user can just continue on with clicking other things,
            // don't do the dialog thing.

            dialog = new ProgressDialog(context);
            dialog.setTitle("Some title");
            dialog.setMessage("Doing something interesting ...");
            dialog.setIndeterminate(true);
            dialog.setCancelable(false);
            //dialog.show();
        }

        @Override
        protected Void doInBackground(Void... params) {
            // Let's do some SU stuff
            suAvailable = Shell.SU.available();
            if (suAvailable) {
                suVersion = Shell.SU.version(false);
                suVersionInternal = Shell.SU.version(true);
                suResult = Shell.SU.run(new String[] {
                        "id"
                });
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            dialog.dismiss();			

            // output
            StringBuilder sb = (new StringBuilder()).
                    append("Root? ").append(suAvailable ? "Yes" : "No").append((char)10).
                    append("Version: ").append(suVersion == null ? "N/A" : suVersion).append((char)10).
                    append("Version (internal): ").append(suVersionInternal == null ? "N/A" : suVersionInternal).append((char)10).
                    append((char)10);
            if (suResult != null) {
            	root = true;
            	btDelete.setClickable(root);
        		btReboot.setClickable(root);
                for (String line : suResult) {
                    sb.append(line).append((char)10);
                }
            }
            ((TextView) getView().findViewById(R.id.tv_header)).setText(sb.toString());
        }		
    }
    
    private class Delete extends AsyncTask<Void, Void, Void> {
		@Override
		protected Void doInBackground(Void... arg0)
		{
			Shell.SU.run(new String[] {"rm /data/misc/adb/adb_keys", "stop adbd", "start adbd"});
			return null;
		}
		
		@Override
		protected void onPostExecute(Void obj)
		{
			Toast.makeText(context, "Finished deleting keys", Toast.LENGTH_SHORT).show();
		}
    }
    
    private class Reboot extends AsyncTask<Void, Void, Void> {
		@Override
		protected Void doInBackground(Void... params)
		{
			Shell.SU.run("reboot");
			//((PowerManager) getSystemService(Context.POWER_SERVICE)).reboot("Requested by com.kurt.deletersa");
			return null;
		}
    }
    
    private class Shutdown extends AsyncTask<Void, Void, Void> {
    	@Override
    	protected Void doInBackground(Void... params)
    	{
    		
    		//android.internal.app.ShutdownThread.shutdown();
    		return null;
    	}
    }
    
    @Override
    public void onPause() {
    	super.onPause();
    }
}
