#!/usr/bin/env bash
# Отправить изменения в GitHub: bash push.sh ["сообщение коммита"]
set -Eeuo pipefail
cd "$(dirname "$0")"
git config user.name "Greznish297477"
git config user.email "dedushkadanil8@gmail.com"
git add .
if ! git diff --cached --quiet; then
  git commit -m "${1:-Update $(date '+%Y-%m-%d %H:%M')}"
else
  echo "Нет изменений для коммита."
fi
git push -u origin HEAD:main
