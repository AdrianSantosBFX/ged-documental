# GED v0.0.6 - Java

A v0.0.6 consolida o fluxo em que o **Cartão CNPJ é a fonte de cadastro da empresa** e a pasta `AAA` é a caixa de entrada automática dos documentos.

## Pré-requisito

- Java 8 ou superior disponível no computador.
- A pasta raiz dos documentos deve existir. Na configuração de exemplo, crie `documentos` ao lado de `run.bat`.
- JDK 9 ou superior para compilar (`javac --release 8`). O programa gerado executa em Java 8 ou superior.
- Não exige Python, Node, Maven, Gradle nem privilégios de administrador.

## Primeira execução

O servidor pode começar contendo apenas:

```text
Servidor Exemplo
└── AAA
```

Na primeira execução, `config/settings.example.json` e `config/companies.example.json` são copiados para os arquivos locais `settings.json` e `companies.json`. O cadastro começa vazio de propósito. Os arquivos locais ficam fora do Git. Uma pasta existente no servidor não é tratada como empresa cadastrada.

## Cadastro de empresas

Na tela **Empresas**, use **Cadastrar por Cartão CNPJ** e envie o Comprovante de Inscrição e de Situação Cadastral em PDF.

Quando o cadastro é confirmado, o GED:

1. valida o Cartão CNPJ;
2. extrai razão social e CNPJ;
3. registra a empresa em `config/companies.json`;
4. cria `Servidor Exemplo\RAZÃO SOCIAL` caso a pasta não exista.

Somente empresas cadastradas dessa forma entram no arquivamento automático. Se um relatório possuir um CNPJ que ainda não está cadastrado, ele é encaminhado para `AAA\REVISAO`; o documento não cria uma empresa por conta própria.

## Processamento automático

Enquanto `run.bat` estiver aberto, a `AAA` é monitorada. Para um PDF de relatório reconhecido com segurança, o GED identifica o tipo, localiza a empresa cadastrada e determina a competência. Em seguida cria somente a estrutura necessária àquela competência:

```text
RAZÃO SOCIAL
└── 2026
    └── 09-2026
        └── RELATÓRIOS
            └── RAZÃO SOCIAL - Documento - 09-2026.pdf
```

Assim, cadastrar uma empresa não cria anos ou meses vazios. `2026\09-2026\RELATÓRIOS` nasce apenas quando entrar um documento efetivamente pertencente a 09/2026.

## Escopo da v0.0.6

Nesta versão, o arquivamento automático continua restrito a **PDFs da categoria RELATÓRIOS**. XML, NF-e/NFS-e em PDF, CT-e e arquivos SPED como arquivos de escrituração ainda não fazem parte do fluxo automático.

O catálogo possui 24 reconhecedores, incluindo os novos layouts parametrizados nesta versão: alíquota ISS/ICMS, declarações municipais de serviços tomados, guias ISSQN, livro de serviços tomados, DeSTDA, relatórios municipais de ISS, conferências, auditorias NFS-e x Questor, EFD-Contribuições e os reconhecedores já existentes.

## Segurança

- Arquivos abaixo da confiança mínima vão para `AAA\REVISAO`.
- Erros técnicos vão para `AAA\ERROS`.
- Empresa não cadastrada nunca recebe pasta automaticamente a partir de um relatório.
- Arquivos existentes não são sobrescritos.
- Duplicidades são verificadas por SHA-256.
- A raiz do servidor não é criada pelo GED; somente estruturas dentro dela.

## Execução

Clone ou baixe este repositório, execute `build.bat` para gerar o JAR e crie a pasta `documentos`. Depois abra:

```text
run.bat
```

A interface é disponibilizada em `http://127.0.0.1:8765/`.

## Build

O projeto inclui o código-fonte Java. Para recompilar em uma máquina com JDK:

```text
build.bat
```

O JAR compilado é `ged-server.jar` e não é versionado. Em Linux/macOS, use `sh build.sh` e `sh run.sh`.

As bibliotecas necessárias estão em `lib/`, com os avisos de terceiros preservados. As regras de reconhecimento ficam em `config/rules.json`; cadastros, histórico, PDFs e configurações locais não são publicados.

O servidor é iniciado em `127.0.0.1`. O aplicativo foi preparado para execução local, sem autenticação multiusuário. O texto dos documentos é usado no reconhecimento; PDFs sem camada textual seguem para revisão conforme a confiança obtida.
