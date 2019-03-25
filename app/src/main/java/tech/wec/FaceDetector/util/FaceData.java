package tech.wec.FaceDetector.util;

import io.realm.RealmList;
import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

/**
 * Created by willi on 3/25/2019.
 */

public class FaceData extends RealmObject {
    @PrimaryKey
    private String id;
    public String name;
    public RealmList<FacePosData> facePosData;
}
