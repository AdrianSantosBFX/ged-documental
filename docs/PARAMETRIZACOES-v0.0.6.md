# Parametrizações - v0.0.6

A categoria operacional desta versão é exclusivamente `RELATÓRIOS`.

Reconhecedores ativos incluem:

- Alíquota ISS / ICMS
- Relação de Declarações NFS-e - Serviços Tomados
- Guia ISSQN - Serviços Tomados
- Guia ISSQN Municipal
- Livro de Serviços Tomados
- Recibo DeSTDA
- Recibo EFD-Contribuições
- Recibo SPED Fiscal
- Recibo ISS - Serviços Prestados
- Recibo ISS - Serviços Tomados
- Relatório Resumo Analítico ISS
- Relatório de Serviços Tomados
- Demonstrativo de Débitos/Créditos e ISSQN a Pagar
- Resumo da Conferência de Entradas
- Conferência de Impostos Retidos
- Total ISS por Natureza
- Total ICMS por Natureza
- Auditoria NFS-e Portal x Questor - Serviços Prestados
- Auditoria NFS-e Portal x Questor - Serviços Tomados
- Declaração de Faturamento
- Demonstração EFD-Contribuições
- Relatório de Lançamentos
- Livro ISSQN
- EFD-REINF - Sem Movimento

## Regra de empresa

O reconhecedor documental pode detectar um CNPJ, mas somente um CNPJ existente no cadastro mestre (`companies.json`) é suficiente para autorizar o arquivamento automático. O cadastro mestre é alimentado pelo Cartão CNPJ.

## Regra de competência

A competência determina diretamente os diretórios `ANO` e `MM-AAAA`. O GED não usa a data de emissão como competência quando o layout contém um período de apuração, referência, mês/ano ou intervalo mensal mais específico.
