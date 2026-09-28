#!/bin/sh
# 이슈 브랜치에서, 계획을 이슈에 게시한 뒤에만 src/ 를 고칠 수 있다.
#
# 루프의 [2] 는 "계획을 이슈 본문에 올린다" 다. CLAUDE.md 에 적어 두는 것만으로는 급할 때 건너뛴다.
# 계획이 틀리면 그 뒤의 구현 전부가 낭비라서, 사람이 계획을 볼 기회를 설정으로 보장한다.
#
# 무엇을 보나 — 승인 여부가 아니라 게시 여부다. 이슈 본문에 `## 🧭 계획` 절이 있는지만 본다.
# 게시는 한 방향이라(올린 계획이 사라지지 않는다) 통과한 결과만 .git 아래에 캐시한다. gh 호출이 느려서다.
# 참고: 덕모임 팀 하네스의 계획 게시 가드(설계: Seulgi Han)에서 착안했다.

payload=$(cat)
tool=$(printf '%s' "$payload" | jq -r '.tool_name // empty')

case "$tool" in
    Write|Edit|NotebookEdit)
        target=$(printf '%s' "$payload" | jq -r '.tool_input.file_path // .tool_input.notebook_path // empty')
        printf '%s' "$target" | grep -qE '(^|/)src/' || exit 0
        ;;
    Bash)
        cmd=$(printf '%s' "$payload" | jq -r '.tool_input.command // empty')
        printf '%s' "$cmd" | grep -qE '(^|[[:space:]"'"'"'=])src/' || exit 0
        printf '%s' "$cmd" | grep -qE '(>|(^|[[:space:];&|])(tee|mv|cp|rm|ln|truncate|patch)[[:space:]]|sed[^|;&]*-i)' || exit 0
        ;;
    *)
        exit 0
        ;;
esac

deny() {
    jq -n --arg reason "$1" '{hookSpecificOutput: {hookEventName: "PreToolUse", permissionDecision: "deny", permissionDecisionReason: $reason}}'
    exit 0
}

cd "${CLAUDE_PROJECT_DIR:-.}" || exit 0
branch=$(git branch --show-current 2>/dev/null)
issue=$(printf '%s' "$branch" | sed -nE 's#^(feat|fix|refactor|test|docs|chore)/([0-9]+)-.*#\2#p')

[ -n "$issue" ] || deny "src/ 는 이슈 브랜치(예: feat/41-예약하기)에서만 고친다. 지금 브랜치: '${branch:-없음}'. ticket 스킬의 [0] 부터 시작한다 (docs/04-harness/loop.md)."

cache="$(git rev-parse --git-dir)/ibid-loop/plan-posted-$issue"
[ -f "$cache" ] && exit 0

body=$(gh issue view "$issue" --json body -q .body 2>/dev/null) || deny "이슈 #$issue 를 읽지 못했다 (gh 인증 · 네트워크 · 이슈 번호를 확인한다). 계획 게시를 확인할 수 없어 src/ 수정을 막는다."

printf '%s\n' "$body" | grep -q '^## 🧭 계획' || deny "이슈 #$issue 에 계획이 아직 없다. 구현 전에 ticket 스킬 [2] 대로 '## 🧭 계획' 절을 이슈 본문에 게시한다 — 계획이 틀리면 그 뒤가 전부 낭비라서 사람이 먼저 본다."

mkdir -p "$(dirname "$cache")" && touch "$cache"
exit 0
