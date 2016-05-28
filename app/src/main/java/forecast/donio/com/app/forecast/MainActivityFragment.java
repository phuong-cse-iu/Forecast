package forecast.donio.com.app.forecast;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.text.format.Time;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * A placeholder fragment containing a simple view.
 */
public class MainActivityFragment extends Fragment {
    private ListView forecast_listview;
    private TextView forecast_item;
    private ArrayAdapter<String> mForecastAdapter;

    public MainActivityFragment() {
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.forecast_menu_fragment, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_refresh) {
            updateWeather();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }



    @Override
    public void onStart() {
        super.onStart();
        updateWeather();
    }

    private void updateWeather() {
        FetchWeatherTask fetchWeatherData = new FetchWeatherTask(getContext(), mForecastAdapter);

        SharedPreferences prefLocation = PreferenceManager.getDefaultSharedPreferences(getActivity());
        SharedPreferences prefUnits = PreferenceManager.getDefaultSharedPreferences(getActivity());

        String location = prefLocation.getString(getString(R.string.pref_location_key),
                getString(R.string.pref_location_default));

        String units = prefUnits.getString(getString(R.string.pref_unit_key), getString(R.string.pref_unit_default));

        fetchWeatherData.execute(location, units);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        forecast_listview = (ListView) rootView.findViewById(R.id.forecast_listview);
        forecast_item = (TextView) rootView.findViewById(R.id.list_item_forecast_textview);

        mForecastAdapter = new ArrayAdapter<String>(getActivity(),
                R.layout.forecast_row,
                R.id.list_item_forecast_textview,
                new ArrayList<String>());

        forecast_listview.setAdapter(mForecastAdapter);

        forecast_listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String mForecastData = mForecastAdapter.getItem(position);
                Intent intent = new Intent(getActivity(), DetailActivity.class);
                intent.putExtra(Intent.EXTRA_TEXT, mForecastData);
                startActivity(intent);
            }
        });

        return rootView;
    }

    // fech weather data using openweather api
    public class FetchWeatherTask extends AsyncTask<String, Void, String[]> {
        // use tag to easy debug
        private final String LOG_TAG = FetchWeatherTask.class.getName();
        private  String[] resultStr = null;
        private Context mContext;

        public FetchWeatherTask(Context context, ArrayAdapter<String> forecastAdapter) {
            mContext = context;
            mForecastAdapter = forecastAdapter;
        }

        // download JSon from the internet with location
        @Override
        protected String[] doInBackground(String... params) {

            if (params.length == 0) {
                return null;
            }

            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            String forecastStr = null;


            String locationQuery = params[0];
            String format = "json";
            String units = "metric";
            int numDays = 7;

            try {

                final String BASE_URL = "http://api.openweathermap.org/data/2.5/forecast/daily?";
                final String QUERY_PARAM = "q";
                final String FORMAT_PARAM = "mode";
                final String UNITS_PARAM = "units";
                final String DAYS_PARAM = "cnt";
                final String APPI_PARAM = "appid";

                // use uri to construct url
                Uri uri = Uri.parse(BASE_URL).buildUpon()
                        .appendQueryParameter(QUERY_PARAM, params[0])
                        .appendQueryParameter(FORMAT_PARAM, format)
                        .appendQueryParameter(UNITS_PARAM, params[1])
                        .appendQueryParameter(DAYS_PARAM, Integer.toString(numDays))
                        .appendQueryParameter(APPI_PARAM, BuildConfig.API_KEY)
                        .build();

                URL url = new URL(uri.toString());
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                Log.v(LOG_TAG, url.toString());

                // Read inputstream into a string
                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();

                if (inputStream == null) {
                    return null;
                }

                reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while ((line = reader.readLine()) != null) {
                    // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                    // But it does make debugging a *lot* easier if you print out the completed
                    // buffer for debugging.
                    buffer.append(line + "\n");
                }

                if (buffer.length() == 0) {
                    // Stream was empty.  No point in parsing.
                    return null;
                }

                forecastStr = buffer.toString();


            } catch (IOException e) {
                Log.e(LOG_TAG, "Error ", e);
                // If the code didn't successfully get the weather data, there's no point in attemping
                // to parse it.
                return null;
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }

                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException e) {
                        Log.e(LOG_TAG, "Error closing stream", e);
                    }
                }
            }

            try {
                return getWeatherDataFromJson(forecastStr, numDays);
            } catch (JSONException e) {
                Log.e(LOG_TAG, e.getMessage(), e);
                e.printStackTrace();
            }

            // This will only happen if there was an error getting or parsing the forecast.
            return null;

        }



        private String[] getWeatherDataFromJson(String forecastJsonStr, int numDays) throws JSONException {

            // These are the names of the JSON objects that need to be extracted.
            final String LIST = "list";
            final String TEMPERATURE = "temp";
            final String WEATHER = "weather";
            final String MAX = "max";
            final String MIN = "min";
            final String MAIN = "main";
            final String DESCRIPTION = "description";

            JSONObject forecastJson = new JSONObject(forecastJsonStr);
            JSONArray weatherArray = forecastJson.getJSONArray(LIST);

            // OWM returns daily forecasts based upon the local time of the city that is being
            // asked for, which means that we need to know the GMT offset to translate this data
            // properly.

            // Since this data is also sent in-order and the first day is always the
            // current day, we're going to take advantage of that to get a nice
            // normalized UTC date for all of our weather.

            Time dayTime = new Time();
            dayTime.setToNow();

            // we start at the day returned by local time. Otherwise this is a mess.
            int juLianStartDay = Time.getJulianDay(System.currentTimeMillis(), dayTime.gmtoff);

            // now we work exclusively in UTC
            dayTime = new Time();

            resultStr = new String[numDays];

            for (int i = 0; i < weatherArray.length(); i++) {
                // For now, using the format "Day, description, hi/low"
                String day;
                String description;
                String highAndLow;

                // get the Json Object represeting the day
                JSONObject dayForecast = weatherArray.getJSONObject(i);

                // The date/time is returned as a long.  We need to convert that
                // into something human-readable, since most people won't read "1400356800" as
                // "this saturday".

                long dateTime;
                // Cheating to convert this to UTC time, which is what we want anyhow
                dateTime = dayTime.setJulianDay(juLianStartDay+i);
                day = getReadableDateString(dateTime);

                // description is in a child array called "weather", which is 1 element long.
                JSONObject weatherObj = dayForecast.getJSONArray(WEATHER).getJSONObject(0);
                // get description
                description = weatherObj.getString(DESCRIPTION);

                // Temperatures are in a child object called "temp".  Try not to name variables
                // "temp" when working with temperature.  It confuses everybody.
                JSONObject tempObj = dayForecast.getJSONObject(TEMPERATURE);
                double high = tempObj.getDouble(MAX);
                double low = tempObj.getDouble(MIN);

                highAndLow = formatHighLows(high, low);

                resultStr[i] = day + " - " + description + " - " + highAndLow;
            }

            return resultStr;

        }

        private String formatHighLows(double high, double low) {
            // For presentation, assume the user doesn't care about tenths of a degree.
            long roundedHigh = Math.round(high);
            long roudedLow = Math.round(low);

            String highLowStr = roundedHigh + "/" + roudedLow;
            return highLowStr;
        }

        private String getReadableDateString(long dateTime) {
            // Because the API returns a unix timestamp (measured in seconds),
            // it must be converted to milliseconds in order to be converted to valid date.
            SimpleDateFormat shortenedDateFormat = new SimpleDateFormat("EEE MMM dd");
            return shortenedDateFormat.format(dateTime);
        }

        @Override
        protected void onPostExecute(String[] result) {
            if (result != null) {
                mForecastAdapter.clear();

                for (String dayForecastStr : result) {
                    mForecastAdapter.add(dayForecastStr);
                }
                mForecastAdapter.notifyDataSetChanged();
            }
        }
    }


}
