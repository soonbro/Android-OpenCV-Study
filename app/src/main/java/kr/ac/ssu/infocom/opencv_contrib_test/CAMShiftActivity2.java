package kr.ac.ssu.infocom.opencv_contrib_test;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.SlidingDrawer;
import android.widget.TextView;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.RotatedRect;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import static org.opencv.core.CvType.CV_8UC3;

public class CAMShiftActivity2 extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2, View.OnTouchListener, View.OnClickListener {

    private static final String TAG = "CamShift";
    private CameraBridgeViewBase mOpenCvCameraView;

    /////////////////////////////////////////////////////////////////////////
    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("opencv_java3");
        System.loadLibrary("camshift");
    }
    /////////////////////////////////////////////////////////////////////////

//    private Mat image;

    //flags and init setting
    private boolean backprojMode = false;
    private boolean selectObject = false;   // 네이티브 코드에서 사용됨
    private boolean isDrawingRect = false;
    private int trackObject = 0;    // 네이티브 코드에서 사용하고 갱신됨
    private boolean showHist = true;
    //private Point origin;
    private Rect selection;
    private int vmin = 10, vmax = 256, smin = 30;

    private Mat                 mRgba;
//    private Mat                 mBgr;
//    private Mat                 hsv;
//    private Mat                 hue;
//    private Mat                 frame;
//    private Mat                 mask;
//    private Mat                 hist;
//    private Mat                 histimg = Mat.zeros(100,160, CV_8UC3);
//    private Mat                 backproj;

    //private Mat matInput;
    private Mat matResult;

    private boolean paused = false;


    private Rect trackWindow;
    private int hsize = 16;
    private float hranges[] = {0, 180};


    public native void camShift(long matAddrInput, long matAddrResult, /*long matAddrImage,*/ int[] rectSelectionArray);


    private ImageButton mPushDrawerIb;
    private SlidingDrawer mPushInfoSd;
    private TextView mPushInfoTv;



    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status){
                case LoaderCallbackInterface.SUCCESS: {
                    mOpenCvCameraView.enableView();
                    mOpenCvCameraView.enableFpsMeter();
                    mOpenCvCameraView.setOnTouchListener(CAMShiftActivity2.this);
                } break;
                default:{
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    private void setResultToToast(final String string) {
        CAMShiftActivity2.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(CAMShiftActivity2.this, string, Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void setResultToText(final String string) {
        if (mPushInfoTv == null) {
            setResultToToast("Push info tv has not be init...");
        }
        CAMShiftActivity2.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mPushInfoTv.setText(string);
            }
        });
    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //상태바 없애기
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        //
        setContentView(R.layout.activity_camshift2);
        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.camshift2_activity_surface_view);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);
        mOpenCvCameraView.setCameraIndex(0); // front-camera(1),  back-camera(0)
        mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);

        mPushDrawerIb = (ImageButton)findViewById(R.id.tracking_drawer_control_ib);
        mPushInfoSd = (SlidingDrawer)findViewById(R.id.tracking_drawer_sd);
        mPushInfoTv = (TextView)findViewById(R.id.tracking_push_tv);

        mPushDrawerIb.bringToFront();

    }

    ///////////////////////////////////////////////

    private double calcManhattanDistance(double point1X, double point1Y, double point2X, double point2Y) {
        return Math.abs(point1X - point2X) + Math.abs(point1Y - point2Y);
    }

    float downX, downY;


    @Override
    public boolean onTouch(View v, MotionEvent event) {

        switch(event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                isDrawingRect = false;
                //selectObject = true;
                downX = event.getX();
                downY = event.getY();
                break;
            case MotionEvent.ACTION_MOVE:
                if (calcManhattanDistance(downX,downY,event.getX(),event.getY()) < 20 && !isDrawingRect){
                    return true;
                }
                isDrawingRect = true;
                selectObject = true;
                //mSendRectIV.setVisibility(View.VISIBLE);
                int l = (int)(downX < event.getX() ? downX : event.getX());
                int t = (int)(downY < event.getY() ? downY : event.getY());
                int r = (int)(downX >= event.getX() ? downX : event.getX());
                int b = (int)(downY >= event.getY() ? downY : event.getY());
//                mSendRectIV.setX(l);
//                mSendRectIV.setY(t);
//                mSendRectIV.getLayoutParams().width = r - l;
//                mSendRectIV.getLayoutParams().height = b - t;
//                mSendRectIV.requestLayout();
                selection.x = l;
                selection.y = t;
                selection.width = r - l;
                selection.height = b - t;

                //selection &= Rect(0, 0, mRgba.cols(), mRgba.rows());

                break;

            case MotionEvent.ACTION_UP:

                selectObject = false;
//                RectF rectF = getTrackingRect(mSendRectIV);
//                PointF pointF = new PointF(downX / mBgLayout.getWidth(), downY / mBgLayout.getHeight());
//                RectF pointRectF = new RectF(pointF.x, pointF.y, 0, 0);

                //mSendRectIV.setVisibility(View.INVISIBLE);
                //selection = new Rect(selection.x, selection.y, selection.width, selection.height); //TODO: 이거 필요없을걸
                if(selection.width > 0 && selection.height >0)
                    trackObject = -1; //TODO: 이부분 확인!
                break;
            default:
                break;
        }
        return true;
    }

    ////////////////////////////////////////////////////////////////////////////////////////
    @Override
    public void onCameraViewStarted(int width, int height) {
        mRgba = new Mat(height, width, CvType.CV_8UC4);
//        image = new Mat();
        //mBgr = new Mat();
        //hsv = new Mat();
        selection = new Rect();
    }

    @Override
    public void onCameraViewStopped() {
        mRgba.release();
        matResult.release();
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba();
        //Imgproc.cvtColor(mRgba, mBgr, Imgproc.COLOR_RGBA2BGR);
        //Imgproc.cvtColor(mBgr, hsv, Imgproc.COLOR_BGR2HSV);

        //Imgproc.cvtColor(mRgba, hsv, Imgproc.COLOR_RGB2HSV_FULL);

        if ( matResult != null ) matResult.release();
        matResult = new Mat(mRgba.rows(), mRgba.cols(), mRgba.type());

        camShift(mRgba.getNativeObjAddr(), matResult.getNativeObjAddr(), /*image.getNativeObjAddr(),*/ selectedRectWrapper(selection));


        StringBuffer sb = new StringBuffer();

        if (trackObject == 1) {
            RotatedRect trackBox = getTrackBox();

            if (trackBox != null) {

                Utils.addLineToSB(sb, "ROI center x: ", trackBox.center.x);
                //Log.d(TAG, "onCameraFrame :: ROI center x: " + String.valueOf(trackBox.center.x));
                Utils.addLineToSB(sb, "ROI center y: ", trackBox.center.y);
                //Log.d(TAG, "onCameraFrame :: ROI center y: " + trackBox.center.y);
                Utils.addLineToSB(sb, "ROI Width: ", trackBox.size.width);
                //Log.d(TAG, "onCameraFrame :: ROI Width: " + trackBox.size.width);
                Utils.addLineToSB(sb, "ROI Height: ", trackBox.size.height);
                //Log.d(TAG, "onCameraFrame :: ROI Height: " + trackBox.size.height);
                Utils.addLineToSB(sb, "ROI Rotated: ", trackBox.angle);
                //Log.d(TAG, "onCameraFrame :: ROI Rotated: " + trackBox.angle);
                Utils.addLineToSB(sb, "ROI Size: ", trackBox.size.area());
                //Log.d(TAG, "onCameraFrame :: ROI Size: " + trackBox.size.area());

                setResultToText(sb.toString());
            }
        }





        return matResult;
    }
    //////////////////////////////////////////////////////////////////////////////////////


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
            Log.d(TAG, "onResume :: 내장된 OpenCV 라이브러리를 찾을 수 없습니다.");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_4_0, this, mLoaderCallback);
        } else{
            Log.d(TAG, "onResume :: OpenCV 라이브러리를 패키지 안에서 찾았습니다.");
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

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.tracking_drawer_control_ib:
                if (mPushInfoSd.isOpened()) {
                    mPushInfoSd.animateClose();
                } else {
                    mPushInfoSd.animateOpen();
                }
                break;
            default:
                break;
        }
    }

    //////////////////////////////////////////////////////////////////

    //static native void nativeSelectedRect(int[] selectedRectArray); //TODO: 이건 필요 없을 듯..

    public static int[] selectedRectWrapper(Rect selection) {
        int[] selectedRectArray = new int[4];
        selectedRectArray[0] = selection.x;
        selectedRectArray[1] = selection.y;
        selectedRectArray[2] = selection.width;
        selectedRectArray[3] = selection.height;

        //nativeSelectedRect(selectedRectArray); //TODO: 이건 필요 없을 듯..
        return selectedRectArray;
    }


    static native float[] nativeGetTrackBox();

    public static RotatedRect getTrackBox(){
        float[] valArray = nativeGetTrackBox();

        //방법1
        //trackBox.center.x = valArray[0];
        //trackBox.center.y = valArray[1];
        //trackBox.size.width = valArray[2];
        //trackBox.size.height = valArray[3];
        //trackBox.angle = valArray[4];

        //방법2
        return new RotatedRect(new Point(valArray[0], valArray[1]), //센터 좌표
                                new Size(valArray[2], valArray[3]), //너비, 높이
                                valArray[4]);   // 회전 각도
    }



    //////////////////////////////////////////////////////////////////


}