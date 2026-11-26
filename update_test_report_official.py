#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""更新测试报告 - 官方口径修正版"""

from docx import Document
import re

# 打开文档
doc = Document(r'D:\AndroidStudio\Project_Pandora\ofTests\测试报告.docx')

# ===== 官方事实（来自 run_all_tests.bat 输出） =====
NEW_GIT_HASH = "3e608a6"
OLD_GIT_HASH = "567b727"
TEST_DATE = "2026 年 11 月 26 日"
OLD_TEST_DATE = "2026 年 11 月 19 日"
WEEK_NUM = "第 11 周"  # 11/19=第10周 -> 11/26=第11周

# 后端测试数据 - 16 类 277 条
BACKEND_TESTS = 16
BACKEND_CASES = 277

# Android 测试数据 - 沿用旧值，标注"沿用待核"
ANDROID_TESTS = 9
ANDROID_CASES = 77

# 总计
TOTAL_TESTS = BACKEND_TESTS + ANDROID_TESTS  # 25
TOTAL_CASES = BACKEND_CASES + ANDROID_CASES  # 354

# 覆盖率（来自 JaCoCo 报告）
INSTRUCTION_COV = "26%"
BRANCH_COV = "23%"

# 总耗时
TOTAL_TIME = "144 秒"

def update_text(text):
    """执行所有文本替换"""
    # 替换 Git 哈希（保留历史记录中的旧哈希）
    if OLD_GIT_HASH in text:
        if "历史" not in text and "首轮" not in text:
            text = text.replace(OLD_GIT_HASH, NEW_GIT_HASH)
    
    # 替换日期
    if OLD_TEST_DATE in text:
        text = text.replace(OLD_TEST_DATE, TEST_DATE)
    
    # 替换周次
    if "第 10 周" in text:
        text = text.replace("第 10 周", WEEK_NUM)
    
    # 替换测试数据 - 总计
    if "17 个测试类，227 条用例" in text:
        text = text.replace("17 个测试类，227 条用例", f"{TOTAL_TESTS} 个测试类，{TOTAL_CASES} 条用例")
    
    # 替换后端数据
    if "后端(150 条" in text:
        text = text.replace("后端(150 条", f"后端({BACKEND_CASES} 条")
    
    # 替换套件总计
    if "17 个测试类、227 条用例" in text:
        text = text.replace("17 个测试类、227 条用例", f"{TOTAL_TESTS} 个测试类、{TOTAL_CASES} 条用例")
    
    # 替换分层套件
    if "17/17 通过" in text:
        text = text.replace("17/17 通过", f"{BACKEND_TESTS}/{BACKEND_TESTS} 通过")
    
    # 替换总计行
    if "后端 150 条 + Android 77 条" in text:
        text = text.replace("后端 150 条 + Android 77 条", f"后端 {BACKEND_CASES} 条 + Android {ANDROID_CASES} 条（沿用待核）")
    
    if "17 类 227 条" in text:
        text = text.replace("17 类 227 条", f"{TOTAL_TESTS} 类 {TOTAL_CASES} 条")
    
    # 替换时间
    if "97 秒" in text:
        text = text.replace("97 秒", TOTAL_TIME)
    
    # 替换覆盖率
    if "79%" in text and "指令" in text:
        text = text.replace("79%", INSTRUCTION_COV)
    if "75%" in text and "分支" in text:
        text = text.replace("75%", BRANCH_COV)
    
    # 替换 A/B 层描述
    if "A 层(核心业务，6 类)" in text:
        text = text.replace("A 层(核心业务，6 类)", "A 层(核心业务，6 类)")
    if "B 层(业务规则，2 类)" in text:
        text = text.replace("B 层(业务规则，2 类)", "B 层(业务规则，2 类)")
    
    # 替换方法论描述
    if "8 个后端类" in text:
        text = text.replace("8 个后端类", f"{BACKEND_TESTS} 个后端类")
    
    return text

# 遍历所有段落进行替换
for para in doc.paragraphs:
    new_text = update_text(para.text)
    if new_text != para.text:
        for run in para.runs:
            run.text = ""
        if para.runs:
            para.runs[0].text = new_text

# 遍历所有表格进行替换
for table in doc.tables:
    for row in table.rows:
        for cell in row.cells:
            for para in cell.paragraphs:
                new_text = update_text(para.text)
                if new_text != para.text:
                    for run in para.runs:
                        run.text = ""
                    if para.runs:
                        para.runs[0].text = new_text

# 保存文档
doc.save(r'D:\AndroidStudio\Project_Pandora\ofTests\测试报告.docx')
print(f"测试报告已更新")
print(f"Git 哈希: {OLD_GIT_HASH} -> {NEW_GIT_HASH}")
print(f"测试数据: 17类227条 -> {TOTAL_TESTS}类{TOTAL_CASES}条")
print(f"后端: {BACKEND_TESTS}类{BACKEND_CASES}条")
print(f"覆盖率: 指令{INSTRUCTION_COV}, 分支{BRANCH_COV}")