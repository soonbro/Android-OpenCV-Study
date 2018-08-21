package kr.ac.ssu.infocom.opencv_contrib_test;

import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ListView;

import java.util.ArrayList;

public class MainApplication extends AppCompatActivity{

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

        setContentView(R.layout.activity_main_application);
        listView = (ListView)findViewById(R.id.activityListView);

        list_activity_itemArrayList = new ArrayList<list_activity_item>();

        ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // activity목록
        list_activity_itemArrayList.add(new list_activity_item("그레이스케일","영상을 그레이스케일로 변환"));
        list_activity_itemArrayList.add(new list_activity_item("얼굴인식","Haar like feature 얼굴 검출 예제"));
        list_activity_itemArrayList.add(new list_activity_item("Touch Event 연습", "사각형 그리기"));
        list_activity_itemArrayList.add(new list_activity_item("Color Blob Detector", "색상 히스토그램을 이용한 Blob Detecting"));
        list_activity_itemArrayList.add(new list_activity_item("CAMShift", "색상 히스토그램을 이용한 CamShifting"));
        list_activity_itemArrayList.add(new list_activity_item("Tracker API by JAVA", "TrackerAPI 이용"));
        list_activity_itemArrayList.add(new list_activity_item("Tracker API by JNI(C++)", "TrackerAPI 이용 JNI로 성능향상"));
        ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        activityListAdapter = new ActivityListAdapter(MainApplication.this,list_activity_itemArrayList);
        listView.setAdapter(activityListAdapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener(){
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent;
                switch(position){
                    case 0:
                        intent = new Intent(getApplicationContext(),Main2Activity.class);
                        startActivity(intent);
                        break;
                    case 1:
                        intent = new Intent(getApplicationContext(),MainActivity.class);
                        startActivity(intent);
                        break;
                    case 2:
                        intent = new Intent(getApplicationContext(),Main3Activity.class);
                        startActivity(intent);
                        break;
                    case 3:
                        intent = new Intent(getApplicationContext(),ColorDetectActivity.class);
                        startActivity(intent);
                        break;
                    case 4:
                        intent = new Intent(getApplicationContext(),CAMShiftActivity.class);
                        startActivity(intent);
                        break;
                    case 5:
                        intent = new Intent(getApplicationContext(),TrackerActivity.class);
                        startActivity(intent);
                        break;
                    case 6:
                        intent = new Intent(getApplicationContext(),trackerChoiceActivity.class);
                        startActivity(intent);
                        break;
                }
                //Intent intent = new Intent(getApplicationContext(),Main2Activity.class);
            }
        });
    }
}
