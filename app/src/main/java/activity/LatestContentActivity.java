package activity;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.ImageView;

import com.example.yandong.zhihu.R;
import com.google.gson.Gson;
import com.loopj.android.http.TextHttpResponseHandler;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;

import org.apache.http.Header;

import datebase.webDBHelper;
import model.Content;
import model.StoriesEntity;
import util.Constant;
import util.HttpUtils;
import view.RevealBackgroundView;

/**
 * Created by yandong on 2017/1/31.
 */

public class LatestContentActivity extends AppCompatActivity implements RevealBackgroundView.OnStateChangeListener {
    private boolean isLight;
    private AppBarLayout mAppBarLayout;
    private RevealBackgroundView mRevealBackgroundView;
    private StoriesEntity entity;
    private ImageView iv;
    private WebView mWebView;
    private Content content;
    private NestedScrollView nestedScrollView;
    private webDBHelper dbHelper;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //getWindow().requestFeature(Window.FEATURE_CONTENT_TRANSITIONS);
        //Transition slide_from_bottom = TransitionInflater.from(this).inflateTransition(R.transition.slide_from_bottom);
        //getWindow().setEnterTransition(slide_from_bottom);
        setContentView(R.layout.latest_content_layout);
        dbHelper = new webDBHelper(this,1);
        isLight = getIntent().getBooleanExtra("isLight",true);
        mAppBarLayout = (AppBarLayout) findViewById(R.id.app_bar_layout);
        //mAppBarLayout.setVisibility(View.INVISIBLE);
        //mRevealBackgroundView = (RevealBackgroundView) findViewById(R.id.revealBackgroundView);
        entity = (StoriesEntity) getIntent().getSerializableExtra("entity");
        iv = (ImageView) findViewById(R.id.iv);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        nestedScrollView = (NestedScrollView) findViewById(R.id.nestedScrollView);
        mWebView = (WebView) findViewById(R.id.webView);
        int color = Color.parseColor("#292929");
        mWebView.setBackgroundColor(color); // 设置加载时背景色
        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.getSettings().setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
        mWebView.getSettings().setDomStorageEnabled(true);
        mWebView.getSettings().setDatabaseEnabled(true);
        mWebView.getSettings().setAppCacheEnabled(true);

        //设置webView的高度为屏幕高度减去toolbar高度
        WindowManager wm = this.getWindowManager();
        int height = wm.getDefaultDisplay().getHeight();

        ViewGroup.LayoutParams  params_bar = (ViewGroup.LayoutParams) toolbar.getLayoutParams();
        ViewGroup.LayoutParams  param_web = (ViewGroup.LayoutParams) nestedScrollView.getLayoutParams();
        param_web.height = height - params_bar.height;
        Log.e("dong",height+"");
        Log.e("dong",params_bar.height+"");
        Log.e("dong",param_web.height+"");
        nestedScrollView.setLayoutParams(param_web);


        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
        CollapsingToolbarLayout mCollapsingToolbarLayout = (CollapsingToolbarLayout) findViewById(R.id.collapsing_toolbar_layout);
        mCollapsingToolbarLayout.setTitle(entity.getTitle());
        mCollapsingToolbarLayout.setContentScrimColor(getResources().getColor(isLight ? R.color.light_toolbar : R.color.dark_toolbar));
        mCollapsingToolbarLayout.setStatusBarScrimColor(getResources().getColor(isLight ? R.color.light_toolbar : R.color.dark_toolbar));

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
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            Cursor cursor = db.rawQuery("select * from WebCacheList where newsID = " + entity.getId(),null);
            if(cursor.moveToFirst()){
                String json = cursor.getString(cursor.getColumnIndex("json"));
                parseJson(json);
            }
            cursor.close();
            db.close();
        }
        //setupRevealBackground(savedInstanceState);
        setStatusBarColor(getResources().getColor(R.color.light_toolbar));


    }


    private void setStatusBarColor(int statusBarColor) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // If both system bars are black, we can remove these from our layout,
            // removing or shrinking the SurfaceFlinger overlay required for our views.
            Window window = this.getWindow();
            if (statusBarColor == Color.BLACK && window.getNavigationBarColor() == Color.BLACK) {
                window.clearFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            } else {
                window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            }
            window.setStatusBarColor(statusBarColor);
        }
    }

    private void setupRevealBackground(Bundle savedInstanceState) {
        mRevealBackgroundView.setOnStateChangeListener(this);
        if (savedInstanceState == null) {
            final int[] startingLocation = getIntent().getIntArrayExtra(Constant.START_LOCATION);
            mRevealBackgroundView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                @Override
                public boolean onPreDraw() {
                    mRevealBackgroundView.getViewTreeObserver().removeOnPreDrawListener(this);
                    mRevealBackgroundView.startFromLocation(startingLocation);
                    return true;
                }
            });
        } else {
            mRevealBackgroundView.setToFinishedFrame();
        }
    }

    private void parseJson(String responseString){
        Gson gson = new Gson();
        content = gson.fromJson(responseString,Content.class);
        final ImageLoader imageLoader = ImageLoader.getInstance();
        DisplayImageOptions options = new DisplayImageOptions.Builder()
                .cacheInMemory(true)
                .cacheOnDisk(true)
                .build();
        imageLoader.displayImage(content.getImage(),iv,options);
        String css = "";
        if(isLight){
            css = "<link rel=\"stylesheet\" href=\"file:///android_asset/news_light.css\" type=\"text/css\">"+"<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0, user-scalable=no\"> " +"<style>img{max-width: 100%; width:auto; height:auto;}</style>";
        } else {
            css = "<link rel=\"stylesheet\" href=\"file:///android_asset/news_dark.css\" type=\"text/css\">"+"<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0, user-scalable=no\"> " +"<style>img{max-width: 100%; width:auto; height:auto;}</style>";
        }
        String html = "<html><head>" + css + "</head><body>" + content.getBody() + "</body></html>";
        html = html.replace("<div class=\"img-place-holder\">", "");
        mWebView.loadDataWithBaseURL("x-data://base", html, "text/html", "UTF-8", null);
    }




    public void onBackPressed(){
        finish();
        overridePendingTransition(0,R.anim.slide_out_to_left_from_right);
    }

    @Override
    public void onStateChange(int state) {
        if (RevealBackgroundView.STATE_FINISHED == state) {
            mAppBarLayout.setVisibility(View.VISIBLE);
            setStatusBarColor(Color.TRANSPARENT);
        }
    }
}
