package tech.wec.FaceDetector.util;

import io.realm.RealmObject;

/**
 * Created by willi on 3/25/2019.
 */

// 一个人对应三个不同位置的128D脸部信息
public class FacePosData extends RealmObject {
    public String pos;
    public byte[] img;
}
