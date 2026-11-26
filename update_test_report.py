#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""更新测试报告"""

from docx import Document
from docx.shared import Pt, Inches
from docx.enum.text import WD_ALIGN_PARAGRAPH
import re
from datetime import datetime

# 打开文档
doc = Document(r'D:\AndroidStudio\Project_Pandora\ofTests\测试报告.docx')

# 新的测试数据
NEW_GIT_HASH = "ae19d32"
OLD_GIT_HASH = "567b727"
TEST_DATE = "2026 年 11 月 26 日"
OLD_TEST_DATE = "2026 年 11 月 19 日"
WEEK_NUM = "第 13 周"  # 从 11/19 到 11/26 是第 13 周
OLD_WEEK_NUM = "第 10 周"

# 后端测试数据
BACKEND_TESTS = 42  # 测试类数
BACKEND_CASES = 465  # 用例数
BACKEND_TIME = "38 秒"

# Android 测试数据（从原始报告保持）
ANDROID_TESTS = 9
ANDROID_CASES = 77

# 总计
TOTAL_TESTS = BACKEND_TESTS + ANDROID_TESTS  # 51
TOTAL_CASES = BACKEND_CASES + ANDROID_CASES  # 542

# 覆盖率数据
INSTRUCTION_COV = "50%"
BRANCH_COV = "44%"

# 遍历所有段落进行替换
for para in doc.paragraphs:
    text = para.text
    
    # 替换 Git 哈希
    if OLD_GIT_HASH in text and "当前版本" not in text and "历史" not in text:
        text = text.replace(OLD_GIT_HASH, NEW_GIT_HASH)
    
    # 替换日期
    if OLD_TEST_DATE in text:
        text = text.replace(OLD_TEST_DATE, TEST_DATE)
    
    # 替换周次
    if OLD_WEEK_NUM in text and "验收周" in text:
        text = text.replace(OLD_WEEK_NUM, WEEK_NUM)
    
    # 替换测试数据
    if "17 个测试类，227 条用例" in text:
        text = text.replace("17 个测试类，227 条用例", f"{TOTAL_TESTS} 个测试类，{TOTAL_CASES} 条用例")
    
    if "后端 150 条 + Android 77 条" in text:
        text = text.replace("后端 150 条 + Android 77 条", f"后端 {BACKEND_CASES} 条 + Android {ANDROID_CASES} 条")
    
    if "17/17 全部通过" in text:
        text = text.replace("17/17 全部通过", f"{BACKEND_TESTS}/{BACKEND_TESTS} 全部通过")
    
    if "总耗时约 97 秒" in text:
        text = text.replace("总耗时约 97 秒", f"总耗时约 {BACKEND_TIME}")
    
    # 更新段落文本
    if text != para.text:
        for run in para.runs:
            run.text = ""
        if para.runs:
            para.runs[0].text = text

# 遍历表格进行更新
for table in doc.tables:
    for row in table.rows:
        for cell in row.cells:
            text = cell.text
            
            # 替换 Git 哈希
            if OLD_GIT_HASH in text:
                text = text.replace(OLD_GIT_HASH, NEW_GIT_HASH)
            
            # 替换测试数据
            if "17" in text and "测试类" in text:
                text = text.replace("17", str(TOTAL_TESTS))
            
            if "227" in text and "条用例" in text:
                text = text.replace("227", str(TOTAL_CASES))
            
            if "150" in text and "后端" in text:
                text = text.replace("150", str(BACKEND_CASES))
            
            if "97 秒" in text:
                text = text.replace("97 秒", BACKEND_TIME.replace("秒", ""))
            
            # 更新单元格文本
            if text != cell.text:
                for para in cell.paragraphs:
                    for run in para.runs:
                        run.text = ""
                    if para.runs:
                        para.runs[0].text = text

# 保存文档
output_path = r'D:\AndroidStudio\Project_Pandora\ofTests\测试报告.docx'
doc.save(output_path)
print(f"测试报告已更新: {output_path}")