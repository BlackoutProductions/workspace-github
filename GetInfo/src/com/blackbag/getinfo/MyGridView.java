package com.blackbag.getinfo;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import android.app.Activity;
import android.content.ContentUris;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcel;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Contacts;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;

public class MyGridView extends Activity
{
    String TAG = "BLACKBAG.MyGridView";
    ArrayList<Bitmap> photos;
    ArrayList<Integer> contactIds;
    ArrayList<byte[]> byteArray;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.image_grid);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            this.contactIds = (ArrayList<Integer>) extras.get("contactIds");
        }
        
        GridView gridview = (GridView) findViewById(R.id.gridview);
        photos = new ArrayList<Bitmap>();
        InputStream in;
        ByteArrayOutputStream baos;
        Bitmap bitmap;
        for (int contactId : contactIds)
        {
            // only add to list if not null
            if ((in = openPhoto(contactId)) != null)
            {
                photos.add(BitmapFactory.decodeStream(in));
                baos = new ByteArrayOutputStream();
                bitmap = photos.get(photos.size() - 1);
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
                Log.d(TAG, contactId + "=" + Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT));
            }
            if ((in = openDisplayPhoto(contactId)) != null)
            {
                photos.add(BitmapFactory.decodeStream(in));
                baos = new ByteArrayOutputStream();
                bitmap = photos.get(photos.size() - 1);
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
                Log.d(TAG, contactId + "=" + Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT));

            }
        }
        
        gridview.setAdapter(new ImageAdapter(this, photos));
    }
    
    /**
     * Helper function that returns an input-stream for a contact thumbnail photo.
     * Required by getContactPhoto()
     * @param contactId the contact ID of the desired user
     * @return InputStream of the contact thumbnail
     */
    public InputStream openPhoto(long contactId) {
        Uri contactUri = ContentUris.withAppendedId(Contacts.CONTENT_URI, contactId);
        Uri photoUri = Uri.withAppendedPath(contactUri, Contacts.Photo.CONTENT_DIRECTORY);
        Log.v(TAG, photoUri.toString());
        Cursor cursor = getContentResolver().query(photoUri,
             new String[] {Contacts.Photo.PHOTO}, null, null, null);
        if (cursor == null) {
            return null;
        }
        try {
            if (cursor.moveToFirst()) {
                byte[] data = cursor.getBlob(0);
                if (data != null) {
                    return new ByteArrayInputStream(data);
                }
            }
        } finally {
            cursor.close();
        }
        return null;
    }

    /**
     * Helper function that returns the full-size contact photo.
     * Required by getContactPhoto()
     * @param contactId the contact ID of the desired contact
     * @return InputStream of the contact photo
     */
    public InputStream openDisplayPhoto(long contactId) {
        Uri contactUri = ContentUris.withAppendedId(Contacts.CONTENT_URI, contactId);
        Uri displayPhotoUri = Uri.withAppendedPath(contactUri, ContactsContract.Contacts.Photo.DISPLAY_PHOTO);
        Log.v(TAG, displayPhotoUri.toString());
        try {
            AssetFileDescriptor fd =
                getContentResolver().openAssetFileDescriptor(displayPhotoUri, "r");
            return fd.createInputStream();
        } catch (IOException e) {
            return null;
        }
    }

    
    public class ImageAdapter extends BaseAdapter {
        private Context mContext;
        private ArrayList<Bitmap> bitmaps;

        public ImageAdapter(Context c, ArrayList<Bitmap> bitmaps) {
            mContext = c;
            this.bitmaps = bitmaps;
        }

        public int getCount() {
            return bitmaps.size();
        }

        public Object getItem(int position) {
            return null;
        }

        public long getItemId(int position) {
            return 0;
        }

        // create a new ImageView for each item referenced by the Adapter
        public View getView(int position, View convertView, ViewGroup parent) {
            ImageView imageView;
            if (convertView == null) {  // if it's not recycled, initialize some attributes
                imageView = new ImageView(mContext);
                imageView.setLayoutParams(new GridView.LayoutParams(160, 160));
                imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                imageView.setPadding(8, 8, 8, 8);
            } else {
                imageView = (ImageView) convertView;
            }
            
            if (bitmaps.get(position) == null)
            {
                imageView.setImageResource(R.drawable.ic_action_user);
                imageView.setBackgroundColor(Color.GRAY);
            }
            else
                imageView.setImageBitmap(bitmaps.get(position));
            return imageView;
        }
    }

}
