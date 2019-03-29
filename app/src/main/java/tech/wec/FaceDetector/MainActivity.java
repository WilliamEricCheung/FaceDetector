package tech.wec.FaceDetector;

import android.Manifest;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Environment;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.facebook.stetho.Stetho;
import com.uphyca.stetho_realm.RealmInspectorModulesProvider;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCamera2View;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.regex.Pattern;

import io.realm.Realm;
import io.realm.RealmConfiguration;
import tech.wec.FaceDetector.util.FaceDataTrans;

import static java.sql.Types.NULL;

@SuppressWarnings("deprecation")
public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    private String TAG = "MainActivity";

    private static final int VIEW_MODE_RECORD = 1;
    private static final int VIEW_MODE_DETECT = 2;
    private int options;
    private String inputName;

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
    // 身份数据管理按钮
    private Button bt_info;
    // 身份信息输入框
    private EditText inputText;
    // native模型管理类
    private MTCNN mtcnn = new MTCNN();
    // 控制模型参数
    private int minFaceSize = 40;
    private int testTimeCount = 10;
    private int threadsNumber = 4;
    // 是否打开仅最大脸检测
    private boolean maxFaceSetting = false;
    // 数据相关工作
    private FaceDataTrans mFaceDataTrans = new FaceDataTrans();

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
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        // The Realm file will be located in Context.getFilesDir() with name "default.realm"
        Realm.init(this);
        RealmConfiguration config = new RealmConfiguration.Builder().name("FaceDetector.realm").deleteRealmIfMigrationNeeded().build();
        Realm.setDefaultConfiguration(config);
        // realm inspection
        Stetho.initialize(
                Stetho.newInitializerBuilder(this)
                        .enableDumpapp(Stetho.defaultDumperPluginsProvider(this))
                        .enableWebKitInspector(RealmInspectorModulesProvider.builder(this).build())
                        .build());

        setContentView(R.layout.activity_main);

        // 获取应用权限
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            Log.e("CameraNew", "Lacking privileges to access camera service, please request permission first.");
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE
            }, REQUEST_CAMERA_PERMISSION);
        }
        // 拷贝模型到sd卡
        try {
            copyBigDataToSD("det1.bin");
            copyBigDataToSD("det2.bin");
            copyBigDataToSD("det3.bin");
            copyBigDataToSD("det1.param");
            copyBigDataToSD("det2.param");
            copyBigDataToSD("det3.param");
            copyBigDataToSD("recognition.param");
            copyBigDataToSD("recognition.bin");
        } catch (IOException e) {
            e.printStackTrace();
        }
        initModel();
        initView();
    }

    private void initModel() {
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
        inputText = new EditText(this);
        bt_output = findViewById(R.id.bt_output);
        bt_info = findViewById(R.id.bt_info);

        javaCamera2View = findViewById(R.id.cv_camera);

//        imageView.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                imageView.setVisibility(View.GONE);
//                javaCamera2View.setVisibility(View.VISIBLE);
//                if (javaCamera2View != null) {
//                    javaCamera2View.disableView();
//                    try {
//                        Thread.sleep(30);
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
//                }
//                javaCamera2View.enableView();
//                javaCamera2View.setCvCameraViewListener(MainActivity.this);
//                javaCamera2View.setCameraIndex(CameraBridgeViewBase.CAMERA_ID_FRONT);
//                javaCamera2View.enableFpsMeter();
//            }
//        });
        bt_input.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                imageView.setVisibility(View.GONE);
                javaCamera2View.setVisibility(View.VISIBLE);
                if (javaCamera2View != null) {
                    javaCamera2View.disableView();
                    try {
                        Thread.sleep(30);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                javaCamera2View.enableView();
                javaCamera2View.setCvCameraViewListener(MainActivity.this);
                javaCamera2View.setCameraIndex(CameraBridgeViewBase.CAMERA_ID_FRONT);
                javaCamera2View.enableFpsMeter();
                showDialog();
            }
        });

        bt_output.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                imageView.setVisibility(View.GONE);
                javaCamera2View.setVisibility(View.VISIBLE);
                if (javaCamera2View != null) {
                    javaCamera2View.disableView();
                    try {
                        Thread.sleep(30);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                javaCamera2View.enableView();
                javaCamera2View.setCvCameraViewListener(MainActivity.this);
                javaCamera2View.setCameraIndex(CameraBridgeViewBase.CAMERA_ID_FRONT);
                javaCamera2View.enableFpsMeter();
                options = VIEW_MODE_DETECT;
            }
        });

        bt_info.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, FaceInfoActivity.class);
                startActivity(intent);
            }
        });
    }

    private void showDialog() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setIcon(R.drawable.profile_round);
        builder.setTitle("Please input your name");
        if (inputText.getParent() != null)
            ((ViewGroup) inputText.getParent()).removeView(inputText);
        builder.setView(inputText);
        builder.setNegativeButton("Cancel", null);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String name = inputText.getText().toString();
                Log.i(TAG, name);

//                byte[] left = {1,2,3,4,5};
//                byte[] center = {6,7,8,9,10};
//                byte[] right = {11,12,13,14,15};
                // 添加成功的条件是，没有这个人的名字，如果已经存在就失败
                final boolean success = mFaceDataTrans.addFace(name);
                if (success) {
                    options = VIEW_MODE_RECORD;
                    View view = findViewById(R.id.activity_main);
                    Snackbar.make(view, "请缓慢移动面部录入不同角度数据", Snackbar.LENGTH_INDEFINITE)
                            .setAction("完毕", new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    Toast.makeText(MainActivity.this, "录入成功", Toast.LENGTH_LONG).show();
                                    options = NULL;
                                }
                            }).show();
                    inputName = name;
                } else {
                    Toast.makeText(MainActivity.this, "人脸信息已经存在，无法重复添加", Toast.LENGTH_LONG).show();
                }
//                mFaceDataTrans.addFaceData(name, "left", left);
//                mFaceDataTrans.addFaceData(name, "center", center);
//                mFaceDataTrans.addFaceData(name, "right", right);
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
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
                return startRecordFace(mRgba);
            case VIEW_MODE_DETECT:
                return startDetectFace(mRgba, true);
        }
        return mRgba;
    }

    /**
     * 记录人脸，与检测人脸前面相同的处理（包括人脸识别和对齐），但是得到对齐后的人脸与3个位置方向后进行数据库的插入工作
     *
     * @param frame
     * @return
     */
    private Mat startRecordFace(Mat frame) {
        maxFaceSetting = true;
        mtcnn.SetMinFaceSize(minFaceSize);
        mtcnn.SetTimeCount(testTimeCount);
        mtcnn.SetThreadsNumber(threadsNumber);
        Bitmap pic = Bitmap.createBitmap(frame.width(), frame.height(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(frame, pic);
        Mat alignedFace = frame.clone();

        int width = pic.getWidth();
        int height = pic.getHeight();
        byte[] imageData = getPixelsRGBA(pic);

        long timeDetectFace = System.currentTimeMillis();
        int[] faceInfo = null;
        if (!maxFaceSetting) {
            faceInfo = mtcnn.FaceDetect(imageData, width, height, 4);
            Log.i(TAG, "检测所有人脸");
        } else {
            faceInfo = mtcnn.MaxFaceDetect(imageData, width, height, 4);
            Log.i(TAG, "检测最大人脸");
        }
        timeDetectFace = System.currentTimeMillis() - timeDetectFace;
        Log.i(TAG, "人脸平均检测时间：" + timeDetectFace / testTimeCount);
        if (faceInfo.length > 1) {
            int faceNum = faceInfo[0];
            Log.i(TAG, "人脸数目：" + faceNum);
            for (int i = 0; i < faceNum; i++) {
                int left, top, right, bottom;
                left = faceInfo[1 + 14 * i];
                top = faceInfo[2 + 14 * i];
                right = faceInfo[3 + 14 * i];
                bottom = faceInfo[4 + 14 * i];
                Point lefttop = new Point(left, top);
                Point rightbottom = new Point(right, bottom);

                // 传入人脸五个特征坐标
                float[] landmarks = new float[10];
                landmarks[0] = faceInfo[5 + 14 * i];
                landmarks[1] = faceInfo[6 + 14 * i];
                landmarks[2] = faceInfo[7 + 14 * i];
                landmarks[3] = faceInfo[8 + 14 * i];
                landmarks[4] = faceInfo[9 + 14 * i];
                landmarks[5] = faceInfo[10 + 14 * i];
                landmarks[6] = faceInfo[11 + 14 * i];
                landmarks[7] = faceInfo[12 + 14 * i];
                landmarks[8] = faceInfo[13 + 14 * i];
                landmarks[9] = faceInfo[14 + 14 * i];

                // 摄像头下的人脸数据
                String pos = mtcnn.FaceAlign(alignedFace.getNativeObjAddr(), landmarks);
                Log.i(TAG, "position: " + pos);
                Bitmap tmp = Bitmap.createBitmap(alignedFace.width(), alignedFace.height(), Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(alignedFace, tmp);
                byte[] alignedFaceData = getPixelsRGBA(tmp);
                Log.i(TAG, "aligned Face Data size: " + alignedFaceData.length);
                float[] faceData = mtcnn.FaceArray(alignedFaceData);
                // 得到pos和faceData后进行数据库的存入
                Log.i(TAG, "input name: " + inputName);
                mFaceDataTrans.addFaceData(inputName, pos, encode(faceData));
//                Log.i(TAG, "Face Data Size: "+faceData.length);
                Imgproc.rectangle(frame, lefttop, rightbottom, new Scalar(255, 255, 0, 255), 2);
//                canvas.drawPoints(new float[]{faceInfo[5+14*i],faceInfo[10+14*i],
//                        faceInfo[6+14*i],faceInfo[11+14*i],
//                        faceInfo[7+14*i],faceInfo[12+14*i],
//                        faceInfo[8+14*i],faceInfo[13+14*i],
//                        faceInfo[9+14*i],faceInfo[14+14*i]}, paint);//画多个点

                //return alignedFace;
            }
        } else {
            Log.i(TAG, "没有检测到人脸!!!");
        }
//        Log.i(TAG, "Mat to Bitmap: "+pic.getWidth()+"*"+pic.getHeight());
//        Utils.bitmapToMat(pic, frame);
        return frame;
    }


    /**
     * @param frame
     * @param detectMode true = maxFace
     * @return
     */
    private Mat startDetectFace(Mat frame, boolean detectMode) {
        maxFaceSetting = detectMode;
        mtcnn.SetMinFaceSize(minFaceSize);
        mtcnn.SetTimeCount(testTimeCount);
        mtcnn.SetThreadsNumber(threadsNumber);
        Bitmap pic = Bitmap.createBitmap(frame.width(), frame.height(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(frame, pic);
        Mat alignedFace = frame.clone();

        int width = pic.getWidth();
        int height = pic.getHeight();
        byte[] imageData = getPixelsRGBA(pic);

        long timeDetectFace = System.currentTimeMillis();
        int[] faceInfo = null;
        if (!maxFaceSetting) {
            faceInfo = mtcnn.FaceDetect(imageData, width, height, 4);
            Log.i(TAG, "检测所有人脸");
        } else {
            faceInfo = mtcnn.MaxFaceDetect(imageData, width, height, 4);
            Log.i(TAG, "检测最大人脸");
        }
        timeDetectFace = System.currentTimeMillis() - timeDetectFace;
        Log.i(TAG, "人脸平均检测时间：" + timeDetectFace / testTimeCount);
        if (faceInfo.length > 1) {
            int faceNum = faceInfo[0];
            Log.i(TAG, "人脸数目：" + faceNum);
            for (int i = 0; i < faceNum; i++) {
                int left, top, right, bottom;
                left = faceInfo[1 + 14 * i];
                top = faceInfo[2 + 14 * i];
                right = faceInfo[3 + 14 * i];
                bottom = faceInfo[4 + 14 * i];
                Point lefttop = new Point(left, top);
                Point rightbottom = new Point(right, bottom);

                // 传入人脸五个特征坐标
                float[] landmarks = new float[10];
                landmarks[0] = faceInfo[5 + 14 * i];
                landmarks[1] = faceInfo[6 + 14 * i];
                landmarks[2] = faceInfo[7 + 14 * i];
                landmarks[3] = faceInfo[8 + 14 * i];
                landmarks[4] = faceInfo[9 + 14 * i];
                landmarks[5] = faceInfo[10 + 14 * i];
                landmarks[6] = faceInfo[11 + 14 * i];
                landmarks[7] = faceInfo[12 + 14 * i];
                landmarks[8] = faceInfo[13 + 14 * i];
                landmarks[9] = faceInfo[14 + 14 * i];

                // 摄像头下的人脸数据
                String pos = mtcnn.FaceAlign(alignedFace.getNativeObjAddr(), landmarks);
                Log.i(TAG, "position: " + pos);
                Bitmap tmp = Bitmap.createBitmap(alignedFace.width(), alignedFace.height(), Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(alignedFace, tmp);
                byte[] alignedFaceData = getPixelsRGBA(tmp);
                Log.i(TAG, "aligned Face Data size: " + alignedFaceData.length);
                float[] faceData = mtcnn.FaceArray(alignedFaceData);
                Log.i(TAG, "Face Data Size: " + faceData.length);
                // 遍历数据库，寻找某姓名中3*128D数据最短欧氏距离的人脸数据
                // 先将摄像头下的人脸数据填入表，作为临时变量name==tmp，然后进行对比，如果找到后面只有tmp符合，就查找失败
                mFaceDataTrans.addFace("tmp");
                mFaceDataTrans.addFaceData("tmp", pos, encode(faceData));
                // 删除临时变量tmp
                mFaceDataTrans.deleteLastName();

                Imgproc.rectangle(frame, lefttop, rightbottom, new Scalar(255, 255, 0, 255), 2);
//                canvas.drawPoints(new float[]{faceInfo[5+14*i],faceInfo[10+14*i],
//                        faceInfo[6+14*i],faceInfo[11+14*i],
//                        faceInfo[7+14*i],faceInfo[12+14*i],
//                        faceInfo[8+14*i],faceInfo[13+14*i],
//                        faceInfo[9+14*i],faceInfo[14+14*i]}, paint);//画多个点

                //return alignedFace;
            }
        } else {
            Log.i(TAG, "没有检测到人脸!!!");
        }
//        Log.i(TAG, "Mat to Bitmap: "+pic.getWidth()+"*"+pic.getHeight());
//        Utils.bitmapToMat(pic, frame);
        return frame;
    }

    private void preprocessFrame() {
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
     *
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
        File file = new File(sdDir.toString() + "/mtcnn/");
        if (!file.exists()) {
            file.mkdir();
        }

        String tmpFile = sdDir.toString() + "/mtcnn/" + strOutFileName;
        File f = new File(tmpFile);
        if (f.exists()) {
            Log.i(TAG, "file exists " + strOutFileName);
            return;
        }
        InputStream myInput;
        java.io.OutputStream myOutput = new FileOutputStream(sdDir.toString() + "/mtcnn/" + strOutFileName);
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

    public static byte[] encode(float floatArray[]) {
        byte byteArray[] = new byte[floatArray.length * 4];
        // wrap the byte array to the byte buffer
        ByteBuffer byteBuf = ByteBuffer.wrap(byteArray);
        // create a view of the byte buffer as a float buffer
        FloatBuffer floatBuf = byteBuf.asFloatBuffer();
        // now put the float array to the float buffer,
        // it is actually stored to the byte array
        floatBuf.put(floatArray);
        return byteArray;
    }


    public static float[] decode(byte byteArray[]) {
        float floatArray[] = new float[byteArray.length / 4];
        // wrap the source byte array to the byte buffer
        ByteBuffer byteBuf = ByteBuffer.wrap(byteArray);
        // create a view of the byte buffer as a float buffer
        FloatBuffer floatBuf = byteBuf.asFloatBuffer();
        // now get the data from the float buffer to the float array,
        // it is actually retrieved from the byte array
        floatBuf.get(floatArray);
        return floatArray;
    }

}