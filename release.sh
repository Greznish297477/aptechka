#!/usr/bin/env bash
# Релиз по тегу. Использование: ./release.sh [1.1.0|v1.1.0]
set -e
cd "$(dirname "$0")"

ver="$1"
if [ -z "$ver" ]; then
  ver=$(grep -oP 'versionName\s*=\s*"\K[^"]+' app/build.gradle.kts | head -1 || true)
fi
if [ -z "$ver" ]; then
  echo "Укажи версию: ./release.sh 1.1.0"
  exit 1
fi
tag="$ver"
[[ "$tag" != v* ]] && tag="v$tag"

git add .
if [ -n "$(git status --porcelain)" ]; then
  git commit -m "Release $tag"
fi
git push origin main 2>/dev/null || git push -u origin main

if git rev-parse "$tag" >/dev/null 2>&1; then
  echo "Удаляю старый тег $tag"
  git tag -d "$tag"
  git push origin ":refs/tags/$tag" 2>/dev/null || true
fi

git tag -a "$tag" -m "Release $tag"
git push origin "$tag"

echo "Готово: $tag"
echo "Actions: https://github.com/Greznish297477/aptechka/actions"
echo "Releases: https://github.com/Greznish297477/aptechka/releases"
