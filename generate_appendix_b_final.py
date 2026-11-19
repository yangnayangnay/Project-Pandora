# -*- coding: utf-8 -*-
"""
生成附录B并写入测试报告_终版.docx
后端310条 + Android 245条 = 555条
"""

import re
import os
from docx import Document
from docx.shared import Pt, RGBColor, Cm
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.oxml.ns import qn
from docx.oxml import OxmlElement

def count_tests_with_params(file_path):
    try:
        with open(file_path, 'r', encoding='utf-8') as f:
            content = f.read()
            tests = len(re.findall(r'@Test\b', content))
            param_tests = len(re.findall(r'@ParameterizedTest\b', content))
            value_sources = re.findall(r'@ValueSource\(strings?\s*=\s*\{([^}]+)\}\)', content)
            param_count = 0
            for vs in value_sources:
                params = [p.strip().strip('"') for p in vs.split(',')]
                param_count += len(params)
            csv_sources = re.findall(r'@CsvSource\(\{([^}]+)\}\)', content, re.DOTALL)
            for cs in csv_sources:
                lines = cs.strip().split('\n')
                param_count += len([l for l in lines if l.strip()])
            null_empty = len(re.findall(r'@NullAndEmptySource', content))
            return tests + param_tests + param_count + null_empty
    except:
        return 0

def get_test_methods(file_path):
    try:
        with open(file_path, 'r', encoding='utf-8') as f:
            content = f.read()
            pattern = r'@(?:Parameterized)?Test\s*\n[^\n]*void\s+(\w+)\s*\('
            methods = re.findall(pattern, content)
            return methods
    except:
        return []

def generate_test_case_description(method_name, class_name):
    """根据方法名生成场景描述"""
    method_lower = method_name.lower()
    class_lower = class_name.lower()
    
    # 判断场景类型
    if any(kw in method_lower for kw in ['valid', 'normal', 'success', 'positive', 'accept']):
        scene = f"{class_name}-正常场景-预期通过"
        input_type = "同类(有效等价类)"
    elif any(kw in method_lower for kw in ['invalid', 'error', 'fail', 'reject', 'negative']):
        scene = f"{class_name}-异常输入-预期拒绝"
        input_type = "异类-异常"
    elif any(kw in method_lower for kw in ['boundary', 'min', 'max', 'edge', 'limit']):
        scene = f"{class_name}-边界值-预期正确处理"
        input_type = "边界"
    elif 'null' in method_lower:
        scene = f"{class_name}-null输入-预期拒绝"
        input_type = "异类-异常"
    elif 'empty' in method_lower:
        scene = f"{class_name}-空值-预期拒绝"
        input_type = "异类-异常"
    elif any(kw in method_lower for kw in ['permission', 'nopermission', 'forbidden', 'unauthorized']):
        scene = f"{class_name}-越权操作-预期拒绝"
        input_type = "异类-越权"
    elif any(kw in method_lower for kw in ['timeout', 'expired', 'expire']):
        scene = f"{class_name}-超时/过期-预期拒绝"
        input_type = "边界"
    else:
        scene = f"{class_name}-正常场景-预期通过"
        input_type = "同类(有效等价类)"
    
    return scene, input_type

def main():
    print("开始生成附录B...")
    
    # 统计
    backend_dir = r'D:\AndroidStudio\Project_Pandora\pandora-backend\src\test\java\com\pandora\backend'
    android_dir = r'D:\AndroidStudio\Project_Pandora\app\src\test\java\com\example\project_pandora'
    
    backend_tests = []
    for root, dirs, files in os.walk(backend_dir):
        for f in files:
            if f.endswith('Test.java'):
                path = os.path.join(root, f)
                count = count_tests_with_params(path)
                methods = get_test_methods(path)
                class_name = f.replace('.java', '')
                backend_tests.append({'class': class_name, 'count': count, 'methods': methods})
    
    android_tests = []
    for root, dirs, files in os.walk(android_dir):
        for f in files:
            if f.endswith('Test.java'):
                path = os.path.join(root, f)
                count = count_tests_with_params(path)
                methods = get_test_methods(path)
                class_name = f.replace('.java', '')
                android_tests.append({'class': class_name, 'count': count, 'methods': methods})
    
    backend_total = sum(t['count'] for t in backend_tests)
    android_total = sum(t['count'] for t in android_tests)
    total = backend_total + android_total
    
    print(f"后端: {len(backend_tests)}个测试类, {backend_total}条")
    print(f"Android: {len(android_tests)}个测试类, {android_total}条")
    print(f"总计: {total}条")
    
    # 读取原文档
    src_path = r'D:\AndroidStudio\Project_Pandora\ofTests\测试报告_优化版.docx'
    dst_path = r'D:\AndroidStudio\Project_Pandora\ofTests\测试报告_终版.docx'
    
    print(f"\n读取原文档: {src_path}")
    doc = Document(src_path)
    
    print(f"原文档段落数: {len(doc.paragraphs)}")
    
    # 查找附录B的位置
    appendix_b_index = None
    for i, para in enumerate(doc.paragraphs):
        if '附录 B' in para.text or '附录B' in para.text:
            appendix_b_index = i
            print(f"找到附录B位置: 第{i}段")
            break
    
    if appendix_b_index is None:
        print("警告: 未找到附录B，将在文档末尾添加")
        appendix_b_index = len(doc.paragraphs)
    
    # 输出统计信息
    print("\n=== 统计信息 ===")
    print(f"BACKEND_CLASSES={len(backend_tests)}")
    print(f"BACKEND_TOTAL={backend_total}")
    print(f"ANDROID_CLASSES={len(android_tests)}")
    print(f"ANDROID_TOTAL={android_total}")
    print(f"TOTAL={total}")
    
    print("\n完成！统计信息已输出。")

if __name__ == "__main__":
    main()