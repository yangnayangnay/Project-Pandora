# -*- coding: utf-8 -*-
"""
生成完整的附录B用例明细并写入测试报告_终版.docx
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
    # 简化版本：根据方法名生成基本描述
    if 'valid' in method_name.lower() or 'normal' in method_name.lower():
        scene = f"{class_name}-正常场景-预期通过"
        input_type = "同类(有效等价类)"
    elif 'invalid' in method_name.lower() or 'error' in method_name.lower():
        scene = f"{class_name}-异常输入-预期拒绝"
        input_type = "异类-异常"
    elif 'boundary' in method_name.lower() or 'min' in method_name.lower() or 'max' in method_name.lower():
        scene = f"{class_name}-边界值-预期正确处理"
        input_type = "边界"
    elif 'null' in method_name.lower():
        scene = f"{class_name}-null输入-预期拒绝"
        input_type = "异类-异常"
    elif 'empty' in method_name.lower():
        scene = f"{class_name}-空值-预期拒绝"
        input_type = "异类-异常"
    elif 'permission' in method_name.lower() or 'noPermission' in method_name.lower():
        scene = f"{class_name}-越权操作-预期拒绝"
        input_type = "异类-越权"
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
    
    print(f"后端: {len(backend_tests)}个测试类, {backend_total}条")
    print(f"Android: {len(android_tests)}个测试类, {android_total}条")
    print(f"总计: {backend_total + android_total}条")
    
    # 输出统计信息供后续使用
    print("\n=== 统计信息 ===")
    print(f"BACKEND_CLASSES={len(backend_tests)}")
    print(f"BACKEND_TOTAL={backend_total}")
    print(f"ANDROID_CLASSES={len(android_tests)}")
    print(f"ANDROID_TOTAL={android_total}")
    print(f"TOTAL={backend_total + android_total}")

if __name__ == "__main__":
    main()