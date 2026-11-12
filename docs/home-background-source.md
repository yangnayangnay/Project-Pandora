# 首页背景照片

- 用途：Android 首页低透明度装饰背景，卡片仅对背景进行磨砂模糊。
- 素材：通用办公楼建筑照片，并非项目所属公司的实景。
- 来源：Unsplash 公共办公楼照片（下方原图 CDN 链接）。
- 原图 CDN：https://images.unsplash.com/photo-1486406146926-c627a92ad1ab
- 许可说明：https://unsplash.com/license
- 本地文件：app/src/main/res/drawable/home_office_building.jpg
- 获取日期：2026-11-09

图片随应用本地加载；首页不依赖外部图片网络服务。

## 公司系列页面背景（2026-11-09）

主导航页面延用首页低透明度照片的处理方式，使用以下不同场景。均为通用素材，不代表公司的真实办公室或员工资料。

| 页面 | 场景 | 本地文件（app/src/main/res/ 下） | Unsplash 原图 |
| --- | --- | --- | --- |
| 首页、首页看板详情 | 办公楼 | drawable/home_office_building.jpg | https://images.unsplash.com/photo-1486406146926-c627a92ad1ab |
| 公司（月/周/日视图） | 办公室内 | drawable-nodpi/company_interior.jpg | https://images.unsplash.com/photo-1497366754035-f200968a6e72 |
| 日志 | 工位 | drawable-nodpi/company_workstation.jpg | https://images.unsplash.com/photo-1499951360447-b19be8fe80f5 |
| AI 地图 | 天空 | drawable-nodpi/company_sky.jpg | https://images.unsplash.com/photo-1501630834273-4b5604d2ee31 |
| 我的、设置 | 办公休息区 | drawable-nodpi/company_lounge.jpg | https://images.unsplash.com/photo-1497366811353-6870744d04b2 |

新增图片使用 `?fit=crop&w=1100&q=78&fm=jpg` 下载，本地打包，许可说明同上：https://unsplash.com/license 。

“我的 → 设置 → 页面背景”可选择纯色或公司系列背景图，默认公司系列。选择只写入独立的设备外观偏好，不上传服务器，不影响账号、同步或登录设置。页面重新附着时重读偏好，已显示的页面通过偏好监听即时更新。纯色模式只绘制主题底色，首页玻璃卡片继续保留原有色调和描边。

照片异步解码并按资源缓存；首页磨砂卡片继续采样同一背景。日间和夜间分别使用 56/255、36/255 的照片透明度，底部仍保留轻微底色遮罩。文字通过主题颜色及内容卡片保持可读。

2026-11-09 最新调整：按用户要求恢复原始照片，取消暖阳后处理。照片以原透明度覆盖完整视口，底部只保留轻微底色遮罩，不再渐变到完全纯色；磨砂卡片仍采样同一背景，卡片收起和滚动隐藏修复保留。
