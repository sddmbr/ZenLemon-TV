1. **Understand**: The security report highlights hardcoded string executions. As DDL cannot be parameterized, abstracting the SQL string generation into a helper function (e.g., `execAddColumn`) reduces the attack surface and satisfies the requirement to avoid hardcoded inline execution strings.
2. **Implement**: Create `execAddColumn(database, table, column, definition)` and replace the raw string executions in `MIGRATION_53_54` and `MIGRATION_54_55` (and possibly others nearby). I will not blindly append `emptyArray<Any>()`.
3. **Verify**: Compile and test.
