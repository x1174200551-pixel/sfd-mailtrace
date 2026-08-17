import { useCallback, useEffect, useMemo, useState } from 'react'
import type { FormEvent } from 'react'
import { authApi } from '../api/auth'
import { REMEMBER_KEY, TOKEN_KEY, USER_KEY } from '../constants/storage'
import { ApiError } from '../shared/api/error-handler'
import type { CurrentUser, LoginResponse } from '../types/auth'

function readStoredSession() {
  const rememberText = localStorage.getItem(REMEMBER_KEY)
  const remember = rememberText == null ? true : rememberText === 'true'
  const token = localStorage.getItem(TOKEN_KEY) || sessionStorage.getItem(TOKEN_KEY) || ''
  const userText = localStorage.getItem(USER_KEY) || sessionStorage.getItem(USER_KEY)
  let user: CurrentUser | null = null

  if (userText) {
    try {
      user = JSON.parse(userText) as CurrentUser
    } catch {
      user = null
    }
  }

  return { remember, token, user }
}

function storeSession(payload: LoginResponse, remember: boolean) {
  const persistentStore = remember ? localStorage : sessionStorage
  const volatileStore = remember ? sessionStorage : localStorage

  volatileStore.removeItem(TOKEN_KEY)
  volatileStore.removeItem(USER_KEY)
  persistentStore.setItem(TOKEN_KEY, payload.token)
  persistentStore.setItem(USER_KEY, JSON.stringify(payload.user))
  localStorage.setItem(REMEMBER_KEY, String(remember))
}

function clearSession() {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(USER_KEY)
  localStorage.removeItem(REMEMBER_KEY)
  sessionStorage.removeItem(TOKEN_KEY)
  sessionStorage.removeItem(USER_KEY)
}

export function useAuthSession() {
  const initialSession = useMemo(readStoredSession, [])
  const [account, setAccount] = useState('')
  const [password, setPassword] = useState('')
  const [rememberMe, setRememberMe] = useState<boolean>(initialSession.remember)
  const [showPassword, setShowPassword] = useState(false)
  const [token, setToken] = useState(initialSession.token)
  const [user, setUser] = useState<CurrentUser | null>(initialSession.user)
  const [formError, setFormError] = useState('')
  const [accountError, setAccountError] = useState('')
  const [passwordError, setPasswordError] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [checkingSession, setCheckingSession] = useState(Boolean(initialSession.token))

  const handleAuthExpired = useCallback((error: unknown) => {
    if (!(error instanceof ApiError) || (error.status !== 401 && error.code !== 40102)) {
      return false
    }

    clearSession()
    setToken('')
    setUser(null)
    setAccount('')
    setPassword('')
    setFormError(error.message || '登录状态已失效，请重新登录')
    return true
  }, [])

  useEffect(() => {
    if (!token) {
      setCheckingSession(false)
      return
    }

    let active = true
    authApi.me()
      .then((currentUser) => {
        if (!active) return
        setUser(currentUser)
      })
      .catch(() => {
        if (!active) return
        clearSession()
        setToken('')
        setUser(null)
      })
      .finally(() => {
        if (active) setCheckingSession(false)
      })

    return () => {
      active = false
    }
  }, [token])

  const validateForm = useCallback(() => {
    const normalizedAccount = account.trim()
    const normalizedPassword = password.trim()
    const nextAccountError = normalizedAccount ? '' : '请输入账号或邮箱'
    const nextPasswordError = normalizedPassword ? '' : '请输入密码'

    setAccountError(nextAccountError)
    setPasswordError(nextPasswordError)

    if (nextAccountError || nextPasswordError) {
      setFormError('请输入账号和密码后再登录')
      return false
    }

    return true
  }, [account, password])

  const changeAccount = useCallback((value: string) => {
    setAccount(value)
    setAccountError('')
    setFormError('')
  }, [])

  const changePassword = useCallback((value: string) => {
    setPassword(value)
    setPasswordError('')
    setFormError('')
  }, [])

  const handleSubmit = useCallback(async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setFormError('')

    if (!validateForm()) return

    setSubmitting(true)
    try {
      const payload = await authApi.login(account.trim(), password.trim(), rememberMe)
      storeSession(payload, rememberMe)
      setToken(payload.token)
      setUser(payload.user)
      setPassword('')
    } catch (error) {
      setFormError(error instanceof Error ? error.message : '登录失败，请稍后重试')
    } finally {
      setSubmitting(false)
    }
  }, [account, password, rememberMe, validateForm])

  const handleLogout = useCallback(async () => {
    try {
      if (token) {
        await authApi.logout()
      }
    } finally {
      clearSession()
      setToken('')
      setUser(null)
      setAccount('')
      setPassword('')
      setFormError('')
    }
  }, [token])

  return {
    account,
    accountError,
    changeAccount,
    changePassword,
    checkingSession,
    formError,
    handleAuthExpired,
    handleLogout,
    handleSubmit,
    password,
    passwordError,
    rememberMe,
    setRememberMe,
    setShowPassword,
    showPassword,
    submitting,
    token,
    user,
  }
}
