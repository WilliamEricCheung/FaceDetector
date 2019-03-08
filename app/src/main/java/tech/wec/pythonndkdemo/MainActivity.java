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
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@SuppressWarnings("deprecation")
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
        python.getModule("py_opencv").callAttr("OpenCVTest").callAttr("cv2Test");
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
                javaCamera2View.setMaxFrameSize(160,160);
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

        Log.i(TAG, "mRgba's size: " + mRgba.size());
        Log.i(TAG, "mGray's size: " + mGray.size());

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

        final int option = options;
        switch (option) {
            case VIEW_MODE_RECORD:
                long startTime = System.currentTimeMillis(); //起始时间
                Size size = new Size(160, 160);
                Mat resize = new Mat(size, CvType.CV_8UC3);
                Imgproc.resize(mRgba, resize, size);
                long endTime = System.currentTimeMillis(); //结束时间
                long runTime = endTime - startTime;
                Log.i(TAG, String.format("resize方法使用时间 %d ms", runTime));
                return mRgba;
            case VIEW_MODE_DETECT:
                return mGray;
        }
        return mRgba;
    }

    private Mat prewhiten(Mat src){

        byte[] input = matToByteArray(src);

        long startTime = System.currentTimeMillis(); //起始时间

        PyObject output = python.getModule("py_opencv").callAttr("OpenCVTest").callAttr("prewhiten", input);
//        Log.i(TAG,"return? ");
//        Log.i(TAG, output.toString());
        Mat out = output.toJava(Mat.class);

        long endTime = System.currentTimeMillis(); //结束时间
        long runTime = endTime - startTime;
        Log.i(TAG, String.format("方法使用时间 %d ms", runTime));
//        Mat ret = byteArrayToMat(out, 640, 640);
//        Log.i(TAG, out.toString());
        Imgproc.resize(out, out, new Size(640, 640));
        return out;
    }

    private byte[] matToByteArray(Mat src){
        byte[] ret = new byte[((int) src.total() * src.channels())];
        src.get(0,0,ret);
        return ret;
    }

    private Mat byteArrayToMat(byte[] src, int height, int width){
        Mat ret = new Mat(height, width, CvType.CV_8UC3);
        ret.put(0,0, src);
        return ret;
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