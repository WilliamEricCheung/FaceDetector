package tech.wec.FaceDetector;

import org.opencv.core.Mat;

public class MTCNN {
    //人脸检测模型导入
    public native boolean FaceDetectionModelInit(String faceDetectionModelPath);

    //人脸检测
    public native int[] FaceDetect(byte[] imageDate, int imageWidth , int imageHeight, int imageChannel);

    public native int[] MaxFaceDetect(byte[] imageDate, int imageWidth , int imageHeight, int imageChannel);

    //人脸检测模型反初始化
    public native boolean FaceDetectionModelUnInit();

    //检测的最小人脸设置
    public native boolean SetMinFaceSize(int minSize);

    //线程设置
    public native boolean SetThreadsNumber(int threadsNumber);

    //循环测试次数
    public native boolean SetTimeCount(int timeCount);

    public native void FaceAlign(long frame, float[] landmarks);

    public native float[] FaceArray(byte[] faceData);

    static {
        System.loadLibrary("mtcnn");
    }
}
