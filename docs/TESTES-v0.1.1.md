# GED v0.1.1 — Evidências de validação

Data: 23/10/2026. Base do projeto: GED v0.0.6.

## Ambiente de teste

- Java 21 compilando fonte Java 8 com `javac --release 8`.
- Testes locais em localhost (`tests/ged/BrasilApiIntegrationTest.java`).
- Mock HTTP simulando respostas da BrasilAPI; **não é validação contra a API real**.
- JavaScript verificado por `node --check assets/app.js`.

## Casos contemplados

- CNPJ numérico com dígitos válidos e inválidos.
- CNPJ alfanumérico com dígitos válidos, com pontuação e sem pontuação.
- Validação recusando CNPJ com 14 dígitos iguais e com DV alfanumérico.
- HTTP 400 para CNPJ inválido antes da requisição externa.
- HTTP 404 para CNPJ não encontrado.
- HTTP 429 para limite de requisições.
- Prévia válida sem criar pasta nem gravar `companies.json`.
- Confirmação rejeitada se o token não corresponde ao CNPJ consultado.
- Cadastro completo de CNPJ válido: persistência de dados e criação de pasta.
- Tentativa de sobrescrever razão social no payload ignorada; servidor só usa o dado da prévia em memória.
- Token consumido após confirmar; impedida reutilização.
- Identificação e proteção de registro de CNPJ já existente.
- Dado booleano `opcao_pelo_simples: false` preservado como `false`, sem inferir regime tributário.

## Limites dos testes

- Não inclui execução real no Windows com Java 8, nem teste de acesso à BrasilAPI pela internet.
- O processamento documental da versão-base não foi novamente testado com o corpus original de PDFs, não incluído no pacote.
- Recomendado executar uma consulta real no computador do usuário e confirmar cadastro, pasta e vínculo dos relatórios antes de uso intensivo.
