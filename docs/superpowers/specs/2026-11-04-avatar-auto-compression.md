# 头像原图不限大小与自动压缩

用户要求：原始头像图片不按文件大小拒绝；超过5MB时自动压缩至5MB以内。全部由 Codex 直接完成，不委派，不生成或安装APK，不上传服务器。

## 实现

- 相册和拍照继续共用 AvatarHelper.prepareForUpload，没有原图大小门槛，也不把原文件整体读入内存。先读取尺寸并采样解码，使处理像素量受控，保留EXIF方向/镜像校正及1600px最大边长。
- 既有流程只尝试几档JPEG质量，失败后提示“图片过大”。改为按实际输出字节数校验：先降低JPEG质量，仍超限则继续等比例缩小像素尺寸后重试，最终产物必须满足0 < 字节数 <= 5 * 1024 * 1024。
- 压缩控制器和Android Bitmap适配分离：控制器可用JVM真实JPEG编码验证尺寸与字节预算，Bitmap适配负责写文件、缩放和释放内存。失败或取消沿用现有临时文件清理，不上传半成品，不修改用户原图。内存不足转换为可重试的图片处理错误。
- 页面说明改为“原图不限大小，超过5MB自动压缩”。现有后台5MB文件限制与6MB请求限制用于校验已处理的上传文件，原图在本地处理，不改生产配置或接口。

## 验证范围

- JVM测试使用真实JPEG编码，验证5MB产物、小于原分辨率的质量压缩、质量不足时继续缩小分辨率、错误和空文件清理、无法满足极小预算时正确终止。
- 仪器测试增加一张实际超过5MB的噪点PNG，验证实际Android处理路径输出有效JPEG且不超过5MB，并保留原文件。保留已有方向校正测试；只编译仪器测试源码，未运行设备测试。
- 运行Gradle前检查任务图，禁止APK相关打包/安装/设备测试任务；对照现有APK元数据，并复核资源XML及代码差异。

## 完成记录

- 应用Java/资源和仪器测试源码编译通过；现有156项Android JVM回归测试全部通过。独立JVM的6项真实JPEG测试全部通过，覆盖画质调整、像素缩小、字节预算及失败清理。
- 首次测试编译发现Android的编译API不包含java.awt/javax.imageio。将真实JPEG测试移至 `tools/verification/avatar-compression/`，使用现有JDK与缓存的JUnit/Hamcrest独立编译运行；没有增加App运行依赖，没有以模拟结果冒充Android Bitmap设备验收。
- 新增的实际超过5MB原图测试及已有EXIF校正测试已编译，未运行设备测试。原图尺寸采样和内存释放路径已复核，具体设备格式支持与内存行为仍需真机验证。
- 128个资源XML解析通过，`git diff --check`通过。现有3个APK的路径、大小和UTC修改时间前后一致，变化数0；未部署服务器。
- 验证脚本 `.build-temp/verify-avatar-compression.ps1`；日志为 `.build-temp/avatar-compression-android-plan.log`、`.build-temp/avatar-compression-android.log`、`.build-temp/avatar-compression-jpeg-tests.log`；首次测试编译问题留存于 `.build-temp/avatar-compression-android-first.log`。
