package tech.wec.FaceDetector;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCamera2View;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import static android.content.ContentValues.TAG;

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
    // 首页图片
    private ImageView imageView;
    // 录入数据按钮
    private Button bt_input;
    // 身份识别按钮
    private Button bt_output;

    // native模型管理类
    private MTCNN mtcnn = new MTCNN();
    // 控制模型参数
    private int minFaceSize = 40;
    private int testTimeCount = 10;
    private int threadsNumber = 8;

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

        // 获取应用权限
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            Log.e("CameraNew", "Lacking privileges to access camera service, please request permission first.");
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE
            }, REQUEST_CAMERA_PERMISSION);
        }
        // 拷贝模型到sd卡
        try{
            copyBigDataToSD("det1.bin");
            copyBigDataToSD("det2.bin");
            copyBigDataToSD("det3.bin");
            copyBigDataToSD("det1.param");
            copyBigDataToSD("det2.param");
            copyBigDataToSD("det3.param");
        }catch (IOException e){
            e.printStackTrace();
        }
        initModel();
        initView();
    }

    private void initModel(){
        File sdDir = Environment.getExternalStorageDirectory();// 获取根目录
        String sdPath = sdDir.toString() + "/mtcnn/";
        mtcnn.FaceDetectionModelInit(sdPath);
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
        preprocessFrame();

        final int option = options;
        switch (option) {
            case VIEW_MODE_RECORD:
                return mRgba;
            case VIEW_MODE_DETECT:
                return mGray;
        }
        return mRgba;
    }

    private void preprocessFrame(){
        Core.transpose(mRgba, mRgbaT); //转置函数，可以水平的图像变为垂直
        Imgproc.resize(mRgbaT, mRgba, mRgba.size(), 0.0D, 0.0D, 0); //将转置后的图像缩放为mRgbaF的大小
        Core.flip(mRgba, mRgba, 0); //根据x,y轴翻转，0-x 1-y

        Core.transpose(mGray, mRgbaT); //转置函数，可以水平的图像变为垂直
        Imgproc.resize(mRgbaT, mGray, mGray.size(), 0.0D, 0.0D, 0); //将转置后的图像缩放为mRgbaF的大小
        Core.flip(mGray, mGray, 0); //根据x,y轴翻转，0-x 1-y

        Imgproc.cvtColor(mRgba, mRgba, Imgproc.COLOR_BGR2RGB);

        mRgba = mirrorY(mRgba);
        mGray = mirrorY(mGray);
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

    //提取像素点
    private byte[] getPixelsRGBA(Bitmap image) {
        // calculate how many bytes our image consists of
        int bytes = image.getByteCount();
        ByteBuffer buffer = ByteBuffer.allocate(bytes); // Create a new buffer
        image.copyPixelsToBuffer(buffer); // Move the byte data to the buffer
        byte[] temp = buffer.array(); // Get the underlying array containing the

        return temp;
    }

    private void copyBigDataToSD(String strOutFileName) throws IOException {
        Log.i(TAG, "start copy file " + strOutFileName);
        File sdDir = Environment.getExternalStorageDirectory();//获取跟目录
        File file = new File(sdDir.toString()+"/mtcnn/");
        if (!file.exists()) {
            file.mkdir();
        }

        String tmpFile = sdDir.toString()+"/mtcnn/" + strOutFileName;
        File f = new File(tmpFile);
        if (f.exists()) {
            Log.i(TAG, "file exists " + strOutFileName);
            return;
        }
        InputStream myInput;
        java.io.OutputStream myOutput = new FileOutputStream(sdDir.toString()+"/mtcnn/"+ strOutFileName);
        myInput = this.getAssets().open(strOutFileName);
        byte[] buffer = new byte[1024];
        int length = myInput.read(buffer);
        while (length > 0) {
            myOutput.write(buffer, 0, length);
            length = myInput.read(buffer);
        }
        myOutput.flush();
        myInput.close();
        myOutput.close();
        Log.i(TAG, "end copy file " + strOutFileName);
    }
}