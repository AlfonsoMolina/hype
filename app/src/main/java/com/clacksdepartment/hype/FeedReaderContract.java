package com.clacksdepartment.hype;

import android.provider.BaseColumns;

final class FeedReaderContract {

    // To prevent someone from accidentally instantiating the contract class,
    // make the constructor private.
    private FeedReaderContract() {}

    /* Inner class that defines the table contents */
    static class FeedEntryReleases implements BaseColumns {
        static final String TABLE_NAME = "RELEASES";
        static final String COLUMN_REF = "Ref";
        static final String COLUMN_TITLE = "Title";
        static final String COLUMN_COVER = "Cover";
        static final String COLUMN_COVER_LINK = "Cover_link";
        static final String COLUMN_SYNOPSIS = "Synopsis";
        static final String COLUMN_TRAILER = "Trailer";
        static final String COLUMN_RELEASE_DATE_STRING = "Release_date_string";
        static final String COLUMN_RELEASE_DATE = "Release_date";
        static final String COLUMN_HYPE = "Hyped";
        static final String COLUMN_TYPE = "Type";
    }

    static final String SQL_CREATE_ENTRIES_RELEASES =
            "CREATE TABLE " + FeedEntryReleases.TABLE_NAME + " (" +
                    FeedEntryReleases._ID + " INTEGER PRIMARY KEY," +
                    FeedEntryReleases.COLUMN_REF + " TEXT," +
                    FeedEntryReleases.COLUMN_TITLE + " TEXT," +
                    FeedEntryReleases.COLUMN_COVER + " BLOB," +
                    FeedEntryReleases.COLUMN_COVER_LINK + " TEXT," +
                    FeedEntryReleases.COLUMN_SYNOPSIS + " TEXT," +
                    FeedEntryReleases.COLUMN_TRAILER + " TEXT," +
                    FeedEntryReleases.COLUMN_RELEASE_DATE_STRING + " TEXT," +
                    FeedEntryReleases.COLUMN_RELEASE_DATE + " DATE," +
                    FeedEntryReleases.COLUMN_HYPE + " INTEGER," +
                    FeedEntryReleases.COLUMN_TYPE + " INTEGER)";

    static final String SQL_DELETE_ENTRIES_RELEASES =
            "DROP TABLE IF EXISTS " + FeedEntryReleases.TABLE_NAME;

}