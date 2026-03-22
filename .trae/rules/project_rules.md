1. Log records must follow the principle of conciseness. Only necessary logs at key nodes (e.g., the controller layer) are permitted. The use of debug and info level logs is strictly prohibited in implementation classes; only warn and error level logs for server-triggered exceptions are allowed.
2. All log content must be written in English.
3. If comments are required, they must be written in Chinese.
4. Logs in controller classes must support full traceability and follow the format below:
   `log.info("User ({}) login failed: {}", userInfo, errorMessage);`
   `log.info("User ({}) verification code sent successfully to email.", userInfo);`
   Each log must contain the `userInfo` field.
5. Database operations should prioritize the `MyBatisPlus` framework, avoiding direct writing of native `MySQL` statements. If native SQL is indeed required, it must be implemented only after user confirmation.
6. Code development must strictly follow production environment standards, and `debug` mode is prohibited.
7. When using `GetMapping` in the `Controller` layer, the `@RequestParam` annotation must be used; the use of `@PathVariable` is prohibited.
8. Methods such as `GetMapping`, `PostMapping`, `PutMapping`, `DeleteMapping`, `PatchMapping`, `OptionsMapping`, `HeadMapping`, and `TraceMapping` in the `Controller` layer must uniformly return data in `JSON` format, except for binary files.
9. Generated code must exclude comments that describe the act of adding, deleting, modifying, or updating code logic, implementation details, or business data. For instance, avoid comments such as "updated userInfo with successful login information" or similar descriptive remarks about code changes.
10. For single-line comment content, line comments should be prioritized over block comments.
11. In `import` statements, wildcard usage is prohibited.
12. Do not extract data from return messages; parameters must be passed manually. Extracting any hard-coded data is prohibited.