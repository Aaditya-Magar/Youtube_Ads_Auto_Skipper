#!/bin/sh
set -eu
cd "$(dirname "$0")/.."
out=$(mktemp -d)
trap 'rm -rf "$out"' EXIT
javac -d "$out" app/src/main/java/io/github/adityamagar/autoskip/SkipRule.java tests/SkipRuleCheck.java
java -ea -cp "$out" com.ytadsskipper.app.SkipRuleCheck
