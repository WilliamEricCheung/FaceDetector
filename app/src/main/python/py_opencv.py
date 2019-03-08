import cv2
import numpy as np
import os
from org.opencv.core import Mat
from org.opencv.core import CvType
from java import cast, jarray, jboolean, jbyte, jchar, jclass, jint, jfloat

class OpenCVTest(object):
    def cv2Test(self):
        print("opencv-python's version: "+cv2.__version__)

        #some image preprocess stuff
    def prewhiten(self, input):
        xx = np.array(input)
        x = xx.astype(np.uint8)

        yy = jarray(jbyte)(x)
        yyy = Mat(160, 160, CvType.CV_8UC3)
        yyy.put(0,0,yy)
        # print (yy)
        print (yyy)
        return yyy