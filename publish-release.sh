#!/usr/bin/env bash
# Первичная загрузка проекта и публикация релиза.
# Использование: bash publish-release.sh [версия] [URL_репозитория]
set -Eeuo pipefail
cd "$(dirname "$0")"

GITHUB_USER="Greznish297477"
GIT_EMAIL="dedushkadanil8@gmail.com"
VERSION="${1:-$(grep -oP 'versionName\s*=\s*"\K[^"]+' app/build.gradle.kts | head -1)}"
REPO_URL="${2:-https://github.com/${GITHUB_USER}/aptechka.git}"
TAG="$VERSION"
[[ "$TAG" == v* ]] || TAG="v$TAG"

command -v git >/dev/null || { echo "Ошибка: установи Git."; exit 1; }
git config user.name "$GITHUB_USER"
git config user.email "$GIT_EMAIL"

if ! git rev-parse --is-inside-work-tree >/dev/null 2>&1; then
  git init
fi
git branch -M main

if git remote get-url origin >/dev/null 2>&1; then
  git remote set-url origin "$REPO_URL"
else
  git remote add origin "$REPO_URL"
fi

echo "Проверяю, что секреты и keystore не добавляются..."
git add .
if git diff --cached --name-only | grep -E '(^|/)([^/]*\.jks|[^/]*\.keystore|keystore\.properties)$|(^|/)\.env$'; then
  echo "ОШИБКА: обнаружены ключи/секреты в индексе Git. Удали их из индекса и повтори."
  exit 1
fi

if ! git diff --cached --quiet; then
  git commit -m "Prepare release $TAG"
fi

git push -u origin main

if git rev-parse "$TAG" >/dev/null 2>&1 || git ls-remote --exit-code --tags origin "refs/tags/$TAG" >/dev/null 2>&1; then
  echo "Тег $TAG уже существует. Не перезаписываю его автоматически."
  echo "Для новой версии запусти: bash publish-release.sh 1.1.7"
  exit 1
fi

git tag -a "$TAG" -m "MedExpiry $TAG"
git push origin "$TAG"

echo
echo "Код и тег $TAG отправлены."
echo "Сборка: https://github.com/${GITHUB_USER}/aptechka/actions"
echo "Релизы: https://github.com/${GITHUB_USER}/aptechka/releases"
