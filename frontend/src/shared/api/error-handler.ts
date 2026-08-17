export class ApiError extends Error {
  status: number
  code?: number
  traceId?: string

  constructor(message: string, status: number, code?: number, traceId?: string) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.code = code
    this.traceId = traceId
  }
}
