#!/bin/sh
# 리뷰어 서브에이전트(.claude/agents/reviewer.md)의 Bash 를 읽기 명령으로만 제한한다.
#
# 리뷰어는 이슈와 diff 를 보려면 Bash 가 있어야 한다. 하지만 쓸 수 있으면 리뷰 중에 고쳐 버리는
# 두 번째 구현자가 되고, 리뷰가 독립이 아니게 된다. 지시문으로는 강제할 수 없어서 화이트리스트로 막는다.
# 이 훅은 리뷰어의 frontmatter 에만 걸린다 — 리뷰어가 도는 동안에만 있고, 메인 루프에는 걸리지 않는다.
#
# 명령을 ; & | 와 줄바꿈으로 나눠, 조각마다 허용 목록의 명령으로 시작하는지 본다. 하나라도 아니면 deny.
# 명령 치환과 리다이렉션은 조각으로 나눠서는 볼 수 없어 통째로 막는다.
# 단 2>/dev/null · 2>&1 은 쓰기가 아니라서 지우고 본다 (INC-03).
# 따옴표 안의 | · ; 도 나눠 버려서 `grep -E 'a|b'` 는 막힌다. 오탐이지만 새지는 않는다.

payload=$(cat)
cmd=$(printf '%s' "$payload" | jq -r '.tool_input.command // empty')
[ -n "$cmd" ] || exit 0

allowed="git diff · log · show · status · rev-parse · merge-base · ls-files · grep · blame, gh issue view, gh pr view · diff, 파이프 뒤 grep · head · tail · wc · sort · uniq"
hint="리뷰어는 읽기만 한다. 허용: $allowed. 파일 읽기 · 검색은 Read · Grep · Glob 을 쓴다. 고칠 것은 고치지 말고 발견으로 돌려준다."

deny() {
    jq -n --arg reason "$1 $hint" '{hookSpecificOutput: {hookEventName: "PreToolUse", permissionDecision: "deny", permissionDecisionReason: $reason}}'
    exit 0
}

stripped=$(printf '%s' "$cmd" | sed -E 's#[0-9]?>[[:space:]]*/dev/null##g; s#2>&1##g')

case "$stripped" in
    *'$('* | *'`'*) deny "명령 치환은 쓸 수 없다." ;;
    *'>'* | *'<'*) deny "리다이렉션은 쓸 수 없다." ;;
esac

# git 이 파일을 쓰거나 외부 프로그램을 부르는 옵션
printf '%s' "$stripped" | grep -qE -- '--output|--open-files-in-pager|(^|[[:space:]])-O|--ext-diff' && deny "파일을 쓰거나 외부 프로그램을 부르는 옵션이다."

ok='^(cd([[:space:]]|$)|git[[:space:]]+(-C[[:space:]]+[^[:space:]]+[[:space:]]+)?(--no-pager[[:space:]]+)?(diff|log|show|status|rev-parse|merge-base|ls-files|grep|blame)([[:space:]]|$)|gh[[:space:]]+(issue[[:space:]]+view|pr[[:space:]]+(view|diff))([[:space:]]|$)|(grep|head|tail|wc|sort|uniq)([[:space:]]|$))'

bad=$(printf '%s\n' "$stripped" | tr ';&|' '\n\n\n' | sed -E 's/^[[:space:]]+//' | grep -v '^$' | grep -vE "$ok" | head -1)
[ -z "$bad" ] || deny "허용되지 않은 명령: '$bad'."

exit 0
