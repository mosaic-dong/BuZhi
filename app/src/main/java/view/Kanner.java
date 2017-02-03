package view;

import android.content.Context;
import android.os.Handler;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.yandong.zhihu.R;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.List;

import model.Latest;

import static com.example.yandong.zhihu.R.id.vp;

/**
 * Created by yandong on 2017/1/28.
 */

public class Kanner extends FrameLayout implements View.OnClickListener{
    private ImageLoader mImageLoader;
    private DisplayImageOptions options;
    private Context context;
    private List<View> views;
    private List<Latest.TopStoriesEntity> topStoriesEntities;
    private List<ImageView> iv_dots;
    private int delayTime;
    private ViewPager vp;
    private LinearLayout ll_dot;
    private boolean isAutoPlay;
    private Handler handler = new Handler();
    private int currentItem;
    private OnItemClickListener mItemClickListener;

    public Kanner(Context context, AttributeSet attrs, int defStyle){
        super(context,attrs,defStyle);
        mImageLoader = ImageLoader.getInstance();
        options = new DisplayImageOptions.Builder()
                .cacheInMemory(true)
                .cacheOnDisk(true)
                .build();
        this.context = context;
        initView();
    }

    public Kanner(Context context,AttributeSet attrs){
        this(context,attrs,0);
    }

    public Kanner(Context context){
        this(context,null);
    }

    private void initView(){
        views = new ArrayList<View>();
        iv_dots = new ArrayList<ImageView>();
        delayTime = 2000;
    }

    public void setTopEntities(List<Latest.TopStoriesEntity> topStoriesEntities){
        this.topStoriesEntities = topStoriesEntities;
        reset();
    }

    private void reset(){
        views.clear();
        initUI();
    }

    /*
    貌似只有加载上次缓存数据才用
     */
    private void initUI(){
        View view = LayoutInflater.from(context).inflate(R.layout.kanner_layout,this,true);
        vp = (ViewPager) view.findViewById(R.id.vp);
        ll_dot = (LinearLayout) view.findViewById(R.id.ll_dot);
        ll_dot.removeAllViews();

        int len = topStoriesEntities.size();
        for(int i = 0; i < len; i++){
            ImageView iv_dot = new ImageView(context);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            params.leftMargin = 5;
            params.rightMargin = 5;
            ll_dot.addView(iv_dot,params);
            iv_dots.add(iv_dot);
        }

        for(int i= 0; i <= len + 1; i++){
            View fm = LayoutInflater.from(context).inflate(
                    R.layout.kanner_content_layout,null);
            ImageView iv = (ImageView) fm.findViewById(R.id.iv_title);
            TextView tv_title = (TextView)fm.findViewById(R.id.tv_title);
            iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
            if (i == 0) {
                mImageLoader.displayImage(topStoriesEntities.get(len - 1).getImage(), iv, options);
                tv_title.setText(topStoriesEntities.get(len - 1).getTitle());
            } else if (i == len + 1) {
                mImageLoader.displayImage(topStoriesEntities.get(0).getImage(), iv, options);
                tv_title.setText(topStoriesEntities.get(0).getTitle());
            } else {
                mImageLoader.displayImage(topStoriesEntities.get(i - 1).getImage(), iv, options);
                tv_title.setText(topStoriesEntities.get(i - 1).getTitle());
            }
            fm.setOnClickListener(this);
            views.add(fm);
        }
        vp.setAdapter(new MyPagerAdapter());
        vp.setFocusable(true);
        vp.setCurrentItem(1);
        currentItem = 1;
        vp.addOnPageChangeListener(new MyOnPageChangeListener());
        startPlay();
    }


    class MyPagerAdapter extends PagerAdapter{

        @Override
        public int getCount() {
            return views.size();
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            container.addView(views.get(position));
            return views.get(position);
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView((View) object);
        }
    }

    class MyOnPageChangeListener implements OnPageChangeListener{

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        }

        @Override
        public void onPageSelected(int position) {
            for(int i = 0; i < iv_dots.size(); i++){
                if(i == position - 1){
                    iv_dots.get(i).setImageResource(R.drawable.dot_focus);
                }else {
                    iv_dots.get(i).setImageResource(R.drawable.dot_blur);
                }
            }
        }

        @Override
        public void onPageScrollStateChanged(int state) {
            switch (state){
                case 1: //开始滑动
                    isAutoPlay = false;
                    break;
                case 2: //滑动结束
                    isAutoPlay = true;
                    break;
                case 0:
                    if(vp.getCurrentItem() == 0){
                        vp.setCurrentItem(topStoriesEntities.size(),false);
                    } else if (vp.getCurrentItem() == topStoriesEntities.size() + 1){
                        vp.setCurrentItem(1,false);
                    }
                    currentItem = vp.getCurrentItem();
                    isAutoPlay = true;
                    break;
            }
        }
    }

    private void startPlay(){
        isAutoPlay = true;
        handler.postDelayed(task,3000);
    }

    private final Runnable task = new Runnable() {
        @Override
        public void run() {
            if(isAutoPlay){
                currentItem = currentItem % (topStoriesEntities.size() + 1) + 1;
                if(currentItem == 1){
                    vp.setCurrentItem(currentItem,false);
                    handler.post(task);
                } else {
                    vp.setCurrentItem(currentItem);
                    handler.postDelayed(task, 5000);
                }
            } else {
                handler.postDelayed(task, 5000);
            }
        }
    };

    public interface OnItemClickListener {
        public void click(View v, Latest.TopStoriesEntity entity);
    }

    public void setOnItemClickListener(OnItemClickListener mItemClickListener) {
        this.mItemClickListener = mItemClickListener;
    }



    public void onClick(View v){
        if (mItemClickListener != null) {
            Latest.TopStoriesEntity entity = topStoriesEntities.get(vp.getCurrentItem() - 1);
            mItemClickListener.click(v, entity);
        }
    }


}
