#!/usr/bin/env bash
# 发版后清理旧 Release：只保留最新 KEEP 个（默认 3），更早的删除。
#
# 用法：
#   GITHUB_TOKEN=xxx bash scripts/prune_releases.sh            # 只打印计划，不删除
#   GITHUB_TOKEN=xxx bash scripts/prune_releases.sh --apply    # 真正删除
#
# 说明：
# - 只删 Release 及其 APK 资产，**tag 保留**（源码仍可从 tag 重建），
#   这样既满足「只保留最新 3 个版本」，又不会把历史源码一起抹掉。
# - 删除顺序固定：先删 Release（否则 tag 删掉后 Release 会变成 untagged-xxx 残留），
#   本次不删 tag，所以不会出现该问题。
set -euo pipefail

KEEP="${KEEP:-3}"
REPO="${REPO:-liangleionline/waxx-english}"
TOKEN="${GITHUB_TOKEN:-}"
APPLY="no"
[ "${1:-}" = "--apply" ] && APPLY="yes"

if [ -z "$TOKEN" ]; then
  TOKEN="$(printf 'protocol=https\nhost=github.com\n\n' | git credential fill 2>/dev/null \
    | awk -F= '/^password=/{print $2}')" || true
fi
if [ -z "$TOKEN" ]; then
  echo "缺少 token：请设置 GITHUB_TOKEN" >&2
  exit 1
fi

API="https://api.github.com/repos/$REPO"
LIST="$(mktemp)"
curl -s -H "Authorization: token $TOKEN" -H "Accept: application/vnd.github+json" \
  "$API/releases?per_page=100" > "$LIST"

echo "仓库: $REPO   保留最新: $KEEP 个"
python3 - "$LIST" <<'PY'
import json, sys
rows = json.load(open(sys.argv[1]))
keep = int(sys.argv[2]) if len(sys.argv) > 2 else 3
print(f"当前 Release 总数: {len(rows)}")
for i, r in enumerate(rows):
    flag = "保留" if i < keep else "待删除"
    print(f"  [{flag}] {r['tag_name']} (id={r['id']})")
PY

# 取需要删除的 id（保持时间倒序，跳过前 KEEP 个）
DEL_IDS="$(python3 -c "
import json,sys
rows=json.load(open('$LIST'))
keep=int('$KEEP')
for r in rows[keep:]:
    print(r['id'])
")"

if [ -z "$DEL_IDS" ]; then
  echo "无需清理：当前版本数量未超过 $KEEP 个。"
  rm -f "$LIST"
  exit 0
fi

if [ "$APPLY" != "yes" ]; then
  echo
  echo "以上为预览。加 --apply 才会真正删除。"
  rm -f "$LIST"
  exit 0
fi

echo
for rid in $DEL_IDS; do
  code=$(curl -s -o /dev/null -w "%{http_code}" -X DELETE \
    -H "Authorization: token $TOKEN" "$API/releases/$rid")
  echo "删除 release id=$rid -> HTTP $code"
done
rm -f "$LIST"

echo
echo "清理后剩余:"
curl -s -H "Authorization: token $TOKEN" "$API/releases?per_page=100" \
 | python3 -c "
import sys,json
rows=json.load(sys.stdin)
print(f'  共 {len(rows)} 个')
for r in rows: print('   ', r['tag_name'], [a['name'] for a in r.get('assets',[])])
"
