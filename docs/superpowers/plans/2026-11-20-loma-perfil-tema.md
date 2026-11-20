# LOMA — foto do perfil e botão de tema: plano

> Executado inline (superpowers:executing-plans). Spec: `docs/superpowers/specs/2026-11-20-loma-perfil-tema-design.md`.

**Global:** sem rede; strings pt/en; testes em `:shared:ui` jvmTest rodam no CI.

### Task 1: tema salvo e botão sol/lua
- `SettingKeys.THEME = "tema"` (`"claro"`/`"escuro"`), `SettingKeys.PROFILE_PHOTO = "perfil.foto"`.
- `shell/ThemeChoice.kt`: `fun resolveDarkTheme(override: Boolean?, saved: String?, system: Boolean): Boolean`;
  `class ThemeToggle(val dark: Boolean, val toggle: () -> Unit)`; `LocalThemeToggle`.
- `LomaApp(darkTheme: Boolean? = null, …)` lê o ajuste e fornece `LocalThemeToggle`.
- Início: botão `theme_toggle` (44 dp) antes do sino; ícones `LomaIcons.Sol`/`Lua`;
  descrição "Mudar para o tema claro" / "Mudar para o tema azul".
- Testes: `ThemeChoiceTest` (override vence; "claro"→false; "escuro"→true; null→sistema);
  `LomaAppTest.temaSalvoVenceOSistema`, `LomaAppTest.botaoDeTemaAlternaESalva`.

### Task 2: foto do perfil e cabeçalho
- Ajustes: `ClientAvatar(nome, foto, 72)` + `profile_photo_pick` / `profile_photo_remove`.
- Início: avatar `home_avatar` 54 dp com `ClientAvatar`; saudação 21 sp.
- `ClientAvatar`: a imagem ganha a tag `avatar_photo`.
- Testes: `SettingsScreenTest.removerFotoDoPerfil`, `HomeScreenTest.fotoDoPerfilNoAvatar`;
  capturas `inicio-claro`, `inicio-escuro` atualizadas e nova `ajustes-perfil`.
