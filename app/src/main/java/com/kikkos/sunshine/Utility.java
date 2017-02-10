/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.kikkos.sunshine;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;
import android.text.format.Time;

import com.kikkos.sunshine.sync.SunshineSyncAdapter;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Utility {

    // We'll default our latlong to 0. Yay, "Earth!"
    public static float DEFAULT_LATLONG = 0F;

    public static boolean isLocationLatLonAvailable(Context context) {
        SharedPreferences prefs
                = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.contains(context.getString(R.string.pref_location_latitude))
                && prefs.contains(context.getString(R.string.pref_location_longitude));
    }

    public static float getLocationLatitude(Context context) {
        SharedPreferences prefs
                = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getFloat(context.getString(R.string.pref_location_latitude),
                DEFAULT_LATLONG);
    }

    public static float getLocationLongitude(Context context) {
        SharedPreferences prefs
                = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getFloat(context.getString(R.string.pref_location_longitude),
                DEFAULT_LATLONG);
    }

    // Format used for storing dates in the database.  ALso used for converting those strings
    // back into date objects for comparison/processing.
    public static final String DATE_FORMAT = "yyyyMMdd";

    public static String getPreferredLocation(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString(context.getString(R.string.pref_location_key),
                context.getString(R.string.pref_location_default));
    }

    public static boolean isMetric(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString(context.getString(R.string.pref_units_key),
                context.getString(R.string.pref_units_metric))
                .equals(context.getString(R.string.pref_units_metric));
    }

    public static String formatTemperature(Context context, double temperature) {
        if ( !isMetric(context) ) {
            temperature = (temperature * 1.8) + 32;
        }
        return context.getString(R.string.format_temp, (int)temperature);
    }

    static String formatDate(long dateInMillis) {
        Date date = new Date(dateInMillis);
        return DateFormat.getDateInstance().format(date);
    }

    /**
     * Helper method to convert the database representation of the date into something to display
     * to users.  As classy and polished a user experience as "20140102" is, we can do better.
     *
     * @param context Context to use for resource localization
     * @param dateInMillis The date in milliseconds
     * @return a user-friendly representation of the date.
     */
    public static String getFriendlyDayString(Context context, long dateInMillis, boolean displayLongToday) {
        // The day string for forecast uses the following logic:
        // For today: "Today, June 8"
        // For tomorrow:  "Tomorrow"
        // For the next 5 days: "Wednesday" (just the day name)
        // For all days after that: "Mon Jun 8"

        Time time = new Time();
        time.setToNow();
        long currentTime = System.currentTimeMillis();
        int julianDay = Time.getJulianDay(dateInMillis, time.gmtoff);
        int currentJulianDay = Time.getJulianDay(currentTime, time.gmtoff);

        // If the date we're building the String for is today's date, the format
        // is "Today, June 24"
        if (displayLongToday && julianDay == currentJulianDay) {
            String today = context.getString(R.string.today);
            int formatId = R.string.format_full_friendly_date;
            return String.format(context.getString(
                    formatId,
                    today,
                    getFormattedMonthDay(context, dateInMillis)));
        } else if ( julianDay < currentJulianDay + 7 ) {
            // If the input date is less than a week in the future, just return the day name.
            return getDayName(context, dateInMillis);
        } else {
            // Otherwise, use the form "Mon Jun 3"
            SimpleDateFormat shortenedDateFormat = new SimpleDateFormat("EEE MMM dd");
            return shortenedDateFormat.format(dateInMillis);
        }
    }

    /**
     * Given a day, returns just the name to use for that day.
     * E.g "today", "tomorrow", "wednesday".
     *
     * @param context Context to use for resource localization
     * @param dateInMillis The date in milliseconds
     * @return
     */
    public static String getDayName(Context context, long dateInMillis) {
        // If the date is today, return the localized version of "Today" instead of the actual
        // day name.

        Time t = new Time();
        t.setToNow();
        int julianDay = Time.getJulianDay(dateInMillis, t.gmtoff);
        int currentJulianDay = Time.getJulianDay(System.currentTimeMillis(), t.gmtoff);
        if (julianDay == currentJulianDay) {
            return context.getString(R.string.today);
        } else if ( julianDay == currentJulianDay +1 ) {
            return context.getString(R.string.tomorrow);
        } else {
            Time time = new Time();
            time.setToNow();
            // Otherwise, the format is just the day of the week (e.g "Wednesday".
            SimpleDateFormat dayFormat = new SimpleDateFormat("EEEE");
            return dayFormat.format(dateInMillis);
        }
    }

    /**
     * Converts db date format to the format "Month day", e.g "June 24".
     * @param context Context to use for resource localization
     * @param dateInMillis The db formatted date string, expected to be of the form specified
     *                in Utility.DATE_FORMAT
     * @return The day in the form of a string formatted "December 6"
     */
    public static String getFormattedMonthDay(Context context, long dateInMillis ) {
        Time time = new Time();
        time.setToNow();
        SimpleDateFormat dbDateFormat = new SimpleDateFormat(Utility.DATE_FORMAT);
        SimpleDateFormat monthDayFormat = new SimpleDateFormat("MMMM dd");
        String monthDayString = monthDayFormat.format(dateInMillis);
        return monthDayString;
    }

    public static String getFormattedWind(Context context, double windSpeed, double degrees) {
        int windFormat;
        if (Utility.isMetric(context)) {
            windFormat = R.string.format_wind_kmh;
        } else {
            windFormat = R.string.format_wind_mph;
            windSpeed = .621371192237334f * windSpeed;
        }

        // From wind direction in degrees, determine compass direction as a string (e.g NW)
        // You know what's fun, writing really long if/else statements with tons of possible
        // conditions.  Seriously, try it!
        String direction = "Unknown";
        if (degrees >= 337.5 || degrees < 22.5) {
            direction = "N";
        } else if (degrees >= 22.5 && degrees < 67.5) {
            direction = "NE";
        } else if (degrees >= 67.5 && degrees < 112.5) {
            direction = "E";
        } else if (degrees >= 112.5 && degrees < 157.5) {
            direction = "SE";
        } else if (degrees >= 157.5 && degrees < 202.5) {
            direction = "S";
        } else if (degrees >= 202.5 && degrees < 247.5) {
            direction = "SW";
        } else if (degrees >= 247.5 && degrees < 292.5) {
            direction = "W";
        } else if (degrees >= 292.5 && degrees < 337.5) {
            direction = "NW";
        }
        return String.format(context.getString(windFormat), windSpeed, direction);
    }

    public static int getIconResourceForWeatherCondition(int viewType, int weatherCode){
        if (viewType == 0){
            if (weatherCode >= 200 && weatherCode <= 232){
                return R.drawable.art_storm;
            }else if (weatherCode >= 300 && weatherCode <= 321){
                return R.drawable.art_light_rain;
            }else if (weatherCode >= 500 && weatherCode <= 531){
                return R.drawable.art_rain;
            }else if (weatherCode >= 600 && weatherCode <= 622){
                return R.drawable.art_snow;
            }else if (weatherCode >= 701 && weatherCode <= 781){
                return R.drawable.art_fog;
            }else if (weatherCode == 800){
                return R.drawable.art_clear;
            }else if (weatherCode >= 801 && weatherCode <=803 ){
                return R.drawable.art_light_clouds;
            }else if (weatherCode >= 804){
                return R.drawable.art_clouds;
            }
        }else if (viewType == 1){
            if (weatherCode >= 200 && weatherCode <= 232){
                return R.drawable.ic_storm;
            }else if (weatherCode >= 300 && weatherCode <= 321){
                return R.drawable.ic_light_rain;
            }else if (weatherCode >= 500 && weatherCode <= 531){
                return R.drawable.ic_rain;
            }else if (weatherCode >= 600 && weatherCode <= 622){
                return R.drawable.ic_snow;
            }else if (weatherCode >= 701 && weatherCode <= 781){
                return R.drawable.ic_fog;
            }else if (weatherCode == 800){
                return R.drawable.ic_clear;
            }else if (weatherCode >= 801 && weatherCode <=803 ){
                return R.drawable.ic_light_clouds;
            }else if (weatherCode >= 804){
                return R.drawable.ic_cloudy;
            }
        }
        return -1;
    }

    public static String getArtUrlForWeatherCondition(Context context, int weatherCode){
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        String formatArtUrl = sp.getString(context.getString(R.string.pref_art_pack_key), context.getString(R.string.pref_art_pack_sunshine));

        if (weatherCode >= 200 && weatherCode <= 232){
            return String.format(Locale.US, formatArtUrl, "storm");
        }else if (weatherCode >= 300 && weatherCode <= 321){
            return String.format(Locale.US, formatArtUrl, "light_rain");
        }else if (weatherCode >= 500 && weatherCode <= 531){
            return String.format(Locale.US, formatArtUrl, "rain");
        }else if (weatherCode >= 600 && weatherCode <= 622){
            return String.format(Locale.US, formatArtUrl, "snow");
        }else if (weatherCode >= 701 && weatherCode <= 781){
            return String.format(Locale.US, formatArtUrl, "fog");
        }else if (weatherCode == 800){
            return String.format(Locale.US, formatArtUrl, "clear");
        }else if (weatherCode >= 801 && weatherCode <=803 ){
            return String.format(Locale.US, formatArtUrl, "light_clouds");
        }else if (weatherCode >= 804){
            return String.format(Locale.US, formatArtUrl, "clouds");
        }
        return null;
    }

    public static boolean isNetworkAvailable(Context context){
        //Checking the internet connectivity.
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }

    @SuppressWarnings("ResourceType")
    public static @SunshineSyncAdapter.LocationStatus int getLocationStatus(Context context){
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        return sp.getInt(context.getString(R.string.pref_location_status_key), SunshineSyncAdapter.LOCATION_STATUS_UNKNOWN);
    }

    public static void resetLocationStatus(Context context){
        // Reset the location status preference to UNKNOWN status.
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor spe = sp.edit();
        spe.putInt(context.getString(R.string.pref_location_status_key), SunshineSyncAdapter.LOCATION_STATUS_UNKNOWN);
        spe.apply();
    }

    /**
     * Helper method to return whether or not Sunshine is using local graphics.
     *
     * @param context Context to use for retrieving the preference
     * @return true if Sunshine is using local graphics, false otherwise.
     */
    public static boolean usingLocalGraphics(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String sunshineArtPack = context.getString(R.string.pref_art_pack_sunshine);
        return prefs.getString(context.getString(R.string.pref_art_pack_key),
                sunshineArtPack).equals(sunshineArtPack);
    }
}