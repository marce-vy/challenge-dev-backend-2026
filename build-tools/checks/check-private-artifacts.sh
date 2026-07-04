#!/bin/sh
set -eu

script_dir=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
repo_root=$(CDPATH= cd -- "$script_dir/../.." && pwd)
cd "$repo_root"

if ! git rev-parse --is-inside-work-tree >/dev/null 2>&1; then
	echo "check-private-artifacts: git repository is required"
	exit 2
fi

failed=0

tracked_private=$(git ls-files '.ai/**' '.codex/**' '.agents/**' '.specify/**' 'scripts/**' '*.jfr' '*.hprof' '.env' '.env.*' 2>/dev/null || true)
if [ -n "$tracked_private" ]; then
	echo "check-private-artifacts: private or generated artifacts are tracked:"
	echo "$tracked_private"
	failed=1
fi

if find . -name '*.jfr' -o -name '*.hprof' -o -name '.env' -o -name '.env.*' | grep -q .; then
	echo "check-private-artifacts: local sensitive/profiling files are present; keep them out of commits"
	find . -name '*.jfr' -o -name '*.hprof' -o -name '.env' -o -name '.env.*'
	failed=1
fi

if [ "$failed" -ne 0 ]; then
	exit 1
fi

echo "check-private-artifacts: pass"
