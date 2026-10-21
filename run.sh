#!/bin/bash
# 恩尼格玛模拟器一键启动:编译 + 自检 + 起 HTTP 服务
# 用法:./run.sh [端口]  默认 8080
set -e
cd "$(dirname "$0")"

echo "== 编译 =="
mkdir -p out
javac -encoding UTF-8 -d out $(find src -name '*.java')

echo "== 自检 =="
java -cp out com.enigma.EnigmaSelfTest

PORT="${1:-8080}"
echo "== 启动服务 http://localhost:${PORT} =="
exec java -cp out com.enigma.server.EnigmaServer "$PORT"
