package tech.wec.FaceDetector.util;

import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.Arrays;

import io.realm.Realm;
import io.realm.RealmList;
import io.realm.RealmResults;

/**
 * Created by willi on 3/26/2019.
 */

public class FaceDataTrans {

    private final String TAG = "FaceDataTrans";

    // 这里仅添加个人姓名，脸部信息在这里添加过后马上继续添加
    public boolean addFace(String name_) {
        Log.i(TAG, "tring to add name: " + name_);
        String foundName = findNameFromFaceData(name_);
        // 如果名字已经存在，不能添加信息
        if (foundName != null && foundName.equals(name_)) {
            Log.i(TAG, "unable to add existed name: " + foundName);
            return false;
        } else {
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

    public void addFaceData(String name_, String pos_, byte[] img_) {
        Realm mRealm = Realm.getDefaultInstance();
        final String name = name_;
        final String pos = pos_;
        final byte[] img = img_;
        final FaceData faceData = mRealm.where(FaceData.class).equalTo("name", name).findFirst();
        FacePosData tmp = faceData.getFacePosDatas().where().equalTo("pos", pos).findFirst();
        if (tmp == null) {
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
        } else {
            mRealm.executeTransaction(new Realm.Transaction() {
                @Override
                public void execute(Realm realm) {
                    FacePosData facePosData = faceData.getFacePosDatas().where().equalTo("pos", pos).findFirst();
                    facePosData.setImg(img);
                }
            });
        }

    }

    public String findNameFromFaceData(String name_) {
        Realm mRealm = Realm.getDefaultInstance();
        final String name = name_;
        FaceData faceData = mRealm.where(FaceData.class).equalTo("name", name).findFirst();
        if (faceData != null) {
            //Log.i(TAG, "found name: " + faceData.getName());
            return faceData.getName();
        } else
            return null;
    }

    public byte[] getFacePosImg(String name_, String pos_) {
        Realm mRealm = Realm.getDefaultInstance();
        final String name = name_;
        final String pos = pos_;
        Log.i(TAG, "trying to get face pos img: " + name + " " + pos);
        FaceData faceData = mRealm.where(FaceData.class).equalTo("name", name).findFirst();
        FacePosData posData = faceData.getFacePosDatas().where().equalTo("pos", pos).findFirst();
        if (posData == null) {
            Log.i(TAG, "姓名：" + name + " 下没有该位置：" + pos);
            return null;
        }
        Log.i(TAG, "found pos: " + posData.getPos());
        Log.i(TAG, "found pos data: " + Arrays.toString(posData.getImg()));
        return posData.getImg();
    }

    public void deleteFaceData(String name_) {
        Realm mRealm = Realm.getDefaultInstance();
        final String name = name_;
        mRealm.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                FaceData faceData = realm.where(FaceData.class).equalTo("name", name).findFirst();
                faceData.getFacePosDatas().deleteAllFromRealm();
                faceData.deleteFromRealm();
            }
        });
    }

    // 默认输入是tmp，之前已经将tmp存入数据库
    public String findName(String tmp_) {
        final String tmp = tmp_;
        float[] left1 = decode(getFacePosImg(tmp, "Left"));
        float[] center1 = decode(getFacePosImg(tmp, "Center"));
        float[] right1 = decode(getFacePosImg(tmp, "Right"));
        if (left1 != null && center1 != null && right1 != null) {
            Log.i(TAG, "left length: " + left1.length);
            Log.i(TAG, "center length: " + center1.length);
            Log.i(TAG, "right length: " + right1.length);

            Realm mRealm = Realm.getDefaultInstance();
            RealmResults<FaceData> faceDatas = mRealm.where(FaceData.class).findAll();
            int size = faceDatas.size();
            Log.i(TAG, "faceDatas size: " + size);
            String name = "tmp";
            float max = -1.0f;
            for (int i = 0; i < size; i++) {
                FaceData faceData = faceDatas.get(i);
                String who = faceData.getName();
                if (who.equals("tmp")) {
                    continue;
                } else {
                    Log.i(TAG, "is? "+ who);
                    RealmList<FacePosData> facePosDatas = faceData.getFacePosDatas();
                    FacePosData leftData = facePosDatas.where().equalTo("pos", "Left").findFirst();
                    FacePosData centerData = facePosDatas.where().equalTo("pos", "Center").findFirst();
                    FacePosData rightData = facePosDatas.where().equalTo("pos", "Right").findFirst();
                    if (leftData==null || centerData==null || rightData == null)
                        continue;
                    else {
                        float[] left2 = decode(leftData.getImg());
                        float[] center2 = decode(centerData.getImg());
                        float[] right2 = decode(rightData.getImg());
                        float grade = similarity(left1, center1, right1, left2, center2, right2);
                        Log.i(TAG, "similarity: " + who + " : " + grade);
                        if (grade > max) {
                            max = grade;
                            name = who;
                        }
                    }
                }
            }
            if (max > 0.33)
                return name;
//            deleteFaceData("tmp");
        }

        return "Unknown";
    }

    public float similarity(float[] left1, float[] center1, float[] right1, float[] left2, float[] center2, float[] right2) {
        float s1 = similarity2(left1, left2);
        float s2 = similarity2(center1, center2);
        float s3 = similarity2(right1, right2);
        float grade = (s1 + s2 + s3) / 3;
        return grade;
    }

    public float similarity2(float[] v1, float[] v2) {
        if (v1.length != v2.length || v1.length < 1)
            return 0;
        double ret = 0.0, mod1 = 0.0, mod2 = 0.0;
        for (int i = 0; i < v1.length; i++) {
            ret += v1[i] * v2[i];
            mod1 += v1[i] * v1[i];
            mod2 += v2[i] * v2[i];
        }
        return (float) (ret / Math.sqrt(mod1) / Math.sqrt(mod2));
    }

    public void cleanRealm() {
        Realm mRealm = Realm.getDefaultInstance();
        mRealm.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                RealmResults<FaceData> faceDatas = realm.where(FaceData.class).findAll();
                RealmResults<FacePosData> facePosDatas = realm.where(FacePosData.class).findAll();
                faceDatas.deleteAllFromRealm();
                facePosDatas.deleteAllFromRealm();
            }
        });
    }

    public static float[] decode(byte byteArray[]) {
        if (byteArray == null)
            return null;
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
