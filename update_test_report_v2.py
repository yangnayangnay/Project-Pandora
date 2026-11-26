#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""更新测试报告 - 完整版"""

from docx import Document
import re

# 打开文档
doc = Document(r'D:\AndroidStudio\Project_Pandora\ofTests\测试报告.docx')

# 新的测试数据
NEW_GIT_HASH = "ae19d32"
OLD_GIT_HASH = "567b727"
TEST_DATE = "2026 年 11 月 26 日"
OLD_TEST_DATE = "2026 年 11 月 19 日"
WEEK_NUM = "第 13 周"
OLD_WEEK_NUM = "第 10 周"

# 后端测试数据 - 42 个测试类，465 条用例
BACKEND_TESTS = 42
BACKEND_CASES = 465
BACKEND_TIME = "38 秒"

# Android 测试数据
ANDROID_TESTS = 9
ANDROID_CASES = 77

# 总计
TOTAL_TESTS = BACKEND_TESTS + ANDROID_TESTS  # 51
TOTAL_CASES = BACKEND_CASES + ANDROID_CASES  # 542

def update_text(text):
    """执行所有文本替换"""
    # 替换 Git 哈希（保留历史记录中的旧哈希）
    if OLD_GIT_HASH in text:
        # 检查是否是历史记录上下文
        if "历史" not in text and "首轮" not in text:
            text = text.replace(OLD_GIT_HASH, NEW_GIT_HASH)
    
    # 替换日期
    if OLD_TEST_DATE in text:
        text = text.replace(OLD_TEST_DATE, TEST_DATE)
    
    # 替换周次
    if OLD_WEEK_NUM in text:
        text = text.replace(OLD_WEEK_NUM, WEEK_NUM)
    
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
        text = text.replace("后端 150 条 + Android 77 条", f"后端 {BACKEND_CASES} 条 + Android {ANDROID_CASES} 条")
    
    if "17 类 227 条" in text:
        text = text.replace("17 类 227 条", f"{TOTAL_TESTS} 类 {TOTAL_CASES} 条")
    
    # 替换时间
    if "97 秒" in text:
        text = text.replace("97 秒", BACKEND_TIME.replace("秒", ""))
    
    return text

# 遍历所有段落进行替换
for para in doc.paragraphs:
    new_text = update_text(para.text)
    if new_text != para.text:
        # 清空所有 run，然后设置新文本
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

# 遍历所有页眉页脚
for section in doc.sections:
    for header in section.header.paragraphs:
        new_text = update_text(header.text)
        if new_text != header.text:
            for run in header.runs:
                run.text = ""
            if header.runs:
                header.runs[0].text = new_text

# 保存文档
output_path = r'D:\AndroidStudio\Project_Pandora\ofTests\测试报告.docx'
doc.save(output_path)
print(f"测试报告已更新: {output_path}")
print(f"Git 哈希: {OLD_GIT_HASH} -> {NEW_GIT_HASH}")
print(f"测试数据: 17类227条 -> {TOTAL_TESTS}类{TOTAL_CASES}条")