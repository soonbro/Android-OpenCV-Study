package kr.ac.ssu.infocom.opencv_contrib_test;

import android.annotation.TargetApi;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.Semaphore;


public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2{

    private static final String TAG_openCV = "opencv";
    private CameraBridgeViewBase mOpenCvCameraView;
    private Mat matInput;
    private Mat matResult;

//    public native void ConvertRGBtoGray(long matAddrInput, long matAddrResult);
    public native long loadCascade(String cascadeFileName);
    public native void detect(long cascadeClassifier_face, long cascadeClassifier_eye, long matAddrInput, long matAddrResult);
    public long cascadeClassifier_face = 0;
    public long cascadeClassifier_eye = 0;

    //세마포어
    private final Semaphore writeLock = new Semaphore(1);

    public void getWriteLock() throws InterruptedException{
        writeLock.acquire();
    }

    public void releaseWriteLock(){
        writeLock.release();
    }

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("opencv_java3");
        System.loadLibrary("native-lib");
    }

    //haar xml 파일 가져오기
    private void copyFile(String filename){
        String baseDir = Environment.getExternalStorageDirectory().getPath();
        String pathDir = baseDir + File.separator + filename;

        AssetManager assetManager = this.getAssets();

        InputStream inputStream = null;
        OutputStream outputStream = null;

        try {
            Log.d( "file", "copyFile :: 다음 경로로 파일복사 "+ pathDir);
            inputStream = assetManager.open(filename);
            outputStream = new FileOutputStream(pathDir);

            byte[] buffer = new byte[1024];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer,0, read);
            }
            inputStream.close();
            inputStream = null;
            outputStream.flush();
            outputStream.close();
            outputStream = null;
        } catch (Exception e) {
            Log.d("file", "copyFile :: 파일 복사 중 예외 발생 "+e.toString() );
        }
    }

    private void read_cascade_file(){
        copyFile("haarcascade_frontalface_alt.xml");
        copyFile("haarcascade_eye_tree_eyeglasses.xml");

        Log.d("file", "read_cascade_file:");

        cascadeClassifier_face = loadCascade("haarcascade_frontalface_alt.xml");
        Log.d("file", "read_cascade_file:");

        cascadeClassifier_eye = loadCascade("haarcascade_eye_tree_eyeglasses.xml");
    }

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status){
                case LoaderCallbackInterface.SUCCESS: {
                    mOpenCvCameraView.enableView();
                    mOpenCvCameraView.enableFpsMeter();
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

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_main);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if(!hasPermissions(PERMISSIONS)){
                requestPermissions(PERMISSIONS, PERMISSIONS_REQUEST_CODE);
            }
            else read_cascade_file();//추가됨
        }
        else read_cascade_file();//추가됨

        mOpenCvCameraView = (CameraBridgeViewBase)findViewById(R.id.activity_surface_view);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);
//        mOpenCvCameraView.setCameraIndex(0); // 전면카메라(1) 후면카메라(0)
        mOpenCvCameraView.setCameraIndex(1);
        mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);

        // Example of a call to a native method
        //TextView tv = (TextView) findViewById(R.id.sample_text);
        //tv.setText(stringFromJNI());

        Button button = (Button)findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                try {
                    getWriteLock();

                    File path = new File(Environment.getExternalStorageDirectory() + "/Images/");
                    path.mkdirs();
                    File file = new File(path, "image.png");

                    String filename = file.toString();

                    Imgproc.cvtColor(matResult, matResult, Imgproc.COLOR_BGR2RGB, 4);
                    boolean ret  = Imgcodecs.imwrite( filename, matResult);
                    if ( ret ) Log.d(TAG_openCV, "SUCESS");
                    else Log.d(TAG_openCV, "FAIL");


                    Intent mediaScanIntent = new Intent( Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                    mediaScanIntent.setData(Uri.fromFile(file));
                    sendBroadcast(mediaScanIntent);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                releaseWriteLock();

            }
        }        );

    }

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    //public native String stringFromJNI();

    @Override
    public void onResume()
    {
        super.onResume();

        if(!OpenCVLoader.initDebug()){
            Log.d(TAG_openCV, "onResume :: 내장된 OpenCV 라이브러리를 찾을 수 없습니다.");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_4_0, this, mLoaderCallback);
        } else{
            Log.d(TAG_openCV, "onResume :: OpenCV 라이브러리를 패키지 안에서 찾았습니다.");
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
    public void onCameraViewStarted(int width, int height){}

    @Override
    public void onCameraViewStopped(){}

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {

        //세마포어-----------
        try {
            getWriteLock();


            //인풋 프레임을 RGBA로
            matInput = inputFrame.rgba();

            if (matResult != null) matResult.release();
            matResult = new Mat(matInput.rows(), matInput.cols(), matInput.type());

//        //RGBA로 인풋된 프레임을 GRAY 스케일로 변환
//        ConvertRGBtoGray(matInput.getNativeObjAddr(), matResult.getNativeObjAddr());
            Core.flip(matInput, matInput, 1);

            detect(cascadeClassifier_face, cascadeClassifier_eye, matInput.getNativeObjAddr(), matResult.getNativeObjAddr());
        } catch(InterruptedException e){    //세
            e.printStackTrace();            //마
        }                                   //포
        releaseWriteLock();                 //어

        return matResult;
    }

    static  final int PERMISSIONS_REQUEST_CODE = 1000;
//    String[] PERMISSIONS = {"android.permission.CAMERA"};
    String[] PERMISSIONS = {"android.permission.CAMERA", "android.permission.WRITE_EXTERNAL_STORAGE"};
    private boolean hasPermissions(String[] permissions){
        int result;
        for(String perms : permissions){
            result = ContextCompat.checkSelfPermission(this,perms);
            if (result == PackageManager.PERMISSION_DENIED){
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults){
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case PERMISSIONS_REQUEST_CODE:
                if (grantResults.length>0){
                    boolean cameraPermissionAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
//                    if(!cameraPermissionAccepted)
//                        showDialogForPermission("앱을 실행하려면 퍼미션을 허가하셔야합니다.");
                    boolean writePermissonAccepted = grantResults[1] == PackageManager.PERMISSION_GRANTED;
                    if(!cameraPermissionAccepted || !writePermissonAccepted){
                        showDialogForPermission("앱을 실행하려면 퍼미션을 허가하셔야합니다.");
                        return;
                    }else{
                        read_cascade_file();
                    }
                }break;
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    private void showDialogForPermission(String msg){
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("알림");
        builder.setMessage(msg);
        builder.setCancelable(false);
        builder.setPositiveButton("예", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                requestPermissions(PERMISSIONS, PERMISSIONS_REQUEST_CODE);
            }
        });
        builder.setNegativeButton("아니오", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                finish();
            }
        });
        builder.create().show();
    }
}