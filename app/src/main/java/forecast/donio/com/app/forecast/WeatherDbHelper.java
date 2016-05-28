package forecast.donio.com.app.forecast;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by TranT.Phuong on 5/22/16.
 */
public class WeatherDbHelper extends SQLiteOpenHelper {
    private static final int DATABSE_VERSION = 1;
    public static final String DATABASE_NAME = "weather.db";


    public WeatherDbHelper(Context context) {
        super(context,DATABASE_NAME, null, DATABSE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        final String CREATE_LOCATION_TABLE = "CREATE TABLE " + WeatherContract.LocationEntry.TABLE_NAME +
                " (" + WeatherContract.LocationEntry._ID + " INTEGER PRIMARY KEY," +
                WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING + " TEXT UNIQUE NOT NULL, " +
                WeatherContract.LocationEntry.COLUMN_CITY_NAME + " TEXT NOT NULL, " +
                WeatherContract.LocationEntry.COLUMN_COORD_LAT + " REAL NOT NULL, " +
                WeatherContract.LocationEntry.COLUMN_COORD_LON + " REAL NOT NULL, " +
                " );";

        final String CREATE_WEATHER_TABLE = "CREATE TABLE " + WeatherContract.WeatherEntry.TABLE_NAME
                + " (" + WeatherContract.WeatherEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                WeatherContract.WeatherEntry.COLUMN_LOC_KEY + " INTEGER NOT NULL," +
                WeatherContract.WeatherEntry.COLUMN_DATE + " INTEGER NOT NULL," +
                WeatherContract.WeatherEntry.COLUMN_SHORT_DESC + " TEXT NOT NULL," +
                WeatherContract.WeatherEntry.COLUMN_WEATHER_ID + " INTEGER NOT NULL," +
                WeatherContract.WeatherEntry.COLUMN_MIN + " REAL NOT NULL," +
                WeatherContract.WeatherEntry.COLUMN_MAX + " REALL NOT NULL," +
                WeatherContract.WeatherEntry.COLUMN_HUMIDITY + " REAL NOT NULL," +
                WeatherContract.WeatherEntry.COLUMN_PRESSURE + " REAL NOT NULL," +
                WeatherContract.WeatherEntry.COLUMN_WIND + " REAL NOT NULL," +
                WeatherContract.WeatherEntry.COLUMN_DEGREES + " REAL NOT NULL," +
                 // Set up the location column as a foreign key to location table.
                " FOREIGN KEY (" + WeatherContract.WeatherEntry.COLUMN_LOC_KEY + ") REFERENCES " +
                WeatherContract.LocationEntry.TABLE_NAME + " (" + WeatherContract.LocationEntry._ID + ")," +
                // To assure the application have just one weather entry per day
                // per location, it's created a UNIQUE constraint with REPLACE strategy
                " UNIQUE (" + WeatherContract.WeatherEntry.COLUMN_DATE + ", " +
                WeatherContract.WeatherEntry.COLUMN_LOC_KEY + ") ON CONFLICT REPLACE);";

        db.execSQL(CREATE_WEATHER_TABLE);
        db.execSQL(CREATE_LOCATION_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + WeatherContract.WeatherEntry.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + WeatherContract.LocationEntry.TABLE_NAME);
        onCreate(db);
    }
}
