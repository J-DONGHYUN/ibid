#!/bin/sh
# 전체 빌드(gradlew build)가 성공하면 그 코드의 빌드 지문을 기록한다 (PostToolUse).
# require-fullbuild.sh 가 PR 을 만들기 전에 이 기록을 본다 (T-27).
#
# 빠른 게이트(architectureTest)는 구조만 본다. 기능 테스트가 깨진 채 PR 이 올라가는 걸 막으려면
# 전체 빌드가 돌았는지 알아야 하는데, 훅은 그걸 직접 못 돌린다(Docker · 1분+). 그래서 빌드가 돌 때
# 결과를 남겨 두고, PR 직전에 그 기록을 확인한다.

payload=$(cat)
cmd=$(printf '%s' "$payload" | jq -r '.tool_input.command // empty')

# gradlew 의 build 태스크인가 (buildHealth 등은 아니다 — 공백으로 둘러싼 build 토큰만)
printf '%s' "$cmd" | grep -q gradlew || exit 0
case " $cmd " in *" build "*) ;; *) exit 0 ;; esac

# 성공했는가 — gradle 은 성공 시 BUILD SUCCESSFUL 을 찍는다. 페이로드 모양(문자열·객체)에 견디게 뽑는다.
# type 을 먼저 본다 — 문자열에 .stdout 을 접근하면 jq 가 에러를 내고 // 가 못 잡아 폴백이 무너진다.
out=$(printf '%s' "$payload" | jq -r '
    (.tool_response
     | if type=="string" then .
       else ((.stdout // "") + "\n" + (.stderr // "")) end)' 2>/dev/null)
printf '%s' "$out" | grep -q 'BUILD SUCCESSFUL' || exit 0

cd "${CLAUDE_PROJECT_DIR:-.}" || exit 0
state_dir="$(git rev-parse --git-dir)/ibid-loop"
mkdir -p "$state_dir"
branch=$(git branch --show-current | tr '/' '_')
fp=$(sh "$(dirname "$0")/build-fingerprint.sh")
[ -n "$fp" ] && printf '%s\n' "$fp" > "$state_dir/${branch:-detached}.fullbuild-passed"
exit 0
