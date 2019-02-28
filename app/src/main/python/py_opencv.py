import cv2
import numpy as np

def cv2Test():
    print("opencv-python's version: "+cv2.__version__)

    #some image preprocess stuff
def prewhiten(xx):
    x = xx.copy()
    mean = np.mean(x)
    print('mean: ' + mean)
    std = np.std(x)
    print('std:' + std)
    '''
    adj(A),表示A的伴随矩阵
    adj(A)=det(A)×A^(-1)
    '''
    std_adj = np.maximum(std, 1.0 / np.sqrt(x.size))
    print ('std_adj: '+ std_adj)
    y = np.multiply(np.subtract(x, mean), 1 / std_adj)
    print ('y: '+y)
    return y