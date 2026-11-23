## Summary

Describe the outcome of this change and why it is needed.

## Changes

- ...
- ...

## Testing

- [ ] `mvn -pl saas-wq-server -am verify` passes
- [ ] `cd saas-wq-web && npm run build` passes
- [ ] Manual check performed (describe the account, tenant and screen)

## Checklist

- [ ] No secrets, credentials or `.env` files are included
- [ ] Documentation updated when configuration or endpoints changed
- [ ] Tenant scoped tables still go through the tenant line interceptor
- [ ] New endpoints declare `@PreAuthorize` with an explicit permission code
