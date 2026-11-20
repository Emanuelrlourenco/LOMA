<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 The LOMA Authors -->
# Som da abertura

`synth.py` e `synth_lento.py` sintetizam do zero o som "Brilho lento" (original, sem amostras de terceiros).

```bash
python3 synth_lento.py                      # gera loma-abertura-brilho-lento.wav
ffmpeg -i loma-abertura-brilho-lento.wav -c:a libvorbis -q:a 5 loma-abertura-brilho-lento.ogg
ffmpeg -i loma-abertura-brilho-lento.wav -c:a aac -b:a 160k loma-abertura-brilho-lento.m4a
```

Os arquivos `.ogg` (Android) e `.m4a` (iOS) vão para `shared/ui/src/commonMain/composeResources/files/`.
