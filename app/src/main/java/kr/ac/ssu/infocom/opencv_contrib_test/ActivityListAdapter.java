package kr.ac.ssu.infocom.opencv_contrib_test;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;


public class ActivityListAdapter extends BaseAdapter{
    Context context;
    ArrayList<list_activity_item> list_activity_itemArrayList;
    ViewHolder viewHolder;

    class ViewHolder{
        TextView activityTitle_textView;
        TextView activityDesc_textView;
    }


    public ActivityListAdapter(Context context, ArrayList<list_activity_item> list_activity_itemArrayList) {
        this.context = context;
        this.list_activity_itemArrayList = list_activity_itemArrayList;
    }

    @Override
    public int getCount() {
        //return 0;
        return this.list_activity_itemArrayList.size();
    }

    @Override
    public Object getItem(int position) {
        return this.list_activity_itemArrayList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        //xml을 연결하여 화면에 표시해주는 부분
        //반복문 실행으로 순차적으로 한 칸씩 화면을 구성함
        //item.xml을 불러와야함. -> Context 이용
        if(convertView == null){
            convertView = LayoutInflater.from(context).inflate(R.layout.activity_info_item, null);
            viewHolder = new ViewHolder();

            viewHolder.activityTitle_textView = (TextView)convertView.findViewById(R.id.activityTitle_textView);
            viewHolder.activityDesc_textView = (TextView)convertView.findViewById(R.id.activityDesc_textView);

            convertView.setTag(viewHolder);
        }else{
            viewHolder = (ViewHolder)convertView.getTag();
        }
        viewHolder.activityTitle_textView.setText(list_activity_itemArrayList.get(position).getTitle());
        viewHolder.activityDesc_textView.setText(list_activity_itemArrayList.get(position).getDesc());
        return convertView;
    }
}


