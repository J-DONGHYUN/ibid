#!/bin/sh
# 게이트 · 동결 기록 · 빌드 설정을 Bash 로 고치려 하면 사람에게 묻는다.
#
# settings.json 의 ask 는 Edit · Write 도구에만 걸린다. `sed -i` · 리다이렉션 · mv 로 고치면
# 승인 없이 지나간다. 이 훅이 그 우회로를 같은 ask 로 되돌린다.
#
# 쓰기로 보이는 명령이면서 보호 경로를 가리킬 때만 본다. 읽기(cat · grep · wc)는 통과시킨다.
# python -c 처럼 인터프리터 안에서 쓰는 경우는 못 잡는다 (docs/04-harness/harness.md 알려진 구멍).

payload=$(cat)
cmd=$(printf '%s' "$payload" | jq -r '.tool_input.command // empty')
[ -n "$cmd" ] || exit 0

protected='archunit-store|archunit\.properties|src/test/java/project/kjhjdh/ibid/architecture|build\.gradle|\.github/workflows|\.claude/settings\.json|\.claude/hooks|\.claude/agents'
writes='(>|(^|[[:space:];&|])(tee|mv|cp|rm|ln|truncate|patch|dd|install)[[:space:]]|sed[^|;&]*-i|git[[:space:]]+(rm|checkout|restore|mv)[[:space:]])'

printf '%s' "$cmd" | grep -qE "$protected" || exit 0
printf '%s' "$cmd" | grep -qE "$writes" || exit 0

reason="게이트 · 동결 기록 · 빌드 설정 · 훅 · 리뷰어를 셸 명령으로 고치려 한다. 이 파일들은 Edit 로 고쳐도 사람에게 묻는다 (ADR-0010). 게이트가 막으면 코드를 고치고, 규칙이 틀렸다고 판단되면 근거를 적어 사람에게 묻는다."
jq -n --arg reason "$reason" '{hookSpecificOutput: {hookEventName: "PreToolUse", permissionDecision: "ask", permissionDecisionReason: $reason}}'
