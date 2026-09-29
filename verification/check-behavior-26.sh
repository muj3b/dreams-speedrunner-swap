#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
# Run with JDK 25 AFTER the JDK 21 clean verify. Never rebuild main classes against 26.
jar=target/speedrunnerswap-4.3.7.jar
test -f "$jar"
before=$(shasum -a 256 "$jar")
classes_before=$(find target/classes -type f -exec shasum -a 256 {} \; | sort)
for release in 26.1.2 26.2; do
  if [[ "$release" == 26.1.2 ]]; then
    api=26.1.2.build.74-stable; mock=4.115.0; junit=6.1.2
  else
    api=26.2.build.129-stable; mock=4.116.1; junit=6.1.3
  fi
  mvn --batch-mode -Ptest-26 "-Dpaper.version=$api" "-Dtest.paper.version=$api" \
    "-Dmockbukkit.artifact=mockbukkit-v$release" "-Dmockbukkit.version=$mock" \
    "-Djunit.platform.version=$junit" "-Dtest.reportsDirectory=$PWD/target/reports-$release" \
    compiler:testCompile surefire:test
done
test "$before" = "$(shasum -a 256 "$jar")"
test "$classes_before" = "$(find target/classes -type f -exec shasum -a 256 {} \; | sort)"
echo "PASS: release JAR and main classes unchanged by 26.x behavior tests"
