#!/usr/bin/env bash
# Sincroniza los .proto que ms-iam consume desde ms-school-management.
#
# El .proto es el contrato entre servicios, pero Java y .NET no comparten el
# artefacto generado: cada servicio corre su propio generador. Este script copia
# el archivo para que ambos compilen contra la misma definicion.
#
# Uso: ./sync-protos.sh   (desde backend/ms-iam)
set -euo pipefail

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SOURCE="$HERE/../ms-school-management/src/ms-school-management.Api/Protos"
TARGET="$HERE/src/main/proto"

mkdir -p "$TARGET"
for proto in "$SOURCE"/*.proto; do
  name="$(basename "$proto")"
  if [ ! -f "$TARGET/$name" ] || ! cmp -s "$proto" "$TARGET/$name"; then
    cp "$proto" "$TARGET/$name"
    echo "sincronizado: $name"
  else
    echo "sin cambios:   $name"
  fi
done