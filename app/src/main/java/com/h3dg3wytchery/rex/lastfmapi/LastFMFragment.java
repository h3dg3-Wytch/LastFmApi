package com.h3dg3wytchery.rex.lastfmapi;

import android.annotation.TargetApi;
import android.net.Uri;
import android.nfc.Tag;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public  class LastFMFragment extends Fragment {

    private static final String TAG ="com.h3dg3wytchery.rex.lastfmapi.lastfm";

        private ListView listView;
        static ArrayAdapter<String> adapter;

        public LastFMFragment() {
        }

        @Override
        public void onCreate(Bundle saveInstanceState){
            super.onCreate(saveInstanceState);
            setHasOptionsMenu(true);
        }

        @Override
        public void onCreateOptionsMenu(Menu menu, MenuInflater inflater){
            inflater.inflate(R.menu.lastfmfragment,menu);


        }
        @TargetApi(11)
        @Override
        public boolean onOptionsItemSelected(MenuItem item){
            switch(item.getItemId()){
                case R.id.refresh_button:
                    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                        new LastFmTask().execute();
                    }
                    return true;
                default:
                    return super.onOptionsItemSelected(item);

            }
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_main, container, false);

            List<String> artistNames = new ArrayList<String>();
            for(int i = 0; i < 10; i++){
                artistNames.add("Artist# " + i);
            }
            ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(), R.layout.fragment_list_view, R.id.artist_list, artistNames);
            listView = (ListView) rootView.findViewById(R.id.listView);
            listView.setAdapter(adapter);

            return rootView;
        }


    public class LastFmTask extends AsyncTask<Void,Void,String[]>{

        @Override
        protected  void onPostExecute(String[] strings) {
            super.onPostExecute(strings);
           adapter = new ArrayAdapter<String>(getActivity(), R.layout.fragment_list_view,R.id.artist_list, strings);
           listView.setAdapter(adapter);
        }

        @Override
        protected String[] doInBackground(Void... params) {
            HttpURLConnection connection = null;
            BufferedReader in = null;

            //Wil Contain the raw XML in the string
            String lastFmJSON = null;
//http://ws.audioscrobbler.com/2.0/?method=geo.gettopartists&country=USA&api_key=32ab7b50ad9fa9c4dc08c2467feb24cf&format=json
            try {
                final String LASTFM_BASE_URL = "http://ws.audioscrobbler.com/2.0/?";
                final String METHOD_PARAM = "method";
                final String COUNTRY_PARAM = "country";
                final String API_KEY = "32ab7b50ad9fa9c4dc08c2467feb24cf";
                final String FORMAT_PARAM = "format";

                //Buils a URL from the base url, then access each method. The method before is seperated by an & symbol
                Uri buildUri = Uri.parse(LASTFM_BASE_URL).buildUpon().appendQueryParameter(METHOD_PARAM, "geo.gettopartists")
                        .appendQueryParameter(COUNTRY_PARAM, "USA").appendQueryParameter("api_key", API_KEY).appendQueryParameter(FORMAT_PARAM, "json")
                        .build();
                URL url = new URL(buildUri.toString());
                Log.d(TAG, url.toString());

                try {
                    // Construct the URL for the OpenWeatherMap query
                    // Possible parameters are avaiable at OWM's forecast API page, at
                    // http://openweathermap.org/API#forecast
                    //URL url = new URL("http://api.openweathermap.org/data/2.5/forecast/daily?q=30306,USA&mode=json&units=metric&cnt=7");

                    // Create the request to OpenWeatherMap, and open the connection
                    connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("GET");
                    connection.connect();

                    // Read the input stream into a String
                    InputStream inputStream = connection.getInputStream();
                    StringBuffer buffer = new StringBuffer();
                    if (inputStream == null) {
                        // Nothing to do.
                        lastFmJSON = null;
                    }
                    in = new BufferedReader(new InputStreamReader(inputStream));

                    String line;
                    while ((line = in.readLine()) != null) {
                        // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                        // But it does make debugging a *lot* easier if you print out the completed
                        // buffer for debugging.
                        buffer.append(line + "\n");
                    }

                    if (buffer.length() == 0) {
                        // Stream was empty.  No point in parsing.
                        lastFmJSON = null;
                    }
                    lastFmJSON = buffer.toString();

                }catch (MalformedURLException e){

                    Log.d(TAG, "MalformedURL, it isn't connected");

                }
            } catch (IOException e) {
                Log.d(TAG, "Stuff");
                // If the code didn't successfully get the weather data, there's no point in attemping
                // to parse it.
                lastFmJSON = null;
            } finally{
                if (connection != null) {
                    connection.disconnect();
                }
                if (in != null) {
                    try {
                        in.close();
                    } catch (final IOException e) {

                    }
                }
            }
                try {
                    Log.d(TAG,"Method reached");
                    return getLastFMDataFromData(lastFmJSON);
                }catch(JSONException e){
                    Log.d(TAG, "error");
                    return null;

                }


        }

        private String[] getLastFMDataFromData(String jsonString) throws JSONException{
           String[] topTen = new String[10];

            JSONObject lastFmJson = new JSONObject(jsonString);

            JSONObject topArtists = lastFmJson.getJSONObject("topartists");
            JSONArray artists = topArtists.getJSONArray("artist");
            Log.d(TAG, "artists loaded");
            for(int i =0; i < 10; i++){
                JSONObject obj = artists.getJSONObject(i);
                topTen[i]= obj.getString("name");
                Log.d(TAG, obj.getString("name"));
            }


            return topTen;



        }
    }

    public static class Log {

        public static void d(String TAG, String message) {
            int maxLogSize = 2000;
            for(int i = 0; i <= message.length() / maxLogSize; i++) {
                int start = i * maxLogSize;
                int end = (i+1) * maxLogSize;
                end = end > message.length() ? message.length() : end;
                android.util.Log.d(TAG, message.substring(start, end));
            }
        }

    }
    }