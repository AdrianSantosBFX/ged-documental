# Changelog

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
