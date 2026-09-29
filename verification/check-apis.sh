#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
test -f target/speedrunnerswap-4.3.7.jar || { echo 'Build the release JAR first with mvn clean verify.' >&2; exit 1; }
while IFS= read -r version || [[ -n "$version" ]]; do
    [[ -z "$version" || "$version" == \#* ]] && continue
    mvn --batch-mode -q -f verification/pom.xml "-Dpaper.version=$version" verify
done < verification/api-versions.txt
