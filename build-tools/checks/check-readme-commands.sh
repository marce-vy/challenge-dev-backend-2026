#!/bin/sh
set -eu

script_dir=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
repo_root=$(CDPATH= cd -- "$script_dir/../.." && pwd)
cd "$repo_root"

if [ ! -f README.md ]; then
	echo "check-readme-commands: skipped (README.md not present yet)"
	exit 0
fi

if ! command -v rg >/dev/null 2>&1; then
	echo "check-readme-commands: rg is required"
	exit 2
fi

target_file=$(mktemp "${TMPDIR:-/tmp}/make-targets.XXXXXX")
readme_file=$(mktemp "${TMPDIR:-/tmp}/readme-targets.XXXXXX")
trap 'rm -f "$target_file" "$readme_file"' EXIT

awk '/^[A-Za-z0-9_.-]+:/ { target = $1; sub(/:.*/, "", target); print target }' Makefile | sort -u >"$target_file"

rg -o 'make[[:space:]]+[A-Za-z0-9_.-]+' README.md |
	awk '{ print $NF }' |
	sort -u >"$readme_file" || true

failed=0
while IFS= read -r target; do
	[ -n "$target" ] || continue
	if ! grep -qx "$target" "$target_file"; then
		echo "check-readme-commands: README references missing Makefile target: make $target"
		failed=1
	fi
done <"$readme_file"

if [ "$failed" -ne 0 ]; then
	exit 1
fi

echo "check-readme-commands: pass"
