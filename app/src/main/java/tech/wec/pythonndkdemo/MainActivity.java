package tech.wec.pythonndkdemo;

import android.content.Context;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.srplab.www.starcore.*;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    public StarSrvGroupClass SrvGroup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initPy();
    }

    protected void initPy(){

        final String appLib = getApplicationInfo().nativeLibraryDir;

        AsyncTask.execute(new Runnable() {

            @Override
            public void run() {
                loadPy(appLib);
            }
        });
    }

    void loadPy(String appLib){
        //拷贝Python相关环境
        //Extract python files from assets
        AssetExtractor assetExtractor = new AssetExtractor(this);
        assetExtractor.removeAssets("python");
        assetExtractor.copyAssets("python");

//        final File appFile = getFilesDir();  /*-- /data/data/packageName/files --*/
        final String pyPath = assetExtractor.getAssetsDataDir() + "python";
        try {
            // 加载Python解释器
            System.load(appLib + File.separator + "libpython3.4m.so");

            // 除了将代码直接拷贝，还支持将代码压缩为zip包，通过Install方法解压到指定路径
//            InputStream dataSource = getAssets().open("py_code.zip");
//            StarCoreFactoryPath.Install(dataSource, appFile.getPath(),true );
        } catch (Exception e) {
            e.printStackTrace();
        }

        /*----init starcore----*/
        StarCoreFactoryPath.StarCoreCoreLibraryPath = appLib;
        StarCoreFactoryPath.StarCoreShareLibraryPath = appLib;
//        StarCoreFactoryPath.StarCoreOperationPath = appFile.getPath();
        StarCoreFactoryPath.StarCoreOperationPath = pyPath;

        StarCoreFactory starcore = StarCoreFactory.GetFactory();
        StarServiceClass Service = starcore._InitSimple("test", "123", 0, 0);
        SrvGroup = (StarSrvGroupClass) Service._Get("_ServiceGroup");
        Service._CheckPassword(false);

        /*----run python code----*/
        SrvGroup._InitRaw("python34", Service);
        StarObjectClass python = Service._ImportRawContext("python", "", false, "");
        // 设置Python模块加载路径
        python._Call("import", "sys");
        StarObjectClass pythonSys = python._GetObject("sys");
        StarObjectClass pythonPath = (StarObjectClass) pythonSys._Get("path");
        pythonPath._Call("insert", 0, pyPath+ File.separator + "python3.4.zip");
        pythonPath._Call("insert", 0, appLib);
        pythonPath._Call("insert", 0, pyPath);

        //调用Python代码
        Service._DoFile("python", pyPath + "/py_code.py", "");
        long time = python._Calllong("get_time");
        Log.d("", "form python time="+time);

        Service._DoFile("python", pyPath + "/test.py", "");
        int result = python._Callint("add", 5, 2);
        Log.d("", "result="+result);

        python._Set("JavaClass", Log.class);
        Service._DoFile("python", pyPath + "/calljava.py", "");
    }

    private void copyFile(Context c, String Name) {
        File outfile = new File(c.getFilesDir(), Name);
        BufferedOutputStream outStream = null;
        BufferedInputStream inStream = null;

        try {
            outStream = new BufferedOutputStream(new FileOutputStream(outfile));
            inStream = new BufferedInputStream(c.getAssets().open(Name));

            byte[] buffer = new byte[1024 * 10];
            int readLen = 0;
            while ((readLen = inStream.read(buffer)) != -1) {
                outStream.write(buffer, 0, readLen);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (inStream != null) inStream.close();
                if (outStream != null) outStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
