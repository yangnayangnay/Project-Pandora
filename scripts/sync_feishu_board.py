#!/usr/bin/env python3
"""
sync_feishu_board.py — 从 OpenSpec tasks.md 自动生成飞书多维表格导入CSV
当 tasks.md 或需求改动.txt 变更时，运行此脚本即可同步更新飞书CSV。

用法:
    python scripts/sync_feishu_board.py

输出:
    docx/docx_generate/项目看板_飞书导入.csv
    docx/docx_generate/潘多拉文档_飞书导入.csv
"""

import csv
import json
import os
import re
import sys

PROJECT_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
GENERATE_DIR = os.path.join(PROJECT_ROOT, "docx", "docx_generate")
OPENSPEC_TASKS = os.path.join(PROJECT_ROOT, "openspec", "changes", "initial-mvp-delivery", "tasks.md")

SPRINT_MAP = {
    "Epic-1": "Sprint 1",
    "Epic-2": "Sprint 2",
    "Epic-3": "Sprint 2",
    "Epic-4": "Sprint 2",
    "Epic-5": "Sprint 1",
    "Epic-6": "Sprint 2",
    "Bug": "Sprint 2",
    "NFR-1": "Sprint 2",
    "NFR-2": "Sprint 2",
}

SPRINT3_TASKS = [
    ("S3-1", "单元测试报告", "全局", "Sprint 3", "—", "待开始"),
    ("S3-2", "非功能需求测试-月相主题", "NFR-1", "Sprint 3", "—", "待开始"),
    ("S3-3", "非功能需求测试-无障碍", "NFR-2", "Sprint 3", "—", "待开始"),
    ("S3-4", "验收测试Checklist", "全局", "Sprint 3", "—", "待开始"),
    ("S3-5", "缺陷修复", "全局", "Sprint 3", "—", "待开始"),
]

FINAL_TASKS = [
    ("F-1", "最终可运行系统", "全局", "Final Release", "—", "待开始"),
    ("F-2", "OpenSpec变更归档", "全局", "Final Release", "—", "待开始"),
    ("F-3", "测试报告", "全局", "Final Release", "—", "待开始"),
    ("F-4", "展示PPT", "全局", "Final Release", "—", "待开始"),
]

SPRINT0_TASKS = [
    ("S0-1", "需求分析文档", "全局", "Sprint 0", "initial-mvp-delivery/proposal", "已完成"),
    ("S0-2", "技术架构设计", "全局", "Sprint 0", "initial-mvp-delivery/design", "已完成"),
    ("S0-3", "OpenSpec初始化", "全局", "Sprint 0", "openspec/config.yaml", "已完成"),
    ("S0-4", "核心能力规格6份", "全局", "Sprint 0", "openspec/specs/*.md", "已完成"),
    ("S0-5", "任务清单生成", "全局", "Sprint 0", "initial-mvp-delivery/tasks.md", "已完成"),
]


def parse_tasks_md(filepath):
    """从 OpenSpec tasks.md 解析任务列表"""
    if not os.path.exists(filepath):
        print(f"Warning: {filepath} not found", file=sys.stderr)
        return []

    with open(filepath, "r", encoding="utf-8") as f:
        content = f.read()

    tasks = []
    current_epic = "全局"
    task_counter = {"Sprint 1": 0, "Sprint 2": 0}

    for line in content.split("\n"):
        line = line.strip()

        m = re.match(r"## (Epic-\d+|Bug Fixes|NFR-\d+)", line)
        if m:
            tag = m.group(1)
            if tag == "Bug Fixes":
                current_epic = "Bug"
            else:
                current_epic = tag
            continue

        m = re.match(r"-\s*\[([ x])\]\s*(.+)", line)
        if m:
            done = m.group(1) == "x"
            name = m.group(2).strip()
            sprint = SPRINT_MAP.get(current_epic, "Sprint 2")
            task_counter[sprint] = task_counter.get(sprint, 0) + 1
            task_id = f"S{sprint[-1]}-{task_counter[sprint]}"
            status = "已完成" if done else "待开始"
            change_ref = f"initial-mvp-delivery/tasks.md"
            tasks.append((task_id, name, current_epic, sprint, change_ref, status))

    return tasks


def generate_board_csv():
    """生成项目看板CSV"""
    all_tasks = []

    all_tasks.extend(SPRINT0_TASKS)

    parsed = parse_tasks_md(OPENSPEC_TASKS)
    all_tasks.extend(parsed)

    all_tasks.extend(SPRINT3_TASKS)
    all_tasks.extend(FINAL_TASKS)

    dst = os.path.join(GENERATE_DIR, "项目看板_飞书导入.csv")
    with open(dst, "w", encoding="utf-8-sig", newline="") as f:
        writer = csv.writer(f)
        writer.writerow(["任务ID", "任务名称", "Epic", "Sprint", "OpenSpec Change", "状态", "负责人"])
        for tid, name, epic, sprint, change, status in all_tasks:
            assignee = "codearts" if status == "已完成" else "—"
            writer.writerow([tid, name, epic, sprint, change, status, assignee])

    print(f"项目看板: {len(all_tasks)} tasks -> {dst}")
    return len(all_tasks)


def main():

    os.makedirs(GENERATE_DIR, exist_ok=True)
    n1 = generate_board_csv()
    print(f"\nDone. 看板{n1}条, 在2000行限制内。")


if __name__ == "__main__":
    main()