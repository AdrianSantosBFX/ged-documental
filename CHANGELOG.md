# Elevar GED v0.1.3 — Nova identidade (23/10/2026)

- Produto renomeado oficialmente de GED para **Elevar GED**, alinhado à marca Elevar.
- Nova identificação no navegador, menu lateral, breadcrumb, página de configurações, logs de inicialização e documentação.
- Arquivo de execução renomeado para `elevar-ged.jar`; scripts de inicialização e build atualizados.
- Versão Java e configuração atualizadas para `0.1.3`.
- Preservados `config/companies.json`, `settings.json`, regras documentais, pastas já vinculadas, motor de processamento e endpoints HTTP.
- O namespace Java `ged` e a propriedade `ged.base` continuam inalterados por compatibilidade.
- Nenhuma funcionalidade da v0.1.2 foi removida.

---

# GED v0.1.2 — Cadastro automático de empresas

- Identifica um único CNPJ válido em relatórios PDF recebidos na AAA.
- Consulta a BrasilAPI, cadastra a empresa e cria a pasta antes do arquivamento.
- Reanalisa o documento após registrar a empresa e preserva o limiar de confiança.
- Documentos com múltiplos CNPJs, consulta falha, competência ausente ou documentos não reconhecidos vão para REVISÃO.
- Nova chave `auto_register_companies` (ativa por padrão, controlável na interface).
- Rastreabilidade no resultado, na tabela de empresas e no histórico local.
- Mantém cadastro manual por PDF e por BrasilAPI, monitor da AAA e regras documentais.

# Changelog

## v0.1.1 — Cadastro por CNPJ com BrasilAPI (23/10/2026)

- Implementada segunda entrada no menu Empresas: **Consultar CNPJ (API)**, preservando o cadastro pelo Cartão CNPJ em PDF.
- Integração externa realizada **exclusivamente no Java**, por HTTPS com tempo limite e respostas JSON de tamanho restrito.
- Validação de CNPJ com dígitos verificadores numéricos ou alfanuméricos, inclusive na identificação de CNPJ nos documentos.
- Prévia dos dados cadastrais com confirmação explícita; tokens temporários de uso único evitam reenviar cadastro manipulado pelo navegador.
- Detecção de empresa já cadastrada por CNPJ normalizado, mantendo a pasta e os dados preexistentes.
- Registro da origem `brasilapi` e de informações cadastrais úteis; não são salvos dados pessoais do QSA.
- Tratamento de HTTP 400, 404, 429, indisponibilidade e timeout com mensagens de erro na interface.
- Interface, documentação e testes atualizados. Mantidos Java 8, PDFBox, interface local e monitoramento da AAA.
- Base técnica utilizada: GED v0.0.6 fornecida pelo usuário.

## v0.0.6 - Cadastro como fonte de verdade e novas parametrizações

- `companies.json` passa a iniciar vazio: a empresa entra no GED pelo Cartão CNPJ.
- Pastas existentes na raiz não são mais importadas automaticamente como empresas.
- Cadastro por Cartão CNPJ cria a pasta da empresa na raiz do Servidor Exemplo.
- Um relatório nunca cria uma empresa não cadastrada; CNPJ desconhecido é encaminhado para REVISÃO.
- O GED cria `EMPRESA\ANO\MM-AAAA\RELATÓRIOS` sob demanda, usando a competência reconhecida do documento.
- Se a pasta de uma empresa cadastrada tiver sido removida, ela pode ser recriada como parte da estrutura de destino.
- Motor de competência ampliado para layouts municipais, DeSTDA, períodos por extenso e relatórios do Questor.
- Catálogo expandido para 24 reconhecedores de relatórios.
- Diagnóstico passa a informar CNPJs detectados mesmo quando a empresa ainda não está cadastrada.
- Interface atualizada para deixar explícito que Cartão CNPJ é a fonte de cadastro.
- Mantido o processamento automático da AAA e o arquivamento físico seguro da v0.0.5.
- Escopo permanece limitado a PDFs de RELATÓRIOS.

## v0.0.5 - Processamento e arquivamento automáticos

- Monitoramento contínuo da AAA com Java WatchService e varredura periódica.
- Processamento automático sem botão.
- Criação de estrutura e movimentação física de relatórios reconhecidos.
- Separação automática entre RELATÓRIOS, REVISÃO e ERROS.
- Proteção contra colisão e duplicidade por SHA-256.
