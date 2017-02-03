package com.example.yandong.zhihu;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.loopj.android.http.JsonHttpResponseHandler;

import org.apache.http.Header;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import datebase.mDBHelper;
import fragment.MainFragment;
import fragment.NewsFragment;
import model.NewsListltem;
import util.Constant;
import util.DownloadUtil;
import util.HttpUtils;
import util.PreUtils;

import static com.example.yandong.zhihu.R.id.lv_news;
import static com.example.yandong.zhihu.R.id.sr;


public class MainActivity extends AppCompatActivity {
    private NavigationView navigationView;
    private TextView tv_header;
    private TextView tv_land;
    private TextView tv_backup;
    private TextView tv_download;
    private List<NewsListltem> items;
    private List<String> item_id;
    private Menu menu;
    private MenuItem menuItem;
    private Toolbar toolbar;
    private SwipeRefreshLayout refreshLayout;
    private FrameLayout fl_content;
    private DrawerLayout drawerLayout;
    private boolean isLight;
    private String curId;
    private SharedPreferences sp;
    private LinearLayout menu_top;
    private mDBHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        dbHelper = new mDBHelper(this, 1);
        initActivity();
        loadLatest();

    }

    private void loadLatest(){
        getSupportFragmentManager().beginTransaction().
                setCustomAnimations(R.anim.slide_in_from_right, R.anim.slide_out_to_left).
                replace(R.id.fl_content, new MainFragment().newInstance(R.string.app_name+""), "latest").
                commit();
        curId = "latest";
    }

    private void initActivity(){
        navigationView=(NavigationView) findViewById(R.id.menu_navigation);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        refreshLayout = (SwipeRefreshLayout) findViewById(sr);
        fl_content = (FrameLayout) findViewById(R.id.fl_content);
        drawerLayout = (DrawerLayout) findViewById(R.id.drawerLayout);
        View view = LayoutInflater.from(this).inflate(R.layout.menu_header,null);
        navigationView.addHeaderView(view);
        tv_header = (TextView) view.findViewById(R.id.tv_main);
        menu_top = (LinearLayout) view.findViewById(R.id.menu_top);
        tv_land = (TextView) view.findViewById(R.id.land);
        tv_backup = (TextView) view.findViewById(R.id.tv_backup);
        tv_download = (TextView) view.findViewById(R.id.tv_download);

        tv_download.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(DownloadUtil.startDwonLoad(MainActivity.this)){
                    Log.e("dong","success");
                    Snackbar snackbar = Snackbar.make(drawerLayout,"离线缓存成功😘",Snackbar.LENGTH_LONG);
                    snackbar.getView().setBackgroundColor(getResources().getColor(isLight() ?  android.R.color.holo_blue_dark : android.R.color.black));
                    snackbar.show();
                }else {
                    Log.e("dong","fail");
                    Snackbar snackbar = Snackbar.make(drawerLayout,"离线缓存失败😔",Snackbar.LENGTH_LONG);
                    snackbar.getView().setBackgroundColor(getResources().getColor(isLight() ?  android.R.color.holo_blue_dark : android.R.color.black));
                    snackbar.show();
                }
            }
        });

        sp = PreferenceManager.getDefaultSharedPreferences(this);
        isLight = sp.getBoolean("isLight",true);

        items = new ArrayList<NewsListltem>();
        menu = navigationView.getMenu();
        initMenu();

        toolbar.setBackgroundColor(getResources().getColor(isLight ? R.color.light_toolbar : R.color.dark_toolbar));
        setSupportActionBar(toolbar);
        getWindow().setStatusBarColor(getResources().getColor(isLight ? R.color.light_toolbar : R.color.dark_toolbar));
        updateNavigationViewTheme(isLight);

        refreshLayout.setColorSchemeResources(android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light);
        refreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                replaceFragment();
                refreshLayout.setRefreshing(false);
            }
        });

        final ActionBarDrawerToggle actionBarDrawerToggle = new ActionBarDrawerToggle(MainActivity.this,
                drawerLayout,toolbar,R.string.app_name,R.string.app_name);
        drawerLayout.setDrawerListener(actionBarDrawerToggle);
        actionBarDrawerToggle.syncState();

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar,menu);
        menu.getItem(0).setTitle(sp.getBoolean("isLight", true) ? "夜间模式" : "日间模式");
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_mode){
            //isLight = sp.getBoolean("isLight", true);
            isLight = !isLight;
            item.setTitle(isLight ? "夜间模式" : "日间模式");
            toolbar.setBackgroundColor(getResources().getColor(isLight ? R.color.light_toolbar : R.color.dark_toolbar));
            setStatusBarColor(getResources().getColor(isLight ? R.color.light_toolbar : R.color.dark_toolbar));
            if(curId.equals("latest")){
                ((MainFragment)getSupportFragmentManager().findFragmentByTag("latest")).updateTheme();
            } else {
                ((NewsFragment)getSupportFragmentManager().findFragmentByTag("news")).updateTheme();
            }
            updateNavigationViewTheme(isLight);
            sp.edit().putBoolean("isLight",isLight).apply();
        }

        return super.onOptionsItemSelected(item);
    }

    private void replaceFragment(){
        if(curId.equals("latest")){
            getSupportFragmentManager().beginTransaction().setCustomAnimations(R.anim.slide_in_from_right,R.anim.slide_out_to_left)
                    .replace(R.id.fl_content,new MainFragment().newInstance("首页"),"latest").commit();
        } else {
            //如果不是最新新闻
        }
    }

    private void initMenu(){

        if(HttpUtils.isNetworkConnected(MainActivity.this)){
            HttpUtils.get(Constant.THEMES,new JsonHttpResponseHandler(){
                @Override
                public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                    super.onSuccess(statusCode, headers, response);
                    String json = response.toString();
                    PreUtils.putStringToDefault(MainActivity.this,Constant.THEMENEWS,json);
                    ParseJson(response);
                }
            });

        } else {
            Snackbar sb = Snackbar.make(drawerLayout,"你已经进入了没有网络的异次元😬",Snackbar.LENGTH_SHORT);
            sb.getView().setBackgroundColor(getResources().getColor(isLight() ?  android.R.color.holo_blue_dark : android.R.color.black));
            sb.show();
            String themeJson = PreUtils.getStringToDefault(MainActivity.this,Constant.THEMENEWS,"");
            try{
                JSONObject response = new JSONObject(themeJson);
                ParseJson(response);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(MenuItem menuItem) {
                //Log.i("dong",menuItem.getItemId()+"");
                int position = menuItem.getItemId();
                getSupportFragmentManager().beginTransaction().setCustomAnimations(R.anim.slide_in_from_right,R.anim.slide_out_to_left)
                        .replace(R.id.fl_content,new NewsFragment((items.get(position).getId()),(items.get(position).getTitle())),"news").commit();
                setCurId(items.get(position).getId());
                closeMenu();
                return true;
            }
        });
        tv_header.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getSupportFragmentManager().beginTransaction().setCustomAnimations(R.anim.slide_in_from_right,R.anim.slide_out_to_left)
                        .replace(R.id.fl_content,new MainFragment().newInstance("首页"),"latest").commit();
                setCurId("latest");
                closeMenu();
            }
        });

    }

    private void ParseJson(JSONObject response){
        try{
            JSONArray itemsArray = response.getJSONArray("others");
            menu.clear();
            for(int i = 0;i < itemsArray.length(); i++ ){
                NewsListltem newsListltem = new NewsListltem();
                JSONObject itemObject = itemsArray.getJSONObject(i);
                //item_id.add();
                newsListltem.setId(itemObject.getString("id"));
                newsListltem.setTitle(itemObject.getString("name"));
                items.add(newsListltem);
                menu.add(0,i,i,itemObject.getString("name"));
                //menuItem =  menu.getItem(i);
                //menuItem.setTitle();
            }
        } catch(JSONException e) {
            e.printStackTrace();
        }
    }

    public void setCurId(String id){
        curId = id;
    }

    public boolean isLight(){
        return isLight;
    }

    public void closeMenu(){
        drawerLayout.closeDrawers();
    }

    public void setSwipeRefreshEnable(boolean enable){
        refreshLayout.setEnabled(enable);
    }

    public void setToolbarTitle(String text){
        toolbar.setTitle(text);
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


    public mDBHelper getDbHelper(){
        return dbHelper;
    }

    private void updateNavigationViewTheme(boolean isLight){
        menu_top.setBackgroundColor(getResources().getColor(isLight ? R.color.light_menu_header : R.color.dark_menu_header));
        tv_land.setTextColor(getResources().getColor(isLight ? R.color.light_menu_header_tv : R.color.dark_menu_header_tv));
        tv_backup.setTextColor(getResources().getColor(isLight ? R.color.light_menu_header_tv : R.color.dark_menu_header_tv));
        tv_download.setTextColor(getResources().getColor(isLight ? R.color.light_menu_header_tv : R.color.dark_menu_header_tv));
        //tv_header.setTextColor(getResources().getColor(isLight ? R.color.light_menu_index_background : R.color.dark_menu_index_background));
        tv_header.setBackgroundColor(getResources().getColor(isLight ? R.color.light_menu_index_background : R.color.dark_menu_index_background));

        navigationView.setBackgroundColor(getResources().getColor(isLight ? R.color.light_menu_listview_background : R.color.dark_menu_listview_background));

//并不能成功运行，未解决………………………………
//        int[][] states = new int[][]{
//                new int[]{android.R.attr.state_enabled} // enabled
//        };
//        int[] Light = new int[]{
//                Color.GREEN
//        };
//        int[] Dark = new int[]{
//                Color.YELLOW
//        };
//        ColorStateList colorStateListLight = new ColorStateList(states, Light);
//        ColorStateList colorStateListDark = new ColorStateList(states, Dark);
//        navigationView.setItemTextColor(isLight ? colorStateListLight : colorStateListDark);
    }
}
