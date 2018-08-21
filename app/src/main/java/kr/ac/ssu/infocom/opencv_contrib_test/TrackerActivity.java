package kr.ac.ssu.infocom.opencv_contrib_test;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Rect2d;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.tracking.Tracker;
import org.opencv.tracking.TrackerCSRT;
import org.opencv.tracking.TrackerKCF;
import org.opencv.tracking.TrackerTLD;

public class TrackerActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2, View.OnTouchListener {

    private static final String TAG_OpenCV = "OpenCV";
    private CameraBridgeViewBase mOpenCvCameraView;

    //public native Mat trackingTest(long matAddrInput,long matAddrResult);


    /////////////////////////////////////////////////////////////////////////
    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("opencv_java3");
        //System.loadLibrary("trackerAPI");
    }
    /////////////////////////////////////////////////////////////////////////

    private Rect2d                roi;
    private Mat                 frame;
    private Mat                 frame_resize;
    private Mat                 frame_gray;
    //private Mat                 mRgba;

    private Tracker tracker = TrackerTLD.create();


    //input 영상

    //flag
    boolean roiSelected = false;
    boolean trackerInitialized = false;



    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status){
                case LoaderCallbackInterface.SUCCESS: {
                    mOpenCvCameraView.enableView();
                    mOpenCvCameraView.enableFpsMeter();
                    mOpenCvCameraView.setOnTouchListener(TrackerActivity.this);
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
        setContentView(R.layout.activity_tracker);
        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.tracker_activity_surface_view);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);
        mOpenCvCameraView.setCameraIndex(0); // front-camera(1),  back-camera(0)
        mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
    }

    @Override
    public void onPause()
    {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume()
    {
        super.onResume();

        if(!OpenCVLoader.initDebug()){
            Log.d(TAG_OpenCV, "onResume :: 내장된 OpenCV 라이브러리를 찾을 수 없습니다.");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_4_0, this, mLoaderCallback);
        } else{
            Log.d(TAG_OpenCV, "onResume :: OpenCV 라이브러리를 패키지 안에서 찾았습니다.");
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


    float downX, downY;
    boolean isDrawingRect;

    private double calcManhattanDistance(double point1X, double point1Y, double point2X, double point2Y) {
        return Math.abs(point1X - point2X) + Math.abs(point1Y - point2Y);
    }


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

                roi.x = l/4;
                roi.y = t/4;
                roi.width = (r - l)/4;
                roi.height = (b - t)/4;

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
        frame = new Mat(height, width, CvType.CV_8UC4);
        frame_resize = new Mat(height, width, CvType.CV_8UC4);
        frame_gray = new Mat(height, width, CvType.CV_8U);
        roi = new Rect2d(0,0,0,0);
    }

    @Override
    public void onCameraViewStopped() { frame.release(); }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        frame = inputFrame.rgba();
        Imgproc.cvtColor(frame, frame, Imgproc.COLOR_RGBA2RGB);
        Size m_size=new Size((double)frame.cols()/4,(double)frame.rows()/4);

        Imgproc.resize(frame, frame_resize, m_size,0,0,Imgproc.INTER_LINEAR);

        if(roiSelected){
            if(!trackerInitialized){
                if(!tracker.init(frame_resize,roi)){
                    Log.e(TAG_OpenCV,"onCameraFrame::트래커를 초기화할 수 없습니다!!");
                }
                trackerInitialized = true;
                //TODO: Toast 메시지 구현 확인 -> 토스트 하려면 UI 쓰레드에서 해야하는 것 같음. (핸들러, 루퍼 알아봐)
                //Toast.makeText(this,"트래킹 프로세스 시작!",Toast.LENGTH_SHORT).show();
            }
            else{
                // 트래킹 결과 업데이트
                if (tracker.update(frame_resize, roi)) {       //TODO: 여기서 에러난다 채널수 문제인듯 그레이스케일로 해보자->x 3채널 RGB로 해서 해결함
                    // 추적된 오브젝트에 초록색(Green) 사각형을 그린다.
                    //Imgproc.rectangle(frame, roi, Scalar(0, 255, 0), 2, 1);
                    Point p_1 = new Point(roi.tl().x*4,roi.tl().y*4) ;
                    Point p_2 = new Point(roi.br().x*4,roi.br().y*4) ;
                    Imgproc.rectangle(frame, p_1, p_2, new Scalar(0, 255, 0), 2);
                }
                else { // 업데이트 실패하면 사각형을 파란색(Blue)으로 그린다.
                    Point p_1 = new Point(roi.tl().x*4,roi.tl().y*4) ;
                    Point p_2 = new Point(roi.br().x*4,roi.br().y*4) ;
                    Imgproc.rectangle(frame, roi.tl(), roi.br(), new Scalar(255, 0, 0), 2);
                    //Toast.makeText(this, "트래커 업데이트 실패..", Toast.LENGTH_SHORT).show();
                }
            }
        }
        return frame;
    }
}