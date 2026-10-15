import re
import os
from docx import Document
from docx.shared import Pt, Inches, RGBColor
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.enum.table import WD_TABLE_ALIGNMENT
from docx.oxml.ns import qn

def set_cell_shading(cell, color):
    shading = cell._element.get_or_add_tcPr()
    shd = shading.makeelement(qn('w:shd'), {
        qn('w:val'): 'clear',
        qn('w:color'): 'auto',
        qn('w:fill'): color
    })
    shading.append(shd)

def add_runs_with_bold(paragraph, text):
    parts = re.split(r'(\*\*.*?\*\*)', text)
    for part in parts:
        if part.startswith('**') and part.endswith('**'):
            run = paragraph.add_run(part[2:-2])
            run.bold = True
        else:
            if part:
                paragraph.add_run(part)

def md_to_docx(md_path, docx_path):
    with open(md_path, 'r', encoding='utf-8') as f:
        lines = f.readlines()

    doc = Document()
    style = doc.styles['Normal']
    style.font.name = '微软雅黑'
    style.font.size = Pt(10.5)
    style._element.rPr.rFonts.set(qn('w:eastAsia'), '微软雅黑')

    i = 0
    in_table = False
    table_rows = []

    while i < len(lines):
        line = lines[i].rstrip('\n')

        if line.strip().startswith('```'):
            i += 1
            code_lines = []
            while i < len(lines) and not lines[i].strip().startswith('```'):
                code_lines.append(lines[i].rstrip('\n'))
                i += 1
            i += 1
            p = doc.add_paragraph()
            run = p.add_run('\n'.join(code_lines))
            run.font.name = 'Consolas'
            run.font.size = Pt(9)
            continue

        if line.strip().startswith('|') and line.strip().endswith('|'):
            cells = [c.strip() for c in line.strip().split('|')[1:-1]]
            if all(re.match(r'^[-:]+$', c) for c in cells):
                i += 1
                continue
            table_rows.append(cells)
            i += 1
            while i < len(lines) and lines[i].strip().startswith('|') and lines[i].strip().endswith('|'):
                row_cells = [c.strip() for c in lines[i].strip().split('|')[1:-1]]
                if not all(re.match(r'^[-:]+$', c) for c in row_cells):
                    table_rows.append(row_cells)
                i += 1

            if table_rows:
                num_cols = max(len(r) for r in table_rows)
                table = doc.add_table(rows=len(table_rows), cols=num_cols)
                table.style = 'Table Grid'
                table.alignment = WD_TABLE_ALIGNMENT.CENTER
                for ri, row in enumerate(table_rows):
                    for ci in range(num_cols):
                        cell = table.cell(ri, ci)
                        cell.text = ''
                        p = cell.paragraphs[0]
                        add_runs_with_bold(p, row[ci] if ci < len(row) else '')
                        if ri == 0:
                            for run in p.runs:
                                run.bold = True
                            set_cell_shading(cell, 'D9E2F3')
                doc.add_paragraph()
                table_rows = []
            continue

        if line.startswith('# '):
            h = doc.add_heading(line[2:], level=1)
        elif line.startswith('## '):
            doc.add_heading(line[3:], level=2)
        elif line.startswith('### '):
            doc.add_heading(line[4:], level=3)
        elif line.startswith('#### '):
            doc.add_heading(line[5:], level=4)
        elif line.strip() == '---':
            doc.add_paragraph('─' * 50)
        elif re.match(r'^[-*] ', line.strip()):
            p = doc.add_paragraph(style='List Bullet')
            add_runs_with_bold(p, line.strip()[2:])
        elif re.match(r'^\d+\. ', line.strip()):
            p = doc.add_paragraph(style='List Number')
            add_runs_with_bold(p, re.sub(r'^\d+\. ', '', line.strip()))
        elif line.strip() == '':
            pass
        else:
            p = doc.add_paragraph()
            add_runs_with_bold(p, line)
        i += 1

    doc.save(docx_path)
    print(f'生成: {docx_path}')

base = r'D:\AndroidStudio\Project_Pandora\docx\docx_generate'
files = [
    ('需求评审会议纪要.md', '需求评审会议纪要.docx'),
    ('产品需求迭代.md', '产品需求迭代.docx'),
    ('简易项目管理工具.md', '简易项目管理工具.docx'),
]
for md, docx in files:
    md_to_docx(os.path.join(base, md), os.path.join(base, docx))
print('全部完成')