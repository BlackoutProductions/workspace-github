package com.blackbag.metadata;

import com.blackbag.metadata.MetaDataContract.MetaDataEntry;

import android.content.Context;
import android.database.sqlite.*;

public class MetaDataDbHelper extends SQLiteOpenHelper
{

    private static final String TEXT_TYPE = " TEXT";
    private static final String COMMA_SEP = ",";
    private static final String SQL_CREATE_ENTRIES = "CREATE TABLE "
            + MetaDataEntry.TABLE_NAME + " (" + MetaDataEntry._ID
            + " INTEGER PRIMARY KEY," + MetaDataEntry.COL_KEY + TEXT_TYPE
            + COMMA_SEP + MetaDataEntry.COL_VALUE + TEXT_TYPE + " )";

    private static final String SQL_DELETE_ENTRIES = "DROP TABLE IF EXISTS "
            + MetaDataEntry.TABLE_NAME;

    // If you change the database schema, you must increment the database
    // version.
    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "metadata.db";

    public MetaDataDbHelper(Context context)
    {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public void onCreate(SQLiteDatabase db)
    {
        db.execSQL(SQL_CREATE_ENTRIES);
    }

    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
    {
        // This database is only a cache for online data, so its upgrade policy
        // is
        // to simply to discard the data and start over
        db.execSQL(SQL_DELETE_ENTRIES);
        onCreate(db);
    }

    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion)
    {
        onUpgrade(db, oldVersion, newVersion);
    }
}
