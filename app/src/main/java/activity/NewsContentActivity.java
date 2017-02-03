package activity;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;

import com.example.yandong.zhihu.R;
import com.google.gson.Gson;
import com.loopj.android.http.TextHttpResponseHandler;

import org.apache.http.Header;

import datebase.webDBHelper;
import model.Content;
import model.StoriesEntity;
import util.Constant;
import util.HttpUtils;
import view.RevealBackgroundView;

/**
 * Created by yandong on 2017/2/2.
 */

public class NewsContentActivity extends AppCompatActivity implements RevealBackgroundView.OnStateChangeListener{

    private WebView mWebView;
    private StoriesEntity entity;
    private Content content;
    private RevealBackgroundView vRevealBackground;
    private CoordinatorLayout coordinatorLayout;
    private boolean isLight;
    private webDBHelper dbHelper;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.news_content_layout);
        dbHelper = new webDBHelper(this,1);
        mWebView = (WebView) findViewById(R.id.webView);
        int color = Color.parseColor("#292929");
        mWebView.setBackgroundColor(color); // 设置加载时背景色
        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.getSettings().setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
        mWebView.getSettings().setDomStorageEnabled(true);
        mWebView.getSettings().setDatabaseEnabled(true);
        mWebView.getSettings().setAppCacheEnabled(true);
        isLight = getIntent().getBooleanExtra("isLight",true);
        entity = (StoriesEntity) getIntent().getSerializableExtra("entity");
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle("Enjoy Your Life");
        toolbar.setBackgroundColor(getResources().getColor(isLight ? R.color.light_toolbar : R.color.dark_toolbar));
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
                overridePendingTransition(0, R.anim.slide_out_to_left);
            }
        });
        if(HttpUtils.isNetworkConnected(this)){
            HttpUtils.get(Constant.CONTENT + entity.getId(), new TextHttpResponseHandler() {
                @Override
                public void onFailure(int i, Header[] headers, String s, Throwable throwable) {

                }

                @Override
                public void onSuccess(int i, Header[] headers, String s) {
                    SQLiteDatabase db = dbHelper.getWritableDatabase();
                    s = s.replaceAll("'", "''");
                    db.execSQL("replace into WebCacheList(newsID,json) values(" + entity.getId() + ",'" + s + "')");
                    db.close();
                    parseJson(s);
                }
            });
        } else {
            //没有网络
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            Cursor cursor = db.rawQuery("select * from WebCacheList where newsId = " + entity.getId(), null);
            if (cursor.moveToFirst()) {
                String json = cursor.getString(cursor.getColumnIndex("json"));
                parseJson(json);
            }
            cursor.close();
            db.close();
        }
    }


    private void parseJson(String responseString){
        Gson gson = new Gson();
        content = gson.fromJson(responseString,Content.class);
        String css = "";
        if(isLight){
            css = "<link rel=\"stylesheet\" href=\"file:///android_asset/news_light.css\" type=\"text/css\">"+"<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0, user-scalable=no\"> " +"<style>img{max-width: 100%; width:auto; height:auto;}</style>";
        } else {
            css = "<link rel=\"stylesheet\" href=\"file:///android_asset/news_dark.css\" type=\"text/css\">"+"<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0, user-scalable=no\"> " +"<style>img{max-width: 100%; width:auto; height:auto;}</style>";
        }
        String html = "<html><head>" + css + "</head><body>" + content.getBody() + "</body></html>";
        html = html.replace("<div class=\"img-place-holder\">", "");
        //LogUtil.e("dong",html);
        //LogUtil.e("dong",content.getBody().toString());
        mWebView.loadDataWithBaseURL("x-data://base", html, "text/html", "UTF-8", null);

    }

    @Override
    public void onStateChange(int state) {

    }
}
