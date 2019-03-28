package tech.wec.FaceDetector;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import io.realm.Realm;
import io.realm.RealmConfiguration;
import tech.wec.FaceDetector.util.FaceDataTrans;

/**
 * Created by willi on 3/28/2019.
 */

public class FaceInfoActivity extends AppCompatActivity {
    // 数据相关工作
    private FaceDataTrans mFaceDataTrans = new FaceDataTrans();
    private Button bt_info;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        // The Realm file will be located in Context.getFilesDir() with name "default.realm"
        Realm.init(this);
        RealmConfiguration config = new RealmConfiguration.Builder().name("FaceDetector").deleteRealmIfMigrationNeeded().build();
        Realm.setDefaultConfiguration(config);
        Toast.makeText(FaceInfoActivity.this, "成功跳转", Toast.LENGTH_LONG).show();
        setContentView(R.layout.activity_faceinfo);
        initView();
    }

    private void initView(){
        bt_info = findViewById(R.id.bt_info);
        bt_info.setText("返回");

        bt_info.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(FaceInfoActivity.this, MainActivity.class);
                startActivity(intent);
            }
        });
    }
}
