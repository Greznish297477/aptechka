# Создать тег и отправить на GitHub → Actions соберёт APK и сделает Release
# Использование:
#   .\release.ps1              → версия из app/build.gradle.kts (versionName)
#   .\release.ps1 1.2.0        → тег v1.2.0
#   .\release.ps1 v1.2.0       → тег v1.2.0

$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot

function Get-VersionFromGradle {
    $path = Join-Path $PSScriptRoot "app\build.gradle.kts"
    if (-not (Test-Path $path)) { return $null }
    $content = Get-Content $path -Raw
    if ($content -match 'versionName\s*=\s*"([^"]+)"') {
        return $Matches[1]
    }
    return $null
}

$raw = $args[0]
if (-not $raw) {
    $raw = Get-VersionFromGradle
    if (-not $raw) {
        Write-Host "Не удалось прочитать versionName. Укажи версию: .\release.ps1 1.1.0" -ForegroundColor Red
        exit 1
    }
    Write-Host "Версия из build.gradle.kts: $raw" -ForegroundColor Yellow
}

$tag = $raw
if (-not $tag.StartsWith("v")) {
    $tag = "v$tag"
}

Write-Host ""
Write-Host "Тег: $tag" -ForegroundColor Cyan
Write-Host "Репозиторий должен быть настроен (git remote origin)." -ForegroundColor DarkGray
Write-Host ""

# 1) Закоммитить незакоммиченное (если есть)
git add .
$pending = git status --porcelain
if ($pending) {
    Write-Host "==> Есть незакоммиченные файлы — коммит перед релизом" -ForegroundColor Cyan
    git commit -m "Release $tag"
}

# 2) Push main
Write-Host "==> git push origin main" -ForegroundColor Cyan
git push origin main 2>$null
if ($LASTEXITCODE -ne 0) {
    git push -u origin main
}

# 3) Тег (пересоздать локально, если уже есть)
$exists = git tag -l $tag
if ($exists) {
    Write-Host "==> Тег $tag уже есть локально — удаляю и создаю заново" -ForegroundColor Yellow
    git tag -d $tag
    git push origin ":refs/tags/$tag" 2>$null
}

Write-Host "==> git tag $tag" -ForegroundColor Cyan
git tag -a $tag -m "Release $tag"

Write-Host "==> git push origin $tag" -ForegroundColor Cyan
git push origin $tag

Write-Host ""
Write-Host "Готово! Тег $tag отправлен." -ForegroundColor Green
Write-Host "GitHub Actions соберёт APK и создаст Release (5–10 мин)." -ForegroundColor Green
Write-Host "Смотри: https://github.com/Greznish297477/aptechka/actions" -ForegroundColor Cyan
Write-Host "Релизы: https://github.com/Greznish297477/aptechka/releases" -ForegroundColor Cyan
