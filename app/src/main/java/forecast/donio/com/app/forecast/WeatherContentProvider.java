package forecast.donio.com.app.forecast;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.location.Location;
import android.net.Uri;
import android.support.annotation.Nullable;

/**
 * Created by TranT.Phuong on 5/22/16.
 */
public class WeatherContentProvider extends ContentProvider {

    static final int WEATHER = 100;
    static final int WEATHER_WITH_LOCATION = 101;
    static final int WEATHER_WITH_LOCATION_AND_DATE = 102;
    static final int LOCATION = 300;

    private static final UriMatcher sUrimatcher = buildUriMatcher();

    private static UriMatcher buildUriMatcher() {
        final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);

        final String authority = WeatherContract.CONTENT_AUTHORITY;

        // For each type of URI you want to add, create a corresponding code.
        matcher.addURI(authority, WeatherContract.PATH_WEATHER, WEATHER);
        matcher.addURI(authority, WeatherContract.PATH_WEATHER + "/*", WEATHER_WITH_LOCATION);
        matcher.addURI(authority, WeatherContract.PATH_WEATHER + "/*/#", WEATHER_WITH_LOCATION_AND_DATE);
        matcher.addURI(authority, WeatherContract.PATH_LOCATION, LOCATION);

        return matcher;
    }

    private static final SQLiteQueryBuilder sWeatherByLocationSettingQueryBuilder;

    // location.location_setting = ?
    private static final String sLocationSettingSelection = WeatherContract.LocationEntry.TABLE_NAME
            + "." + WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING + " = ?";

    // location.location_setting = ? AND date >= ?
    private static final String sLocationSettingWithStartDateSelection = WeatherContract.LocationEntry.TABLE_NAME
            + "." + WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING + " = ? AND " +
            WeatherContract.WeatherEntry.COLUMN_DATE + " >= ?";

    private static final String sLocationSettingAndDaySelection = WeatherContract.LocationEntry.TABLE_NAME +
            "." + WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING + " = ? AND " +
            WeatherContract.WeatherEntry.COLUMN_DATE + " = ?";

    static {
        sWeatherByLocationSettingQueryBuilder = new SQLiteQueryBuilder();

        //This is an inner join which looks like
        //weather INNER JOIN location ON weather.location_id = location._id
        sWeatherByLocationSettingQueryBuilder.setTables(WeatherContract.WeatherEntry.TABLE_NAME
         + " INNER JOIN " + WeatherContract.LocationEntry.TABLE_NAME + " ON " +
         "." + WeatherContract.WeatherEntry.COLUMN_LOC_KEY + " = " + WeatherContract.LocationEntry.TABLE_NAME
         + "." + WeatherContract.LocationEntry._ID);
    }
    private WeatherDbHelper mDbHelper;


    @Override
    public boolean onCreate() {
        mDbHelper = new WeatherDbHelper(getContext());
        return true;
    }

    @Nullable
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        Cursor cursor;
        switch (sUrimatcher.match(uri)) {
            // "weather/*/#"
            case WEATHER_WITH_LOCATION_AND_DATE:
                cursor = getWeatherByLocationAndDate(uri, projection, sortOrder);
                break;
            // "weather/*"
            case WEATHER_WITH_LOCATION:
                cursor = getWeatherByLocationSetting(uri, projection, sortOrder);
                break;
            case WEATHER:
                cursor = mDbHelper.getReadableDatabase().query(WeatherContract.WeatherEntry.TABLE_NAME,
                                                                                            projection,
                                                                                            selection,
                                                                                            selectionArgs,
                                                                                            null,
                                                                                            null,
                                                                                            sortOrder);
                break;
            case LOCATION:
                cursor = mDbHelper.getReadableDatabase().query(WeatherContract.LocationEntry.TABLE_NAME,
                                                                                            projection,
                                                                                            selection,
                                                                                            selectionArgs,
                                                                                            null,
                                                                                            null,
                                                                                            sortOrder);
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri" + uri);
        }

        cursor.setNotificationUri(getContext().getContentResolver(), uri);
        return cursor;
    }

    private Cursor getWeatherByLocationSetting(Uri uri, String[] projection, String sortOrder) {
        String locationSetting = WeatherContract.WeatherEntry.getLocationSettingFromUri(uri);
        long startDate = WeatherContract.WeatherEntry.getStartDateFromUri(uri);

        String[] selectionArgs;
        String selection;

        if (startDate == 0) {
            selection = sLocationSettingSelection;
            selectionArgs = new String[] {locationSetting};
        } else {
            selection = sLocationSettingWithStartDateSelection;
            selectionArgs = new String[] {locationSetting, Long.toString(startDate)};
        }

        return sWeatherByLocationSettingQueryBuilder.query(mDbHelper.getReadableDatabase(),
                projection,
                selection,
                selectionArgs,
                null,
                null,
                sortOrder);
    }

    private Cursor getWeatherByLocationAndDate(Uri uri, String[] projection, String sortOrder) {
        // get location setting from uri that parsed in
        String locationSetting = WeatherContract.WeatherEntry.getLocationSettingFromUri(uri);

        long date = WeatherContract.WeatherEntry.getDateFromUri(uri);

        return sWeatherByLocationSettingQueryBuilder.query(mDbHelper.getReadableDatabase(),
                projection,
                sLocationSettingAndDaySelection,
                new String[] {locationSetting, Long.toString(date)},
                null,
                null,
                sortOrder);
    }

    @Nullable
    @Override
    public String getType(Uri uri) {
        switch (sUrimatcher.match(uri)) {
            case WEATHER_WITH_LOCATION:
                return WeatherContract.WeatherEntry.CONTENT_TYPE;
            case WEATHER_WITH_LOCATION_AND_DATE:
                return WeatherContract.WeatherEntry.CONTENT_ITEM_TYPE;
            case WEATHER:
                return WeatherContract.WeatherEntry.CONTENT_TYPE;
            case LOCATION:
                return WeatherContract.LocationEntry.CONTENT_TYPE;
            default:
                throw new UnsupportedOperationException("Unknown uri" + uri);
        }
    }

    @Nullable
    @Override
    public Uri insert(Uri uri, ContentValues values) {
        final SQLiteDatabase db = mDbHelper.getWritableDatabase();
        Uri returnUri = null;

        switch (sUrimatcher.match(uri)) {
            case WEATHER: {
                normalizeDate(values);
                long _id = db.insert(WeatherContract.WeatherEntry.TABLE_NAME, null, values);

                // if added successfully
                if (_id > 0) {
                    returnUri = WeatherContract.WeatherEntry.buildWeatherUri(_id);
                } else {
                    throw new android.database.SQLException("Failed to insert row into" + uri);
                }
                break;
            }
            case LOCATION:
                long _id = db.insert(WeatherContract.LocationEntry.TABLE_NAME, null, values);

                if (_id > 0) {
                    returnUri = WeatherContract.LocationEntry.buildLocationUri(_id);
                } else {
                    throw new android.database.SQLException("Failed to insert row into" + uri);
                }
                break;
        }

        getContext().getContentResolver().notifyChange(uri, null);

        return returnUri;
    }

    @Override
    public int bulkInsert(Uri uri, ContentValues[] values) {
        final SQLiteDatabase db = mDbHelper.getWritableDatabase();
        switch (sUrimatcher.match(uri)) {
            case WEATHER:
                db.beginTransaction();
                int returnCount = 0;
                try {
                    for (ContentValues value : values) {
                        normalizeDate(value);
                        long _id = db.insert(WeatherContract.WeatherEntry.TABLE_NAME, null, value);

                        if (_id != -1) {
                            returnCount++;
                        }
                    }
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
                getContext().getContentResolver().notifyChange(uri, null);
                return returnCount;
            default:
                throw new UnsupportedOperationException("Unknown uri" + uri);
        }
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        int rowsDeleted = 0;
        final SQLiteDatabase db = mDbHelper.getWritableDatabase();

        switch (sUrimatcher.match(uri)) {
            case WEATHER:
                rowsDeleted = db.delete(WeatherContract.WeatherEntry.TABLE_NAME, selection, selectionArgs);
                break;
            case LOCATION:
                rowsDeleted = db.delete(WeatherContract.LocationEntry.TABLE_NAME, selection, selectionArgs);
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri" + uri);
        }

        if (rowsDeleted != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }

        return rowsDeleted;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        int rowsUpdated = 0;

        final SQLiteDatabase db = mDbHelper.getWritableDatabase();
        switch (sUrimatcher.match(uri)) {
            case WEATHER:
                normalizeDate(values);
                rowsUpdated = db.update(WeatherContract.WeatherEntry.TABLE_NAME, values, selection, selectionArgs);
                break;
            case LOCATION:
                normalizeDate(values);
                rowsUpdated = db.update(WeatherContract.LocationEntry.TABLE_NAME, values, selection, selectionArgs);
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri" + uri);

        }

        if (rowsUpdated != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }

        return rowsUpdated;
    }

    private void normalizeDate(ContentValues values) {

        if (values.containsKey(WeatherContract.WeatherEntry.COLUMN_DATE)) {
            long dateValue = values.getAsLong(WeatherContract.WeatherEntry.COLUMN_DATE);

            values.put(WeatherContract.WeatherEntry.COLUMN_DATE, dateValue);
        }
    }
}
