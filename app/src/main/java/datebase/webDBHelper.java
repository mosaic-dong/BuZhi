package datebase;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by yandong on 2017/2/3.
 */

public class webDBHelper extends SQLiteOpenHelper{
    public webDBHelper(Context context, int version) {
        super(context, "webCache", null, version);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("create table if not exists WebCacheList (id INTEGER primary key autoincrement,newsID INTEGER unique,json text)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
}
