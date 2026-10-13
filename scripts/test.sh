#!/usr/bin/env bash
# Kiểm thử tự động trên CSDL THAY THẾ Apache Derby (không phải SQL Server).
# Cần lib/test/derby-10.14.2.0.jar. Giao diện: cần màn hình (hoặc xvfb-run trên Linux).
set -e
cd "$(dirname "$0")/.."
rm -rf out/test && mkdir -p out/test
javac -encoding UTF-8 --release 17 -cp "lib/test/*" -d out/test $(find src/main/java src/test/java -name '*.java')
java -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -cp "out/test:lib/test/*" vn.n08.nhakhoa.KiemThuTichHop
if [ "$1" = "--giao-dien" ]; then
  java -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -cp "out/test:lib/test/*" vn.n08.nhakhoa.KiemThuGiaoDien out/anh-giao-dien
fi
