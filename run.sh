#!/bin/bash
# 恩尼格玛模拟器一键启动:Maven 构建 + 测试 + 启动服务
# 用法:./run.sh [端口]  默认 8080
set -e
cd "$(dirname "$0")"

echo "== 构建 =="
mvn -B -q package -DskipTests=false

PORT="${1:-8080}"
echo "== 启动服务 http://localhost:${PORT} =="
exec java -cp "target/enigma.jar:web" com.enigma.server.EnigmaServer "$PORT"
