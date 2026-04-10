import { useCallback, useState } from 'react'
import Editor from '@monaco-editor/react'
import './App.css'

/** One line per input() call; adds final newline so input() reads a full line. */
function normalizeStdin(raw) {
  if (raw === '') return ''
  return raw.endsWith('\n') ? raw : raw + '\n'
}

const DEFAULT_CODE = `#!/usr/bin/env python3
# Pyrite — edit and run Python on the server

def greet(name):
    return "Hello, " + name + "!"

if __name__ == "__main__":
    print(greet("Pyrite"))
`

export default function App() {
  const [code, setCode] = useState(DEFAULT_CODE)
  const [stdin, setStdin] = useState('')
  const [timeoutSeconds, setTimeoutSeconds] = useState(30)
  const [stdout, setStdout] = useState('')
  const [stderr, setStderr] = useState('')
  const [exitCode, setExitCode] = useState(null)
  const [timedOut, setTimedOut] = useState(false)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)

  const run = useCallback(async () => {
    setLoading(true)
    setError(null)
    setStdout('')
    setStderr('')
    setExitCode(null)
    setTimedOut(false)
    const stdinPayload = normalizeStdin(stdin)
    try {
      const res = await fetch('/api/execute', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          code,
          stdin: stdinPayload,
          timeoutSeconds,
        }),
      })
      const text = await res.text()
      if (!res.ok) {
        let msg = text || res.statusText
        try {
          const j = JSON.parse(text)
          if (j.error) msg = j.error
        } catch {
          /* use raw */
        }
        setError(msg)
        return
      }
      const data = JSON.parse(text)
      const errText = data.stderr ?? ''
      setStdout(data.stdout ?? '')
      setStderr(errText)
      setExitCode(typeof data.exitCode === 'number' ? data.exitCode : null)
      setTimedOut(!!data.timedOut)
      if (stdin === '' && errText.includes('EOFError')) {
        setError(
          'input() tried to read stdin, but the stdin box was empty. Put each answer on its own line before Run, or press Enter once in that box for an empty line.',
        )
      }
    } catch (e) {
      setError(e.message || 'Request failed')
    } finally {
      setLoading(false)
    }
  }, [code, stdin, timeoutSeconds])

  return (
    <div className="app">
      <header className="header">
        <div className="brand">
          <h1>Pyrite</h1>
          <span>Python IDE · server-side execution</span>
        </div>
        <div className="toolbar">
          <label className="timeout-field">
            Timeout (s)
            <input
              type="number"
              min={1}
              max={300}
              value={timeoutSeconds}
              onChange={(e) => setTimeoutSeconds(Number(e.target.value) || 1)}
            />
          </label>
          <button type="button" className="run-btn" onClick={run} disabled={loading}>
            {loading ? 'Running…' : 'Run'}
          </button>
        </div>
      </header>

      {error && <div className="error-banner">{error}</div>}

      <main className="main">
        <section className="panel">
          <div className="panel-header">Editor</div>
          <div className="editor-wrap">
            <Editor
              height="100%"
              defaultLanguage="python"
              theme="vs-dark"
              value={code}
              onChange={(v) => setCode(v ?? '')}
              options={{
                minimap: { enabled: true },
                fontSize: 14,
                fontFamily: "'IBM Plex Mono', ui-monospace, monospace",
                scrollBeyondLastLine: false,
                automaticLayout: true,
                tabSize: 4,
              }}
            />
          </div>
        </section>

        <section className="panel output-panel">
          <div className="panel-header">Output</div>
          <pre className="output-body">
            {timedOut && (
              <div className="output-body meta">
                Process exceeded the timeout and was terminated.
              </div>
            )}
            {exitCode !== null && !timedOut && (
              <div className="output-body meta">Exit code: {exitCode}</div>
            )}
            {!stdout && !stderr && !timedOut && exitCode === null && (
              <span className="output-empty">Run your code to see output here.</span>
            )}
            {stdout}
            {stderr && (
              <>
                {stdout ? '\n' : null}
                <span className="stderr">{stderr}</span>
              </>
            )}
          </pre>
        </section>

        <section className="panel stdin-panel">
          <div className="panel-header">Standard input (stdin)</div>
          <p className="stdin-hint">
            Pyrite is not an interactive terminal: all stdin is sent before your program runs. Use one line per{' '}
            <code>input()</code> call. For “press Enter” with nothing typed, click in this box and press Enter once.
          </p>
          <textarea
            value={stdin}
            onChange={(e) => setStdin(e.target.value)}
            placeholder={'For input(): type answers here before Run (one line per input). Empty reply → press Enter here once.'}
            spellCheck={false}
          />
        </section>
      </main>
    </div>
  )
}
