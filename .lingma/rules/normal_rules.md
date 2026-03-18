---
trigger: always_on
---

1. Database operations should prioritize the `MyBatisPlus` framework, avoiding direct writing of native `MySQL` statements. If native SQL is indeed required, it must be implemented only after user confirmation.
2. Code development must strictly follow production environment standards, and `debug` mode is prohibited.
3. When using `GetMapping` in the `Controller` layer, the `@RequestParam` annotation must be used; the use of `@PathVariable` is prohibited.
4. Methods such as `GetMapping`, `PostMapping`, `PutMapping`, `DeleteMapping`, `PatchMapping`, `OptionsMapping`, `HeadMapping`, and `TraceMapping` in the `Controller` layer must uniformly return data in `JSON` format, except for binary files.
5. Generated code must exclude comments that describe the act of adding, deleting, modifying, or updating code logic, implementation details, or business data. For instance, avoid comments such as "updated userInfo with successful login information" or similar descriptive remarks about code changes.
6. For single-line comment content, line comments should be prioritized over block comments.
7. In `import` statements, wildcard usage is prohibited.
8. Do not extract data from return messages; parameters must be passed manually. Extracting any hard-coded data is prohibited.
