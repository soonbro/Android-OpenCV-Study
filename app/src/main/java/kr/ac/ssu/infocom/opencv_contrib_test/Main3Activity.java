package kr.ac.ssu.infocom.opencv_contrib_test;

import android.graphics.PointF;
import android.graphics.RectF;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

public class Main3Activity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2, View.OnTouchListener {

    /////
    private static final String TAG_OpenCV = "OpenCV";
    private CameraBridgeViewBase mOpenCvCameraView;
    private Mat matInput;
    private Mat matResult;

    private ImageView mSendRectIV;
    private RelativeLayout mBgLayout;

    // 사각형 그리는 상태 flag
    private boolean isDrawingRect = false;

    /////////////////////////////////////////////////////////////////////////
    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("opencv_java3");
        System.loadLibrary("native-lib");
    }
    /////////////////////////////////////////////////////////////////////////

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status){
                case LoaderCallbackInterface.SUCCESS: {
                    mOpenCvCameraView.enableView();
                    mOpenCvCameraView.enableFpsMeter();
                    mOpenCvCameraView.setOnTouchListener(Main3Activity.this);
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

        setContentView(R.layout.activity_main3);

//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//            if(!hasPermissions(PERMISSIONS)){
//                requestPermissions(PERMISSIONS, PERMISSIONS_REQUEST_CODE);
//            }
//        }



        mOpenCvCameraView = (CameraBridgeViewBase)findViewById(R.id.activity_surface_view);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);
//        mOpenCvCameraView.setCameraIndex(0); // 전면카메라(1) 후면카메라(0)
        mOpenCvCameraView.setCameraIndex(0);
        mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);


        ////////////////////////////
        //mTrackingImage = (ImageView) findViewById(R.id.tracking_rst_rect_iv);
        mBgLayout = (RelativeLayout)findViewById(R.id.tracking_bg_layout);
        mSendRectIV = (ImageView)findViewById(R.id.tracking_send_rect_iv);
        ////////////////////////////

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

    @Override
    public void onDestroy(){
        super.onDestroy();
        if(mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }



    @Override
    public void onCameraViewStarted(int width, int height) {    }

    @Override
    public void onCameraViewStopped() {    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {

        matInput = inputFrame.rgba();
        if ( matResult != null ) matResult.release();
        matResult = new Mat(matInput.rows(), matInput.cols(), matInput.type());

        ////////////////////////////////////////////////////////////////////////////////

        matResult = matInput;



        return matResult;
    }

/*
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        return false;
    }

*/


    ////////////////////////////////

    private RectF getTrackingRect(View iv){
        View parent = (View)iv.getParent();
        return new RectF(
                ((float)iv.getLeft() + iv.getX()) / (float)parent.getWidth(),
                ((float)iv.getTop() + iv.getY()) / (float)parent.getHeight(),
                ((float)iv.getRight() + iv.getX()) / (float)parent.getWidth(),
                ((float)iv.getBottom() + iv.getY()) / (float)parent.getHeight()
        );
    }


    //////





    private double calcManhattanDistance(double point1X, double point1Y, double point2X, double point2Y) {
        return Math.abs(point1X - point2X) + Math.abs(point1Y - point2Y);
    }

    float downX, downY;

    @Override
    public boolean onTouch(View v, MotionEvent event){

        switch (event.getAction()){
            case MotionEvent.ACTION_DOWN:
                isDrawingRect = false;
                downX = event.getX();
                downY = event.getY();
                break;
            case MotionEvent.ACTION_MOVE:
                if (calcManhattanDistance(downX,downY,event.getX(),event.getY()) < 20 && !isDrawingRect){
                    return true;
                }
                isDrawingRect = true;
                mSendRectIV.setVisibility(View.VISIBLE);
                int l = (int)(downX < event.getX() ? downX : event.getX());
                int t = (int)(downY < event.getY() ? downY : event.getY());
                int r = (int)(downX >= event.getX() ? downX : event.getX());
                int b = (int)(downY >= event.getY() ? downY : event.getY());
                mSendRectIV.setX(l);
                mSendRectIV.setY(t);
                mSendRectIV.getLayoutParams().width = r - l;
                mSendRectIV.getLayoutParams().height = b - t;
                mSendRectIV.requestLayout();

                break;

            case MotionEvent.ACTION_UP:

                RectF rectF = getTrackingRect(mSendRectIV);
                PointF pointF = new PointF(downX / mBgLayout.getWidth(), downY / mBgLayout.getHeight());
                RectF pointRectF = new RectF(pointF.x, pointF.y, 0, 0);


            /*
                mActiveTrackMission = isDrawingRect ? new ActiveTrackMission(rectF, ActiveTrackMode.TRACE) :
                        new ActiveTrackMission(pointRectF, ActiveTrackMode.TRACE);

                getActiveTrackOperator().startTracking(mActiveTrackMission, new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onResult(DJIError error) {
                        setResultToToast("Start Tracking: " + (error == null
                                ? "Success"
                                : error.getDescription()));
                    }
                });
            */


                mSendRectIV.setVisibility(View.INVISIBLE);
                break;
            default:
                break;
        }
        return true;
    }


}
