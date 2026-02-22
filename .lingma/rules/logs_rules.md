---
trigger: always_on
---

1. Log records must follow the principle of conciseness. Only necessary logs at key nodes (e.g., the controller layer) are permitted. The use of debug and info level logs is strictly prohibited in implementation classes; only warn and error level logs for server-triggered exceptions are allowed.
2. All log content must be written in English.
3. If comments are required, they must be written in Chinese.
4. Logs in controller classes must support full traceability and follow the format below:
   `log.info("User ({}) login failed: {}", userInfo, errorMessage);`
   `log.info("User ({}) verification code sent successfully to email.", userInfo);`
   Each log must contain the `userInfo` field.
5. Each log must be printed on a single line; manual line breaks in log content are strictly prohibited.