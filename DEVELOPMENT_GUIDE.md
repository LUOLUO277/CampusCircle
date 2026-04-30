# Development Guide

This guide is for multi-person collaboration when there is no shared database and no shared persistent data.

## 1. Core rule

Treat the database as local-only development state, not as a shared truth source.

That means:

- Do not assume your teammate has the same rows or the same IDs.
- Do not hardcode local primary keys in frontend logic.
- Do not rely on manually created records unless they are documented.

## 2. Branching

Recommended:

1. `main` stays releasable.
2. Each feature or bugfix uses a separate branch.
3. Backend and frontend changes for the same API contract should be committed in the same branch whenever possible.

Suggested branch names:

- `feature/post-publish`
- `fix/login-error-handling`
- `refactor/upload-flow`

## 3. Database collaboration strategy

Because there is no shared DB, use one of these approaches:

### Preferred

Keep local DBs separate and synchronize only schema and seed rules through code.

Use:

- JPA entity changes
- startup initialization
- SQL scripts checked into git

### Avoid

- Sharing exported raw DB files casually
- Depending on one teammate’s local rows
- Making debugging decisions based only on one machine’s data state

## 4. Data seeding recommendations

Keep a minimal reproducible local dataset.

Recommended practice:

1. Seed only the smallest useful data set.
2. Seed stable reference data through code.
3. Put explicit SQL or setup notes in the repo if a feature needs special records.

Current built-in seed behavior:

- default admin user
- default category data

If a new module needs seed data, add it in a controlled way and document it in the same branch.

## 5. API collaboration rules

Before changing a request or response shape:

1. Confirm the field contract.
2. Update both sides in the same branch if possible.
3. Record sample payloads in the PR description.

Recommended PR notes:

- endpoint path
- request example
- response example
- breaking-change risk

## 6. How to avoid local-data drift problems

Use these rules:

- Query by business meaning, not by guessed row ID.
- Prefer usernames, codes, or stable keys over raw numeric IDs when testing manually.
- Add fallback UI for empty state, missing state, and no-data state.
- Test with both seeded data and empty data when possible.

## 7. Upload and static resource rules

Uploads are local filesystem data during development.

Implications:

- Your uploaded files are not automatically available on your teammate’s machine.
- Do not use personal local upload files as required test assets.
- If a feature depends on an image or file, add a reusable sample asset to the repo instead.

## 8. Recommended daily workflow

1. Pull latest `main`
2. Start backend
3. Start frontend
4. Reproduce with your own local DB
5. Develop on a feature branch
6. Test empty-state and seeded-state behavior
7. Open PR with API and data notes

## 9. Merge checklist

Before merging:

- backend starts locally
- frontend starts locally
- changed APIs are documented
- no machine-specific paths were introduced
- no personal secrets were committed
- local-only debug artifacts were excluded

## 10. Conflict-prone areas in this project

Pay extra attention when modifying:

- API contracts between frontend and backend
- upload path handling
- image URL rendering
- authentication and token flow
- local DB initialization logic

## 11. Recommended communication pattern

When you change something that affects others, message the team with:

1. what changed
2. which files changed
3. whether local DB reset is needed
4. whether frontend or backend must be updated together

## 12. If local databases diverge badly

Use this recovery order:

1. Keep the code
2. Drop only your local DB data if necessary
3. Restart from documented seed/setup flow
4. Re-test from a clean local state
