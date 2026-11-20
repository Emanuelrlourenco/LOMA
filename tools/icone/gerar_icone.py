# SPDX-License-Identifier: Apache-2.0
# Copyright 2026 The LOMA Authors
"""Gera o ícone 6B "O menta" do LOMA a partir dos contornos da Outfit ExtraBold.

Desenho aprovado (canvas, IconeAssinatura variante "o-menta"): caixa 108×108 marinho #17165C,
texto "loma" centrado em x=54, linha de base y=62, Outfit 800 tamanho 29, espaçamento -0,8,
l/m/a brancos e o "o" menta #5ED6B4.

Saídas:
- Android (ícone adaptativo): o launcher mostra só os 72 do centro dos 108, então o desenho é
  reduzido a 2/3 em volta do centro para aparecer igual ao canvas.
- iOS / referência: tools/icone/loma-icone.svg com o desenho em 108 sem redução.

- iOS: PNG 1024×1024 sem transparência em iosApp/iosApp/Assets.xcassets/AppIcon.appiconset/.

Uso: python3 tools/icone/gerar_icone.py   (precisa de fonttools, uharfbuzz, cairosvg e Pillow)
"""
import os
import sys

import uharfbuzz as hb
from fontTools.pens.svgPathPen import SVGPathPen
from fontTools.pens.transformPen import TransformPen
from fontTools.ttLib import TTFont

RAIZ = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
FONTE = os.path.join(RAIZ, "shared/ui/src/commonMain/composeResources/font/outfit_extrabold.ttf")
RES = os.path.join(RAIZ, "androidApp/src/main/res")
SVG = os.path.join(RAIZ, "tools/icone/loma-icone.svg")
PNG_IOS = os.path.join(RAIZ, "iosApp/iosApp/Assets.xcassets/AppIcon.appiconset/AppIcon-1024.png")

MARINHO, MENTA, BRANCO = "#17165C", "#5ED6B4", "#FFFFFF"
TEXTO, TAMANHO, ESPACO, CENTRO_X, BASE_Y = "loma", 29.0, -0.8, 54.0, 62.0
ANDROID_ESCALA = 72.0 / 108.0

CABECALHO_XML = (
    "<!-- SPDX-License-Identifier: Apache-2.0 -->\n"
    "<!-- Copyright 2026 The LOMA Authors -->\n"
    "<!-- Gerado por tools/icone/gerar_icone.py; não editar à mão. -->\n"
)


def num(v):
    s = f"{v:.2f}".rstrip("0").rstrip(".")
    return "0" if s in ("-0", "") else s


def letras():
    """Posições x (em unidades da caixa 108) de cada letra, com o kerning da fonte via HarfBuzz."""
    dados = open(FONTE, "rb").read()
    face = hb.Face(dados)
    font = hb.Font(face)
    buf = hb.Buffer()
    buf.add_str(TEXTO)
    buf.guess_segment_properties()
    hb.shape(font, buf, {"kern": True, "liga": False})
    upem = face.upem
    escala = TAMANHO / upem
    cursor, itens = 0.0, []
    for info, pos in zip(buf.glyph_infos, buf.glyph_positions):
        itens.append((info.codepoint, cursor + pos.x_offset * escala))
        cursor += pos.x_advance * escala + ESPACO
    inicio = CENTRO_X - cursor / 2.0
    return [(gid, inicio + x) for gid, x in itens], escala


def caminhos(k):
    """pathData de cada letra, reduzida pelo fator k em volta do centro (54, 54)."""
    tt = TTFont(FONTE)
    ordem = tt.getGlyphOrder()
    gs = tt.getGlyphSet()
    posicoes, escala = letras()
    saida = []
    for (gid, x), letra in zip(posicoes, TEXTO):
        pen = SVGPathPen(gs, ntos=num)
        # unidades da fonte → caixa 108 (y para baixo) → redução k em volta do centro.
        t = (k * escala, 0, 0, -k * escala, 54 + k * (x - 54), 54 + k * (BASE_Y - 54))
        gs[ordem[gid]].draw(TransformPen(pen, t))
        saida.append((letra, pen.getCommands()))
    return saida


def vetor(paths, monocromatico=False):
    corpo = []
    for letra, d in paths:
        cor = "#FF000000" if monocromatico else (MENTA if letra == "o" else BRANCO)
        corpo.append(f'    <path android:fillColor="{cor}" android:pathData="{d}" />')
    return (
        '<?xml version="1.0" encoding="utf-8"?>\n' + CABECALHO_XML +
        '<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
        '    android:width="108dp" android:height="108dp"\n'
        '    android:viewportWidth="108" android:viewportHeight="108">\n' + "\n".join(corpo) + "\n</vector>\n"
    )


def escrever(caminho, texto):
    os.makedirs(os.path.dirname(caminho), exist_ok=True)
    with open(caminho, "w", encoding="utf-8") as f:
        f.write(texto)
    print("gerado", os.path.relpath(caminho, RAIZ))


def main():
    android = caminhos(ANDROID_ESCALA)
    escrever(f"{RES}/drawable/ic_launcher_foreground.xml", vetor(android))
    escrever(f"{RES}/drawable/ic_launcher_monochrome.xml", vetor(android, monocromatico=True))
    escrever(f"{RES}/values/ic_launcher_background.xml",
             '<?xml version="1.0" encoding="utf-8"?>\n' + CABECALHO_XML +
             f'<resources>\n    <color name="ic_launcher_background">{MARINHO}</color>\n</resources>\n')
    adaptativo = (
        '<?xml version="1.0" encoding="utf-8"?>\n' + CABECALHO_XML +
        '<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">\n'
        '    <background android:drawable="@color/ic_launcher_background" />\n'
        '    <foreground android:drawable="@drawable/ic_launcher_foreground" />\n'
        '    <monochrome android:drawable="@drawable/ic_launcher_monochrome" />\n'
        '</adaptive-icon>\n'
    )
    escrever(f"{RES}/mipmap-anydpi-v26/ic_launcher.xml", adaptativo)
    escrever(f"{RES}/mipmap-anydpi-v26/ic_launcher_round.xml", adaptativo)

    cheio = caminhos(1.0)
    svg = [
        "<!-- SPDX-License-Identifier: Apache-2.0 -->",
        "<!-- Copyright 2026 The LOMA Authors -->",
        "<!-- Gerado por tools/icone/gerar_icone.py; não editar à mão. -->",
        '<svg xmlns="http://www.w3.org/2000/svg" width="1024" height="1024" viewBox="0 0 108 108">',
        f'  <rect width="108" height="108" fill="{MARINHO}"/>',
    ]
    svg += [f'  <path fill="{MENTA if l == "o" else BRANCO}" d="{d}"/>' for l, d in cheio]
    svg.append("</svg>")
    escrever(SVG, "\n".join(svg) + "\n")

    import io
    import cairosvg
    from PIL import Image
    png = cairosvg.svg2png(url=SVG, output_width=1024, output_height=1024)
    os.makedirs(os.path.dirname(PNG_IOS), exist_ok=True)
    Image.open(io.BytesIO(png)).convert("RGB").save(PNG_IOS, optimize=True)
    print("gerado", os.path.relpath(PNG_IOS, RAIZ))


if __name__ == "__main__":
    sys.exit(main())
