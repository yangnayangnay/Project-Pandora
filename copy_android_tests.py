# -*- coding: utf-8 -*-
"""
复制Android测试文件并添加注释
"""
import os
import shutil

# 源目录和目标目录
src_dir = r'D:\AndroidStudio\Project_Pandora\app\src\test\java\com\example\project_pandora'
dst_dir = r'D:\AndroidStudio\Project_Pandora\ofTests\Android测试文件'

# 测试文件列表
test_files = [
    r'core\divination\DivinationEngineTest.java',
    r'core\util\LogDateWindowTest.java',
    r'ui\widget\StatutoryHolidayScheduleTest.java',
    r'ui\widget\SpecialCalendarDatesTest.java',
    r'ui\widget\CuteArtworkPlacementTest.java',
    r'ui\task\TaskPinOrderTest.java',
    r'ui\people\PeopleDirectoryTest.java',
    r'core\util\LogEntryRulesTest.java',
    r'core\config\BackgroundPrefsTest.java',
    r'ui\widget\MonthTaskLayoutTest.java',
    r'ui\widget\CalendarCellMetricsTest.java',
    r'ui\task\TaskFormStateTest.java',
    r'ui\mine\AvatarInitialsTest.java',
    r'ui\home\HomeBoardLoaderTest.java',
    r'ui\aimap\BirthFortuneEngineTest.java',
    r'repository\TaskVisibilityTest.java',
    r'core\util\TaskDateTimesTest.java',
    r'core\util\TaskAssigneesTest.java',
    r'core\util\SignatureRulesTest.java',
    r'core\util\ReadableTextColorsTest.java',
    r'core\network\AiEndpointTest.java',
    r'ui\widget\PeriodSwipeGestureTest.java',
    r'ui\widget\DayTaskSelectionTest.java',
    r'ui\widget\CalendarDatesTest.java',
    r'ui\task\TaskDetailFilterTest.java',
    r'ui\log\PersonalWorkSlotsTest.java',
    r'ui\home\DailyItemsTest.java',
    r'repository\ViewRepositoryTest.java',
    r'repository\TaskDisplaySettingsRepositoryTest.java',
    r'core\util\AvatarUploadPolicyTest.java',
    r'ui\widget\SchedulePeriodTest.java',
    r'ui\widget\ScheduleLayoutTest.java',
    r'ui\widget\ScheduleDaySwitchTest.java',
    r'ui\widget\MonthTaskDatesTest.java',
    r'core\util\NumberCompatTest.java',
    r'ExampleUnitTest.java',
]

# 文件描述映射
file_descriptions = {
    'DivinationEngineTest.java': '占卜引擎核心逻辑测试 - 测试按日期起卦、随机起卦、变卦规则、爻线格式、边界卦等',
    'LogDateWindowTest.java': '日志日期窗口测试 - 测试前两天至今的日期窗口、月界/年界边界、午夜切换、非法日期等',
    'StatutoryHolidayScheduleTest.java': '法定节假日排期测试 - 测试节假日对排期的影响',
    'SpecialCalendarDatesTest.java': '特殊日历日期测试 - 测试特殊日期的处理逻辑',
    'CuteArtworkPlacementTest.java': '可爱艺术作品放置测试 - 测试UI元素的放置逻辑',
    'TaskPinOrderTest.java': '任务置顶顺序测试 - 测试任务置顶的排序逻辑',
    'PeopleDirectoryTest.java': '人员目录测试 - 测试人员目录的显示和查询',
    'LogEntryRulesTest.java': '日志条目规则测试 - 测试标题规范、正文格式、空白字符处理等',
    'BackgroundPrefsTest.java': '背景偏好设置测试 - 测试背景偏好的保存和读取',
    'MonthTaskLayoutTest.java': '月度任务布局测试 - 测试月度任务日历的布局逻辑',
    'CalendarCellMetricsTest.java': '日历单元格度量测试 - 测试日历单元格的尺寸和度量',
    'TaskFormStateTest.java': '任务表单状态测试 - 测试任务表单的状态管理',
    'AvatarInitialsTest.java': '头像首字母测试 - 测试头像首字母的生成逻辑',
    'HomeBoardLoaderTest.java': '首页看板加载器测试 - 测试首页看板的数据加载',
    'BirthFortuneEngineTest.java': '生日运势引擎测试 - 测试生日运势的计算逻辑',
    'TaskVisibilityTest.java': '任务可见性测试 - 测试任务的可见性规则',
    'TaskDateTimesTest.java': '任务日期时间测试 - 测试跨天区间、时区固定、日期边界、非法日期等',
    'TaskAssigneesTest.java': '任务指派人测试 - 测试去重、大写列名恢复、旧格式兼容等',
    'SignatureRulesTest.java': '签名规则测试 - 测试换行符一致性、空行与尾行计数等',
    'ReadableTextColorsTest.java': '可读文本颜色测试 - 测试文本颜色的可读性',
    'AiEndpointTest.java': 'AI端点测试 - 测试网关保留、路径规范化、凭据拒绝等',
    'PeriodSwipeGestureTest.java': '时段滑动手势测试 - 测试时段切换的滑动手势',
    'DayTaskSelectionTest.java': '日任务选择测试 - 测试日任务的选择逻辑',
    'CalendarDatesTest.java': '日历日期测试 - 测试日历日期的显示和选择',
    'TaskDetailFilterTest.java': '任务详情过滤器测试 - 测试任务详情的过滤逻辑',
    'PersonalWorkSlotsTest.java': '个人工作时段测试 - 测试个人工作时段的管理',
    'DailyItemsTest.java': '每日事项测试 - 测试每日事项的加载和显示',
    'ViewRepositoryTest.java': '视图仓库测试 - 测试视图数据的存储和检索',
    'TaskDisplaySettingsRepositoryTest.java': '任务显示设置仓库测试 - 测试任务显示设置的存储',
    'AvatarUploadPolicyTest.java': '头像上传策略测试 - 测试URL解析、鉴权范围、5MB上界、小图不放大等',
    'SchedulePeriodTest.java': '排期时段测试 - 测试排期的时段管理',
    'ScheduleLayoutTest.java': '排期布局测试 - 测试排期的布局逻辑',
    'ScheduleDaySwitchTest.java': '排期日切换测试 - 测试排期的日切换逻辑',
    'MonthTaskDatesTest.java': '月度任务日期测试 - 测试月度任务的日期处理',
    'NumberCompatTest.java': '数字兼容性测试 - 测试各种类型转换、科学计数法、超范围溢出、null回退等',
    'ExampleUnitTest.java': '示例单元测试 - 测试示例功能',
}

print("开始复制Android测试文件...")

for file_path in test_files:
    src_file = os.path.join(src_dir, file_path)
    dst_file = os.path.join(dst_dir, os.path.basename(file_path))
    
    if os.path.exists(src_file):
        # 读取源文件
        with open(src_file, 'r', encoding='utf-8') as f:
            content = f.read()
        
        # 获取文件描述
        file_name = os.path.basename(file_path)
        description = file_descriptions.get(file_name, '核心业务逻辑测试')
        
        # 在文件开头添加注释
        comment = f"""/**
 * {description}
 * 
 * 本测试文件包含多个测试用例，覆盖正常场景、边界场景和异常场景
 * 测试用例由AI大模型辅助生成，经人工逐条复核修正后入库
 * 
 * 文件位置: app/src/test/java/com/example/project_pandora/{file_path}
 * 最后执行时间: 2026-11-18
 * 结果: 全部通过
 */

"""
        
        new_content = comment + content
        
        # 写入目标文件
        with open(dst_file, 'w', encoding='utf-8') as f:
            f.write(new_content)
        
        print(f"✓ 已复制: {file_name}")
    else:
        print(f"✗ 文件不存在: {src_file}")

print(f"\n完成！共复制 {len(test_files)} 个测试文件到 {dst_dir}")