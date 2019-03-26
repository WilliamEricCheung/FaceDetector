package tech.wec.FaceDetector.util;

import android.util.Log;

import java.util.Arrays;

import io.realm.Realm;

/**
 * Created by willi on 3/26/2019.
 */

public class FaceDataTrans {

    private final String TAG = "FaceDataTrans";

    // 这里仅添加个人姓名，脸部信息在这里添加过后马上继续添加
    public boolean addFace(String name_){
        Log.i(TAG, "tring to add name: "+ name_);
        String foundName = findNameFromFaceData(name_);
        // 如果名字已经存在，不能添加信息
        if ( foundName != null && foundName.equals( name_)) {
            Log.i(TAG, "unable to add existed name: " + foundName);
            return false;
        }
        else {
            Realm mRealm = Realm.getDefaultInstance();
            final String name = name_;

            mRealm.executeTransaction(new Realm.Transaction() {
                @Override
                public void execute(Realm realm) {

                    FaceData faceData = realm.createObject(FaceData.class, name);
                    realm.copyToRealmOrUpdate(faceData);
                }
            });
            return true;
        }
    }

    public void addFaceData(String name_, String pos_, byte[] img_){
        Realm mRealm = Realm.getDefaultInstance();
        final String name = name_;
        final String pos = pos_;
        final byte[] img = img_;
        final FaceData faceData = mRealm.where(FaceData.class).equalTo("name", name).findFirst();
        mRealm.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                FacePosData facePosData = realm.createObject(FacePosData.class);
                facePosData.setPos(pos);
                facePosData.setImg(img);
                faceData.getFacePosDatas().add(facePosData);
                realm.copyToRealm(facePosData);
                realm.copyToRealmOrUpdate(faceData);
            }
        });

    }

    public String findNameFromFaceData(String name_){
        Realm mRealm = Realm.getDefaultInstance();
        final String name = name_;
        FaceData faceData = mRealm.where(FaceData.class).equalTo("name", name).findFirst();
        if (faceData != null) {
            //Log.i(TAG, "found name: " + faceData.getName());
            return faceData.getName();
        }else
            return null;
    }

    public byte[] getFacePosImg(String name_, String pos_){
        Realm mRealm = Realm.getDefaultInstance();
        final String name = name_;
        final String pos = pos_;
        FaceData faceData = mRealm.where(FaceData.class).equalTo("name", name).findFirst();
        FacePosData posData= faceData.getFacePosDatas().where().equalTo("pos", pos).findFirst();
        Log.i(TAG, "found pos: "+ posData.getPos());
        Log.i(TAG, "found pos data: "+ Arrays.toString(posData.getImg()));
        return posData.getImg();
    }

    public void deleteFaceData(String name_){
        Realm mRealm = Realm.getDefaultInstance();
        final String name = name_;
        mRealm.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                FaceData faceData = realm.where(FaceData.class).equalTo("name", name).findFirst();
                faceData.deleteFromRealm();
            }
        });
    }
}
