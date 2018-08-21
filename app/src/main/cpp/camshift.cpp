//
// Created by park on 2018-08-09.
//
#include <jni.h>
#include <opencv2/opencv.hpp>
#include "opencv2/core.hpp"
#include <opencv2/core/utility.hpp>
#include "opencv2/imgproc.hpp"
#include "opencv2/video/tracking.hpp"

using namespace cv;
using namespace std;

// Flag 설정 부분
bool backprojMode = false; //백프로젝션모드
//bool selectObject = false; //오브젝트 선택여부

// xTxOxDxOx: 이 부분 잘 되는지 확인.. 아예 함수 호출때 넣어서 쓸까? -> 필요없음(java에서 가져오기 때문)
//int trackObject = 0; //트래킹을 초기화하기 위한 플래그 (0보다 작을때)

bool showHist = true; //히스토그램 표시 설정

Mat image;


// TODO: Rect 받아오기.  Point는 터치이벤트로 처리할거라 필요없을지도..
//Point origin;
Rect selection;

int vmin = 10, vmax = 256, smin = 30;


Rect trackWindow;
int hsize = 16; //히스토그램의 bin 크기 (16구간으로 나눔)
float hranges[] = {0, 180}; //hue range   0<hue<180
const float *phranges = hranges; //hue range 포인터


Mat     hsv, //hsv 색상 영역 선언
        hue, //hue 색상 영역 선언
        mask, //mask 영역 선언
        hist, //히스토그램 행렬 선언
        histimg = Mat::zeros(200, 320, CV_8UC3), //320*200 히스토그램을 그릴 행렬 선언
        backproj; // 백프로젝션 영역 선언

bool paused = false;



extern "C"
JNIEXPORT void JNICALL
Java_kr_ac_ssu_infocom_opencv_1contrib_1test_CAMShiftActivity_camShift(JNIEnv *env,
                                                                       jobject instance,
                                                                       jlong matAddrInput,
                                                                       jlong matAddrResult,
                                                                       //jlong matAddrImage,
                                                                       jintArray rectSelectionArray) {


    Mat &matInput = *(Mat *) matAddrInput;
    Mat &matResult = *(Mat *) matAddrResult;
//    Mat &image = *(Mat *) matAddrImage;


    // java의 trackObject 변수를 jni로 가져오기 위한 과정
    //TODO: 여기 잘 되는지 확인!
    jclass class_fieldcontrol = env->GetObjectClass(instance);
    jfieldID id_trackObject = env->GetFieldID(class_fieldcontrol, "trackObject", "I");
    jint trackObject = env->GetIntField(instance, id_trackObject);

    //TODO: 셀렉트 오브젝트 boolean 값 자바단에서 가져와야함
    jfieldID id_selectObject = env->GetFieldID(class_fieldcontrol, "selectObject", "Z");
    jboolean selectObject = env->GetBooleanField(instance, id_selectObject);

    //값 변경할떄는
    //env->SetIntField(instance, id_trackObject, trackObject);
    //이런식으로..

    //rect selection
    jint *ptrRectSelectionArray = env->GetIntArrayElements(rectSelectionArray, 0);
    Rect rectSelection(ptrRectSelectionArray[0], ptrRectSelectionArray[1],
                   ptrRectSelectionArray[2], ptrRectSelectionArray[3]);
    env->ReleaseIntArrayElements(rectSelectionArray, ptrRectSelectionArray, 0);
    //


    selection = rectSelection;
//
//    Rect trackWindow;
//    int hsize = 16; //히스토그램의 bin 크기 (16구간으로 나눔)
//    float hranges[] = {0, 180}; //hue range   0<hue<180
//    const float *phranges = hranges; //hue range 포인터

//    Mat     hsv, //hsv 색상 영역 선언
//            hue, //hue 색상 영역 선언
//            mask, //mask 영역 선언
//            hist, //히스토그램 행렬 선언
//            histimg = Mat::zeros(200, 320, CV_8UC3), //320*200 히스토그램을 그릴 행렬 선언
//            backproj; // 백프로젝션 영역 선언

    // 영상 중지 플래그
//    bool paused = false;

    if (!paused) {
        if (matInput.empty())
            return;
    }

    matInput.copyTo(image); //Mat image에 프레임 복사

    if (!paused) {
        //TODO: RGB2HSV_FULL로 변경했는데 맞는지 확인 필요함(input 색상영역이 rgba라서 이렇게 했다.) (지금은 다시 BGR2HSV로)
        cvtColor(image, hsv, COLOR_BGR2HSV); //이미지를 HSV 색상영역으로 변환해서 hsv에 저장

        if (trackObject) {
            int _vmin = vmin, _vmax = vmax;

            inRange(hsv, Scalar(0, smin, MIN(_vmin, _vmax)),
                    Scalar(180, 256, MAX(_vmin, _vmax)), mask);
            int ch[] = {0, 0};
            hue.create(hsv.size(), hsv.depth());
            mixChannels(&hsv, 1, &hue, 1, ch, 1);

            if (trackObject < 0) {
                // 객체가 선택되면, CAMShift를 하기 위한 설정을 한다.
                // Object has been selected by user, set up CAMShift search properties once

                Mat roi(hue, selection), maskroi(mask, selection);

                //roi 행렬을 단일채녈로 maskroi 영역에 대해서 결과를 hist에 hsize만큼의 차원수로 hrange만큼
                calcHist(&roi, 1, 0, maskroi, hist, 1, &hsize, &phranges);
                //
                //hist

                normalize(hist, hist, 0, 255, NORM_MINMAX);

                //마우스로 선택한 영역의 Rect를 trackWindow로 복사
                trackWindow = selection;
                //트랙오브젝트 플래그를 1로 설정
                trackObject = 1; // Don't set up again, unless user selects new ROI
                // 변경된 값을 java로 전달해주려고
                env->SetIntField(instance, id_trackObject, trackObject);


                ////////////////////////////////////////////////
                //      histimg 만드는 부분
                //histimg(320*200)을 모두 0으로 초기화
                histimg = Scalar::all(0);
                int binW = histimg.cols / hsize; //binWidth를 계산 cols(320)/hsize(16)
                Mat buf(1, hsize, CV_8UC3);
                for (int i = 0; i < hsize; i++)
                    buf.at<Vec3b>(i) = Vec3b(saturate_cast<uchar>(i * 180. / hsize), 255, 255);
                cvtColor(buf, buf, COLOR_HSV2BGR);

                for (int i = 0; i < hsize; i++) {
                    int val = saturate_cast<int>(hist.at<float>(i) * histimg.rows / 255);
                    rectangle(histimg, Point(i * binW, histimg.rows),
                              Point((i + 1) * binW, histimg.rows - val),
                              Scalar(buf.at<Vec3b>(i)), -1, 8);
                }
                /////////////////////////////////////////

            }

            // Perform CAMShift
            calcBackProject(&hue, 1, 0, hist, backproj, &phranges); //TODO: 여기서 버그 Error: Assertion failed (dims > 0 && !hist.empty())
            backproj &= mask;
            RotatedRect trackBox = CamShift(backproj, trackWindow,
                                            TermCriteria(
                                                    TermCriteria::EPS | TermCriteria::COUNT, 10, 1));
            if (trackWindow.area() <= 1) {
                int cols = backproj.cols, rows = backproj.rows, r = (MIN(cols, rows) + 5) / 6;
                trackWindow = Rect(trackWindow.x - r, trackWindow.y - r,
                                   trackWindow.x + r, trackWindow.y + r) &
                              Rect(0, 0, cols, rows);
            }

            if (backprojMode)
                cvtColor(backproj, image, COLOR_GRAY2BGR);
            ellipse(image, trackBox, Scalar(0, 0, 255), 3, LINE_AA);

        }
    } else if (trackObject < 0)
        paused = false;


    //이건 잘 되는 듯
    //여기는 오브젝트 셀렉트 진행 중일 때 영상 반전시키는 부분인듯.
    if (selectObject && selection.width > 0 && selection.height > 0) {
        Mat roi(image, selection);
        bitwise_not(roi, roi);
    }


    //이제 이미지를 보여줘야함
    //imshow("CamShift Demo", image);
    //imshow("Histogram", histimg);
    image.copyTo(matResult);
    //cvtColor(image, matResult, CV_BGR2RGBA);

    //TODO: 여기 잘못된게 분명하다.

}


extern "C"
JNIEXPORT void JNICALL
Java_kr_ac_ssu_infocom_opencv_1contrib_1test_CAMShiftActivity_nativeSelectedRect(JNIEnv *env,
                                                                                 jclass type,
                                                                                 jintArray selectedRectArray_) {
    jint *selectedRectArray = env->GetIntArrayElements(selectedRectArray_, 0);

    Rect selectedRegion(selectedRectArray[0], selectedRectArray[1], selectedRectArray[2], selectedRectArray[3]);

    env->ReleaseIntArrayElements(selectedRectArray_, selectedRectArray, 0);
}

