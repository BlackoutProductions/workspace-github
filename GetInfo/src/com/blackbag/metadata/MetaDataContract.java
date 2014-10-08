package com.blackbag.metadata;

import android.provider.BaseColumns;

public final class MetaDataContract
{
    // To prevent someone from accidentally instantiating the contract class,
    // give it an empty constructor.
    public MetaDataContract()
    {
    }

    /* Inner class that defines the table constants */
    public static abstract class MetaDataEntry implements BaseColumns
    {
        public static final String TABLE_NAME = "metadata";
        public static final String COL_KEY = "key";
        public static final String COL_VALUE = "value";
    }
}
