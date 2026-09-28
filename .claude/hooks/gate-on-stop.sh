#!/bin/sh
# 에이전트가 끝내려 할 때 게이트를 돌리고, 실패하면 끝내지 못하게 한다.
#
# "코드를 바꿨으면 게이트를 돌린다" 를 부탁에서 설정으로 바꾼다. 실패 메시지는 그대로 에이전트에게
# 돌려준다 — 그 메시지가 다음 수정의 컨텍스트다.
#
# - "무엇이 바뀌었나" 는 main 과 비교한 이 브랜치의 커밋 + 커밋 안 된 변경으로 본다. git status 만 보면
#   계획 한 줄마다 커밋하는 루프에서 작업 트리가 늘 깨끗해 게이트를 한 번도 못 돈다 (INC-02)
# - src/ · build.gradle 이 바뀌지 않았으면 돌리지 않는다. 문서만 고친 작업을 기다리게 하지 않는다
# - 같은 코드 상태(src/ · build.gradle 트리 해시)로 이미 통과했으면 다시 돌리지 않는다
# - 빠른 게이트(architectureTest)만 돈다. 전체 테스트는 Docker 가 필요하고 1분이 넘어서 PR 전에 한 번 돈다
# - 3번 연속 실패하면 더 막지 않고 사람을 부른다. 끝없이 고치는 루프를 막는다
# - 결과를 .git/ibid-loop/<브랜치>.log 에 남긴다. PR 의 루프 기록이 이걸 읽는다

MAX_CONSECUTIVE_FAILURES=3
BASE_BRANCH=main

cat > /dev/null
cd "${CLAUDE_PROJECT_DIR:-.}" || exit 0

{ git diff --name-only "$BASE_BRANCH...HEAD" 2>/dev/null; git status --porcelain | cut -c4-; } \
    | grep -qE '^(src/|build\.gradle)' || exit 0

state_dir="$(git rev-parse --git-dir)/ibid-loop"
mkdir -p "$state_dir"
branch=$(git branch --show-current | tr '/' '_')
fails_file="$state_dir/${branch:-detached}.fails"
passed_file="$state_dir/${branch:-detached}.passed"
log_file="$state_dir/${branch:-detached}.log"
now=$(date '+%Y-%m-%dT%H:%M:%S')

code_hash=$(
    tmp_index="$state_dir/gate.index.$$"
    empty_tree=$(git hash-object -t tree /dev/null)
    GIT_INDEX_FILE="$tmp_index" git read-tree "$empty_tree" 2>/dev/null
    GIT_INDEX_FILE="$tmp_index" git add -A -- src build.gradle 2>/dev/null
    GIT_INDEX_FILE="$tmp_index" git write-tree 2>/dev/null
    rm -f "$tmp_index"
)

if [ -n "$code_hash" ] && [ "$code_hash" = "$(cat "$passed_file" 2>/dev/null)" ]; then
    echo "$now SKIP $code_hash" >> "$log_file"
    exit 0
fi

if ./gradlew architectureTest -q > "$state_dir/last-gate.out" 2>&1; then
    rm -f "$fails_file"
    [ -n "$code_hash" ] && printf '%s\n' "$code_hash" > "$passed_file"
    echo "$now PASS $code_hash" >> "$log_file"
    exit 0
fi

fails=$(( $(cat "$fails_file" 2>/dev/null || echo 0) + 1 ))
echo "$fails" > "$fails_file"
echo "$now FAIL $fails" >> "$log_file"

summary=$(python3 - <<'EOF'
import glob, xml.etree.ElementTree as ET
lines = []
for path in sorted(glob.glob('build/test-results/architectureTest/*.xml')):
    for case in ET.parse(path).getroot().iter('testcase'):
        failure = case.find('failure')
        if failure is None:
            continue
        message = (failure.get('message') or '').replace('project.kjhjdh.ibid.', '')
        title = f"- {case.get('classname').split('.')[-1]} > {case.get('name')}"
        if "' was violated" in message:
            rule, _, rest = message.partition("' was violated")
            because = rule.split(', because ', 1)[1] if ', because ' in rule else ''
            count = rest.split(':', 1)[0].strip()
            violations = [v for v in rest.split('\n')[1:] if v.strip()][:3]
            lines.append(f"{title} {count}")
            if because:
                lines.append(f"  이유: {because}")
            lines.extend(f"  위반: {v.strip()[:200]}" for v in violations)
        else:
            lines.append(f"{title}: {message[:400]}")
print('\n'.join(lines) if lines else '- 컴파일 실패일 수 있다. .git/ibid-loop/last-gate.out 을 본다')
EOF
)

if [ "$fails" -ge "$MAX_CONSECUTIVE_FAILURES" ]; then
    rm -f "$fails_file"
    echo "$now STOP_FOR_HUMAN" >> "$log_file"
    jq -n --arg msg "[루프 멈춤] 게이트가 ${fails}번 연속 실패해 더 막지 않는다. 사람이 봐야 한다 — 방향이 틀렸는지, 규칙이 틀렸는지 판단하고 docs/04-harness/incidents.md 에 기록한다.
$summary" '{systemMessage: $msg}'
    exit 0
fi

jq -n --arg reason "게이트 실패 (${fails}/${MAX_CONSECUTIVE_FAILURES}). 끝내기 전에 고친다. 규칙이나 동결 기록을 고쳐 통과시키지 않는다 — 코드를 고친다.
$summary" '{decision: "block", reason: $reason}'
exit 0
