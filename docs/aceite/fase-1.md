<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 The LOMA Authors -->
# Aceite da fase 1 (Fundação) — roteiro no celular

O que esta fase entrega: o app instala, abre com a abertura animada e o som "Brilho lento", mostra a
barra inferior com as quatro abas (as telas de cada aba chegam nas próximas fases), não pede nenhuma
permissão e não acessa a internet.

## Antes de começar (uma vez só)

O APK é publicado automaticamente quando a branch é integrada na `main`, **depois** que os 4 Secrets
da chave de assinatura estiverem gravados no GitHub (Settings › Secrets and variables › Actions):
`LOMA_KEYSTORE_BASE64`, `LOMA_KEYSTORE_PASSWORD`, `LOMA_KEY_ALIAS`, `LOMA_KEY_PASSWORD`.

## Android

1. **Instalar.** No celular, abra o link fixo
   <https://github.com/Emanuelrlourenco/LOMA/releases/download/loma-mais-recente/LOMA.apk>
   (salve nos favoritos) e instale. Se o Android pedir, permita "instalar apps desconhecidos" só para o
   navegador que você usou. **Para cada atualização futura:** abra o mesmo link e instale por cima — os
   dados continuam.
2. **Abertura.** Abra o LOMA: a tela fica marinho, o brilho menta cresce, o ponto "estoura" e vira o
   "o" menta de **loma**, e em cerca de 4,6 s aparece o Início. Com o celular no **silencioso ou
   vibrar**, a abertura acontece **sem som**.
3. **Pular.** Feche o app (tire dos recentes), abra de novo e toque na tela durante a abertura → o
   Início aparece na hora.
4. **Sem permissões.** Nenhum pedido de permissão aparece ao abrir.
5. **Abas.** A barra inferior mostra Início, Agenda, Clientes e Financeiro, nas cores das telas
   aprovadas; toque em cada aba e veja o título mudar.
6. **Voltar.** Na aba Financeiro, toque "voltar" do Android → vai para Início; toque "voltar" de novo
   → o app fecha.
7. **Girar.** Na aba Clientes, gire o celular → continua em Clientes, e a abertura **não** toca de
   novo.
8. **Permissões e dados.** Ajustes do Android › Apps › LOMA › Permissões → "Nenhuma permissão
   solicitada"; Ajustes › Apps › LOMA › Uso de dados (móveis e Wi-Fi) → sem tráfego.
9. **Tema escuro.** Ative o modo escuro do sistema → o app troca para o tema escuro, legível.
10. **Rapidez.** Do toque no ícone até a abertura começar: menos de 1 segundo.
11. **Atualizar por cima.** Depois da próxima publicação, abra o mesmo link e instale: o Android
    mostra **"Atualizar"** (não "conflito de assinatura") e Ajustes › Apps › LOMA mostra a versão nova
    (0.1.N maior que a anterior). As abas e o resto continuam funcionando.

## iOS

Nesta fase o app iOS é conferido pelo CI (build no simulador e testes). Instalar num iPhone exige
um Mac com Xcode e uma conta da Apple; o passo a passo está em `iosApp/README.md`.

## Resultado

Anote qualquer item que não aconteceu como descrito (número do passo e o que apareceu).
