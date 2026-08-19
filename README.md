# Finance Module

API REST modular para gestão de usuários, contas bancárias e lançamentos financeiros. O projeto usa Java 17, Spring Boot 4.1 e MongoDB. Autenticação é JWT (RSA) com 2FA TOTP obrigatório. Lançamentos podem ser criados manualmente ou importados de arquivos OFX.

## Status

Fases 0, 1 e 2 estão concluídas. A fase 3 (cobertura de testes) está no início — hoje existe apenas o teste de contexto da aplicação.

| Fase | Escopo | Status |
| --- | --- | --- |
| 0 | Spring Boot, Maven, MongoDB, Lombok | Concluída |
| 1 | Users, Accounts, Launches (CRUD + OFX) | Concluída |
| 2 | Spring Security, JWT RS256, MFA TOTP | Concluída |
| 3 | Testes unitários e de integração | Em progresso |

## Stack

- Java 17
- Spring Boot 4.1.0
- Spring Web MVC (API versionada no path)
- Spring Data MongoDB
- Spring Security
- Spring Validation
- JWT (JJWT 0.12, assinatura RS256)
- TOTP (`dev.samstevens.totp`)
- MapStruct e Lombok
- Maven Wrapper

## Requisitos

- Java 17+
- Maven (ou o wrapper `mvnw` / `mvnw.cmd`)
- MongoDB em execução
- OpenSSL (para gerar o par de chaves RSA)
- Git

## Configuração

1. Copie o arquivo de exemplo e preencha as variáveis:

```bash
cp .env.example .env
```

Variáveis usadas pela aplicação:

| Variável | Uso |
| --- | --- |
| `MONGO_URL` | URI do MongoDB (ex.: `mongodb://localhost:27017/finance_module`) |
| `ADMIN_PASSWORD` | Senha do usuário admin criado no bootstrap |
| `MFA_SECRET` | Chave AES para criptografar o secret TOTP no banco |

A configuração do Spring está em `src/main/resources/application.yml`.

2. Gere o par de chaves RSA (4096 bits) usado para assinar JWT. O script cria `keys/rsa_key.pem` e `keys/rsa_key.pub` (a pasta `keys/` está no `.gitignore`):

```bash
./keys.sh
```

Tokens:

- Access: 12 horas
- Pre-auth (após login, antes do MFA): 5 minutos
- Setup 2FA: 10 minutos

## Como executar

```bash
git clone https://github.com/samuelfilho-dev/spring-finance-module.git
cd spring-finance-module
```

Unix:

```bash
./mvnw spring-boot:run
```

Windows:

```bash
mvnw.cmd spring-boot:run
```

Na primeira subida, o `AdminSeedRunner` cria o usuário `admin@admin.com.br` com a senha de `ADMIN_PASSWORD`, se ele ainda não existir.

## Autenticação

Rotas autenticadas esperam `Authorization: Bearer <token>`.

Fluxo:

1. `POST /api/v1/auth/login` com e-mail e senha.
2. Se o 2FA ainda não estiver ativo, a resposta inclui token de setup, QR Code (Base64) e URL `otpauth://`. Confirme em `POST /api/v1/auth/mfa/enable`.
3. Se o 2FA já estiver ativo, a resposta traz um pre-token. Envie o código TOTP em `POST /api/v1/auth/mfa/verify` para obter o access token.
4. Admin pode resetar MFA em `POST /api/v1/auth/mfa/reset`.

Rotas públicas:

- `POST /api/v1/auth/**`
- `POST /api/v1/users` (cadastro)

Somente `ROLE_ADMIN`:

- `GET /api/v1/users`
- `POST /api/v1/users/create-admin`
- `DELETE /api/v1/users/{id}`
- `POST /api/v1/auth/mfa/reset`

As demais rotas exigem usuário autenticado com access token.

## API (v1)

Base: `/api/v1`

### Auth — `/api/v1/auth`

| Método | Path | Descrição |
| --- | --- | --- |
| POST | `/login` | Valida credenciais e inicia o fluxo MFA |
| POST | `/mfa/enable` | Ativa 2FA após o setup |
| POST | `/mfa/verify` | Valida o código TOTP e devolve access token |
| POST | `/mfa/reset` | Reseta MFA (admin) |

### Users — `/api/v1/users`

| Método | Path | Descrição |
| --- | --- | --- |
| GET | `/` | Lista usuários (admin) |
| GET | `/{id}` | Busca por id |
| POST | `/` | Cadastro |
| POST | `/create-admin` | Cria admin (admin) |
| PUT | `/{id}` | Atualiza |
| DELETE | `/{id}` | Remove (admin) |

### Accounts — `/api/v1/accounts`

CRUD de contas bancárias (`bankName`, `agency`, `accountNumber`, `balance`, status `ACTIVE` / `INACTIVE`).

### Launches — `/api/v1/launches`

CRUD de lançamentos (receita `RECIPE` ou despesa `EXPENSE`, categoria, valor, data, conta).

Importação OFX (multipart):

```http
POST /api/v1/launches/ofx
Content-Type: multipart/form-data

bankAccountId=<objectId>
file=<arquivo.ofx>
```

## Estrutura

```text
src/main/java/com/samuelfilho_dev/finance_module/
  auth/          # login, JWT, MFA
  users/         # usuários e endereço
  account/       # contas bancárias
  launches/      # lançamentos e parser OFX
  config/        # Security, JWT filter, Mongo
  exceptions/    # exceções de domínio
  handles/       # exception handlers
  seeds/         # seed do admin
  validators/    # validação de ObjectId
  utils/         # AES para secret MFA
```

Persistência MongoDB: collections `users`, `bankAccounts` e `launches`.

## Testes

```bash
./mvnw test
```

Há um teste de smoke (`FinanceModuleApplicationTests`). Ampliar unitários e integração é o próximo foco.

## Próximos passos

- Ampliar cobertura de testes (unitários e integração)
- Documentar a API em OpenAPI / Postman
- Definir licença de distribuição

## Contribuição

Pull requests são bem-vindos. Abra issues para bugs e propostas de melhoria.

## Licença

Ainda não definida.

## Contato

Autor: Samuel Filho — [samuelfilho-dev/spring-finance-module](https://github.com/samuelfilho-dev/spring-finance-module)
