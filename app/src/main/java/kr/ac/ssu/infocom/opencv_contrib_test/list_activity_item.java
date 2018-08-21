package kr.ac.ssu.infocom.opencv_contrib_test;

/**
 * Created by park on 2018-07-31.
 */

public class list_activity_item {
    private String title;
    private String desc;

    public list_activity_item(String title, String desc) {
        this.title = title;
        this.desc = desc;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }
}
