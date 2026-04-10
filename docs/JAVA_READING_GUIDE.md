# How to read the Pyrite Java code (beginner → intermediate)

Use this **order** the first time. Each file builds on the previous idea.

---

## 1. Start here: what Spring Boot is doing

| Order | File | What you learn |
|------:|------|----------------|
| 1 | `PyriteApplication.java` | The `main` method that boots Spring; what `@SpringBootApplication` implies. |
| 2 | `ExecutionRequest.java` / `ExecutionResponse.java` | **Records** as JSON shapes; **Bean Validation** annotations. |
| 3 | `ExecutionController.java` | **REST**: mapping a URL to a method; `@RequestBody`, `@Valid`, **constructor injection**. |
| 4 | `GlobalExceptionHandler.java` | **Global exception handling**: turn validation errors into HTTP 400 + JSON. |
| 5 | `PythonExecutionService.java` | **Business logic**: `ProcessBuilder`, processes, streams, timeouts. |
| 6 | `ExecutionControllerTest.java` | **MockMvc**: testing the web layer without a real server. |

---

## 2. Mini glossary (terms you will see in the code)

| Term | Meaning in this project |
|------|-------------------------|
| **Bean** | An object Spring creates and injects (e.g. `PythonExecutionService`). |
| **Constructor injection** | Dependencies passed through the constructor (`ExecutionController` receives `PythonExecutionService`)—dependencies are explicit and the object is easy to test. |
| **REST controller** | A class whose methods answer HTTP requests and usually return data (often JSON). |
| **DTO** (Data Transfer Object) | Simple types used for request/response JSON (`ExecutionRequest`, `ExecutionResponse`). |
| **Record** | Immutable data class with accessors named after fields (`code()`, not `getCode()`). |
| **Validation** | Annotations on fields; Spring runs checks when `@Valid` is present on a controller parameter. |
| **Process / ProcessBuilder** | JDK API to start another program (`python3`) and talk to its stdin/stdout/stderr. |

---

## 3. Request path (mental model)

1. Browser sends `POST /api/execute` with JSON.
2. **DispatcherServlet** picks `ExecutionController.execute`.
3. JSON → `ExecutionRequest`; validation runs.
4. Controller calls `PythonExecutionService.execute(...)`.
5. Service runs Python, builds `ExecutionResponse`.
6. Spring serializes `ExecutionResponse` to JSON for the HTTP response.

If step 3 fails validation → **`GlobalExceptionHandler`** runs instead of step 4.

---

## 4. Where to dig deeper (official docs)

- [Spring Boot](https://docs.spring.io/spring-boot/documentation.html) — “Developing Your First Spring Boot Application”
- [Spring MVC](https://docs.spring.io/spring-framework/reference/web/webmvc.html) — controllers, validation
- [Java `ProcessBuilder`](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/ProcessBuilder.html)

The Javadoc on each class in `src/main/java/com/pyrite/` is written to match this guide.
