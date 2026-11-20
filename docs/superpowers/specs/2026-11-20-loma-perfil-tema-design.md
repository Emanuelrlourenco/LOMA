# LOMA — foto do perfil e botão de tema (design curto)

Pedido do usuário depois de aceitar a fase 2A (20/11/2026). Mudança pequena, antes da fase 2B.

## Por quê
O perfil é a primeira coisa no Início e só mostrava a inicial; e o tema claro das telas aprovadas
só aparecia com o celular em modo claro.

## O que entra
1. **Foto do perfil.** Em Ajustes > Perfil, um avatar com "Escolher foto" / "Remover foto", igual ao
   cadastro de cliente. A foto fica no armazenamento privado do app (mesmo `PhotoStore`) e vale na hora,
   sem precisar de "Salvar". Ela aparece no avatar do Início (canto superior esquerdo); sem foto, a inicial.
2. **Cabeçalho do Início.** Avatar um pouco maior (44 → 54 dp); saudação um pouco menor (24 → 21 sp);
   nome do perfil igual (32 sp) — pedido do usuário.
3. **Botão de tema ao lado do sino.** No tema azul (escuro) mostra um **sol**: tocar deixa claro.
   No tema claro mostra uma **lua**: tocar volta ao azul. A escolha fica salva. Sem escolha salva,
   o app segue o tema do celular (como hoje). O tema claro é o das telas aprovadas.

## Abordagens
- **Escolhida:** ajuste `tema` = `claro`/`escuro` na tabela de ajustes (Room); `LomaApp` resolve
  `override do teste ?: ajuste salvo ?: tema do sistema`. Sem código novo de plataforma.
- Rejeitada: preferência de plataforma síncrona (SharedPreferences/NSUserDefaults) — evitaria um
  instante com o tema do sistema na abertura, mas exige código Android/iOS só para isso. O instante
  cai sob a abertura animada.
- Rejeitada: tela de tema em Ajustes (claro/escuro/sistema/AMOLED, §2 do design) — fica para depois;
  o usuário pediu o atalho no Início.

## Decisões minhas
- Sol/lua mostram o tema do próximo toque (aprovado pelo usuário).
- Foto do perfil grava na hora (escolher/remover), o nome continua com "Salvar".
- Remover foto apaga o arquivo; trocar apaga o anterior.
