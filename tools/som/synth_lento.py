# SPDX-License-Identifier: Apache-2.0
# Copyright 2026 The LOMA Authors
# "Brilho lento": versão progressiva (~4,6 s) do som de abertura do LOMA.
import numpy as np
from scipy.io import wavfile
from scipy.signal import fftconvolve, butter, sosfilt
from synth import SR, t, env, bell, mallet, place

rng = np.random.default_rng(11)
TOTAL = 4.6
HIT = 2.10      # o "toque" principal (ponto menta aparece)
SHINE = 3.30    # reflexo de luz passando no nome
buf = np.zeros(int(SR * TOTAL))

# 1) Subida progressiva: ruído filtrado com corte subindo de 300 Hz a 5 kHz, crescendo até o toque
dur = HIT + 0.05
n = int(SR * dur); noise = rng.standard_normal(n)
out = np.zeros(n); block = 2048
for s in range(0, n, block):
    frac = s / n
    lo = 300 + 1800 * frac ** 2; hi = 900 + 4100 * frac ** 1.5
    sos = butter(2, [lo, hi], 'band', fs=SR, output='sos')
    out[s:s+block] = sosfilt(sos, noise[s:s+block])
rise = (0.04 + 0.96 * np.linspace(0, 1, n) ** 2) * np.exp(-np.maximum(0, t(dur) - HIT) / 0.02)
place(buf, out * rise, 0.0, 0.16)

# 2) Acorde de fundo (Lá3, Mi4, Lá4, Dó#5) entrando bem devagar, com leve tremolo; some depois do toque
pdur = 4.3; x = t(pdur)
pad = sum(a * np.sin(2*np.pi*f*x + 0.3*np.sin(2*np.pi*0.21*x)) for f, a in ((220, .5), (329.63, .35), (440, .3), (554.37, .18)))
penv = (0.06 + 0.94 * np.clip(x / HIT, 0, 1) ** 1.6) * np.where(x < HIT + 0.3, 1.0, np.exp(-(x - HIT - 0.3) / 0.7))
place(buf, pad * penv * (1 + 0.08*np.sin(2*np.pi*5.5*x)), 0.0, 0.16)

# 3) Cintilado que acelera perto do toque
for k in range(14):
    at = HIT - 1.3 * (0.82 ** k)
    place(buf, mallet(2637.0 * (1.5 if k % 2 else 1.0), 0.18, 0.035), at, 0.025 + 0.004 * k)

# 4) O toque: dois sinos (Lá5, Mi6) + grave que floresce + arpejo de faíscas
place(buf, np.sin(2*np.pi*110*t(1.6)) * env(int(SR*1.6), 0.05, 0.5), HIT - 0.02, 0.28)
place(buf, bell(880.0, 2.2, 0.55), HIT, 0.55)
place(buf, bell(1318.5, 2.0, 0.48), HIT + 0.07, 0.45)
for i, f in enumerate((1760.0, 2217.46, 2637.0, 3520.0)):
    place(buf, mallet(f, 0.4, 0.07), HIT + 0.14 + 0.07 * i, 0.10)

# 5) Brilho no nome: um "glint" suave e agudo
place(buf, bell(3520.0, 0.9, 0.18, partials=((1, 1.0), (2.0, .3))), SHINE, 0.10)
place(buf, bell(2637.0, 0.9, 0.2, partials=((1, 1.0), (2.0, .3))), SHINE + 0.06, 0.08)

# Reverb mais longo e final limpo
ir_len = 1.4
ir = rng.standard_normal(int(SR * ir_len)) * np.exp(-t(ir_len) / (ir_len / 5))
ir = sosfilt(butter(2, 7000, 'low', fs=SR, output='sos'), ir); ir /= np.abs(ir).sum() ** 0.5 * 8
buf = 0.68 * buf + 0.32 * fftconvolve(buf, ir)[: len(buf)]
fade = int(0.5 * SR); buf[-fade:] *= np.linspace(1, 0, fade) ** 2
buf /= np.max(np.abs(buf)); buf *= 10 ** (-3 / 20)
wavfile.write('loma-abertura-brilho-lento.wav', SR, (buf * 32767).astype(np.int16))
