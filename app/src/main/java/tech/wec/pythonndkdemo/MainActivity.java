package tech.wec.pythonndkdemo;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.graphics.drawable.Drawable;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.Range;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.chaquo.python.Python;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity implements TextureView.SurfaceTextureListener{

    private String TAG = "MainActivity";
    // 获取相机权限
    private final int REQUEST_CAMERA_PERMISSION = 0;
    // 初始化python实例
    protected Python python = Python.getInstance();
    // 为了照片竖直显示
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }
    // 相机预览界面
    private TextureView tv_camera;
    // 首页图片
    private ImageView imageView;
    // 录入数据按钮
    private Button bt_input;
    // 身份识别按钮
    private Button bt_output;
    // 相机预览Surface
    private Surface previewSurface;
    // 相机处理线程
    HandlerThread handlerThread;
    // 相机处理
    Handler handler;
    CameraDevice cameraDevice;
    CameraCaptureSession captureSession;
    // 相机开启回调
    CameraDevice.StateCallback cameraDeviceOpenCallback = null;
    // 预览请求构建
    CaptureRequest.Builder previewRequestBuilder;
    // 预览请求
    CaptureRequest previewRequest;
    // 预览回调
    CameraCaptureSession.CaptureCallback previewCallback;
    int[] faceDetectModes;
    // 相机成像尺寸
    Size pixelSize;
    int cameraOrientation;
    Bitmap bitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
//        python.getModule("py_numpy").callAttr("numpyTest");
//        python.getModule("py_opencv").callAttr("cv2Test");
//        python.getModule("py_tensorflow").callAttr("tfTest");

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
    private void initView(){
        imageView = findViewById(R.id.imageView);
        tv_camera = findViewById(R.id.tv_camera);
        bt_input = findViewById(R.id.bt_input);
        bt_output = findViewById(R.id.bt_output);

        bt_input.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                imageView.setVisibility(View.GONE);
                tv_camera.setVisibility(View.VISIBLE);
                openCamera();
            }
        });

        bt_output.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                imageView.setVisibility(View.GONE);
                openCamera();
            }
        });
    }

    private void openCamera(){
        closeCamera();
        String cameraId = CameraCharacteristics.LENS_FACING_BACK + "";
        CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try{
            // 获取开启相机的相关参数
            CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            // 获取预览尺寸
            Size[] previewSizes = map.getOutputSizes(SurfaceTexture.class);
            // 获取相机角度
            cameraOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
            // 获取成像区域
            Rect cameraRect = characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
            // 获取成像尺寸
            pixelSize = characteristics.get(CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE);
            Log.i(TAG, previewSizes.toString());
//            Size size = previewSizes[0];
            Size size = new Size(1600, 1600);

            if (tv_camera == null){
                Log.i(TAG, "tv_camera is null");
            }else{
                Log.i(TAG, tv_camera.toString());
            }
            // 设置预览尺寸（避免控件尺寸与预览画面尺寸不一致时画面变形）
            tv_camera.getSurfaceTexture().setDefaultBufferSize(size.getWidth(),size.getHeight());
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                Toast.makeText(this,"请授予摄像头权限",Toast.LENGTH_LONG).show();
                ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.CAMERA}, 0);
                return;
            }
            // 根据摄像头ID，开启摄像头
            try{
                cameraManager.openCamera(cameraId, getCameraDeviceOpenCallback(), getCameraHandler());
            }catch (CameraAccessException e){
                e.printStackTrace();
            }
        }catch (CameraAccessException e){
            e.printStackTrace();
        }
    }

    private void closeCamera(){
        if (captureSession != null){
            captureSession.close();
            captureSession = null;
        }
        if (cameraDevice!=null){
            cameraDevice.close();
            cameraDevice = null;
        }
        if(handlerThread!=null){
            handlerThread.quitSafely();
            try {
                handlerThread.join();
                handlerThread = null;
                handler = null;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private CameraDevice.StateCallback getCameraDeviceOpenCallback(){
        if (cameraDeviceOpenCallback == null){
            cameraDeviceOpenCallback = new CameraDevice.StateCallback() {
                @Override
                public void onOpened(@NonNull CameraDevice camera) {
                    cameraDevice = camera;
                    try{
                        //创建Session，需先完成画面呈现目标（此处为预览和拍照Surface）的初始化
                        camera.createCaptureSession(Arrays.asList(getPreviewSurface()), new CameraCaptureSession.StateCallback() {
                            @Override
                            public void onConfigured(@NonNull CameraCaptureSession session) {
                                captureSession = session;
                                //构建预览请求，并发起请求
                                Log.i(TAG,"[发出预览请求]");
                                try{
                                    session.setRepeatingRequest(getPreviewRequest(), getPreviewCallback(), getCameraHandler());
                                }catch (CameraAccessException e){
                                    e.printStackTrace();
                                }
                            }

                            @Override
                            public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                                session.close();
                            }
                        }, getCameraHandler());
                    }catch (CameraAccessException e){
                        e.printStackTrace();
                    }
                }

                @Override
                public void onDisconnected(@NonNull CameraDevice camera) {
                    camera.close();
                }

                @Override
                public void onError(@NonNull CameraDevice camera, int error) {
                    camera.close();
                }
            };
        }
        return cameraDeviceOpenCallback;
    }

    /**
     * 初始化并获取相机线程处理
     * @return
     */
    private Handler getCameraHandler(){
        if(handler==null){
            // 单独开一个线程给相机使用
            handlerThread = new HandlerThread("handlerThread");
            handlerThread.start();
            handler = new Handler(handlerThread.getLooper());
        }
        return handler;
    }

    private int getFaceDetectMode(){
        if(faceDetectModes == null){
            return CaptureRequest.STATISTICS_FACE_DETECT_MODE_FULL;
        }else{
            return faceDetectModes[faceDetectModes.length-1];
        }
    }

    ///////////////////////////////预览相关/////////////////////////////
    /**
     * 初始化并获取预览回调对象
     * @return
     */
    private CameraCaptureSession.CaptureCallback getPreviewCallback(){
        if (previewCallback == null){
            previewCallback = new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
                    MainActivity.this.onCameraImagePreviewed(result);
                }
            };
        }
        return previewCallback;
    }

    /**
     * 生成并获取预览请求
     * @return
     */
    private CaptureRequest getPreviewRequest(){
        previewRequest = getPreviewRequestBuilder().build();
        return previewRequest;
    }

    /**
     * 初始化并获取预览请求构建对象，进行通用配置，并每次获取时进行人脸检测级别配置
     * @return
     */
    private CaptureRequest.Builder getPreviewRequestBuilder(){
        if(previewRequestBuilder == null){
            try{
                previewRequestBuilder = captureSession.getDevice().createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                previewRequestBuilder.addTarget(getPreviewSurface());
                // 自动曝光、白平衡、对焦
                previewRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
            }catch (CameraAccessException e){
                e.printStackTrace();
            }
        }
        previewRequestBuilder.set(CaptureRequest.STATISTICS_FACE_DETECT_MODE, getFaceDetectMode());//设置人脸检测级别
        return previewRequestBuilder;
    }

    /**
     * 获取预览Surface
     * @return
     */
    private Surface getPreviewSurface(){
        if (previewSurface == null){
            previewSurface = new Surface(tv_camera.getSurfaceTexture());
        }
        return previewSurface;
    }

    /**
     * 处理相机画面处理完成事件
     * @param result
     */
    private void onCameraImagePreviewed(CaptureResult result){

    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {

    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {

    }

    ////////////////////////预览相关////////////////////////////

}
