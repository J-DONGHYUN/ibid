#!/bin/sh
# 게이트 · 동결 기록 · 빌드 설정 · 훅 · 리뷰어를 Bash 로 고치려 하면 사람에게 묻는다.
#
# settings.json 의 ask 는 Edit · Write 도구에만 걸린다. sed -i · 리다이렉션 · mv 로 고치면
# 승인 없이 지나간다. 이 훅이 그 우회로를 같은 ask 로 되돌린다.
#
# 무엇을 보나 — 명령 어딘가의 쓰기 기호가 아니라 write-target.sh 가 뽑은 "쓰기 대상"이 보호 경로인지.
# 그래야 2>&1 · heredoc 본문 · 읽기 인자의 경로에서 묻지 않는다 (INC-03). python -c 처럼
# 인터프리터 안에서 쓰는 경우는 못 잡는다 (docs/04-harness/harness.md 알려진 구멍).

payload=$(cat)
cmd=$(printf '%s' "$payload" | jq -r '.tool_input.command // empty')
[ -n "$cmd" ] || exit 0

protected='archunit-store|archunit\.properties|src/test/java/project/kjhjdh/ibid/architecture|build\.gradle|\.github/workflows|\.claude/settings\.json|\.claude/hooks|\.claude/agents'

hit=$(printf '%s' "$cmd" | sh "$(dirname "$0")/write-target.sh" | grep -E "$protected" | head -3 | tr '\n' ' ')
[ -n "$hit" ] || exit 0

reason="이 명령이 보호 파일에 씁니다: ${hit}
명령: ${cmd}
게이트 · 동결 기록 · 빌드 설정 · 훅 · 리뷰어 정의는 Edit 로 고쳐도 묻습니다 (ADR-0010). 이 티켓이 정말 이 파일을 바꾸는 것이면 승인하고, 아니면 거절하세요."
jq -n --arg reason "$reason" '{hookSpecificOutput: {hookEventName: "PreToolUse", permissionDecision: "ask", permissionDecisionReason: $reason}}'
