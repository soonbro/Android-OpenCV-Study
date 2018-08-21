package kr.ac.ssu.infocom.opencv_contrib_test;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ListView;

import java.util.ArrayList;

public class trackerChoiceActivity extends AppCompatActivity {

    ListView listView;
    ActivityListAdapter activityListAdapter;
    ArrayList<list_activity_item> list_activity_itemArrayList;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //상태바 없애기
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        //

        setContentView(R.layout.activity_tracker_choice);
        listView = (ListView) findViewById(R.id.activityListView);
        list_activity_itemArrayList = new ArrayList<list_activity_item>();


        ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // activity목록
        list_activity_itemArrayList.add(new list_activity_item("1. Boosting", "ROI를 pos로 하고 나머지는 배경으로 처리"));
        list_activity_itemArrayList.add(new list_activity_item("2. MIL", "Boosting 업그레이드 객체 주변의 이웃도 잠재적인 pos"));
        list_activity_itemArrayList.add(new list_activity_item("3. KCF", "Kernelized Correlation Filters MIL의 pos 중복영역 처리로 속도 향상"));
        list_activity_itemArrayList.add(new list_activity_item("4. TLD", "Tracking Learnig and Detection"));
        list_activity_itemArrayList.add(new list_activity_item("5. MEDIANFLOW", "ForwardBackWard Tracking 궤적간의 불일치 측정(큰동작취약)"));
        list_activity_itemArrayList.add(new list_activity_item("6. MOSSE", "상관필터이용 조명,스케일,자세,변형에 강함 "));
        list_activity_itemArrayList.add(new list_activity_item("7. CSRT", "Channel and Spatial Reliability 사용 차별상관필터"));
        ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        activityListAdapter = new ActivityListAdapter(trackerChoiceActivity.this, list_activity_itemArrayList);
        listView.setAdapter(activityListAdapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent;
                switch (position) {
                    case 0:
                        intent = new Intent(getApplicationContext(), TrackerJNIActivity.class);
                        intent.putExtra("KEY_TRACKER", 0);
                        startActivity(intent);
                        break;
                    case 1:
                        intent = new Intent(getApplicationContext(), TrackerJNIActivity.class);
                        intent.putExtra("KEY_TRACKER", 1);
                        startActivity(intent);
                        break;
                    case 2:
                        intent = new Intent(getApplicationContext(), TrackerJNIActivity.class);
                        intent.putExtra("KEY_TRACKER", 2);
                        startActivity(intent);
                        break;
                    case 3:
                        intent = new Intent(getApplicationContext(), TrackerJNIActivity.class);
                        intent.putExtra("KEY_TRACKER", 3);
                        startActivity(intent);
                        break;
                    case 4:
                        intent = new Intent(getApplicationContext(), TrackerJNIActivity.class);
                        intent.putExtra("KEY_TRACKER", 4);
                        startActivity(intent);
                        break;
                    case 5:
                        intent = new Intent(getApplicationContext(), TrackerJNIActivity.class);
                        intent.putExtra("KEY_TRACKER", 5);
                        startActivity(intent);
                        break;
                    case 6:
                        intent = new Intent(getApplicationContext(), TrackerJNIActivity.class);
                        intent.putExtra("KEY_TRACKER", 6);
                        startActivity(intent);
                        break;
                }

            }
        });
    }
}