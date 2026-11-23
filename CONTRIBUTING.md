# Contributing

Thanks for taking the time to improve `saas-wq`. This guide keeps changes consistent with the
architecture described in the README.

## Getting started

```bash
docker compose up -d mysql redis
mvn -pl saas-wq-server -am package -DskipTests
java -jar saas-wq-server/target/saas-wq-server.jar

cd saas-wq-web
npm install
npm run dev
```

Flyway applies the schema and the demo data on first start. Every demo account uses the password
`Admin@123`.

## Before opening a pull request

- Backend: `mvn -pl saas-wq-server -am verify` must pass. The integration tests use Testcontainers
  and therefore need a running Docker engine.
- Frontend: `cd saas-wq-web && npm run build` must pass; it runs `vue-tsc` before Vite.
- Add or update tests when behaviour changes. Prefer a focused unit test for pure logic and an
  integration test when tenant isolation, permissions or transactions are involved.
- Keep the README accurate when you change configuration, endpoints or the deployment topology.

## Code conventions

- Java code, comments, Javadoc and commit messages are written in English.
- Single line `if` statements always use braces.
- Application packages live under `com.wq.*`; framework concerns belong to `saas-wq-framework`,
  business use cases to `saas-wq-server`.
- Controllers stay thin: validation and permission checks in the controller, decisions in the
  service, persistence through mappers.
- Never bypass the tenant line interceptor. A table either carries `tenant_id` or is explicitly
  listed in `TenantConstants` as global.
- Frontend components use `<script setup lang="ts">`, typed API helpers in `src/api` and the shared
  classes from `src/styles/index.css`.

## Commit messages

Use the conventional prefix that matches the change: `feat:`, `fix:`, `docs:`, `test:`, `ci:`,
`refactor:` or `chore:`. Describe the outcome, not the mechanics.

## Security

Do not commit secrets, `.env` files, Stripe keys or database dumps. Report a vulnerability privately
to the maintainer instead of opening a public issue.
