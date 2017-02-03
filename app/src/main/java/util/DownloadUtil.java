package util;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.google.gson.Gson;
import com.loopj.android.http.TextHttpResponseHandler;

import org.apache.http.Header;

import java.util.List;

import datebase.mDBHelper;
import datebase.webDBHelper;
import model.Latest;
import model.StoriesEntity;

/**
 * Created by yandong on 2017/2/3.
 */

public class DownloadUtil {

    public static boolean startDwonLoad(Context context) {
        final mDBHelper dbHelper = new mDBHelper(context, 1);
        final webDBHelper webDBHelper = new webDBHelper(context, 1);
        if (HttpUtils.isNetworkConnected(context)) {
            HttpUtils.get(Constant.LATESTNEWS, new TextHttpResponseHandler() {
                @Override
                public void onFailure(int i, Header[] headers, String s, Throwable throwable) {
                }

                @Override
                public void onSuccess(int i, Header[] headers, String s) {
                    SQLiteDatabase db = dbHelper.getWritableDatabase();
                    db.execSQL("replace into CacheList(date,json) values(" + Constant.LATEST_COLUMN + ",'" + s + "')");
                    db.close();
                    Gson gson = new Gson();
                    Latest latest = gson.fromJson(s, Latest.class);
                    final List<StoriesEntity> stories = latest.getStories();

                    final SQLiteDatabase webDB = webDBHelper.getWritableDatabase();

                    for (int j = 0; j < stories.size(); j++) {
                        StoriesEntity story = stories.get(j);
                        parseStory(webDB, story);
                    }
                    //webDB.close();
                }
            });
            return true;
        } else {
            return false;
        }
    }

        private static void parseStory(SQLiteDatabase webDB1,StoriesEntity story1){
        final SQLiteDatabase webDB = webDB1;
        final StoriesEntity story = story1;
        HttpUtils.get(Constant.CONTENT + story.getId(), new TextHttpResponseHandler() {
            @Override
            public void onFailure(int i, Header[] headers, String s, Throwable throwable) {
            }

            @Override
            public void onSuccess(int i, Header[] headers, String s) {
                s = s.replaceAll("'", "''");
                webDB.execSQL("replace into WebCacheList(newsID,json) values(" + story.getId() + ",'" + s + "')");
            }
        });
    }




}
