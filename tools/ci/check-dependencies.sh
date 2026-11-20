#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
# Copyright 2026 The LOMA Authors
#
# Confere se toda dependência resolvida do app está na lista permitida.
# Uso: tools/ci/check-dependencies.sh <arquivo com uma dependência grupo:artefato[:versão] por linha>
# Sai 1 se alguma não casar com um prefixo de tools/ci/allowed-dependencies.txt, ou se for bloqueada.
set -euo pipefail

deps="${1:?uso: check-dependencies.sh <arquivo>}"
here="$(cd "$(dirname "$0")" && pwd)"
allowed="$here/allowed-dependencies.txt"
blocked=(com.google.firebase com.google.android.gms io.ktor com.squareup.okhttp3 com.squareup.retrofit2)
bad=0
count=0

while IFS= read -r line; do
  dep="$(printf '%s' "$line" | tr -d '[:space:]' | cut -d: -f1,2)"
  [ -z "$dep" ] && continue
  case "$dep" in \#*) continue ;; esac
  count=$((count + 1))
  for b in "${blocked[@]}"; do
    if [[ "$dep" == "$b"* ]]; then echo "dependência bloqueada: $dep"; bad=1; continue 2; fi
  done
  ok=false
  while IFS= read -r prefix; do
    prefix="$(printf '%s' "$prefix" | tr -d '[:space:]')"
    [ -z "$prefix" ] && continue
    case "$prefix" in \#*) continue ;; esac
    if [[ "$dep" == "$prefix"* ]]; then ok=true; break; fi
  done < "$allowed"
  if ! $ok; then echo "dependência fora da lista permitida: $dep"; bad=1; fi
done < "$deps"

if [ "$bad" -ne 0 ]; then exit 1; fi
echo "$count dependências, todas permitidas"
