# 护眼休息提醒

这是一个 Kotlin 原生 Android APK 项目，用于在华为手机上本地提醒老人减少长时间连续看手机。

## 功能

- 引导开启 Android“使用情况访问权限”。
- 每 15 分钟通过 WorkManager 检查一次近期使用情况。
- 手机重启后自动重新安排后台提醒任务。
- 当检测到连续使用约 30 分钟时，发送系统通知。
- 可选 TextToSpeech 语音朗读提醒。
- 家人设置页使用 PIN 保护，默认 PIN 为 `1234`。
- 所有数据只保存在手机本地，不上传服务器。

## 构建

推荐使用 Android Studio 打开本目录，等待 Gradle 同步完成后运行：

```powershell
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleDebug
```

如果首次运行下载 Gradle 或 Android Gradle Plugin 较慢，请保持网络连接后重试。

当前项目位于中文路径时，Android Gradle Plugin 在 Windows 上可以通过 `android.overridePathCheck=true` 构建 APK，但 `testDebugUnitTest` 的 JUnit worker 可能加载不到测试类。遇到这种情况，把项目复制到纯英文路径后运行单元测试，例如：

```powershell
robocopy D:\文档\提醒 D:\elder_reminder_ascii /E /XD .git .gradle .idea app\build /XF local.properties
Set-Content D:\elder_reminder_ascii\local.properties 'sdk.dir=D\:\\Android\\Sdk'
cd D:\elder_reminder_ascii
.\gradlew.bat testDebugUnitTest
```

## 真机使用

1. 安装 APK 到华为手机。
2. 首次打开应用，点击“开启使用情况权限”。
3. 在系统页面中允许“护眼休息提醒”读取使用情况。
4. 回到应用，点击“开始提醒”。
5. 点击“家人设置”，输入默认 PIN `1234`，可修改提醒间隔、语音开关、提醒文案和 PIN。

## 注意

- Android 不允许普通应用自动开启“使用情况访问权限”，只能跳转系统页面由用户手动授权。
- WorkManager 的周期任务最小间隔为 15 分钟，因此提醒时间会接近阈值，不保证秒级精确。
- 华为手机可能会限制后台任务，建议在系统电池/应用启动管理里允许本应用后台运行。
- 本应用只做生活习惯提醒，不提供医学诊断或治疗建议。
