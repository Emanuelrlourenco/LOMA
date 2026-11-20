# SPDX-License-Identifier: Apache-2.0
# Copyright 2026 The LOMA Authors
# Sons de abertura originais do LOMA, sintetizados do zero.
import numpy as np
from scipy.io import wavfile
from scipy.signal import fftconvolve, butter, sosfilt

SR = 48000
rng = np.random.default_rng(7)

def t(sec): return np.arange(int(SR * sec)) / SR

def env(n, attack, decay):
    x = np.arange(n) / SR
    a = np.clip(x / max(attack, 1e-4), 0, 1)
    return a * np.exp(-x / decay)

def bell(freq, dur, decay, partials=((1, 1.0), (2.76, 0.35), (5.40, 0.12), (8.93, 0.05))):
    x = t(dur); out = np.zeros_like(x)
    for ratio, amp in partials:
        out += amp * np.sin(2 * np.pi * freq * ratio * x) * np.exp(-x * ratio / (decay * 1.2))
    return out * env(len(x), 0.004, decay * 3)

def mallet(freq, dur, decay):
    x = t(dur)
    return (np.sin(2*np.pi*freq*x) + 0.25*np.sin(2*np.pi*freq*4*x)*np.exp(-x/0.05)) * env(len(x), 0.002, decay)

def place(buf, sig, at, gain):
    i = int(at * SR); j = min(len(buf), i + len(sig)); buf[i:j] += gain * sig[: j - i]

def reverb(sig, length=0.7, mix=0.28):
    ir = rng.standard_normal(int(SR * length)) * np.exp(-t(length) / (length / 5))
    sos = butter(2, 6000, 'low', fs=SR, output='sos'); ir = sosfilt(sos, ir); ir /= np.abs(ir).sum() ** 0.5 * 8
    wet = fftconvolve(sig, ir)[: len(sig)]
    return (1 - mix) * sig + mix * wet

def finish(buf, name):
    buf = reverb(buf)
    fade = int(0.12 * SR); buf[-fade:] *= np.linspace(1, 0, fade)
    buf /= np.max(np.abs(buf)); buf *= 10 ** (-3 / 20)
    wavfile.write(f'{name}.wav', SR, (buf * 32767).astype(np.int16))

if __name__ == "__main__":
    # A — "Brilho": swell suave + dois sinos (Lá5, Mi6) + faíscas agudas. ~1,1 s
    total = 1.15; a = np.zeros(int(SR * total))
    noise = rng.standard_normal(int(SR * 0.32)); sos = butter(2, [800, 5000], 'band', fs=SR, output='sos')
    sw = sosfilt(sos, noise) * np.linspace(0, 1, len(noise)) ** 2 * np.exp(-np.maximum(0, t(0.32) - 0.24) / 0.03)
    place(a, sw, 0.0, 0.10)
    place(a, np.sin(2*np.pi*220*t(1.0)) * env(int(SR*1.0), 0.12, 0.35), 0.10, 0.20)
    place(a, bell(880.0, 0.95, 0.30), 0.20, 0.55)
    place(a, bell(1318.5, 0.85, 0.26), 0.27, 0.45)
    place(a, mallet(2637.0, 0.25, 0.05), 0.31, 0.12)
    place(a, mallet(3520.0, 0.22, 0.04), 0.36, 0.09)
    finish(a, 'loma-abertura-brilho')

    # B — "Pétala": duas notas de marimba subindo (Mi5 → Si5) com acorde macio. ~1,0 s
    total = 1.0; b = np.zeros(int(SR * total))
    place(b, mallet(659.25, 0.6, 0.16), 0.05, 0.6)
    place(b, mallet(987.77, 0.8, 0.22), 0.17, 0.7)
    for f in (329.63, 493.88, 659.25):
        place(b, np.sin(2*np.pi*f*t(0.8)) * env(int(SR*0.8), 0.06, 0.25), 0.17, 0.10)
    place(b, bell(1975.5, 0.5, 0.12), 0.24, 0.10)
    finish(b, 'loma-abertura-petala')
