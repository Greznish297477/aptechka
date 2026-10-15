# Пуш кода в GitHub (ветка main)
# Использование:  .\push.ps1
#                 .\push.ps1 "Текст коммита"

$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot

$msg = $args[0]
if (-not $msg) {
    $msg = "Update $(Get-Date -Format 'yyyy-MM-dd HH:mm')"
}

Write-Host "==> git add ." -ForegroundColor Cyan
git add .

$status = git status --porcelain
if (-not $status) {
    Write-Host "Нет изменений для коммита." -ForegroundColor Yellow
} else {
    Write-Host "==> git commit: $msg" -ForegroundColor Cyan
    git commit -m $msg
}

Write-Host "==> git push origin main" -ForegroundColor Cyan
git push -u origin main

Write-Host "Готово." -ForegroundColor Green
