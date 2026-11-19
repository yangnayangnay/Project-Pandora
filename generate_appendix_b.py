# -*- coding: utf-8 -*-
"""
生成完整的附录B用例明细（555条）
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
    """统计测试类中的测试用例数（含参数化展开）"""
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
    """获取测试类中的所有测试方法名"""
    try:
        with open(file_path, 'r', encoding='utf-8') as f:
            content = f.read()
            # 匹配 @Test 或 @ParameterizedTest 后的方法名
            pattern = r'@(?:Parameterized)?Test\s*\n[^\n]*void\s+(\w+)\s*\('
            methods = re.findall(pattern, content)
            return methods
    except:
        return []

def generate_test_cases():
    """生成所有测试用例明细"""
    backend_dir = r'D:\AndroidStudio\Project_Pandora\pandora-backend\src\test\java\com\pandora\backend'
    android_dir = r'D:\AndroidStudio\Project_Pandora\app\src\test\java\com\example\project_pandora'
    
    # 后端测试类
    backend_tests = []
    for root, dirs, files in os.walk(backend_dir):
        for f in files:
            if f.endswith('Test.java'):
                path = os.path.join(root, f)
                count = count_tests_with_params(path)
                methods = get_test_methods(path)
                class_name = f.replace('.java', '')
                backend_tests.append({
                    'class': class_name,
                    'count': count,
                    'methods': methods,
                    'path': path
                })
    
    # Android测试类
    android_tests = []
    for root, dirs, files in os.walk(android_dir):
        for f in files:
            if f.endswith('Test.java'):
                path = os.path.join(root, f)
                count = count_tests_with_params(path)
                methods = get_test_methods(path)
                class_name = f.replace('.java', '')
                android_tests.append({
                    'class': class_name,
                    'count': count,
                    'methods': methods,
                    'path': path
                })
    
    return backend_tests, android_tests

def main():
    print("开始生成附录B用例明细...")
    
    backend_tests, android_tests = generate_test_cases()
    
    backend_total = sum(t['count'] for t in backend_tests)
    android_total = sum(t['count'] for t in android_tests)
    
    print(f"后端: {len(backend_tests)}个测试类, {backend_total}条用例")
    for t in backend_tests:
        print(f"  {t['class']}: {t['count']}条")
    
    print(f"\nAndroid: {len(android_tests)}个测试类, {android_total}条用例")
    for t in android_tests:
        print(f"  {t['class']}: {t['count']}条")
    
    print(f"\n总计: {backend_total} + {android_total} = {backend_total + android_total}条")

if __name__ == "__main__":
    main()