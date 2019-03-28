package tech.wec.FaceDetector.util;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

/**
 * Created by willi on 3/25/2019.
 */

// 一个人对应三个不同位置的128D脸部信息
public class FacePosData extends RealmObject {
    private String pos;
    private byte[] img;

    public String getPos() {
        return pos;
    }

    public void setPos(String pos) {
        this.pos = pos;
    }

    public byte[] getImg() {
        return img;
    }

    public void setImg(byte[] img) {
        this.img = img;
    }

}
