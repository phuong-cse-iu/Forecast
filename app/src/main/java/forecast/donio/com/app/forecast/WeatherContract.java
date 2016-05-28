package forecast.donio.com.app.forecast;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.net.Uri;
import android.provider.BaseColumns;
import android.text.format.Time;

/**
 * Created by TranT.Phuong on 5/22/16.
 */
public class WeatherContract {
    // authority
    public static final String CONTENT_AUTHORITY = "forecast.donio.com.app.forecast";

    // base uri = content://forecast.donio.com.app.forecast
    public static final Uri BASE_URI = Uri.parse("content://" + CONTENT_AUTHORITY);

    // this means we will create two table for maintaining data downloaded from the internet
    // that is weather and location table
    static final String PATH_WEATHER = "weather";
    static final String PATH_LOCATION = "location";

    // To make it easy to query for the exact date, we normalize all dates that go into
    // the database to the start of the the Julian day at UTC.
    public static long normalizeDate(long startDate) {
        // normalize the start date to the beginning of the (UTC) day
        Time time = new Time();
        time.setToNow();
        time.set(startDate);
        int julianDay = Time.getJulianDay(startDate, time.gmtoff);
        return time.setJulianDay(julianDay);
    }

    // the inner class to define the contents of location table

    public static final class LocationEntry implements BaseColumns {

        static final Uri CONTENT_URI = BASE_URI.buildUpon().appendPath(PATH_LOCATION).build();

        /*
            Because we have to manipulate with particular item or list of items
            So we need a way to control that kind of problem
         */

        // the "list of item" path
        static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/" +
                                                    CONTENT_AUTHORITY + "/" +
                                                    PATH_LOCATION;
        // the "particular item" path
        static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/"
                                                    + CONTENT_AUTHORITY
                                                    + "/" + PATH_LOCATION;

        // define table name
        // The location setting string is what will be sent to openweathermap
        // as the location query.
        static final String TABLE_NAME = "location";

        // define column name for location table
        static final String COLUMN_LOCATION_SETTING = "location_setting";

        static final String COLUMN_CITY_NAME = "city_name";

        // In order to uniquely pinpoint the location on the map when we launch the
        // map intent, we store the latitude and longitude as returned by openweathermap.
        static final String COLUMN_COORD_LAT = "coord_lat";
        static final String COLUMN_COORD_LON = "coord_lon";

        // we need a way to build the uri with id
        // the format is like this: content://authority/location/id
        // this is the helper method to insert row into database
        public static Uri buildLocationUri(long id) {
            return ContentUris.withAppendedId(CONTENT_URI, id);
        }
    }



    // the inner class to define the contents of weather table
    public static final class WeatherEntry implements BaseColumns {

        static final Uri CONTENT_URI = BASE_URI.buildUpon().appendPath(PATH_WEATHER).build();

        static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/" +
                                                    CONTENT_AUTHORITY + "/" +
                                                    PATH_WEATHER;

        static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" +
                                                    CONTENT_AUTHORITY + "/" +
                                                    PATH_WEATHER;

        static final String TABLE_NAME = "weather";
        // Column with the foreign key into the location table.
        static final String COLUMN_LOC_KEY = "location_id";
        // Date, stored as long in milliseconds since the epoch
        static final String COLUMN_DATE = "date";
        // Weather id as returned by API, to identify the icon to be used
        static final String COLUMN_WEATHER_ID = "weather_id";

        // Short description and long description of the weather, as provided by API.
        // e.g "clear" vs "sky is clear".
        static final String COLUMN_SHORT_DESC = "short_desc";

        // Min and max temperatures for the day (stored as floats)
        static final String COLUMN_MIN = "min";
        static final String COLUMN_MAX = "max";

        // Humidity is stored as a float representing percentage
        static final String COLUMN_HUMIDITY = "humidity";

        static final String COLUMN_PRESSURE = "pressure";

        static final String COLUMN_WIND = "wind";

        static final String COLUMN_DEGREES = "degrees";

        public static Uri buildWeatherUri(long id) {
            return ContentUris.withAppendedId(CONTENT_URI, id);
        }


         /*
            Student: Fill in this buildWeatherLocation function
         */
        public static Uri buildWeatherLocation(String locationSetting) {
            return null;
        }

        // content://app.com.donio.weather/weather/locationsetting?date=dateNumber
        public static Uri buildWeatherLocationWithStartDate(String locationSetting, long startDate) {
            long normalizeDate = normalizeDate(startDate);
            return CONTENT_URI.buildUpon().appendPath(locationSetting).appendQueryParameter(COLUMN_DATE, Long.toString(normalizeDate)).build();
        }

        public static Uri buildWeatherLocationWithDate(String locationSetting, long date) {
            return CONTENT_URI.buildUpon().appendPath(locationSetting).appendPath(Long.toString(normalizeDate(date))).build();
        }

        public static String getLocationSettingFromUri(Uri uri) {
            return uri.getPathSegments().get(1);

        }

        public static long getDateFromUri(Uri uri) {
           return Long.parseLong(uri.getPathSegments().get(2));
        }

        public static long getStartDateFromUri(Uri uri) {
            String dateString = uri.getQueryParameter(COLUMN_DATE);
            if (dateString != null && dateString.length() > 0) {
                return Long.parseLong(dateString);
            } else
                return 0;
        }
    }




}
