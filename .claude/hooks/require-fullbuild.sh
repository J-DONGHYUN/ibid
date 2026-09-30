#!/bin/sh
# PR 을 만들기 전에, 현재 코드로 전체 빌드가 통과한 기록이 있는지 본다 (PreToolUse, gh pr create) (T-27).
# 기록은 record-fullbuild.sh 가 남긴다 — 이 훅은 그 기록만 본다.
#
# deny 가 아니라 ask 다. Docker 를 못 켜면 로컬 전체 빌드가 불가능한데(이 훅이 도는 환경도 그렇다),
# CI 가 PR 에서 전체 빌드를 최종적으로 돌린다. 그래서 실수로 빠뜨리는 건 막되, 의식적 우회는 사람이 승인한다.

payload=$(cat)
cmd=$(printf '%s' "$payload" | jq -r '.tool_input.command // empty')
[ -n "$cmd" ] || exit 0
printf '%s' "$cmd" | grep -qE 'gh[[:space:]]+pr[[:space:]]+create' || exit 0

cd "${CLAUDE_PROJECT_DIR:-.}" || exit 0
state_dir="$(git rev-parse --git-dir)/ibid-loop"
branch=$(git branch --show-current | tr '/' '_')
rec="$state_dir/${branch:-detached}.fullbuild-passed"
cur=$(sh "$(dirname "$0")/build-fingerprint.sh")

if [ -n "$cur" ] && [ "$cur" = "$(cat "$rec" 2>/dev/null)" ]; then
    exit 0
fi

reason="이 브랜치에서 지금 코드로 전체 빌드가 통과한 기록이 없다 (빌드 안 돌렸거나, 빌드 뒤 src·build.gradle 이 바뀌었다). PR 전에 전체 테스트를 돌린다: AWS_ACCESS_KEY=test AWS_SECRET_KEY=test ./gradlew build (Docker 필요). Docker 를 못 켜는 등 로컬 빌드가 불가능하면, CI 가 PR 에서 전체 빌드를 돌리는 걸 알고 승인해도 된다."
jq -n --arg reason "$reason" '{hookSpecificOutput: {hookEventName: "PreToolUse", permissionDecision: "ask", permissionDecisionReason: $reason}}'
