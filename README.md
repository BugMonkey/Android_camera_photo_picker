# CameraPhotoPicker

#### 介绍
Android 调用相机拍照和图片选择


#### 安装教程

Add it in your root build.gradle at the end of repositories:

	allprojects {
		repositories {
			...
			maven { url 'https://jitpack.io' }
		}
	}


	dependencies {
	        implementation 'com.gitee.BugMonkey:camera-photo-picker:0.2'
	}

#### 使用说明

1.  页面调用
    `CameraViewActivity.start(context);`
2.  选择的照片
`onActivityResult{
        CameraViewActivity.obtainPathResult(data,context)
    }`
    
![输入图片说明](https://images.gitee.com/uploads/images/2022/0805/140622_be86b708_1005925.jpeg "Screenshot_20220805_135923_com.ztstech.android.znet.test.jpg")
![输入图片说明](https://images.gitee.com/uploads/images/2022/0805/140706_fc9e60a5_1005925.jpeg "Screenshot_20220805_135938_com.ztstech.android.znet.test.jpg")