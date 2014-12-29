package com.kurt.swapi;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;

import android.app.Activity;
import android.os.AsyncTask;
import android.util.Log;

public class CacheManager {

    // instance fields
    private static Activity activity;
    private static File cacheDir;
    private static long timeLimit = 5 * 60 * 1000;
    
    public CacheManager(Activity aActivity) {
        activity = aActivity;
        cacheDir = activity.getCacheDir();
    }
    
    /**
     * Find the desired file from the passed-in url.
     * @param the url that relates to the file name
     * @return a reference to the file if it exists, nil if not
     */
    public File findByUrl(String url) {
        // Parse url for file name
        // normalize url into file string
        String search = normalizeUrl(url);
        Log.d("SWAPI", "Searching for file: " + search + " from url " + url);
        
        // Search files in cache for matching file name
        for (String s : cacheDir.list()) {
            if (search.equals(s.split("-")[0])) {
                // file found, check age
                long now = new Date().getTime();
                long old = Long.parseLong(s.split("-")[1]);
                if (now - old > timeLimit) {
                    // file is older than limit, delete file and return null
                    File file = new File(cacheDir.getPath() + "/" + s);
                    file.delete();
                    return null;
                }
                else {
                    // file is within time limit, return file
                    return new File(cacheDir.getPath() + "/" + s);
                }
            }
        }
        
        // catch all
        return null;
    }
    
    public void cacheFileByUrl(String url, String payload) {
        try
        {
            // cache naming convention: <data><page>-<current time in milliseconds>
            File file = new File(cacheDir, normalizeUrl(url) + "-" + new Date().getTime());
            OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(file));
            outputStream.write(payload.getBytes());
            outputStream.close();
            // check cache size and clean if necessary
            new BlastCache().execute(MainActivity.FITCACHE);
        } catch (IOException e)
        {
            //e.printStackTrace();
            Log.e("SWAPI", "Cache file creation failure: CacheManager.cacheFilebByUrl");
        }
    }
    
    /**
     * Normalize url into a file name by removing GET variable string and remaining '/'
     * @param url the url to normalize
     * @return the normalized url as a cache file name
     */
    private String normalizeUrl(String url) {
        return url.replace(activity.getString(R.string.root), "").replace("/?", "").replace("=", "").replace("/", "");
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
                    cacheFiles = cacheDir.listFiles();
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
                        cacheFiles = cacheDir.listFiles();
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
            for (File f: cacheDir.listFiles())
            {
                size += f.length();
            }
            return size;
        }
        
    }
}
