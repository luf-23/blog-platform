#!/usr/bin/env bash

# This file may be sourced by the official MySQL image when the executable bit
# is not preserved. Keep all shell options and variables inside a subshell.
(
    set -Eeuo pipefail

    dump_file=/docker-entrypoint-initdb.d/02_export_data.sql.raw

    : "${MYSQL_ROOT_PASSWORD:?MYSQL_ROOT_PASSWORD is required}"
    : "${MYSQL_DATABASE:?MYSQL_DATABASE is required}"

    if [[ ! -r "$dump_file" ]]; then
        echo "Database dump is not readable: $dump_file" >&2
        exit 1
    fi

    echo "Importing database dump with mysql binary mode"
    export MYSQL_PWD="$MYSQL_ROOT_PASSWORD"

    mysql \
        --binary-mode \
        --protocol=socket \
        --default-character-set=utf8mb4 \
        --user=root \
        --database="$MYSQL_DATABASE" \
        < "$dump_file"
)
