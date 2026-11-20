# LOMA — Bloqueio por PIN e digital (fase 2B, parte 1)

Base: design `docs/design/2026-11-19-loma-design.md` §7.12a e tela aprovada **T-Bloqueio**
(`docs/design/telas/T-Bloqueio.dc.html`). O usuário pediu esta parte antes da Agenda (20/11/2026).

## Por quê
O celular da profissional fica no balcão do salão. Clientes, telefones, alergias e valores não devem
ficar à vista de quem pegar o aparelho destravado.

## Objetivos
- PIN de 4 dígitos opcional, ligado em **Ajustes › Segurança**.
- **Usar digital** (Android: digital/rosto do sistema; iPhone: Face ID/Touch ID).
- Tela "Bem-vinda de volta, <nome>" exatamente como T-Bloqueio (fundo #17165C, avatar 84 dp,
  pontos menta, teclado 3×4 com botões de 72 dp, "Usar digital" com borda menta).
- Bloqueia ao abrir o app e depois de um tempo em segundo plano: **na hora / 1 minuto (padrão) / 5 minutos**.
- **Esqueci o PIN** (decisão do usuário, muda o §7.12a): confirma com o bloqueio do próprio celular
  (senha, padrão ou digital do aparelho) e então pede um PIN novo. Nenhum dado é apagado.

## Não objetivos
- Entrada / configuração inicial (fica para a parte 2 da 2B; lá o bloqueio também será oferecido).
- Ocultar o conteúdo na tela de apps recentes; esconder valores em notificações (2C).

## Anti-objetivos
- O app abrir mostrando dados por um instante antes da tela de bloqueio.
- Perder dados por esquecer o PIN.
- Qualquer acesso à rede ou guardar o PIN legível.

## Restrições
- Sem rede; dependências só da lista permitida (`androidx.biometric` cabe em `androidx.`).
- Android 8.0+ (minSdk 26); `USE_BIOMETRIC` é permissão normal (entra pelo manifest da biblioteca).
- iOS: `NSFaceIDUsageDescription` no Info.plist.
- PIN guardado como hash **PBKDF2-HMAC-SHA256** com sal aleatório de 16 bytes e 120.000 iterações.

## Abordagens
1. **Escolhida — estado do bloqueio num armazenamento síncrono da plataforma** (SharedPreferences
   privado no Android, NSUserDefaults no iOS; fora do backup em nuvem pelas regras já existentes).
   O app sabe na hora, sem esperar o banco, se deve abrir bloqueado: nada aparece antes da tela de PIN.
2. Rejeitada — guardar no banco (tabela de ajustes, Room). A leitura é assíncrona: o Início apareceria
   por um instante antes do bloqueio, ou o app teria de mostrar uma tela vazia enquanto lê.
3. Rejeitada — Keystore/Keychain com chave presa à digital. Mais forte contra quem tem root, mas complica
   o "Esqueci o PIN" e o Android 8–10; o risco do salão é alguém pegar o celular, não extrair arquivos.

## Comportamento
- **Ligar:** Ajustes › Segurança › "Bloquear o app com PIN" → digita 4 dígitos, confirma, vê o aviso:
  "Se esquecer o PIN, você poderá criar outro usando o bloqueio do celular." Se o celular não tiver
  bloqueio de tela, o aviso diz que, sem ele, esquecer o PIN obriga a apagar os dados do app.
- **Usar digital:** chave em Segurança, só aparece se o aparelho tiver digital/rosto cadastrado.
  Na tela de bloqueio, o pedido da digital abre sozinho uma vez e há o botão "Usar digital".
- **Errar o PIN:** os pontos tremem e limpam; depois de 5 erros seguidos, espera de 30 s
  ("Tente de novo em 30 s"). A contagem fica salva (fechar o app não zera).
- **Tempo:** ao voltar do segundo plano, bloqueia se passou o tempo escolhido; abrir o app do zero
  sempre bloqueia (com PIN ligado).
- **Trocar PIN:** pede o atual e depois o novo duas vezes. **Desligar:** pede o PIN atual.
- **Voltar (Android) na tela de bloqueio:** sai do app.
- A abertura animada continua tocando antes; a tela de bloqueio aparece por baixo dela.

## O que fica para o construtor
Organização interna das classes, textos de acessibilidade, animação de "tremer".
