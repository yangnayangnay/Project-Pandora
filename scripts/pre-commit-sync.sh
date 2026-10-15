#!/bin/sh
# Git pre-commit hook: 自动同步飞书看板CSV
# 当 openspec/ 或 docx/docx_generate/ 下的文件有变更时，自动重新生成飞书导入CSV

CHANGED=$(git diff --cached --name-only --diff-filter=ACM | grep -E '^(openspec/|docx/docx_generate/需求改动\.txt)')

if [ -n "$CHANGED" ]; then
    echo "[pre-commit] 检测到 OpenSpec/需求文档变更，自动同步飞书看板CSV..."
    python scripts/sync_feishu_board.py
    git add docx/docx_generate/项目看板_飞书导入.csv
    echo "[pre-commit] 飞书CSV已更新并加入本次提交"
fi