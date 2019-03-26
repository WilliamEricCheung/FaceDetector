package tech.wec.FaceDetector.util;

import io.realm.RealmList;
import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

/**
 * Created by willi on 3/25/2019.
 */

public class FaceData extends RealmObject {
    @PrimaryKey
    private String name;
    private RealmList<FacePosData> facePosDatas;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public RealmList<FacePosData> getFacePosDatas() {
        return facePosDatas;
    }

    public void setFacePosDatas(RealmList<FacePosData> facePosDatas) {
        this.facePosDatas = facePosDatas;
    }

}
