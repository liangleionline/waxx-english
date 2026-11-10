#!/usr/bin/env bash
# 打印 GitHub 仓库的访问 / 克隆 / Release 下载统计
# 用法：
#   GITHUB_TOKEN=xxx bash scripts/github_stats.sh [owner/repo]
# token 缺省时尝试用本机 git 凭据（git credential fill）。
set -euo pipefail

REPO="${1:-liangleionline/waxx-english}"
TOKEN="${GITHUB_TOKEN:-}"

if [ -z "$TOKEN" ]; then
  TOKEN="$(printf 'protocol=https\nhost=github.com\n\n' | git credential fill 2>/dev/null \
    | awk -F= '/^password=/{print $2}')" || true
fi

if [ -z "$TOKEN" ]; then
  echo "缺少 token：请设置 GITHUB_TOKEN，或先让 git 记住 github.com 的凭据" >&2
  exit 1
fi

api() { curl -s -H "Authorization: token $TOKEN" -H "Accept: application/vnd.github+json" "https://api.github.com/repos/$REPO$1"; }

echo "== 仓库：$REPO =="
api "" | python3 -c "import sys,json;d=json.load(sys.stdin);print(f\"Star {d['stargazers_count']} · Fork {d['forks_count']} · 体积 {d['size']} KB\")"

echo
echo "== 访问量（近 14 天）=="
api "/traffic/views" | python3 -c "import sys,json;d=json.load(sys.stdin);print(f\"总计 {d['count']} 次 / 独立 {d['uniques']}\")"

echo
echo "== 克隆量（近 14 天）=="
api "/traffic/clones" | python3 -c "import sys,json;d=json.load(sys.stdin);print(f\"总计 {d['count']} 次 / 独立 {d['uniques']}\")"

echo
echo "== Release 下载量 =="
api "/releases" | python3 -c "
import sys,json
rows=json.load(sys.stdin)
total=0
for r in rows:
    line=[]
    for a in r.get('assets',[]):
        line.append(f\"{a['name']}={a['download_count']}\")
        total+=a['download_count']
    print(f\"{r['tag_name']:>8}  \" + (' '.join(line) if line else '(无资产)'))
print(f'合计 {total} 次')
"
