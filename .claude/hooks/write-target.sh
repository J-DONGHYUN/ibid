#!/bin/sh
# 명령(stdin)에서 "쓰기 대상" 경로만 한 줄씩 출력한다. guard-protected · guard-plan 이 함께 쓴다.
#
# 왜 — 두 훅이 "명령 어딘가에 쓰기 기호"와 "명령 어딘가에 경로"를 따로 찾으면 2>&1 · heredoc 본문 ·
# grep 인자의 경로에서 오탐이 난다 (INC-03). 무엇에 쓰는지 보려면 쓰기 대상을 뽑아야 한다.
#
# 뽑는 것 — 리다이렉션(> · >> · n>)의 파일, cp/mv 의 도착지(마지막 인자), rm · tee · sed -i ·
# truncate · patch · ln · install · git rm/checkout/restore/mv 의 경로 인자, dd 의 of=.
# 대상이 아닌 것 — fd 리다이렉션(2>&1 · >&2), heredoc 본문, 읽기 명령의 인자.
# 애매하면 더 뽑는 쪽(ask)으로 기운다 — 막는 경로가 줄어드는 미탐이 오탐보다 위험하다.

set -f
cmd=$(cat)

# 1. heredoc 본문을 지운다 — 본문 속 단어를 대상으로 오인하지 않도록. <<['"]?WORD ... WORD
stripped=$(printf '%s\n' "$cmd" | awk '
  skip == 1 { if ($0 ~ endre) skip = 0; next }
  {
    if (match($0, /<<-?[[:space:]]*["'\'']?[A-Za-z_][A-Za-z0-9_]*/)) {
      w = substr($0, RSTART, RLENGTH)
      gsub(/^<<-?[[:space:]]*["'\'']?/, "", w)
      endre = "^[[:space:]]*" w "[[:space:]]*$"
      skip = 1
    }
    print
  }
')

emit() {   # 따옴표를 벗기고 비어있지 않으면 출력
  t=$1
  t=${t#\"}; t=${t%\"}; t=${t#\'}; t=${t%\'}
  [ -n "$t" ] && printf '%s\n' "$t"
}

# 2. 리다이렉션 대상 — ([digit]|&)>>?|? 뒤의 파일. &fd(2>&1 · >&2)는 char class 가 & 를 빼서 안 걸린다
printf '%s\n' "$stripped" \
  | grep -oE '([0-9]*|&)>>?\|?[[:space:]]*("[^"]*"|'\''[^'\'']*'\''|[^[:space:]|&;<>()]+)' \
  | sed -E 's/^([0-9]*|&)>>?\|?[[:space:]]*//' \
  | while IFS= read -r t; do emit "$t"; done

# 3. 쓰기 명령의 경로 인자 — 구분자로 조각을 나눠 조각마다 첫 낱말을 본다
printf '%s\n' "$stripped" \
  | sed -E 's/(\|\||&&|\||;|&)/\
/g' \
  | while IFS= read -r seg; do
      set -- $(printf '%s' "$seg" | tr -s ' \t' '\n')
      [ $# -ge 1 ] || continue
      c=$1; shift
      case "$c" in
        cp|mv)
          dst=""
          for a in "$@"; do case "$a" in -*) ;; *) dst=$a ;; esac; done
          emit "$dst" ;;
        rm|tee|truncate|patch|ln|install)
          for a in "$@"; do case "$a" in -*) ;; *) emit "$a" ;; esac; done ;;
        sed)
          echo " $* " | grep -qE ' -i' || continue
          for a in "$@"; do case "$a" in -*) ;; *) emit "$a" ;; esac; done ;;
        dd)
          for a in "$@"; do case "$a" in of=*) emit "${a#of=}" ;; esac; done ;;
        git)
          sub=$1
          case "$sub" in
            rm|checkout|restore|mv)
              shift
              for a in "$@"; do case "$a" in -*) ;; *) emit "$a" ;; esac; done ;;
          esac ;;
      esac
    done
