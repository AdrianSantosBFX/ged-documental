# GED v0.1.2 — Testes da automação de cadastro

Data: 23/10/2026. Origem: GED v0.1.1.

## Ambiente

- Compilação do código com Java 21 usando modo de compatibilidade Java 8.
- Verificação sintática do JavaScript com Node.js.
- Consulta BrasilAPI **simulada localmente** por um servidor HTTP de teste; não foram feitas consultas externas à BrasilAPI.
- PDFs verdadeiros com camada de texto gerados no próprio teste e efetivamente processados e movidos em árvore temporária.

## Casos cobertos

- Compatibilidade: 19 verificações anteriores de cadastro manual e validação do CNPJ.
- Nova automação: 46 verificações de fluxo ponta a ponta, incluindo:
  - cadastro automático de CNPJ único e válido;
  - criação de pasta da empresa e árvore por competência;
  - movimentação física e renomeação do PDF reconhecido;
  - persistência da origem `brasilapi_auto` e dos campos cadastrais;
  - empresa previamente cadastrada sem nova consulta à API;
  - múltiplos CNPJs desconhecidos: revisão, sem cadastramento;
  - documento não parametrizado: revisão, sem consulta externa;
  - tipo documental identificado somente pelo nome do arquivo: revisão;
  - competência ausente: revisão;
  - CNPJ inexistente (HTTP 404): revisão, sem cadastro;
  - função de cadastro automático desativada: revisão;
  - criação de pastas desativada: revisão;
  - geração de histórico local.

## Limitações de validação

- Nenhuma execução foi feita no Windows ou com uma consulta real da BrasilAPI.
- Não houve teste com o acervo real de documentos do usuário; o teste cobre os exemplos criados para a suíte automatizada.
- PDFs digitalizados sem texto selecionável exigem tecnologia de OCR, ausente nesta versão.
- Documentos contendo múltiplos CNPJs (como emissor e destinatário) devem ser validados manualmente quando a empresa ainda não estiver cadastrada.
