#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
# Copyright 2026 The LOMA Authors
#
# Garante que o LOMA não tem código de rede nem permissão de internet.
# Uso: tools/ci/check-no-network.sh <pasta-de-código> [manifest-mesclado.xml]
# Sai 1 (e mostra a linha culpada) se achar uso de rede no código ou INTERNET no manifest.
set -euo pipefail

root="${1:?uso: check-no-network.sh <pasta> [manifest]}"
manifest="${2:-}"
found=0

pattern='java\.net\.|HttpURLConnection|okhttp|io\.ktor|retrofit|URLSession|NSURLSession|NSURLConnection|CFNetwork'

is_root_call=false
if [ "$(cd "$root" && pwd)" = "$(pwd)" ]; then is_root_call=true; fi

while IFS= read -r -d '' file; do
  rel="${file#./}"
  if $is_root_call; then
    case "$rel" in
      tools/ci/fixtures/*|docs/*|*/build/*|build/*|.gradle/*|*/.gradle/*|.superpowers/*) continue ;;
    esac
  fi
  if hits="$(grep -nE "$pattern" "$file")"; then
    while IFS= read -r line; do echo "uso de rede: $rel:$line"; done <<< "$hits"
    found=1
  fi
done < <(cd "$root" && find . -type f \( -name '*.kt' -o -name '*.kts' -o -name '*.java' -o -name '*.swift' -o -name '*.m' -o -name '*.h' \) -print0 | sed -z 's#^\./##' | while IFS= read -r -d '' f; do printf '%s\0' "$root/$f"; done)

if [ -n "$manifest" ]; then
  if hits="$(grep -n 'android.permission.INTERNET' "$manifest")"; then
    echo "permissão de internet no manifest: $manifest:$hits"
    found=1
  fi
fi

if [ "$found" -ne 0 ]; then exit 1; fi
echo "sem código de rede${manifest:+ e sem INTERNET no manifest}"
