import type { CurrentUser, LoginResponse } from '../types/auth'
import { requestApi } from '../shared/api/request'

export const authApi = {
  login(account: string, password: string, rememberMe: boolean) {
    return requestApi<LoginResponse>('/api/v1/auth/login', {
      method: 'POST',
      skipAuth: true,
      body: JSON.stringify({ account, password, rememberMe }),
    })
  },

  me() {
    return requestApi<CurrentUser>('/api/v1/auth/me')
  },

  logout() {
    return requestApi<void>('/api/v1/auth/logout', {
      method: 'POST',
    })
  },
}
