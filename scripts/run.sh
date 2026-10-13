#!/usr/bin/env bash
# Chạy ứng dụng. Cần: out/classes (scripts/build.sh), lib/mssql-jdbc-*.jar, config/db.properties
cd "$(dirname "$0")/.."
java -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -cp "out/classes:lib/*" vn.n08.nhakhoa.App "$@"
