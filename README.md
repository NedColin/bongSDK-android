
##无线开放 android SDK

###[首页 http://bong.cn/share](http://bong.cn/share)

###android sdk 1.2.0 版特性
- 独立的蓝牙能力：后台扫描、感知Yes！键触摸事件
- oauth授权，并获取该用户绑定设备信息
- 绑定设备同步：同步数据并上传云端
- 绑定设备通信：点亮、震动、获取传感器原始数据
- 切换环境、调试模式等
- 加入触摸Yes！键震动开关

触摸Yes!键大约1秒为短触后松开(亮灯一次)，3秒触摸为长触（亮灯两次），[手机]也会对应有短、长震动反馈。


###快速调试

- 1. 下载或clone SDK开发包，可运行Demo程序。
- 2. 运行后，首先点击“用户授权”，就取到了令牌以及绑定设备信息，即可调试demo。

###调测、账号注意事项
1. [测试环境]和[线上环境]的账号、数据完全独立，交叉登录则会报密码错误或未注册；开发者可到[开放平台](http://www.bong.cn/share/mobile.html)
下载测试环境的安装包，来自助注册测试账号并在测试环境使用、调测。
2. 默认最初分配的AppID、AppKey等信息仅对测试环境生效，意味着只能在测试环境授权通过并调测，否则可能报授权失败。
3. 测试完成将要上线时请到bong[开放平台] ，申请应用上线，通过后线上环境即生效。

###开发环境
1. 测试环境：账号、数据与正式环境隔离，测试专用，线上用户看不到。
2. 预发环境：预发环境（GM）和线上环境账号、数据体系一致，预发环境测试通过后将发布线上。
3. 正式环境：线上用户使用的环境。

###快速集成


- 1. 注册:  在bong开放平台[申请创建应用]，注册并获取AppID、AppKey、AppSecret等信息。
- 2. 集成： 将开发包里libs文件夹里jar包拷入你项目的libs文件夹并引入项目。
- 3. 注册： 将下面权限等信息注册到你项目的manifest文件。

permission
```xml
    <!-- bong sdk 必须的权限声明 over-->
    <permission android:name="cn.bong.android.permission.EVENT"
                android:label="bong event"
                android:protectionLevel="dangerous"/>
    <uses-permission android:name="cn.bong.android.permission.EVENT"/>
    <uses-permission android:name="android.permission.INTERNET"/>
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE"/>
    <uses-permission android:name="android.permission.VIBRATE"/>
    <uses-permission android:name="android.permission.BLUETOOTH"/>
    <uses-permission android:name="android.permission.BLUETOOTH_ADMIN"/>
    <!-- bong sdk  必须的权限声明 over-->
```
application
```xml
    <!-- bong sdk 必须的一些组件声明 -->
    <activity
            android:name="cn.bong.android.sdk.AuthActivity"
            android:launchMode="singleTask"
            android:screenOrientation="portrait"/>

    <receiver android:name="cn.bong.android.sdk.BongDataReceiver"
              android:permission="cn.bong.android.permission.EVENT">
        <intent-filter>
            <action android:name="cn.bong.android.action.event"/>
        </intent-filter>
    </receiver>
    <service android:name="cn.bong.android.sdk.BlueService"/>
    <!-- bong sdk 必须的一些组件声明 over-->
```
- 4. 使用简介：

####初始化
必须要到开放平台[申请]申请专属的获取AppID、AppKey、AppSecret等信息。
```java
        // 初始化sdk(接入api时需要AppID和AppSecret，且请注意AppSecret等信息的保密工作，防止被盗用)
        BongManager.initialize(this, "1419735044202", "", "558860f5ba4546ddb31eafeee11dc8f4");
        // 开启 调试模式，打印日志（默认关闭）
        BongManager.setDebuged(true);
        //设置 环境（默认线上）：Daily（测试）,  PreDeploy（预发，线上数据）, Product（线上）;
        BongManager.setEnvironment(Environment.Daily);
        // 设置触摸Yes键时震动
        BongManager.setTouchVibrate(true);
```

####反序列化，释放资源
```java
        // 会清空各种数据,包括授权信息，恢复到调用initialize方法前的状态。
        BongManager.unitialize();
```

####开启授权（注意一次授权有效期为3个月，所以注意不要频繁调用，仅当没有授权和授权过期时才调用此方法）
注意SDK初始化时输入你的AppID以及AppKey，测试环境注册一个测试账号，调用下面方法来完成授权。
```java
      // 开启 bong 触摸监听 实例 
      BongManager.bongAuth(this, "demo", new AuthUiListener() {
          @Override
          public void onError(AuthError error) {
          }

          @Override
          public void onSucess(AuthInfo result) {
          }

          @Override
          public void onCancel() {
          }
      });
```

####授权相关方法（授权的同时也获取了绑定设备信息，同步、Yes键监听时需要绑定设备才行）
```java
      // 清除AccessToken和UserID等信息并登出
      BongManager.bongLogout();
      // 判断是否有AccessToken和UserID
      BongManager.isSessionValid();
      // 获取AccessToken
      BongManager.getAccessToken();
      // 获取UserID
      BongManager.getLoginUid();
```

####同步数据
```java
        // 增量同步：最后一次同步到现在
        BongManager.bongDataSyncnizedByUpdate(listener);
        // 同步过去的24小时到现在
        BongManager.bongDataSyncnizedByHours(listener, System.currentTimeMillis(), 24);
        // 同步指定时间内数据
        BongManager.bongDataSyncnizedByTime(listener, startTime, endTime);
        
        DataSyncUiListener listener = new DataSyncUiListener() {
        
                @Override
                public void onStateChanged(DataSyncState state) {
                }
        
                @Override
                public void onError(DataSyncError error) {
                }
        
                @Override
                public void onSucess() {
                }
            };
```

####开启触摸监听
```java
        // 开启 bong 触摸监听 实例 
        BongManager.turnOnTouchEventListen(this, new TouchEventListener() {
            @Override
            public void onTouch(TouchEvent event) {
            }

            @Override
            public void onLongTouch(TouchEvent event) {
            }
        });
```

####关闭触摸监听
```java
        // 关闭 bong 触摸监听服务(与开启对应，注意在合适时机关闭)
        BongManager.turnOffTouchEventListen(context);
```

###注意：下面方法需要和bong通信，需要触摸Yes键来建立连接
####开启传感器示例 
```java
         BongManager.bongStartSensorOutput(sensorUiListener);
         
         ConnectUiListener sensorUiListener = new ConnectUiListener() {
         
             @Override
             public void onStateChanged(ConnectState state) {
             }
     
             @Override
             public void onFailed(String msg) {
             }
     
             @Override
             public void onSucess() {
             }
     
             @Override
             public void onDataReadInBackground(byte[] data) {
                // data是一个length为3的数组，data[0]为X轴数据，data[1]为Y轴数据，data[3]为Z轴数据。
             }
         };
```
####关闭传感器示例 
```java
        和开启传感器是一对，请注意在合适的时候注销监听防止内存泄露
        BongManager.bongStopSensorOutput(sensorUiListener);
```
####震动示例  
```java
        BongManager.bongVibrate(3, null);
```
####点亮示例  
```java
        BongManager.bongLight(3, null);
```

- 3. 其他参见 demo 项目。

