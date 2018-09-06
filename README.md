## Android-OpenCV-Study
Android application with OpenCV contrib module. Tracker API Test

안드로이드 어플리케이션에 OpenCV(+contrib module)모듈을 통합하여 여러가지 기능을 구현하고 Test 했다.

 Step by Step
  1. 그레이스케일 
  2. 얼굴인식 (Haar like feature 이용한 얼굴검출)
  3. Touch Event를 통한 이미지 출력 (사각형 그리기기)
  4. Color Blob Detection 예제
  5. CAM Shift 예제 (JNI 이용 C++)
  6. Tracker API (OpenCV JAVA module 이용)
  7. Tracker API (JNI 이용 C++ native code)

# Object Tracker Test
OpenCV contrib module의 TrackerAPI를 이용해 각각의 Tracker를 Test 후 비교해봤다.
 
 Tracker List
  1. Boosting
  2. MIL
  3. KCF
  4. TLD
  5. MEDIANFLOW
  6. MOSSE
  7. CSRT
  
  
