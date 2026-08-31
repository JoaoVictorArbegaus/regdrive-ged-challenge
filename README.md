# RegDrive GED

Módulo simples de Gestão Eletrônica de Documentos desenvolvido para um desafio técnico de Java Back-End Pleno.

A aplicação permite autenticar usuários, administrar documentos, filtrar o acervo, enviar e baixar versões de arquivos e consultar a trilha de auditoria. O ambiente completo é executado com Angular, Spring Boot, PostgreSQL e Docker Compose.

## Funcionalidades

- Autenticação stateless com JWT.
- Perfis `ADMIN`, `USER` e `VIEWER`.
- Isolamento de documentos por tenant.
- Criação, consulta e edição de metadados.
- Fluxo de status `DRAFT -> PUBLISHED -> ARCHIVED`.
- Listagem paginada, ordenada e filtrável.
- Upload de PDF, PNG e JPG/JPEG com limite de 10 MiB.
- Histórico incremental de versões e checksum SHA-256.
- Download de uma versão específica.
- Auditoria dos seis eventos exigidos.
- Interface Angular responsiva para o fluxo completo.

## Tecnologias

- Java 21 e Spring Boot 4.1.1.
- Spring Security, JWT, JPA/Hibernate e Flyway.
- PostgreSQL 17.
- Angular 21.
- Nginx para servir o frontend e encaminhar `/api` ao backend.
- JUnit, Mockito e Vitest.
- Docker Compose e GitHub Actions.

## Execução com Docker

### Pré-requisitos

- Git.
- Docker Desktop ou Docker Engine com Docker Compose.

### Iniciar

```bash
git clone https://github.com/JoaoVictorArbegaus/regdrive-ged-challenge.git
cd regdrive-ged-challenge
docker compose --env-file .env.example up --build -d
```

O primeiro build pode demorar alguns minutos enquanto Maven e npm baixam as dependências.

Verifique os containers:

```bash
docker compose --env-file .env.example ps
```

Os três serviços devem ficar com status `healthy`.

### Endereços

| Serviço | Endereço |
|---|---|
| Aplicação Angular | http://localhost:4200 |
| API | http://localhost:8080 |
| Health check | http://localhost:8080/actuator/health |
| PostgreSQL | `localhost:5432` |

### Usuários de demonstração

| Perfil | Usuário | Senha | Permissões |
|---|---|---|---|
| ADMIN | `admin` | `admin123` | Todos os tenants e operações |
| USER | `user` | `user123` | Leitura e escrita no próprio tenant |
| VIEWER | `viewer` | `viewer123` | Consulta e download no próprio tenant |

As credenciais podem ser alteradas por variáveis definidas em um arquivo `.env`. Use `.env.example` como referência e não versione credenciais reais.

### Parar

```bash
docker compose --env-file .env.example down
```

Os dados permanecem nos volumes `postgres-data` e `file-storage`.

Para remover também banco e arquivos armazenados:

```bash
docker compose --env-file .env.example down -v
```

Esse último comando apaga permanentemente os dados locais.

## Migrations

O Flyway executa automaticamente as migrations quando o backend inicia. Para iniciar somente banco e backend e acompanhar a aplicação do schema:

```bash
docker compose --env-file .env.example up -d postgres backend
docker compose --env-file .env.example logs backend
```

Migrations disponíveis:

- `V1`: baseline do banco.
- `V2`: usuários e perfis.
- `V3`: documentos e tags.
- `V4`: versões dos documentos.
- `V5`: auditoria com metadata PostgreSQL `jsonb`.

## Build e testes locais

### Backend no Windows

```powershell
cd backend
.\mvnw.cmd verify
```

### Backend no Linux ou macOS

```bash
cd backend
./mvnw verify
```

O backend possui 8 testes unitários relevantes na camada de service, cobrindo autenticação, permissões, tenant, filtros, status, versionamento, checksum, download e auditoria.

### Frontend

Requer uma versão do Node.js compatível com Angular 21. O projeto foi validado com Node.js 24.13 e npm 11.

```bash
cd frontend
npm ci
npm test -- --watch=false
npm run build
```

Para desenvolvimento local com o backend na porta `8080`:

```bash
npm start
```

O proxy de desenvolvimento encaminha `/api` para o backend.

## Fluxo de validação

1. Acesse http://localhost:4200 e entre como `user`.
2. Crie um documento em estado `DRAFT`.
3. Localize-o usando título, tag ou status.
4. Atualize os metadados.
5. Envie dois arquivos para gerar as versões 1 e 2.
6. Confira o checksum e baixe uma versão.
7. Publique e arquive o documento.
8. Consulte os eventos de auditoria.
9. Entre como `viewer` e confirme que ações de escrita não são exibidas.

## API principal

```text
POST   /api/auth/login

POST   /api/documents
GET    /api/documents
GET    /api/documents/{documentId}
PUT    /api/documents/{documentId}
PATCH  /api/documents/{documentId}/status

POST   /api/documents/{documentId}/versions
GET    /api/documents/{documentId}/versions
GET    /api/documents/{documentId}/versions/{versionNumber}
GET    /api/documents/{documentId}/versions/{versionNumber}/download

GET    /api/documents/{documentId}/audit
```

A API retorna erros no padrão Problem Details, com um código estável para consumo do frontend.

## Decisões técnicas

- Monólito Spring Boot organizado por funcionalidade, sem camadas ou abstrações desnecessárias.
- Controllers implementam contratos HTTP `*Api`; regras ficam em serviços `*ApplicationService`.
- Entidades JPA não são expostas pela API; os contratos usam DTOs e records.
- Tenant e owner são derivados do JWT para usuários não administrativos.
- Arquivos são armazenados fora do banco por uma interface `FileStorage` e uma implementação local.
- O caminho físico e o `fileKey` nunca são escolhidos ou expostos ao cliente.
- O volume Docker `file-storage` preserva arquivos após reinícios.
- Extensão, MIME type e assinatura do conteúdo são validados antes do armazenamento.
- A metadata de auditoria usa `jsonb` e não armazena token ou conteúdo do arquivo.
- O frontend usa componentes standalone, services simples e `sessionStorage`, sem biblioteca de estado ou design system.
- Nginx serve os arquivos estáticos do Angular, trata o fallback da SPA e mantém frontend e API na mesma origem.

## Integração contínua

O workflow `.github/workflows/ci.yml` executa em pushes e pull requests para `main`:

- Backend com Java 21 e Maven `verify`.
- Frontend com Node.js 24, `npm ci`, testes e build de produção.

## Limitações

- Os arquivos são armazenados localmente; não há integração com storage em nuvem.
- Não há refresh token, recuperação de senha, exclusão de documentos ou OCR.
- Uploads simultâneos usam a restrição única do banco, sem lock ou retry adicional.
- O token JWT expira em 15 minutos.
- Não há deploy público. Vercel atende bem ao frontend estático, mas não executa este conjunto completo com Spring Boot, PostgreSQL e storage persistente. A entrega prioriza a execução reproduzível e gratuita pelo Docker Compose local.

## Repositório

https://github.com/JoaoVictorArbegaus/regdrive-ged-challenge
