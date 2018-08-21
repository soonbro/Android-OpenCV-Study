package kr.ac.ssu.infocom.opencv_contrib_test;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;


/**
 *      JNI 이용해 TrackerAPI 사용하는 Activity
 */
public class TrackerJNIActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2, View.OnTouchListener {

    private static final String TAG = "TrackerJNI";
    private CameraBridgeViewBase mOpenCvCameraView;

    /////////////////////////////////////////////////////////////////////////
    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("opencv_java3");
        System.loadLibrary("trackerAPI");
    }
    /////////////////////////////////////////////////////////////////////////
    // 선언

    private Mat         matInput;
    private Mat         matResult;

    private Rect        roi;

    private boolean     roiSelected = false;    //JNI에서 사용
    private boolean     isDrawingRect = false;

    private int         trackerType;            //JNI에서 사용


    //네이티브 메소드 선언 (native method declaration)
    public native void jniTracker(long matAddrInput, long matAddrResult, int[] selectedRectArray);



    //OpenCV Base 로더 콜백
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status){
                case LoaderCallbackInterface.SUCCESS: {
                    mOpenCvCameraView.enableView();         // 카메라뷰
                    mOpenCvCameraView.enableFpsMeter();     // FPS 미터 설정
                    mOpenCvCameraView.setOnTouchListener(TrackerJNIActivity.this);  //터치이벤트 처리
                } break;
                default:{
                    super.onManagerConnected(status);
                } break;
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //상태바 없애기
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        //
        setContentView(R.layout.activity_tracker_jni);

        Intent intent = getIntent();//이 액티비티를 호출한 인텐트를 받아온다.
        trackerType = intent.getIntExtra("KEY_TRACKER",3); // 디폴트->TLD 트래커

        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.tracker_jni_activity_surface_view);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);
        mOpenCvCameraView.setCameraIndex(0); // front-camera(1),  back-camera(0)
        mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
    }


    private double calcManhattanDistance(double point1X, double point1Y, double point2X, double point2Y) {
        return Math.abs(point1X - point2X) + Math.abs(point1Y - point2Y);
    }

    float downX, downY;

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        //TODO: 터치 이벤트 ROI 설정 구현
        switch (event.getAction()){
            case MotionEvent.ACTION_DOWN:
                isDrawingRect = false;
                roiSelected = false;
                downX = event.getX();
                downY = event.getY();
                break;
            case MotionEvent.ACTION_MOVE:
                if (calcManhattanDistance(downX,downY,event.getX(),event.getY()) < 20 && !isDrawingRect){
                    return true;
                }
                isDrawingRect = true;
                roiSelected = false;
                //mSendRectIV.setVisibility(View.VISIBLE);
                int l = (int)(downX < event.getX() ? downX : event.getX());
                int t = (int)(downY < event.getY() ? downY : event.getY());
                int r = (int)(downX >= event.getX() ? downX : event.getX());
                int b = (int)(downY >= event.getY() ? downY : event.getY());
                /*mSendRectIV.setX(l);
                mSendRectIV.setY(t);
                mSendRectIV.getLayoutParams().width = r - l;
                mSendRectIV.getLayoutParams().height = b - t;
                mSendRectIV.requestLayout();*/

                //원본크기로 할 때
                //roi.x = l;
                //roi.y = t;
                //roi.width = (r - l);
                //roi.height = (b - t);

                //영상 축소해서 할 때
                roi.x = l/4;
                roi.y = t/4;
                roi.width = (r - l)/4;
                roi.height = (b - t)/4;

                Imgproc.rectangle(matResult, new Point(roi.x * 4, roi.y * 4),
                        new Point((roi.x + roi.width)*4, (roi.y + roi.height)*4),
                        new Scalar(0, 0, 255), 2);

                break;

            case MotionEvent.ACTION_UP:
                if(roi.width > 0 && roi.height >0)
                    roiSelected = true;
                break;
            default:
                break;
        }
        return true;
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
       matInput = new Mat(height, width, CvType.CV_8UC4);
       roi = new Rect();
       //
    }

    @Override
    public void onCameraViewStopped() {
        matInput.release();
        matResult.release();
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {

        matInput = inputFrame.rgba();
        if ( matResult != null ) matResult.release();
        matResult = new Mat(matInput.rows(), matInput.cols(), matInput.type());

        //TODO

        jniTracker(matInput.getNativeObjAddr(), matResult.getNativeObjAddr(), selectedRectWrapper(roi));

        return matResult;
    }






    @Override
    public void onPause()
    {
        super.onPause();
        if (mOpenCvCameraView != null) {
            mOpenCvCameraView.disableView();
            mOpenCvCameraView.disableFpsMeter();
        }
    }

    @Override
    public void onResume()
    {
        super.onResume();

        if(!OpenCVLoader.initDebug()){
            //Log.d(TAG, "onResume :: 내장된 OpenCV 라이브러리를 찾을 수 없습니다.");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_4_0, this, mLoaderCallback);
        } else{
            //Log.d(TAG, "onResume :: OpenCV 라이브러리를 패키지 안에서 찾았습니다.");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null){
            mOpenCvCameraView.disableView();
            mOpenCvCameraView.disableFpsMeter();
        }

    }


    public static int[] selectedRectWrapper(Rect selectedRect) {
        int[] selectedRectArray = new int[4];
        selectedRectArray[0] = selectedRect.x;
        selectedRectArray[1] = selectedRect.y;
        selectedRectArray[2] = selectedRect.width;
        selectedRectArray[3] = selectedRect.height;
        return selectedRectArray;
    }

}