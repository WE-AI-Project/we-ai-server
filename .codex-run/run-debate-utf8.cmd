@echo off
title WE-AI Debate Live UTF-8
chcp 65001 >nul
cd /d D:\_ProJects\WeAi\we-ai-server

echo Logging in...
curl.exe -sS -X POST http://localhost:8080/api/v1/auth/login ^
  -H "Content-Type: application/json" ^
  --data-binary "@.codex-run\login-request.json" ^
  -o .codex-run\login-response.json

for /f "delims=" %%T in ('node -p "require('./.codex-run/login-response.json').data.accessToken"') do set "TOKEN=%%T"

if not defined TOKEN (
  echo Login failed.
  node -e "console.log(require('./.codex-run/login-response.json'))"
  exit /b 1
)

echo Starting Oracle - Backend - Frontend - Inspector debate...
echo.
curl.exe -sS --max-time 300 -X POST http://localhost:8080/api/v1/ai/debate ^
  -H "Authorization: Bearer %TOKEN%" ^
  -H "Content-Type: application/json" ^
  --data-binary "@.codex-run\debate-request.json" ^
  -o .codex-run\debate-response.json

echo ===== AI DEBATE RESULT =====
node -e "const d=require('./.codex-run/debate-response.json'); const x=d.data; console.log('Success:',d.success); console.log('Completed:',x.completed,'/ Rounds:',x.executedRounds); console.log(); for(const t of x.turns) console.log('['+t.agent+' / '+t.model+']\n'+t.message+'\n')"
echo ===== COMPLETE =====
