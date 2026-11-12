# 个人签名最多四行（2026-11-10）

## 实际修改

- 保留已有最多 20 字规则，新增最多四行规则；手动换行、空行、末尾空行都计入行数。
- Android 输入过滤按编辑框实际宽度、字号和行距计算自动折行，拒绝会产生第五行的输入；粘贴和选区替换同样检查，拒绝替换时保留原选中文字。
- 编辑框高度继续最多四行，提示“最多 20 字、4 行（含自动换行）”。保存前再次检查，窗口宽度变化后重新校验。
- 通讯录人员详情签名展示最多四行，旧内容超出时省略，避免撑高页面。
- Web 个人中心同步限制手动换行和按实际输入框宽度产生的折行；输入框最多四行，静态签名展示最多四行。
- Android、Web 和后端将 CRLF、CR、Unicode 换行符统一为 LF；后端在去除首尾空白之前检查换行数，避免通过末尾空行绕过校验。
- 旧签名不做数据库截断；编辑旧的不合规签名时允许删除修正，但必须符合当前限制才能再次保存。

## 主要文件

- `app/src/main/java/com/example/project_pandora/core/util/SignatureRules.java`：新增换行规范化与行数规则。
- `app/src/main/java/com/example/project_pandora/ui/mine/SignatureLineFilter.java`：新增按实际宽度测量的输入过滤。
- `app/src/main/java/com/example/project_pandora/ui/mine/ProfileActivity.java`：输入、布局变化和保存校验。
- `app/src/main/res/layout/activity_profile.xml`、`activity_company_person.xml`：提示与最大展示高度。
- `pandora-web/src/utils/signature.js`、`pandora-web/src/views/user/ProfileView.vue`：Web 输入、保存和展示限制。
- `pandora-backend/src/main/java/com/pandora/backend/user/UserController.java`：后端四行校验。
- 新增 `SignatureRulesTest.java`、`SignatureLineFilterTest.java`，补充 `UserAvatarAndRelationsTest.java`。

## 数据库、API 与旧逻辑

- 数据库结构不变，不批量修改或截断已保存签名。
- 现有 `PUT /api/user/me` 增加签名最多四个显式行的校验，参数结构不变；后端无法知道各设备的实际字号和可用宽度，自动折行由客户端测量，展示端独立限制四行高度。
- 替换了只靠 `maxLines=4` 隐藏额外内容、实际上仍能输入第五行的旧行为。

## 已完成验证

- Android Java/资源与仪器测试源码编译通过；JVM 测试 212 项通过，0 失败、0 错误，其中本轮新增两项换行规则测试。
- 新增仪器测试源码覆盖回车产生第五行、选区替换保留原文、删除后重新补到四行、窄框自动折行以及粘贴换行规范化；仅编译，未在设备执行。
- 后端 `UserAvatarAndRelationsTest` 共 17 项通过，包括新补充的四行保存、CRLF/Unicode 换行、末尾空行和拒绝失败时不写入其他资料字段。
- Web 构建通过；独立执行签名文本规则断言通过。构建有既有的大分包体积提示，没有构建错误。
- 两个修改的 Android XML 解析通过，`git diff --check` 通过。

## 限制与资料

- 未打包、未安装 APK，旧 APK 不包含本轮修正。
- 未部署后端或网页；服务器新增校验需要后续部署才会在当前线上接口生效。
- 未运行设备界面或浏览器排版验收，自动折行与输入法交互需在改后版本上进一步验收。
- 没有因缺少资料而搁置的本轮实现。
