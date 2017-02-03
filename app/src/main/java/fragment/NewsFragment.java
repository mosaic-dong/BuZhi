package fragment;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.example.yandong.zhihu.MainActivity;
import com.example.yandong.zhihu.R;
import com.google.gson.Gson;
import com.loopj.android.http.TextHttpResponseHandler;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;

import org.apache.http.Header;

import activity.NewsContentActivity;
import adapter.NewsItemAdapter;
import datebase.webDBHelper;
import model.News;
import model.StoriesEntity;
import util.Constant;
import util.HttpUtils;
import util.PreUtils;

/**
 * Created by yandong on 2017/1/30.
 */

public class NewsFragment extends BaseFragment{
    private String urlid;
    private String title;
    private ImageLoader mImageLoader;
    private ListView lv_news;
    private ImageView iv_title;
    private TextView tv_title;
    private News news;
    private NewsItemAdapter mAdapter;
    private webDBHelper dbHelper;

//    public static NewsFragment newInstance(String urlid,String title){
//        NewsFragment newsFragment = new NewsFragment();
//        Bundle bundle = new Bundle();
//        bundle.putString("urlid",urlid);
//        bundle.putString("title",title);
//        newsFragment.setArguments(bundle);
//        return newsFragment;
//    }

    public NewsFragment(String urlID,String title){
        this.urlid = urlID;
        this.title = title;
    }

    @Override
    protected View initView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        dbHelper = new webDBHelper(mActivity,1);
        ((MainActivity)mActivity).setToolbarTitle(title);
        View view = inflater.inflate(R.layout.main_news_layout,container,false);
        mImageLoader = ImageLoader.getInstance();
        lv_news = (ListView)view.findViewById(R.id.lv_news);
        View header = LayoutInflater.from(mActivity).inflate(R.layout.news_header,lv_news,false);
        iv_title = (ImageView) header.findViewById(R.id.iv_title);
        tv_title = (TextView) header.findViewById(R.id.tv_title);
        lv_news.addHeaderView(header);
        //lv_news.setOnItemClickListener();
        lv_news.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                StoriesEntity entity = (StoriesEntity) parent.getAdapter().getItem(position);

                SQLiteDatabase db = dbHelper.getReadableDatabase();
                Cursor cursor = db.rawQuery("select * from WebCacheList where newsID = " + entity.getId(),null);
                if(!HttpUtils.isNetworkConnected(mActivity) && cursor.getCount() == 0){
                    Snackbar sb = Snackbar.make(lv_news,"您没有缓存这篇文章😬",Snackbar.LENGTH_SHORT);
                    sb.getView().setBackgroundColor(getResources().getColor(((MainActivity)mActivity).isLight() ?  android.R.color.holo_blue_dark : android.R.color.black));
                    sb.show();
                    return;
                }else {
                    int[] startingLocation = new int[2];
                    view.getLocationOnScreen(startingLocation);
                    startingLocation[0] += view.getWidth() / 2;

                    Intent intent = new Intent(mActivity, NewsContentActivity.class);
                    intent.putExtra(Constant.START_LOCATION, startingLocation);
                    intent.putExtra("entity", entity);
                    intent.putExtra("isLight", ((MainActivity) mActivity).isLight());

                    String readSequence = PreUtils.getStringToDefault(mActivity, "read", "");
                    String[] splits = readSequence.split(",");
                    StringBuffer sb = new StringBuffer();
                    if (splits.length >= 200) {
                        for (int i = 100; i < splits.length; i++) {
                            sb.append(splits[i] + ",");
                        }
                        readSequence = sb.toString();
                    }

                    if (!readSequence.contains(entity.getId() + "")) {
                        readSequence = readSequence + entity.getId() + ",";
                    }
                    PreUtils.putStringToDefault(mActivity, "read", readSequence);
                    TextView tv_title = (TextView) view.findViewById(R.id.tv_title);
                    tv_title.setTextColor(getResources().getColor(R.color.clicked_tv_textcolor));
                    startActivity(intent);
                    mActivity.overridePendingTransition(R.anim.zoomin,0);
                }

            }
        });
        lv_news.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {

            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                if(lv_news != null && lv_news.getChildCount() > 0){
                    boolean enable = (firstVisibleItem == 0) && (view.getChildAt(firstVisibleItem).getTop() == 0);
                    ((MainActivity)mActivity).setSwipeRefreshEnable(enable);
                }

            }
        });
        return view;
    }

    protected void initData(){
        super.initData();
        if(HttpUtils.isNetworkConnected(mActivity)){
            HttpUtils.get(Constant.THEMENEWS + urlid, new TextHttpResponseHandler() {
                @Override
                public void onFailure(int i, Header[] headers, String s, Throwable throwable) {

                }

                @Override
                public void onSuccess(int i, Header[] headers, String s) {
                    SQLiteDatabase db = ((MainActivity)mActivity).getDbHelper().getWritableDatabase();
                    db.execSQL("replace into CacheList(date,json) values(" + (Constant.BASE_COLUMN + Integer.parseInt(urlid)) + ",'" + s + "')");
                    db.close();
                    parseJson(s);
                }
            });
        } else {
            //没有网络
            SQLiteDatabase db = ((MainActivity)mActivity).getDbHelper().getReadableDatabase();
            Cursor cursor = db.rawQuery("select * from CacheList where date = " + (Constant.BASE_COLUMN + Integer.parseInt(urlid)),null);
            if(cursor.moveToFirst()){
                String json = cursor.getString(cursor.getColumnIndex("json"));
                parseJson(json);
            } else {
                Snackbar sb = Snackbar.make(lv_news,"没有缓存此内容😬",Snackbar.LENGTH_SHORT);
                sb.getView().setBackgroundColor(getResources().getColor(((MainActivity)mActivity).isLight() ?  android.R.color.holo_blue_dark : android.R.color.black));
                sb.show();
            }
            cursor.close();
            db.close();
        }
    }


    private void parseJson(String responseString){
        Gson gson = new Gson();
        news = gson.fromJson(responseString,News.class);
        DisplayImageOptions options = new DisplayImageOptions.Builder()
                .cacheInMemory(true)
                .cacheOnDisk(true)
                .build();
        tv_title.setText(news.getDescription());
        mImageLoader.displayImage(news.getImage(),iv_title,options);
        mAdapter = new NewsItemAdapter(mActivity,news.getStories());
        lv_news.setAdapter(mAdapter);
    }

    public void updateTheme(){
        mAdapter.updateTheme();
    }
}
