export function createTraceId() {
  if (globalThis.crypto?.randomUUID) {
    return globalThis.crypto.randomUUID()
  }
  return `trace-${Date.now()}-${Math.random().toString(16).slice(2)}`
}
