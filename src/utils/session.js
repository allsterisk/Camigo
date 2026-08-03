const SESSION_KEY = 'camigo.session'

export function readSession() {
  try {
    const value = JSON.parse(localStorage.getItem(SESSION_KEY) || 'null')
    return value && typeof value === 'object' ? value : null
  } catch {
    localStorage.removeItem(SESSION_KEY)
    return null
  }
}

export function getAccessToken() {
  return readSession()?.accessToken || ''
}

export function saveSession(loginData) {
  const session = {
    tokenType: loginData.tokenType || 'Bearer',
    accessToken: loginData.accessToken,
    user: loginData.user,
  }
  localStorage.setItem(SESSION_KEY, JSON.stringify(session))
  return session
}

export function updateSessionUser(user) {
  const current = readSession()
  if (!current) return null
  const session = { ...current, user }
  localStorage.setItem(SESSION_KEY, JSON.stringify(session))
  return session
}

export function clearSession() {
  localStorage.removeItem(SESSION_KEY)
}
