package tech.wec.pythonndkdemo;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import com.chaquo.python.PyObject;
import com.chaquo.python.Python;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCamera2View;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfInt;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    private String TAG = "MainActivity";

    private static final int VIEW_MODE_RECORD = 0;
    private static final int VIEW_MODE_DETECT = 1;
    private int options = 0;

    private JavaCamera2View javaCamera2View;

    private Mat mRgba;
    private Mat mRgbaT;
    private Mat mGray;
    private Mat mFlipRgba;

    // 获取相机权限
    private final int REQUEST_CAMERA_PERMISSION = 0;
    // 初始化python实例
    protected Python python = Python.getInstance();
    // 首页图片
    private ImageView imageView;
    // 录入数据按钮
    private Button bt_input;
    // 身份识别按钮
    private Button bt_output;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i(TAG, "OpenCV loaded successfully");
                    javaCamera2View.enableView();
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };

    public MainActivity() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        python.getModule("py_numpy").callAttr("numpyTest");
        python.getModule("py_opencv").callAttr("cv2Test");
        python.getModule("py_tensorflow").callAttr("tfTest");

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            Log.e("CameraNew", "Lacking privileges to access camera service, please request permission first.");
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE
            }, REQUEST_CAMERA_PERMISSION);
        }

        initView();
    }

    /**
     * 初始化界面元素
     */
    private void initView() {
        imageView = findViewById(R.id.imageView);
//        tv_camera = findViewById(R.id.tv_camera);
        bt_input = findViewById(R.id.bt_input);
        bt_output = findViewById(R.id.bt_output);

        javaCamera2View = findViewById(R.id.cv_camera);

        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                imageView.setVisibility(View.GONE);
                javaCamera2View.setVisibility(View.VISIBLE);
                if (javaCamera2View != null) {
                    javaCamera2View.disableView();
                    try {
                        Thread.sleep(30);
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
                options = VIEW_MODE_DETECT;
                javaCamera2View.enableView();
                javaCamera2View.setCvCameraViewListener(MainActivity.this);
                javaCamera2View.setCameraIndex(CameraBridgeViewBase.CAMERA_ID_FRONT);
                javaCamera2View.setMaxFrameSize(640, 640);
                javaCamera2View.enableFpsMeter();
            }
        });
        bt_input.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                options = VIEW_MODE_RECORD;
            }
        });

        bt_output.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                options = VIEW_MODE_DETECT;
            }
        });
    }

    @Override
    public void onPause() {
        super.onPause();
        if (javaCamera2View != null) {
            javaCamera2View.disableView();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (javaCamera2View != null)
            javaCamera2View.disableView();
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        mRgba = new Mat(height, width, CvType.CV_8UC3);
        mFlipRgba = new Mat(height, width, CvType.CV_8UC3);
        mGray = new Mat(height, width, CvType.CV_8UC3);
        mRgbaT = new Mat(height, width, CvType.CV_8UC3);
    }

    @Override
    public void onCameraViewStopped() {
        mRgba.release();
        mRgbaT.release();
        mGray.release();
        mFlipRgba.release();
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba();
        mGray = inputFrame.gray();

        Core.transpose(mRgba, mRgbaT); //转置函数，可以水平的图像变为垂直
        Imgproc.resize(mRgbaT, mRgba, mRgba.size(), 0.0D, 0.0D, 0); //将转置后的图像缩放为mRgbaF的大小
        Core.flip(mRgba, mRgba, 0); //根据x,y轴翻转，0-x 1-y

        Core.transpose(mGray, mRgbaT); //转置函数，可以水平的图像变为垂直
        Imgproc.resize(mRgbaT, mGray, mGray.size(), 0.0D, 0.0D, 0); //将转置后的图像缩放为mRgbaF的大小
        Core.flip(mGray, mGray, 0); //根据x,y轴翻转，0-x 1-y

        Imgproc.cvtColor(mRgba, mRgba, Imgproc.COLOR_BGR2RGB);

        mRgba = mirrorY(mRgba);
        mGray = mirrorY(mGray);

//        mRgba = prewhiten(mRgba);

//        Log.i(TAG, mRgba.dump());
//        mRgba = whiteBalance(mRgba);
        final int option = options;
        switch (option) {
            case VIEW_MODE_RECORD:
                return mGray;
            case VIEW_MODE_DETECT:
                return mRgba;
        }
        return mRgba;
    }

//    private Mat prewhiten(Mat src){
//        double[][] array = matToArray(src);
////        PyObject res = python.getModule("py_opencv").callAttr("prewhiten", array[0]);
////        double[][] ret  = res.toJava(double.class);
////        return ret;
//    }

    public static double[][] matToArray(Mat frame){
        double array[][] = new double[frame.height()][frame.width()];
        for (int i =0;i<frame.height();i++){
            for (int j =0;j<frame.width();j++){
                array[i][j] = frame.get(i,j)[0];
            }
        }
        return array;
    }

    public static Mat arrayToMat(double[][] array,int height, int width, int matType)
    {
        Mat image = new Mat(height,width,matType);
        for (int i=0; i<height; i++)
        {
            for (int j=0; j<width; j++)
            {
                image.put(i,j,array[i][j]);
            }
        }
        return image;
    }

    /**
     * 图像水平翻转
     * @param frame
     * @return
     */
    private Mat mirrorY(Mat frame) {
        int row = frame.rows();
        int col = frame.cols();
        Mat res = frame.clone();
        for (int i = 0; i < col; i++) {
            frame.col(col - 1 - i).copyTo(res.col(i));
        }
        return res;
    }

}