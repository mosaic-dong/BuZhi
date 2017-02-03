package util;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * Created by yandong on 2017/1/29.
 */

public class PreUtils {

    public static void putStringToDefault(Context context,String key,String value){
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        sharedPreferences.edit().putString(key,value).commit();
    }

    public static String getStringToDefault(Context context,String key,String defValue){
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getString(key,defValue);
    }
}
