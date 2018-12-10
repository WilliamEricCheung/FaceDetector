import imp  #test load path
import requests
from bs4 import BeautifulSoup

def log(content):
    JavaClass.d("formPython",content)

# 存在连接超时的问题，但确实将bs4库引用进来了
def testGet():
    log('Hello,World from python')
    # r = requests.get("https://www.baidu.com/")
    r = requests.get("http://www.sina.com.cn/china")
    r.encoding ='utf-8'
    print(r.text)
    bsObj = BeautifulSoup(r.text,"html.parser")
    for node in bsObj.findAll("a"):
        log("---**--- "+node.text)

testGet()