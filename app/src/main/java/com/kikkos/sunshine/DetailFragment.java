package com.kikkos.sunshine;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.ShareActionProvider;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.kikkos.sunshine.data.WeatherContract;

/**
 * Created by kikkos on 8/28/2016.
 */
public class DetailFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final String LOG_TAG = DetailFragment.class.getSimpleName();

    private static final String FORECAST_SHARE_HASHTAG = "#SunshineApp";
    static final String DETAIL_URI = "URI";
    static final String DETAIL_TRANSITION_ANIMATION = "DTA";

    private ShareActionProvider mShareActionProvider;
    private String forecastShare;
    private Uri mUri;
    private boolean mTransitionAnimation;

    TextView mDateView;
    TextView mHighView;
    TextView mLowView;
    TextView mHumidityView;
    private TextView mHumidityLabelView;
    TextView mWindView;
    private TextView mWindLabelView;
    TextView mPressureView;
    private TextView mPressureLabelView;
    TextView mDescriptionView;
    ImageView mIconView;
    //MyView mWindIcon;

    private static final int DETAIL_LOADER = 0;

    private static final String[] DETAIL_COLUMNS = {
            WeatherContract.WeatherEntry.TABLE_NAME + "." + WeatherContract.WeatherEntry._ID,
            WeatherContract.WeatherEntry.COLUMN_DATE,
            WeatherContract.WeatherEntry.COLUMN_SHORT_DESC,
            WeatherContract.WeatherEntry.COLUMN_MAX_TEMP,
            WeatherContract.WeatherEntry.COLUMN_MIN_TEMP,
            WeatherContract.WeatherEntry.COLUMN_HUMIDITY,
            WeatherContract.WeatherEntry.COLUMN_PRESSURE,
            WeatherContract.WeatherEntry.COLUMN_WIND_SPEED,
            WeatherContract.WeatherEntry.COLUMN_DEGREES,
            WeatherContract.WeatherEntry.COLUMN_WEATHER_CONDITION_ID,
    };

    // these constants correspond to the projection defined above, and must change if the
    // projection changes
    private static final int COL_WEATHER_ID = 0;
    private static final int COL_WEATHER_DATE = 1;
    private static final int COL_WEATHER_DESC = 2;
    private static final int COL_WEATHER_MAX_TEMP = 3;
    private static final int COL_WEATHER_MIN_TEMP = 4;
    private static final int COL_WEATHER_HUMIDITY = 5;
    private static final int COL_WEATHER_PRESSURE = 6;
    private static final int COL_WEATHER_WIND_SPEED = 7;
    private static final int COL_WEATHER_DEGREE = 8;
    private static final int COL_CONDITION_ID = 9;

    public DetailFragment(){
        setHasOptionsMenu(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        Bundle arguments = getArguments();
        if (arguments != null){
            mUri = arguments.getParcelable(DetailFragment.DETAIL_URI);
            mTransitionAnimation = arguments.getBoolean(DETAIL_TRANSITION_ANIMATION, false);
        }

        View rootView = inflater.inflate(R.layout.fragment_detail_start, container, false);
        mDateView = (TextView) rootView.findViewById(R.id.detail_date_textview);
        mHighView = (TextView) rootView.findViewById(R.id.detail_high_textview);
        mLowView = (TextView) rootView.findViewById(R.id.detail_low_textview);
        mHumidityView = (TextView) rootView.findViewById(R.id.detail_humidity_textview);
        mHumidityLabelView = (TextView) rootView.findViewById(R.id.detail_humidity_label_textview);
        mWindView = (TextView) rootView.findViewById(R.id.detail_wind_textview);
        mWindLabelView = (TextView) rootView.findViewById(R.id.detail_wind_label_textview);
        mPressureView = (TextView) rootView.findViewById(R.id.detail_pressure_textview);
        mPressureLabelView = (TextView) rootView.findViewById(R.id.detail_pressure_label_textview);
        mDescriptionView = (TextView) rootView.findViewById(R.id.detail_forecast_textview);
        mIconView = (ImageView) rootView.findViewById(R.id.detail_icon);
        //mWindIcon = (MyView) rootView.findViewById(R.id.details_wind_direction_icon);
        return rootView;
    }

    private void finishCreatingMenu(Menu menu) {
        // Retrieve the share menu item
        MenuItem menuItem = menu.findItem(R.id.menu_item_share);
        // Get the provider and hold onto it to set/change the share intent.
        mShareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(menuItem);
        // If onLoadFinished happens before this, we can go ahead and set the share intent now.
        if(forecastShare != null){
            mShareActionProvider.setShareIntent(createShareForecastIntent());
        }else {
            menuItem.setIntent(createShareForecastIntent());
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (getActivity() instanceof DetailActivity){
            // Inflate the menu; this adds items to the action bar if it is present.
            inflater.inflate(R.menu.detailfragment, menu);
            finishCreatingMenu(menu);
        }
    }

    private Intent createShareForecastIntent(){
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, forecastShare + FORECAST_SHARE_HASHTAG);
        return shareIntent;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        getLoaderManager().initLoader(DETAIL_LOADER, null, this);
        super.onActivityCreated(savedInstanceState);
    }

    void onLocationChanged(String newLocation){
        // replace the uri, since the location has changed
        Uri uri = mUri;
        if (null != uri){
            long date = WeatherContract.WeatherEntry.getDateFromUri(uri);
            Uri updatedUri = WeatherContract.WeatherEntry.buildWeatherLocationWithDate(newLocation, date);
            mUri = updatedUri;
            getLoaderManager().restartLoader(DETAIL_LOADER, null, this);
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        if (null != mUri){
            return new CursorLoader(getActivity(), mUri, DETAIL_COLUMNS, null, null, null);
        }
        ViewParent vp = getView().getParent();
        if (vp instanceof CardView){
            ((View)vp).setVisibility(View.INVISIBLE);
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (!data.moveToFirst()){
            return;
        }
        ViewParent vp = getView().getParent();
        if (vp instanceof CardView){
            ((View)vp).setVisibility(View.VISIBLE);
        }
        int weatherConditionId = data.getInt(COL_CONDITION_ID);
        String date = Utility.getFriendlyDayString(getContext(), data.getLong(COL_WEATHER_DATE), true);
        String weatherDescription = data.getString(COL_WEATHER_DESC);
        String high = Utility.formatTemperature(getContext(), data.getDouble(COL_WEATHER_MAX_TEMP));
        String low = Utility.formatTemperature(getContext(), data.getDouble(COL_WEATHER_MIN_TEMP));
        String humidity = String.format(getContext().getString(R.string.format_humidity), data.getDouble(COL_WEATHER_HUMIDITY));
        String wind = Utility.getFormattedWind(getContext(), data.getDouble(COL_WEATHER_WIND_SPEED), data.getDouble(COL_WEATHER_DEGREE));
        String pressure = String.format(getContext().getString(R.string.format_pressure), data.getDouble(COL_WEATHER_PRESSURE));
        forecastShare = String.format("%s - %s - %s/%s", date, weatherDescription, high, low);


        mDateView.setText(date);

        mHighView.setText(high);
        mHighView.setContentDescription(getString(R.string.a11y_high_temp, high));

        mLowView.setText(low);
        mLowView.setContentDescription(getString(R.string.a11y_low_temp, low));

        mHumidityView.setText(humidity);
        mHumidityView.setContentDescription(humidity);
        mHumidityLabelView.setContentDescription(humidity);

        mWindView.setText(wind);
        mWindView.setContentDescription(wind);
        mWindLabelView.setContentDescription(wind);

//        mWindIcon.setEnableWindIcon(true);
//        mWindIcon.setWindDirection(data.getDouble(COL_WEATHER_DEGREE));

        mPressureView.setText(pressure);
        mPressureView.setContentDescription(pressure);
        mPressureLabelView.setContentDescription(pressure);

        mDescriptionView.setText(weatherDescription);
        mDescriptionView.setContentDescription(getString(R.string.a11y_forecast, weatherDescription));

        if ( Utility.usingLocalGraphics(getActivity()) ) {
            mIconView.setImageResource(Utility.getIconResourceForWeatherCondition(0, weatherConditionId));
        }else {
            Glide.with(this)
                    .load(Utility.getArtUrlForWeatherCondition(getContext(), weatherConditionId))
                    .error(Utility.getIconResourceForWeatherCondition(0, weatherConditionId))
                    .crossFade()
                    .override(432, 432)
                    .into(mIconView);
        }
        mIconView.setContentDescription(getContext().getString(R.string.a11y_forecast_icon, weatherDescription));


        // If onCreateOptionsMenu has already happened, we need to update the share intent now.
        if (mShareActionProvider != null){
            mShareActionProvider.setShareIntent(createShareForecastIntent());
        }

        AppCompatActivity activity = (AppCompatActivity)getActivity();
        Toolbar toolbarView = (Toolbar) getView().findViewById(R.id.toolbar);

        // We need to start the enter transition after the data has loaded
        if ( mTransitionAnimation ) {
            activity.supportStartPostponedEnterTransition();

            if ( null != toolbarView ) {
                activity.setSupportActionBar(toolbarView);

                activity.getSupportActionBar().setDisplayShowTitleEnabled(false);
                activity.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            }
        } else {
            if ( null != toolbarView ) {
                Menu menu = toolbarView.getMenu();
                if ( null != menu ) menu.clear();
                toolbarView.inflateMenu(R.menu.detailfragment);
                finishCreatingMenu(toolbarView.getMenu());
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }
}
