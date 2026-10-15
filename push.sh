#!/usr/bin/env bash
# Пуш в main. Использование: ./push.sh ["сообщение коммита"]
set -e
cd "$(dirname "$0")"
msg="${1:-Update $(date '+%Y-%m-%d %H:%M')}"
git add .
if [ -n "$(git status --porcelain)" ]; then
  git commit -m "$msg"
else
  echo "Нет изменений для коммита."
fi
git push -u origin main
echo "Готово."
