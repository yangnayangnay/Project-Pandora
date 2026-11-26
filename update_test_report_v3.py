#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""更新测试报告 - 补充修复"""

from docx import Document

# 打开文档
doc = Document(r'D:\AndroidStudio\Project_Pandora\ofTests\测试报告.docx')

# 新的测试数据
BACKEND_TESTS = 42
BACKEND_CASES = 465

# 遍历所有段落进行替换
for para in doc.paragraphs:
    text = para.text
    new_text = text
    
    # 修复 PASSED 计数
    if "PASSED: 17 FAILED: 0" in text:
        new_text = text.replace("PASSED: 17 FAILED: 0", f"PASSED: {BACKEND_TESTS} FAILED: 0")
    
    # 修复后端类数和用例数
    if "后端 8 类 150 条" in text:
        new_text = text.replace("后端 8 类 150 条", f"后端 {BACKEND_TESTS} 类 {BACKEND_CASES} 条")
    
    # 修复后端表格标题
    if "后端(150 条" in text:
        new_text = text.replace("后端(150 条", f"后端({BACKEND_CASES} 条")
    
    if new_text != text:
        for run in para.runs:
            run.text = ""
        if para.runs:
            para.runs[0].text = new_text
        print(f"更新: {text[:80]} -> {new_text[:80]}")

# 遍历表格
for table in doc.tables:
    for row in table.rows:
        for cell in row.cells:
            for para in cell.paragraphs:
                text = para.text
                new_text = text
                
                if "150 条" in text and "后端" in text:
                    new_text = text.replace("150 条", f"{BACKEND_CASES} 条")
                
                if "8 类" in text and "后端" in text:
                    new_text = text.replace("8 类", f"{BACKEND_TESTS} 类")
                
                if new_text != text:
                    for run in para.runs:
                        run.text = ""
                    if para.runs:
                        para.runs[0].text = new_text
                    print(f"表格更新: {text[:60]} -> {new_text[:60]}")

# 保存
doc.save(r'D:\AndroidStudio\Project_Pandora\ofTests\测试报告.docx')
print("更新完成")