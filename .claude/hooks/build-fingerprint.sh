#!/bin/sh
# 빌드에 영향 주는 파일의 tree hash 를 찍는다 — record-fullbuild · require-fullbuild 가 같이 쓴다 (T-27).
# 문서 · 백로그만 바뀌면 지문이 그대로라, 빌드 뒤 리뷰 반영 · ✅ 커밋이 PR 을 막지 않는다.
cd "${CLAUDE_PROJECT_DIR:-.}" || exit 0
state_dir="$(git rev-parse --git-dir)/ibid-loop"
mkdir -p "$state_dir"
tmp="$state_dir/fp.index.$$"
empty=$(git hash-object -t tree /dev/null)
GIT_INDEX_FILE="$tmp" git read-tree "$empty" 2>/dev/null
GIT_INDEX_FILE="$tmp" git add -A -- src build.gradle settings.gradle gradle gradlew gradlew.bat 2>/dev/null
GIT_INDEX_FILE="$tmp" git write-tree 2>/dev/null
rm -f "$tmp"
