# Elevar GED v0.1.3 — Teste de identidade e regressão

Data: 23/10/2026. Base: GED v0.1.2.

## Alterações verificadas

- Nome Elevar GED e versão 0.1.3 no título HTML, no menu lateral, no breadcrumb e na tela de configurações.
- Nome Elevar GED nos logs de inicialização e no novo JAR `elevar-ged.jar`.
- Manifest do JAR apontando para o mesmo ponto de entrada `ged.GedServer`.
- Configurações mantidas em `config/settings.json` com a versão 0.1.3, sem alterar o formato.
- Script de compilação, inicialização e testes atualizados para utilizar o novo nome do JAR.
- Preservação da propriedade Java `ged.base`, do pacote Java `ged` e dos arquivos de cadastro e regras para evitar quebrar versões anteriores.

## Testes de regressão

Executar `sh tests/run-tests.sh` recompila o novo JAR, executa 19 verificações da integração e cadastro e 46 verificações do fluxo automático e valida o JavaScript. A BrasilAPI é simulada: nenhum dado real de clientes é transmitido neste teste.

## Limites

- É uma atualização de identidade, sem novas regras documentais.
- Não houve validação da execução no Windows nem da consulta real à BrasilAPI nesta versão.
- Recomenda-se testar em uma cópia de `config/companies.json` e `config/settings.json` antes de colocar em produção.
