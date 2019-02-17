import numpy as np
# import java.io.PrintStream as p
from android.util import Log
def numpyTest():
    arr1 = np.random.randint(0,10,size=(3,4))
    print(arr1*arr1) # 相对应元素相乘
    print(arr1+arr1) # 对应元素相加
    print(arr1-arr1) # 对应元素相减
    #
    # p.print(arr1*arr1)
    # p.print(arr1+arr1)
    # p.print(arr1-arr1)