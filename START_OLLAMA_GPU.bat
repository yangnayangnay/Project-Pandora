@echo off
title Ollama GPU ģʽ������
echo ============================================
echo   Ollama GPU ģʽ���� (Round24 T1 �����̻�)
echo ============================================
echo.

rem ���ǿ�� CPU ����ʷ����������������������
set "OLLAMA_LLM_LIBRARY="
set "OLLAMA_NUM_GPU="

rem ���� 30 ���ӣ�������������ض�����
set "OLLAMA_KEEP_ALIVE=30m"

rem ģ�ʹ洢λ�ã����� 8 ��ģ�ͣ�
set "OLLAMA_MODELS=E:\Code\modes"

set "OLLAMA_HOST=localhost:11434"

echo [INFO] ����������: LLM_LIBRARY/NUM_GPU ǿ�� CPU �������ڱ��������
echo [INFO] KEEP_ALIVE=30m  MODELS=E:/Code/modes
echo [INFO] �������� Ollama ���� (GPU �Զ�̽��)...
echo.

taskkill /IM "ollama.exe" /F >nul 2>&1
timeout /t 2 /nobreak >nul

start "Ollama-GPU-Server" /MIN "D:/�½��ļ��� (3)/ollama/ollama.exe" serve

timeout /t 5 /nobreak >nul
echo [����] ģ���б�:
curl -s http://localhost:11434/api/tags
echo.
echo.
echo [��ʾ] �������ں�̨���У������ڿɹرա�
echo        ��֤ GPU: curl http://localhost:11434/api/ps  (size_vram Ӧ���� 0)
echo.
pause