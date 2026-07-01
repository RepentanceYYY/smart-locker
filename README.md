# 智能格口柜(V1.0.0)

#### 硬件通信相关:

当前是一种类型的设备一个Manager，每个Manager下存放key为通信地址，value为设备类的Map，所以，同个通信链路上不能出现两种不用的设备，但是可以同种设备存在于不同的通信链路上。



#### 百度人脸离线SDK集成使用步骤：

##### 1.安装open-cv到本地maven仓库命令：

mvn org.apache.maven.plugins:maven-install-plugin:3.1.4:install-file "-Dfile=C:\Users\Alice\Desktop\opencv-320.jar" "-DgroupId=org.opencv" "-DartifactId=opencv-320" "-Dversion=3.2.0" "-Dpackaging=jar"

###### C:\Users\Alice\Desktop\opencv-320.jar为完整jar包路径

##### 2.然后将dll放到jdk的bin目录下。



