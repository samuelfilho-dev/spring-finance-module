# Finance Module

Sistema financeiro para gestão de usuários, contas bancárias e lançamentos, desenvolvido com Java e Spring Boot. O projeto tem como objetivo fornecer uma base sólida para operações financeiras, com arquitetura modular, persistência em MongoDB e evolução por fases até chegar a um ambiente com autenticação, segurança e testes automatizados.

## Visão Geral

Este módulo está sendo estruturado para evoluir em etapas, começando pelos fundamentos da aplicação e avançando para autenticação, autorização e garantia de qualidade.

Atualmente, o projeto já conta com a base de configuração e a modelagem inicial dos seguintes domínios:

- Users
- Accounts
- Launches (em desenvolvimento)

## Stack Tecnológica

- Java 17
- Spring Boot 4.1.0
- Spring Web MVC
- Spring Data MongoDB
- Spring Validation
- Lombok
- MapStruct
- Maven
- MongoDB
- Loggers e estrutura de observabilidade em evolução

## Estrutura do Projeto

```text
src/
  main/
    java/
      com/samuelfilho_dev/finance_module/
        account/
        users/
        config/
        validators/
    resources/
      application.yml
  test/
    java/
```

### Domínios principais

- Users: cadastro, atualização e consulta de usuários
- Accounts: gestão de contas bancárias
- Launches: controle de lançamentos financeiros (em criação)

## RoadMap

### Fase 0: Configuração

- Spring Boot 4
- Lombook
- MongoDB
- Loggers

### Fase 1: Modelagem (em criação)

- Users
- Accounts
- Launches

### Fase 2: Segurança

- JWT Token
- 2FA

### Fase 3: Testes

- Criação de testes unitários
- Criação de testes e2e

## Requisitos

Antes de executar o projeto, verifique se seu ambiente possui:

- Java 17+
- Maven
- MongoDB em execução
- Git

## Configuração

O projeto utiliza a variável de ambiente abaixo para conexão com o MongoDB:

```bash
MONGO_URL=mongodb://localhost:27017/finance_module
```

A configuração do Spring está em:

```text
src/main/resources/application.yml
```

## Como Executar

Clone o projeto:

```bash
git clone https://github.com/seu-usuario/finance_module.git
cd finance_module
```

Inicie a aplicação:

```bash
./mvnw spring-boot:run
```

Ou, no Windows:

```bash
mvnw.cmd spring-boot:run
```

## Testes

Para executar os testes do projeto:

```bash
./mvnw test
```

## Endpoints principais

A aplicação usa versionamento de API e a base segue o padrão:

```text
/api/{version}/...
```

Exemplo:

```text
/api/v1/users
/api/v1/accounts
```

## Status do Projeto

O projeto está em fase inicial de desenvolvimento, com a base da aplicação pronta e os módulos de usuários e contas em evolução. A próxima etapa é consolidar a modelagem de lançamentos e avançar para as camadas de segurança e testes.

## Próximos Passos

- Finalizar a modelagem de lançamentos
- Implementar regras de negócio dos domínios
- Adicionar autenticação com JWT
- Implementar MFA/2FA
- Criar testes unitários e de integração
- Documentar endpoints e exemplos de requisições

## Licença

Este projeto ainda não possui uma licença definida. Em breve, a licença será adicionada conforme a estratégia de distribuição do projeto.
