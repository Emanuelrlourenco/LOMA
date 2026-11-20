#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
# Copyright 2026 The LOMA Authors
#
# Confere se todo arquivo-fonte começa com as duas linhas de cabeçalho:
#   <comentário> SPDX-License-Identifier: Apache-2.0
#   <comentário> Copyright 2026 The LOMA Authors
# Uso: tools/ci/check-spdx.sh <pasta>
# Sai 0 se todos têm o cabeçalho; sai 1 e lista os que faltam.
set -euo pipefail

root="${1:-.}"
fixtures_dir="tools/ci/fixtures"
missing=0

is_root_call=false
if [ "$(cd "$root" && pwd)" = "$(pwd)" ]; then is_root_call=true; fi

while IFS= read -r -d '' file; do
  rel="${file#./}"
  if $is_root_call; then
    case "$rel" in
      "$fixtures_dir"/*|docs/*|*/build/*|build/*|.gradle/*|*/.gradle/*|.superpowers/*|iosApp/*.xcodeproj/*) continue ;;
    esac
  fi
  # Pula a linha "#!" de scripts antes de procurar o cabeçalho.
  header="$(grep -v '^#!' "$file" | head -n 2 || true)"
  line1="$(printf '%s\n' "$header" | sed -n 1p)"
  line2="$(printf '%s\n' "$header" | sed -n 2p)"
  if [[ "$line1" != *"SPDX-License-Identifier: Apache-2.0"* || "$line2" != *"Copyright 2026 The LOMA Authors"* ]]; then
    echo "sem cabeçalho SPDX: $rel"
    missing=1
  fi
done < <(cd "$root" && find . -type f \( -name '*.kt' -o -name '*.kts' -o -name '*.swift' -o -name '*.sh' -o -name '*.yml' -o -name '*.yaml' \) -print0 | sed -z 's#^\./##' | while IFS= read -r -d '' f; do printf '%s\0' "$root/$f"; done)

if [ "$missing" -ne 0 ]; then
  exit 1
fi
echo "cabeçalhos SPDX ok"
