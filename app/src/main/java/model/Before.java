package model;

import java.util.List;

/**
 * Created by yandong on 2017/1/30.
 */

public class Before {

    private List<StoriesEntity> stories;
    private String date;

    public void setStories(List<StoriesEntity> stories) {
        this.stories = stories;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public List<StoriesEntity> getStories() {
        return stories;
    }

    public String getDate() {
        return date;
    }
}
