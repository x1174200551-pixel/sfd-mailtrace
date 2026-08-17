export type BasicResult<T> = {
  code: number
  message: string
  data: T
}

export type PageResponse<T> = {
  records: T[]
  total: number
  page: number
  size: number
  pages: number
}
