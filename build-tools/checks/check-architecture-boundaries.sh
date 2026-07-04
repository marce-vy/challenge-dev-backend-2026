#!/bin/sh
set -eu

script_dir=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
repo_root=$(CDPATH= cd -- "$script_dir/../.." && pwd)
cd "$repo_root"

if ! command -v rg >/dev/null 2>&1; then
	echo "check-architecture-boundaries: rg is required"
	exit 2
fi

if [ ! -d src/main/java ]; then
	echo "check-architecture-boundaries: skipped (src/main/java not present yet)"
	exit 0
fi

tmp_file=$(mktemp "${TMPDIR:-/tmp}/architecture-boundaries.XXXXXX")
trap 'rm -f "$tmp_file"' EXIT

failed=0

if find src/main/java -path '*/domain/*' -type f -name '*.java' | grep -q .; then
	if rg -n '^\s*import\s+(org\.springframework|jakarta\.persistence|javax\.persistence|org\.hibernate|java\.sql)\.' src/main/java --glob '*/domain/*.java' >"$tmp_file"; then
		echo "check-architecture-boundaries: domain imports framework or persistence types:"
		cat "$tmp_file"
		failed=1
	fi
fi

if find src/main/java -path '*/application/*' -type f -name '*.java' | grep -q .; then
	if rg -n '^\s*import\s+(org\.springframework\.web|jakarta\.persistence|javax\.persistence|org\.hibernate)\.' src/main/java --glob '*/application/*.java' >"$tmp_file"; then
		echo "check-architecture-boundaries: application imports web or persistence types:"
		cat "$tmp_file"
		failed=1
	fi
fi

if [ "$failed" -ne 0 ]; then
	exit 1
fi

echo "check-architecture-boundaries: pass"
