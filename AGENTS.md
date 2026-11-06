# Android 验证与交付约束

- 用户最新要求：所有任务由 Codex 直接完成，不再委派给 DeepSeek 或其他代理。

- 除非用户明确要求打包 APK，否则禁止生成、重新打包、复制交付或安装 APK；应用 APK 和仪器测试 APK 均在此限制内。
- 验证可以运行 Java/资源编译、仪器测试源码编译和 JVM 单元测试。执行前检查 Gradle 任务图，确保不会触发 APK 生成。
- 不运行 assemble、APK package、install、connectedAndroidTest、Android Studio Run 等会生成或安装 APK 的任务。
- 明确区分源码编译、JVM 测试与设备界面验收；未运行的设备测试不能报告为通过。
