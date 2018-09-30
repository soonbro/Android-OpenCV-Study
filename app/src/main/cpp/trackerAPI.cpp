//
// Created by park on 2018-08-02.
//
#include <jni.h>
#include <opencv2/opencv.hpp>
#include <opencv2/core/utility.hpp>
#include <opencv2/tracking.hpp>
#include <android/log.h>

//#include <iostream>
//#include <cstring>
//#include <opencv2/videoio.hpp>
//#include <opencv2/highgui.hpp>
//#include <opencv2/core/ocl.hpp>

using namespace cv;
using namespace std;

// 리사이즈 할 때 쓸 변수 매크로
#define RESIZE_VAL 4

/**********************************************
*		            변수 선언
***********************************************/
Rect2d      roi;	//region of interesting을 그릴 Rect2d
Mat         frame;  //프레임을 저장할 Mat 선언
Mat         frame_resize;
Point       pt_1, pt_2; //축소된 이미지에서 얻은 ROI 좌표를 원래 이미지의 좌표로 변환시켜 저장.


//flag
bool trackerInitialized = false;
//roiSelected는 java에서 가져온다. //TODO: JNI와 JAVA에서 전역변수 처리하는 방법 알아보자...

// 트래커 선언
Ptr<Tracker> tracker;

extern "C"
JNIEXPORT void JNICALL
Java_kr_ac_ssu_infocom_opencv_1contrib_1test_TrackerJNIActivity_jniTracker(JNIEnv *env,
                                                                           jobject instance,
                                                                           jlong matAddrInput,
                                                                           jlong matAddrResult,
                                                                           jintArray selectedRectArray_) {

    // JAVA단에서 Mat 주소를 가져온다.
    Mat &matInput = *(Mat *) matAddrInput;
    Mat &matResult = *(Mat *) matAddrResult;

    // Rect selectedROI 선언 //터치이벤트로 설정된 Rect를 Wrapper를 통해 좌표를 가져와서 JNI단에서 재설정
    jint *ptrSelectedRectArray = env->GetIntArrayElements(selectedRectArray_, NULL);
    Rect selectedROI(ptrSelectedRectArray[0], ptrSelectedRectArray[1],
                       ptrSelectedRectArray[2], ptrSelectedRectArray[3]);
    env->ReleaseIntArrayElements(selectedRectArray_, ptrSelectedRectArray, 0);


    jclass class_fieldcontrol = env->GetObjectClass(instance);
    jfieldID id_roiSelected = env->GetFieldID(class_fieldcontrol, "roiSelected", "Z");
    jboolean roiSelected = env->GetBooleanField(instance, id_roiSelected);

    jfieldID id_trackerType = env->GetFieldID(class_fieldcontrol, "trackerType", "I");
    jint trackerType = env->GetIntField(instance, id_trackerType);


/***********************
 *		프로세스 시작
 ************************/
    matInput.copyTo(frame);

    if (roiSelected) {
        //roiSelected flag는 JAVA단에서 터치이벤트로 ROI가 설정되면 True로 변경된다.
        //터치 이벤트로 설정된 영역의 좌표를 가져와서 ROI를 설정한다.
        roi = selectedROI;

        //__android_log_print(ANDROID_LOG_DEBUG, "frame :: ", "%d x %d", frame.cols, frame.rows);

        //resize(frame, frame_resize, Size(frame.cols / 4, frame.rows / 4), 0, 0, INTER_NEAREST);
        resize(frame, frame_resize, Size(frame.cols / RESIZE_VAL, frame.rows / RESIZE_VAL), 0, 0, INTER_NEAREST);
        //__android_log_print(ANDROID_LOG_DEBUG, "frame :: ", "%d x %d", frame_resize.cols, frame_resize.rows);

        //3ch에서 동작 하는 트래커를 위해서 이미지 변환
        if(trackerType == 2 || trackerType == 5 || trackerType == 6){
            cvtColor(frame_resize, frame_resize, COLOR_RGBA2RGB, 3);
        }

        /***********************
        *		ROI Tracking
        ************************/
        if (!trackerInitialized) {  //트래커가 초기화 되지 않았으면
            // tracker 생성
            switch(trackerType){
                case 0:
                    tracker = TrackerBoosting::create();
                    __android_log_print(ANDROID_LOG_DEBUG, "TrackerJNI :: ", "Boosting 트래커 생성!");
                    break;
                case 1:
                    tracker = TrackerMIL::create();
                    __android_log_print(ANDROID_LOG_DEBUG, "TrackerJNI :: ", "MIL 트래커 생성!");
                    break;
                case 2:
                    tracker = TrackerKCF::create();
                    __android_log_print(ANDROID_LOG_DEBUG, "TrackerJNI :: ", "KCF 트래커 생성!");
                    break;
                case 3:
                    tracker = TrackerTLD::create();
                    __android_log_print(ANDROID_LOG_DEBUG, "TrackerJNI :: ", "TLD 트래커 생성!");
                    break;
                case 4:
                    tracker = TrackerMedianFlow::create();
                    __android_log_print(ANDROID_LOG_DEBUG, "TrackerJNI :: ", "MedianFlow 트래커 생성!");
                    break;
                case 5:
                    tracker = TrackerMOSSE::create();
                    __android_log_print(ANDROID_LOG_DEBUG, "TrackerJNI :: ", "MOSSE 트래커 생성!");
                    break;
                case 6:
                    tracker = TrackerCSRT::create();
                    __android_log_print(ANDROID_LOG_DEBUG, "TrackerJNI :: ", "CSRT 트래커 생성!");
                    break;
                default:
                    tracker = TrackerTLD::create();
                    __android_log_print(ANDROID_LOG_DEBUG, "TrackerJNI :: ", "TLD 트래커 생성!");
                    break;
            }

            // tracker 초기화
            if (!tracker->init(frame_resize, roi))
            {	//초기화 실패시
                //printf("트래커를 초기화할 수 없습니다.\n");
                //return -1;
                __android_log_print(ANDROID_LOG_ERROR, "TrackerJNI :: ", "트래커를 초기화할 수 없습니다.");
                return;
            }
            else {
                //초기화 성공시
                trackerInitialized = true; // 트래커초기화 플래그 설정 [true]
                //printf("트래킹 프로세스 시작! 종료하려면 ESC 키 입력\n");
                __android_log_print(ANDROID_LOG_DEBUG, "TrackerJNI :: ", "트래킹 프로세스 시작!");
            }
        }
        else {
            // 트래킹 결과 업데이트
            if (tracker->update(frame_resize, roi)) {
                // 추적된 오브젝트에 초록색(Green) 사각형을 그린다.
                //rectangle(frame, roi, Scalar(0, 255, 0), 2, 1);//원본영상기준
                //Point pt_1(roi.tl()*4), pt_2(roi.br()*4); // 축소된 영상에서 원본영상 기준으로 좌표 계산
                //pt_1 = roi.tl() * 4,      pt_2 = roi.br() * 4;
                pt_1 = roi.tl() * RESIZE_VAL,      pt_2 = roi.br() * RESIZE_VAL;
                rectangle(frame, pt_1, pt_2, Scalar(0, 255, 0), 2, 1);
            }
            else {
                // 업데이트 실패하면 사각형을 빨간색(Red)으로 그린다.
                //rectangle(frame, roi, Scalar(255, 0, 0), 2, 1); //원본영상기준
                //Point pt_1(roi.tl()*4), pt_2(roi.br()*4); // 축소된 영상에서 원본영상 기준으로 좌표 계산
                rectangle(frame, pt_1, pt_2, Scalar(255, 0, 0), 2, 1);
            }
        }
    }
    else {
        trackerInitialized = false;
        __android_log_print(ANDROID_LOG_DEBUG, "TrackerJNI :: ", "트래커가 초기화 되지 않았습니다. ROI를 설정해주세요.");

    }

    frame.copyTo(matResult);

}

