# Pyrite learning guide (start here if you are new)

This document assumes you are comfortable using a terminal and a browser, but **no prior experience** with Java, Spring, React, or Maven. Read sections in order; skip what you already know.

---

## 1. What problem does Pyrite solve?

Normally you write Python in an editor on your computer and run `python3 yourfile.py` in a terminal. Pyrite lets you do something similar **through a website**:

1. You type Python in the browser.
2. Your code is sent to a **server** over the network.
3. The server runs **real Python** on the machine where the server runs.
4. The server sends **stdout**, **stderr**, and **exit code** back to the browser.

So Pyrite is not “Python in the browser” like WebAssembly—it is **Python on the server**, controlled from the browser.

---

## 2. Big picture: two programs

| Part | Technology | Runs where |
|------|------------|------------|
| **Frontend** | HTML, CSS, JavaScript, **React** | Your **browser** |
| **Backend** | **Java** + **Spring Boot** | **Server** (your laptop or a remote machine) |

They talk to each other using **HTTP**: the browser sends an **HTTP request**; the server sends an **HTTP response**.

```
┌─────────────┐     HTTP (JSON)      ┌─────────────────────┐
│   Browser   │  ─────────────────►  │  Spring Boot (Java)  │
│  (React UI) │  ◄─────────────────  │  runs python3        │
└─────────────┘     JSON + output     └─────────────────────┘
```

---

## 3. Concepts worth knowing (short glossary)

### HTTP and REST

- **HTTP** is how browsers and servers exchange messages.
- **POST** is one kind of request, often used when you **send data** (here: your Python source).
- **REST** is a style of API where URLs represent resources; Pyrite exposes something like “run code” at **`/api/execute`**.

### JSON

**JSON** is a text format for structured data. Example body the UI sends:

```json
{
  "code": "print('hi')",
  "stdin": "",
  "timeoutSeconds": 30
}
```

The server responds with JSON too (`stdout`, `stderr`, `exitCode`, `timedOut`).

### Java and Spring Boot

- **Java** is the language the backend is written in.
- **Spring Boot** is a framework that:
  - Starts an **embedded web server** (Tomcat) so you do not install Tomcat separately.
  - Maps Java methods to URLs (e.g. “when `POST /api/execute` arrives, call this method”).
  - Helps with **JSON** conversion and **validation**.

### Maven

**Maven** is a **build tool** for Java projects. It:

- Downloads libraries (**dependencies**) your code needs.
- Compiles `.java` files.
- Runs tests.
- Packages everything into a **JAR** file—an archive that can run the app (`java -jar ...`).

The file **`pom.xml`** tells Maven what to do.

### JAR file

A **JAR** (Java Archive) is a zip-like file. Pyrite’s JAR contains:

- Compiled Java bytecode.
- Spring and other libraries.
- **Static files** for the website (HTML, JS, CSS) so one file can run the whole app.

### React and Vite

- **React** is a JavaScript library for building user interfaces. You describe **components** (e.g. editor, buttons) and React updates the page when data changes.
- **Vite** is a **development server** and **bundler**: it packages many JS files into optimized bundles for production.

### Monaco Editor

**Monaco** is the editor engine used in **Visual Studio Code**. It gives syntax highlighting and editing features in the browser.

### Process and ProcessBuilder (Java)

To run Python, the **Java** code does **not** use the shell. It uses **`ProcessBuilder`** to start a process:

- Program: `python3`
- Argument: path to a temporary `.py` file containing your code

That is safer than stringing together a shell command, which is easy to get wrong for security.

### `input()` in Pyrite (not interactive)

`input()` reads from **stdin**. Pyrite sends whatever you typed in the **stdin** box **before** execution starts—it does **not** wait for each `input()` while the program runs.

- Use **one line** in the box for each **`input()`** call (first line → first `input()`, and so on).
- If the box is **empty**, stdin is closed immediately → **`EOFError`** when `input()` runs.
- For an empty answer (user just presses Enter), put a **blank line** in the box (e.g. press Enter once in the textarea).

### Standard input and output (stdin / stdout / stderr)

- **stdin:** input the program reads (e.g. what you type in the “stdin” box).
- **stdout:** normal printed output.
- **stderr:** error messages and some libraries print here too.

The Python process has **exit code** `0` usually means success; non-zero often means an error.

---

## 4. How a “Run” click travels through the system

1. **Browser (React)**  
   `App.jsx` collects `code`, `stdin`, `timeoutSeconds` and calls **`fetch('/api/execute', { method: 'POST', body: JSON })`**.

2. **Network**  
   The request goes to the Spring Boot server (port **8080** in production).

3. **Spring Boot**  
   **`ExecutionController`** receives the JSON and Spring converts it into a Java **`ExecutionRequest`** object. Validation runs (e.g. code must not be empty).

4. **Service layer**  
   **`PythonExecutionService`**:
   - Writes `code` to a **temporary file** ending in `.py`.
   - Starts **`python3 /path/to/temp.py`** via **`ProcessBuilder`**.
   - Writes `stdin` to the process’s stdin stream.
   - Reads stdout and stderr (with a **timeout**).
   - Deletes the temp file.
   - Returns an **`ExecutionResponse`**.

5. **Response**  
   Spring converts **`ExecutionResponse`** to JSON and sends it back.

6. **Browser**  
   React updates the **output** panel with stdout, stderr, exit code, or timeout message.

---

## 5. Map of important files (read in this order)

For **Java-only** reading order and Spring terms, see **[JAVA_READING_GUIDE.md](JAVA_READING_GUIDE.md)**.

### Backend (Java)

| File | Purpose |
|------|---------|
| `PyriteApplication.java` | Spring Boot **entry point**—starts the app. |
| `ExecutionController.java` | Defines **`POST /api/execute`** and delegates to the service. |
| `ExecutionRequest.java` / `ExecutionResponse.java` | **Shape** of JSON request/response (Java **records**). |
| `PythonExecutionService.java` | Runs **`python3`** via **`ProcessBuilder`**, handles timeout and streams. |
| `GlobalExceptionHandler.java` | Turns validation errors into **HTTP 400** with `{ "error": "..." }`. |

### Frontend (React)

| File | Purpose |
|------|---------|
| `frontend/index.html` | Page shell; loads the React app. |
| `frontend/src/main.jsx` | React **mount** point (`#root`). |
| `frontend/src/App.jsx` | Layout: Monaco editor, stdin, output, **Run** button, **`fetch`** to API. |
| `frontend/src/App.css` / `index.css` | Styling. |
| `frontend/vite.config.js` | Builds into `../src/main/resources/static` and **proxies `/api`** during dev. |

### Build

| File | Purpose |
|------|---------|
| `pom.xml` | Maven: Spring Boot, Spring Boot plugin, **frontend-maven-plugin** (Node + `npm install` + `npm run build` before packaging). |
| `src/main/resources/application.properties` | e.g. `server.port=8080`. |

---

## 6. Why Maven builds the frontend too

For a **single deployable JAR**, the production website must live **inside** the JAR. The workflow is:

1. **Vite** builds React → outputs HTML/JS/CSS under `src/main/resources/static/`.
2. **Maven** packages the JAR including those static files.
3. Spring Boot **serves** them at `/` (and loads `index.html` for the app).

So `mvn package` does **both** Java compilation and (in `prepare-package`) the frontend build. You do **not** commit hand-edited files under `static/`; they are **generated**.

---

## 7. Common issues

| Symptom | Things to check |
|--------|-------------------|
| `python3: command not found` | Install Python 3 and ensure `python3` is on **PATH** for the same user that runs `java -jar`. |
| Port 8080 already in use | Change `server.port` in `application.properties` or stop the other program using 8080. |
| Blank page after build | Run `mvn package` so `static/` is populated; open the **root** URL of the app. |
| `npm` errors during `mvn package` | Run from project root; network must allow downloading Node/npm packages. First build can take a few minutes. |

---

## 8. Suggested learning path

1. Run the JAR and use the UI until it feels familiar.
2. Read **`ExecutionController.java`** and **`PythonExecutionService.java`**—they are the core behavior.
3. Read **`App.jsx`** and follow the **`fetch`** call.
4. Learn **Spring Boot** basics: controllers, dependency injection, `@RestController`.
5. Learn **React** basics: components, `useState`, `useCallback`, `fetch`.
6. Experiment: add a small field to the request (e.g. label) end-to-end and display it in the UI.

---

## 9. Official links (for deeper study)

- [Spring Boot documentation](https://docs.spring.io/spring-boot/documentation.html)  
- [React documentation](https://react.dev/)  
- [Vite documentation](https://vitejs.dev/)  
- [Maven Getting Started](https://maven.apache.org/guides/getting-started/index.html)  

Pyrite is intentionally small so you can read **every file** and understand the full path from click to `python3` process.
