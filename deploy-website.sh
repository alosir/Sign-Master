#!/usr/bin/env bash
# 部署官网到云服务器 sign.alosir.com
# 官网源码：site/index.html + site/logo.png
# 需要本机已安装并配置 workbench CLI（~/.workbench/config.json，参见 BUILD.md「官网部署」）
# 用法：bash deploy-website.sh
set -e

export MSYS_NO_PATHCONV=1
INSTANCE="i-0jl74tl0h58wis1uui3i"
REMOTE_DIR="/var/www/sign"
cd "$(dirname "$0")"

sync_file() {
    local file="$1"
    local local_md5 remote_md5
    local_md5=$(md5sum "site/$file" | awk '{print $1}')
    remote_md5=$(workbench exec --instance-id "$INSTANCE" --command "md5sum $REMOTE_DIR/$file 2>/dev/null | awk '{print \$1}'" --timeout 30 2>/dev/null | tr -d '[:space:]')
    if [ "$local_md5" = "$remote_md5" ]; then
        echo "skip $file（远端一致）"
    else
        echo "upload $file ..."
        workbench upload "site/$file" "$REMOTE_DIR/$file" --instance-id "$INSTANCE" --force
    fi
}

sync_file index.html
sync_file logo.png
echo "完成：https://sign.alosir.com"
