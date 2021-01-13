Function Implemented:
- dir
- get
- upload
- exit
- detect wrong username and Password
- detect file not exist

Username and Password:
aaa 111
bbb 222
ccc 333
ddd 444
eee 555

Steps:
    javac Server.java
    java Server

    javac Client.java
    java Server

    ftpclient 127.0.0.1 8000
    qweqweqwe (detect wrong username and Password)
    aaa 111
    dir
    get qwejqweqwe (detect file not exist)
    get a.pdf
    get b.pptx
    dir
    upload qweqweqweqwe (detect file not exist)
    upload c.txt
    dir
    exit



Version:
java version "1.8.0_221"
Java(TM) SE Runtime Environment (build 1.8.0_221-b11)
Java HotSpot(TM) 64-Bit Server VM (build 25.221-b11, mixed mode)