export type CurrentUser = {
  id: number
  account: string
  displayName: string
  email: string
  roleCode: string
  roles?: string[]
  permissions?: string[]
  dataScopes?: Record<string, string[]>
}

export type LoginResponse = {
  token: string
  tokenType: string
  expiresIn: number
  user: CurrentUser
}
