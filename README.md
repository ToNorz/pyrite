# Pyrite

Pyrite is a small **full-stack web app**: you write Python in the browser, and a **Java (Spring Boot) server** runs your code with the real `python3` installed on that machine and sends the output back.

This README is the **quick reference**.

- New to the **whole stack** (browser + server + Python)? Read **[docs/LEARNING_GUIDE.md](docs/LEARNING_GUIDE.md)**.
- Focused on **Java / Spring**? Read the classes in the order listed in **[docs/JAVA_READING_GUIDE.md](docs/JAVA_READING_GUIDE.md)** (each source file has Javadoc for the same audience).

---

## What you need installed

| Tool | Why |
|------|-----|
| **Java 21+** | Runs the Spring Boot server and builds the project with Maven. |
| **Maven** | Builds the project and bundles the web UI into one `.jar` file. |
| **Python 3** (`python3` on your PATH) | Actually executes your scripts. The server only starts the process. |
| **Node.js** (optional) | Only if you want to change the React UI with hot reload (`npm run dev`). Maven can download Node for production builds. |

Check versions:

```bash
java -version
mvn -version
python3 --version
```

---

## Build and run (one JAR)

From the project root:

```bash
mvn package
java -jar target/pyrite-1.0.0-SNAPSHOT.jar
```

Open **http://localhost:8080** in your browser.

- **Stop the server:** press `Ctrl+C` in the terminal.

---

## Development: frontend with live reload

Terminal 1 (backend):

```bash
java -jar target/pyrite-1.0.0-SNAPSHOT.jar
```

Terminal 2 (frontend; after `cd frontend` and `npm install` once):

```bash
cd frontend
npm run dev
```

Open the URL Vite prints (usually **http://localhost:5173**). The dev server **proxies** `/api` to **http://localhost:8080**, so the browser still talks to the real Java backend.

---

## Project layout

| Path | Role |
|------|------|
| `pom.xml` | Maven config: Java dependencies, Spring Boot, and steps to build the React app into `src/main/resources/static`. |
| `src/main/java/com/pyrite/` | Backend Java code (REST API, Python runner). |
| `src/main/resources/application.properties` | Server port and Spring settings. |
| `src/main/resources/static/` | **Generated** by `npm run build`—do not edit by hand; it is the production web UI inside the JAR. |
| `frontend/` | React + Vite + Monaco editor source. |
| `docs/LEARNING_GUIDE.md` | Beginner-oriented explanation of how everything fits together. |

---

## HTTP API

**`POST /api/execute`**

- **Content-Type:** `application/json`

**Body:**

| Field | Type | Required | Notes |
|-------|------|------------|--------|
| `code` | string | yes | Python source (max ~1 MB). |
| `stdin` | string | no | Text piped to the process’s standard input (default empty). |
| `timeoutSeconds` | integer | no | 1–300 (default 30). |

**Success (200):**

```json
{
  "stdout": "printed output\n",
  "stderr": "",
  "exitCode": 0,
  "timedOut": false
}
```

If the process runs longer than `timeoutSeconds`, it is killed; you typically see `timedOut: true` and `exitCode: -1`.

**Validation error (400):**

```json
{ "error": "human-readable message" }
```

---

## `input()` and `EOFError`

Pyrite sends **all stdin in one go** before your script runs—it is **not** a live interactive terminal.

- **`input()`** reads from that stdin. Put **one line per `input()`** in the **Standard input** box before you click **Run**.
- If the box is **empty**, Python hits **end-of-file** immediately and you may see **`EOFError: EOF when reading a line`**.
- To simulate “press Enter” with **no text**, click in the stdin box and press **Enter** once (so stdin is a newline).

---

## Security note

Pyrite runs **arbitrary Python** as the same OS user that runs the Java process. Use only on trusted machines or behind proper isolation (containers, VMs, dedicated sandboxes) if you ever expose it to others.

---

## Where to go next

- **[docs/LEARNING_GUIDE.md](docs/LEARNING_GUIDE.md)** — concepts, glossary, and how a request moves through the system.
- **[docs/JAVA_READING_GUIDE.md](docs/JAVA_READING_GUIDE.md)** — order to read the Java packages, mini glossary, request path.
