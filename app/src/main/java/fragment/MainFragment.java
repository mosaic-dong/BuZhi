package fragment;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.Snackbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import com.example.yandong.zhihu.MainActivity;
import com.example.yandong.zhihu.R;
import com.google.gson.Gson;
import com.loopj.android.http.TextHttpResponseHandler;

import org.apache.http.Header;

import java.util.List;

import activity.LatestContentActivity;
import adapter.MainNewsItemAdapter;
import datebase.webDBHelper;
import model.Before;
import model.Latest;
import model.StoriesEntity;
import util.Constant;
import util.HttpUtils;
import util.PreUtils;
import view.Kanner;

/**
 * Created by yandong on 2017/1/29.
 */

public class MainFragment extends BaseFragment{
    private ListView lv_news;
    private Kanner kanner;
    private Latest latest;
    private List<Latest> items;
    private Handler handler = new Handler();
    private boolean isLoading;
    private String date;
    private MainNewsItemAdapter mAdapter;
    private Before before;
    private String title;
    private webDBHelper dbHelper;

    public static MainFragment newInstance(String title){
        MainFragment mainFragment = new MainFragment();
        Bundle bundle = new Bundle();
        bundle.putString("title",title);
        mainFragment.setArguments(bundle);
        return mainFragment;
    }

//    public MainFragment(String title){
//        this.title = title;
//    }

    @Override
    protected View initView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        dbHelper = new webDBHelper(mActivity,1);
        ((MainActivity)mActivity).setToolbarTitle(title);
        View view = inflater.inflate(R.layout.main_news_layout,container,false);
        lv_news = (ListView) view.findViewById(R.id.lv_news);
        View header = inflater.inflate(R.layout.kanner,lv_news,false);
        kanner = (Kanner) header.findViewById(R.id.kanner);
        kanner.setOnItemClickListener(new Kanner.OnItemClickListener() {
            @Override
            public void click(View v, Latest.TopStoriesEntity entity) {
                StoriesEntity storiesEntity = new StoriesEntity();
                storiesEntity.setId(entity.getId());
                storiesEntity.setTitle(entity.getTitle());

                SQLiteDatabase db = dbHelper.getReadableDatabase();
                Cursor cursor = db.rawQuery("select * from WebCacheList where newsID = " + storiesEntity.getId(),null);
                if(!HttpUtils.isNetworkConnected(mActivity) && cursor.getCount() == 0){
                    Snackbar sb = Snackbar.make(lv_news,"您没有缓存这篇文章😬",Snackbar.LENGTH_SHORT);
                    sb.getView().setBackgroundColor(getResources().getColor(((MainActivity)mActivity).isLight() ?  android.R.color.holo_blue_dark : android.R.color.black));
                    sb.show();
                    return;
                } else {
                    int[] startingLocation = new int[2];
                    v.getLocationOnScreen(startingLocation);
                    startingLocation[0] += v.getWidth() / 2;
                    Intent intent = new Intent(mActivity,LatestContentActivity.class);
                    intent.putExtra(Constant.START_LOCATION, startingLocation);
                    intent.putExtra("entity",storiesEntity);
                    intent.putExtra("isLight",((MainActivity)mActivity).isLight());
                    startActivity(intent);
                    mActivity.overridePendingTransition(R.anim.zoomin,0);
                }


            }
        });
        mAdapter = new MainNewsItemAdapter(mActivity);
        lv_news.setAdapter(mAdapter);
        lv_news.addHeaderView(header);
        lv_news.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {

            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                if(lv_news != null && lv_news.getChildCount() > 0){
                    boolean enable = (firstVisibleItem == 0) && (view.getChildAt(firstVisibleItem).getTop() == 0);
                    ((MainActivity) mActivity).setSwipeRefreshEnable(enable);

                    if(firstVisibleItem + visibleItemCount == totalItemCount && !isLoading){
                        LoadMore(Constant.BEFORE + date);
                    }
                }
            }
        });
        lv_news.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                StoriesEntity entity = (StoriesEntity)parent.getAdapter().getItem(position);

                SQLiteDatabase db = dbHelper.getReadableDatabase();
                Cursor cursor = db.rawQuery("select * from WebCacheList where newsID = " + entity.getId(),null);
                if(!HttpUtils.isNetworkConnected(mActivity) && cursor.getCount() == 0){
                    Snackbar sb = Snackbar.make(lv_news,"您没有缓存这篇文章😬",Snackbar.LENGTH_SHORT);
                    sb.getView().setBackgroundColor(getResources().getColor(((MainActivity)mActivity).isLight() ?  android.R.color.holo_blue_dark : android.R.color.black));
                    sb.show();
                    return;
                } else {
                    int[] startingLocation = new int[2];
                    view.getLocationOnScreen(startingLocation);
                    startingLocation[0] += view.getWidth() /2;
                    Intent intent = new Intent(mActivity, LatestContentActivity.class);
                    intent.putExtra(Constant.START_LOCATION,startingLocation);
                    intent.putExtra("entity",entity);
                    intent.putExtra("isLight",((MainActivity)mActivity).isLight());
                    String readSequence = PreUtils.getStringToDefault(mActivity,"read","");
                    String[] splits = readSequence.split(",");
                    StringBuffer sb = new StringBuffer();
                    if(splits.length >= 200){
                        for(int i = 100; i < splits.length; i++){
                            sb.append(splits[i] + ",");
                        }
                        readSequence = sb.toString();
                    }
                    if(!readSequence.contains(entity.getId() + "")){
                        readSequence = readSequence + entity.getId() + "";
                    }
                    PreUtils.putStringToDefault(mActivity,"read",readSequence);
                    TextView tv_title = (TextView) view.findViewById(R.id.tv_title);
                    tv_title.setTextColor(getResources().getColor(R.color.clicked_tv_textcolor));
                    startActivity(intent);
                    mActivity.overridePendingTransition(R.anim.zoomin,0);
                }

            }
        });
        return view;
    }

    @Override
    protected void initData() {
        super.initData();
        LoadFirst();
    }

    private void LoadFirst(){
        isLoading = true;
        if(HttpUtils.isNetworkConnected(mActivity)){
            HttpUtils.get(Constant.LATESTNEWS, new TextHttpResponseHandler() {
                @Override
                public void onFailure(int i, Header[] headers, String s, Throwable throwable) {

                }

                @Override
                public void onSuccess(int i, Header[] headers, String s) {
                    SQLiteDatabase db = ((MainActivity)mActivity).getDbHelper().getWritableDatabase();
                    db.execSQL("replace into CacheList(date,json) values(" + Constant.LATEST_COLUMN + ",'" + s + "')");
                    db.close();
                    parseLatestJson(s);
                }
            });
        } else {
            //没有网络
            SQLiteDatabase db = ((MainActivity)mActivity).getDbHelper().getReadableDatabase();
            Cursor cursor = db.rawQuery("select * from CacheList where date = " + Constant.LATEST_COLUMN,null);
            if(cursor.moveToFirst()){
                String json = cursor.getString(cursor.getColumnIndex("json"));
                parseLatestJson(json);
            } else {
                isLoading = false;
            }
            cursor.close();
            db.close();
        }
    }

    private void LoadMore(final String url){
        isLoading = true;
        if(HttpUtils.isNetworkConnected(mActivity)){
            HttpUtils.get(url, new TextHttpResponseHandler() {
                @Override
                public void onFailure(int i, Header[] headers, String s, Throwable throwable) {

                }

                @Override
                public void onSuccess(int i, Header[] headers, String s) {
                    SQLiteDatabase db = ((MainActivity)mActivity).getDbHelper().getWritableDatabase();
                    db.execSQL("replace into CacheList(date,json) values(" + date + ",'" + s + "')");
                    db.close();
                    parseBeforeJson(s);
                }
            });
        } else {
            //没有网络
            SQLiteDatabase db = ((MainActivity)mActivity).getDbHelper().getReadableDatabase();
            Cursor cursor = db.rawQuery("select * from CacheList where date = " + date,null);
            if(cursor.moveToFirst()){
                String json = cursor.getString(cursor.getColumnIndex("json"));
                parseBeforeJson(json);
            } else {
                //离线缓存到头了
                db.delete("CacheList","date < " + date,null);
                isLoading = false;
                Snackbar sb = Snackbar.make(lv_news,"没有更多的离线缓存啦😬",Snackbar.LENGTH_SHORT);
                sb.getView().setBackgroundColor(getResources().getColor(((MainActivity)mActivity).isLight() ?  android.R.color.holo_blue_dark : android.R.color.black));
                sb.show();
            }
            cursor.close();
            db.close();
        }
    }


    private void parseLatestJson(String responseString){
        Gson gson = new Gson();
        latest = gson.fromJson(responseString,Latest.class);
        date = latest.getDate();
        kanner.setTopEntities(latest.getTop_stories());
        handler.post(new Runnable() {
            @Override
            public void run() {
                List<StoriesEntity> storiesEntities = latest.getStories();
                StoriesEntity topic = new StoriesEntity();
                topic.setType(Constant.TOPIC);
                topic.setTitle("今日热闻");
                storiesEntities.add(0,topic);
                mAdapter.addList(storiesEntities);
                isLoading = false;
            }
        });
    }

    private void parseBeforeJson(String responseString){
        Gson gson = new Gson();
        before = gson.fromJson(responseString,Before.class);
        if(before == null){
            isLoading = false;
            return;
        }
        date = before.getDate();
        handler.post(new Runnable() {
            @Override
            public void run() {
                List<StoriesEntity> storiesEntities = before.getStories();
                StoriesEntity topic = new StoriesEntity();
                topic.setType(Constant.TOPIC);
                topic.setTitle(convertDate(date));
                //topic.setTitle("今日热闻");
                storiesEntities.add(0,topic);
                mAdapter.addList(storiesEntities);
                isLoading = false;
            }
        });
    }

    private String convertDate(String date){
        String result = date.substring(0, 4);
        result += "年";
        result += date.substring(4, 6);
        result += "月";
        result += date.substring(6, 8);
        result += "日";
        return result;
    }

    public void updateTheme(){
        mAdapter.updateTheme();
    }
}
