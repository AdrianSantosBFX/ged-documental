# Testes - v0.0.6

O corpus temporário fornecido para parametrização continha 28 PDFs. Ele foi usado somente durante desenvolvimento e validação e não é distribuído com o projeto.

Resultado do teste de reconhecimento com as empresas correspondentes previamente cadastradas em um ambiente temporário:

- 27 PDFs reconhecidos e aptos ao fluxo automático.
- 1 PDF encaminhado para revisão por ausência de camada textual e confiança insuficiente (EFD-REINF sem movimento).
- 0 erros de processamento.

Também foram validados dois comportamentos estruturais:

1. Com empresa não cadastrada, um relatório reconhecido foi encaminhado para `AAA\REVISAO` e nenhuma pasta de empresa foi criada.
2. Com empresa cadastrada, um relatório de competência 09/2026 criou exatamente `EMPRESA\2026\09-2026\RELATÓRIOS` e foi renomeado/movido para esse destino.

O cadastro por Cartão CNPJ foi novamente testado: o vínculo foi persistido e somente a pasta da empresa foi criada no momento do cadastro.
