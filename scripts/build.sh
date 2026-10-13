#!/usr/bin/env bash
# Biên dịch bằng javac (JDK 17+), không cần Maven. Kết quả: out/classes
set -e
cd "$(dirname "$0")/.."
rm -rf out/classes && mkdir -p out/classes
javac -encoding UTF-8 --release 17 -d out/classes $(find src/main/java -name '*.java')
echo "Biên dịch xong: out/classes"
